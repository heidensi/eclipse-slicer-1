package de.hu_berlin.slice.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.SubtypesEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

import de.hu_berlin.slice.plugin.jobs.SlicingContext;
import de.hu_berlin.slice.plugin.jobs.SlicingTask;;

public class ContextSetup {

    static String exclusionpath = Paths.get("../de.hu-berlin.slice.tests/dat/Java60RegressionExclusions.txt")
	    .toAbsolutePath().toString();
    static String jarDirectory = Paths.get("../de.hu-berlin.slice.plugin/dat/testfiles/classes/classes.jar")
	    .toAbsolutePath().toString();

    /**
     * Slices a statement
     * @param className Name of the class the statement belongs to
     * @param methodName Name of the method the statement belongs to
     * @param lineNumber Line number of the statement
     * @param sliceType Type of slice to be done. Pick one of backward/forward/thinBackward/fullBackward
     * @return Returns the slice with the line numbers mapped as a list to the class names.
     * @throws ClassHierarchyException
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws InvalidClassFileException
     * @throws CancelException
     */
    public Map<String, List<Integer>> doSlicing(String className, String methodName, int lineNumber, String sliceType)
	    throws ClassHierarchyException, IllegalArgumentException, IOException, InvalidClassFileException,
	    CancelException {

	SlicingContext context = setupMockContext();
	List<NormalStatement> statements = getStatements(context, className, methodName, lineNumber);
	SlicingTask st = new SlicingTask();

	switch (sliceType) {
	case "backward":
	    st.backwardSlice(statements, context);
	    break;
	case "forward":
	    st.forwardSlice(statements, context);
	    break;
	case "thinBackward":
	    st.thinSlice(statements, context);
	    break;
	case "fullBackward":
	    st.backwardFullSlice(statements, context);
	    break;
	}
	return context.getMap();
    }

    /**
     * Creates the necessary parts for a slice from a jar instead of the eclipse workspace.
     *
     * @return
     * @throws IOException
     * @throws ClassHierarchyException
     * @throws IllegalArgumentException
     * @throws CallGraphBuilderCancelException
     */
    private static SlicingContext setupMockContext()
	    throws IOException, ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException {
	AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarDirectory, new File(exclusionpath));
	ClassHierarchy cha = ClassHierarchyFactory.make(scope);
	List<Entrypoint> entrypoints = getEntrypoints(cha);
	AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
	Util.addDefaultSelectors(options, cha);
	CallGraphBuilder<InstanceKey> callGraphBuilder = ZeroXCFABuilder.make(cha, options, new AnalysisCacheImpl(),
		null, null, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC);
	CallGraph callGraph = callGraphBuilder.makeCallGraph(options, null);
	PointerAnalysis<InstanceKey> pointerAnalysis = callGraphBuilder.getPointerAnalysis();

	SlicingContext mockContext = new SlicingContext(null, null);
	mockContext.setAnalysisScope(scope);
	mockContext.setClassHierarchy(cha);
	mockContext.setAnalysisScope(scope);
	mockContext.setPointerAnalysis(pointerAnalysis);
	mockContext.setCallGraph(callGraph);
	return mockContext;
    }

    private List<NormalStatement> getStatements(SlicingContext context, String className, String methodName,
	    int lineNumber) throws InvalidClassFileException {
	List<CGNode> mainMethods = findMethods(context, className, methodName);
	List<NormalStatement> statements = null;
	for (CGNode mainNode : mainMethods) {
	    statements = SlicingTask.findStatements(mainNode, lineNumber);
	    if (statements.isEmpty()) {
		System.err.println("failed to find statement in the call graph. Please select a different statement");
		continue;
	    }
	}
	return statements;
    }

    private List<CGNode> findMethods(SlicingContext context, String className, String methodName) {
	Atom aclassname = Atom.findOrCreateUnicodeAtom(stringSplit(className)); // class the statement belongs to
	Atom name = Atom.findOrCreateUnicodeAtom(methodName); // method the statement belongs to
	List<CGNode> result = new ArrayList<>();
	for (Iterator<? extends CGNode> it = context.getCallGraph()
		.getSuccNodes(context.getCallGraph().getFakeRootNode()); it.hasNext();) {
	    CGNode n = it.next();
	    if (n.getMethod().getDeclaringClass().getName().getClassName() == aclassname) {
		if (n.getMethod().getName().equals(name)) {
		    result.add(n);
		}
	    }

	}
	if (result.isEmpty()) {
	    Assertions.UNREACHABLE("failed to find method");
	}
	return result;
    }

    public static Map<String, List<Integer>> dumpSlice(Collection<Statement> slice) {
	Map<String, List<Integer>> src = new HashMap<String, List<Integer>>();
	for (Statement s : slice) {
	    if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
		int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
		try {
		    bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
		    String key = s.getNode().getMethod().getDeclaringClass().getName().getClassName().toString();
		    try {

			int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
			if (src.containsKey(key)) {
			    src.get(key).add(src_line_number);
			} else {
			    List<Integer> a = new ArrayList<Integer>();
			    a.add(src_line_number);
			    src.put(key, a);
			}
		    } catch (Exception e) {
		    }
		} catch (Exception e) {
		}
	    }

	}
	return src;
    }

    public static Map<String, List<Integer>> merge(Map<String, List<Integer>> map1, Map<String, List<Integer>> map2) {
	for (Map.Entry<String, List<Integer>> entry : map2.entrySet()) {
	    if (map1.containsKey(entry.getKey())) {
		for (Integer i : map2.get(entry.getKey())) {
		    map1.get(entry.getKey()).add(i);
		}
	    } else {
		map1.put(entry.getKey(), entry.getValue());
	    }
	}
	return map1;
    }

    private static String stringSplit(String s) {
	String[] segs = s.split(Pattern.quote("."));
	return segs[0];
    }

    public static List<Entrypoint> getEntrypoints(ClassHierarchy classHierarchy) {
	IClassLoader classLoader = classHierarchy.getLoader(ClassLoaderReference.Application);
	List<Entrypoint> entrypoints = new ArrayList<>();

	for (Iterator<IClass> classIterator = classLoader.iterateAllClasses(); classIterator.hasNext();) {
	    IClass klass = classIterator.next();
	    for (IMethod method : klass.getDeclaredMethods()) {
		Entrypoint entrypoint = new SubtypesEntrypoint(method, classHierarchy);
		// System.out.println("Entrypoints " + entrypoint.getMethod().getName());
		entrypoints.add(entrypoint);
	    }
	}
	return entrypoints;
    }
}
