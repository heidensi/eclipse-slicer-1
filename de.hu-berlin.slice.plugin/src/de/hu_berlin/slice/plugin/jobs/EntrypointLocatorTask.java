package de.hu_berlin.slice.plugin.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.SubtypesEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.thin.ThinSlicer;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

import de.hu_berlin.slice.plugin.context.EditorContextFactory.EditorContext;
import de.hu_berlin.slice.highlighting.Highlighting;

/**
 * Task where the call graph gets build. 
 * @author IShowerNaked
 * Task where the slicing happens
 */
public class EntrypointLocatorTask implements ITask {

    @Override
    public void run(IProgressMonitor monitor, SlicingContext context) throws TaskException {
        monitor.subTask("Locating entrypoint...");

        try {
            EditorContext editorContext = context.editorContext;

            ASTNode          ast             = editorContext.getAST();
            ICompilationUnit compilationUnit = editorContext.getCompilationUnit();
            String           absolutePath    = compilationUnit.getCorrespondingResource().getLocation().toOSString();

            ClassHierarchy classHierarchy = context.classHierarchy;

            // System.err.println(classHierarchy.toString());

            for (IClassLoader cl : classHierarchy.getLoaders()) {
                System.err.println(cl.getClass().getName());
            }

            //Iterate through all Application modules to collect the entrypoints.
            IClassLoader classLoader = classHierarchy.getLoader(ClassLoaderReference.Application);

            List<Entrypoint> entrypoints = new ArrayList<>();

            for (Iterator<IClass> classIterator = classLoader.iterateAllClasses(); classIterator.hasNext(); ) {

                IClass klass = classIterator.next();

                for (IMethod method : klass.getDeclaredMethods()) {

                    Entrypoint entrypoint = new SubtypesEntrypoint(method, classHierarchy);
                    entrypoints.add(entrypoint);
                }
            }
            
            AnalysisOptions options = new AnalysisOptions(context.analysisScope, entrypoints);

            // TODO: not sure what exactly this is for, but it seems to place a LambdaMethodTargetSelector to `options`. we must understand this sooner or later...
            Util.addDefaultSelectors(options, classHierarchy);

            CallGraphBuilder<InstanceKey> callGraphBuilder = ZeroXCFABuilder.make(classHierarchy, options, new AnalysisCacheImpl(), null, null, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC);

            // AstJavaCFABuilder callGraphBuilder = new AstJavaCFABuilder(classHierarchy, options, new AnalysisCacheImpl());

            CallGraph callGraph = callGraphBuilder.makeCallGraph(options, null);
            context.callGraph = callGraph;
            
            PointerAnalysis<InstanceKey> pointerAnalysis = callGraphBuilder.getPointerAnalysis();
            context.pointerAnalysis = pointerAnalysis;
            
            /*
            List<CGNode> mainMethods = findMainMethods(callGraph);
            
            //LISTEN FÜR DIE ZEILEN
            List<Integer> k = new ArrayList<Integer>();
            List<Integer> l = new ArrayList<Integer>();
            
            for (CGNode mainNode : mainMethods) {
            		System.out.println(mainNode.toString()+ "main method");
                Statement statement = findCallTo(mainNode, "println");
                testStatement(mainNode);
                
                if (statement == null) {
                    System.err.println("failed to find call to " + "println" + " in " + mainNode);
                    continue;
                }
                Collection<Statement> slice;

                // context-sensitive traditional slice
                slice = Slicer.computeBackwardSlice(statement, callGraph, pointerAnalysis, DataDependenceOptions.NO_BASE_PTRS, ControlDependenceOptions.NONE);
                 k = dumpSlice(slice);
                 

              // context-sensitive thin slice
                 ThinSlicer ts = new ThinSlicer(callGraph, pointerAnalysis);
                 Collection<Statement> slice1 = ts.computeBackwardThinSlice ( statement );
                 l = dumpSlice(slice1);
                
                
            }
                //TEST
            */
            
            for (CGNode cgEntryoint : callGraph.getEntrypointNodes()) {
                IR ir = cgEntryoint.getIR();
                IBytecodeMethod cgBytecodeEntryoint = (IBytecodeMethod)ir.getMethod();

                System.err.println(cgEntryoint.getMethod().toString() + " test");

                int ssaInstructionIndex = -1;
                for (Iterator<SSAInstruction> iter = ir.iterateAllInstructions(); iter.hasNext();) {

                    SSAInstruction ssaInstruction = iter.next();
                    int bcIndex = cgBytecodeEntryoint.getBytecodeIndex(++ssaInstructionIndex);

                    if (-1 == bcIndex) {
                        System.err.println("Na, damn it!");
                    }
                    else {
                        System.err.println(String.format("Instruction Index: %d, Line number: %d, Instruction: %s", bcIndex, cgBytecodeEntryoint.getLineNumber(bcIndex), ssaInstruction.toString()));
                    }

                    // this demo shows how Wala aggregates multiple source lines into one SSAInstruction
                    if (ssaInstruction instanceof SSABinaryOpInstruction) {
                        SSABinaryOpInstruction test = (SSABinaryOpInstruction)ssaInstruction;
                        System.err.println("- Operator: " + test.getOperator() );
                    }
                    
                }
                
            }
            
            
        }
        catch (Exception e) {
            throw new TaskException(null, e);
        }

        monitor.done();
    }
    

    
}