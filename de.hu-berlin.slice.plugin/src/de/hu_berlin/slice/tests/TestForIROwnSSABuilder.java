package de.hu_berlin.slice.tests;




import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.ShrikeCFG;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.classLoader.ShrikeIRFactory;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.SubtypesEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.StringStuff;

import de.hu_berlin.wala.MyIRFactory;
import de.hu_berlin.wala.MyShrikeIRFactory;




public class TestForIROwnSSABuilder {	
	private static final String CONSOLE_LINE = "-------------------------------------------";
	private static final String TEST_DATA_DIR = "../de.hu-berlin.slice.tests/dat/";
	private static final String ANT_BUILD_FILE = "../de.hu-berlin.slice.tests/dat/build.xml";
	//List of all the IRs that are being tested
	private IR ir;
	private CallGraph callGraph;
	private PointerAnalysis<InstanceKey> pointerAnalysis;
	private ShrikeCFG shrikeCfg;
	
	//This method expects the source file to be in the de.hu-berlin.tests.dat directory
	//for exemplary use: test("TestingSomeStuff", 4, "TestingSomeStuff.testing()V");
	/** method to test whether a source file is optimized during IR generation by WALA, only been tested with files that include only one method!
	 * 
	 * @param srcFile name of the source file under test
	 * @param methodSig method signature of the method, something like TestingSomeStuff.testing()V ->  package.classname.method(parameter)methodType
	 * @return returns the actual number of SSAInstructions in the IR representation that is made by WALA
	 */
	public int test(String srcFile, String methodSig) {
		setUpAntScript(srcFile);
		//compile .class
		compileSrc(srcFile);
		//create .jar
		//Simon: clean target didn't work for me, since the script couldn't delete files
		String [] targets = {//"clean", 
				"jar"};
		runAntScript(targets);
		addIR(methodSig);
		//delete .class and .jar
//		String [] target = {"cleanAfterwards"};
//		runAntScript(target);
		
		File build = new File(Paths.get(ANT_BUILD_FILE).toAbsolutePath().toString());
		build.delete();


		
		int counter  = 0;
//		SSAInstruction[] instructions = ir.getInstructions();
//		for(int i = 0; i < instructions.length; i++) {
//			if(instructions[i] != null) {
//				System.out.println(instructions[i].toString());
//				counter++;
//			}
//		}
		IBytecodeMethod bytecodeMethod = (IBytecodeMethod)ir.getMethod();
		for (Iterator<ISSABasicBlock> iterBlocks = ir.getControlFlowGraph().iterator(); iterBlocks.hasNext();) {
        	for (Iterator<SSAInstruction> iter = iterBlocks.next().iterator(); iter.hasNext();) {

        		++counter;
        		SSAInstruction ssaInstruction = iter.next();
        		
        		if (ssaInstruction.iindex == SSAInstruction.NO_INDEX) {
        			System.out.println(String.format("(Line: -1) %s", 
        					ssaInstruction.toString()));
        		} else {
        			int bcIndex;
					try {
						bcIndex = bytecodeMethod.getBytecodeIndex(ssaInstruction.iindex);
					} catch (InvalidClassFileException e) {
						e.printStackTrace();
						return -1;
					}
                    int lineNumber = bytecodeMethod.getLineNumber(bcIndex);
                    
        			System.out.println(String.format("(Line: %d) %s", 
        					lineNumber, ssaInstruction.toString()));
        		}
        	}
        }
		System.out.flush();
		
		try {
			debugIRSSACfgBC();
			debugCallgraphSSA(callGraph);
			debugCallgraphBC(callGraph);
		} catch (InvalidClassFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return counter;
		
	}
	
	//This method expects the source file to be in the de.hu-berlin.tests.dat directory
	//for exemplary use: test("TestingSomeStuff", 4, "TestingSomeStuff.testing()V");
	/** method to test whether a source file is optimized during IR generation by WALA, only been tested with files that include only one method!
	 * 
	 * @param srcFile name of the source file under test
	 * @param numberOfTotalSSAInstructions number of the total instructions in the method, including a return statement!
	 * @param methodSig method signature of the method, something like TestingSomeStuff.testing()V ->  package.classname.method(parameter)methodType
	 * @return returns true iff the numberOfTotalSSAInstructions == the actual number of SSAInstructions in the IR representation that is made by WALA
	 */
	public boolean test2(String srcFile, int numberOfTotalSSAInstructions, String methodSig) {
		setUpAntScript(srcFile);
		//compile .class
		compileSrc(srcFile);
		//create.jar
		String [] targets = {//"clean", 
				"jar"};
		runAntScript(targets);
		addIR(methodSig);
		//delete .class and .jar
//		String [] target = {"cleanAfterwards"};
//		runAntScript(target);
		
		File build = new File(Paths.get(ANT_BUILD_FILE).toAbsolutePath().toString());
		build.delete();


		
		int counter  = 0;
		SSAInstruction[] instructions = ir.getInstructions();
		for(int i = 0; i < instructions.length; i++) {
			if(instructions[i] != null) {
				System.out.println(instructions[i].toString());
				counter++;
			}
		}
		return(counter == numberOfTotalSSAInstructions);
		
	}
	
	private void compileSrc(String srcFile) {
		//set to own JRE/JDK, if no compiler is found!
		System.setProperty("java.home", "C:\\Program Files\\Java\\jdk1.8.0_162\\jre");
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		Objects.requireNonNull(compiler, "No compiler found!")
		.run(System.in, System.out, System.err, Paths.get(TEST_DATA_DIR + srcFile + ".java").toAbsolutePath().toString());
	}
		
	
	private void addIR(String methodSig) {
		
		String appJar = Paths.get(TEST_DATA_DIR + "TestForIR.jar").toAbsolutePath().toString();
		String exclusionsFilePath = Paths.get("../de.hu-berlin.slice.plugin/dat/Java60RegressionExclusions.txt").toAbsolutePath().toString();
				
		// Build an AnalysisScope which represents the set of classes to analyze.  In particular,
		// we will analyze the contents of the appJar jar file and the Java standard libraries.
		
		File exclusionsFile = null;
		try {
			exclusionsFile = new FileProvider().getFile(exclusionsFilePath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

				
		AnalysisScope scope = null;
		try {
			scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, exclusionsFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
			
		// Build a class hierarchy representing all classes to analyze.  This step will read the class
		// files and organize them into a tree.
		ClassHierarchy cha = null;
		try {
			cha = ClassHierarchyFactory.make(scope);
		} catch (ClassHierarchyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		// Create a name representing the method whose IR we will visualize
		MethodReference mr = StringStuff.makeMethodReference(methodSig);
			
		// Resolve the method name into the IMethod, the canonical representation of the method information.
		IMethod m = cha.resolveMethod(mr);
		if (m == null) {
			Assertions.UNREACHABLE("could not resolve " + mr);
		}
			      
		// Set up options which govern analysis choices.  In particular, we will use all Pi nodes when
		// building the IR.
//		AnalysisOptions options = new AnalysisOptions();
//		options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
//			      
		// Create an object which caches IRs and related information, reconstructing them lazily on demand.
		IAnalysisCacheView cache = new AnalysisCacheImpl(new MyIRFactory()); //TODO remove options?
		//AnalysisCache cache = new AnalysisCache(null, null, null); 
		
		List<Entrypoint> entrypoints = getEntrypoints(cha);
        
        AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
        options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());

        // TODO: not sure what exactly this is for, but it seems to place a LambdaMethodTargetSelector to `options`. we must understand this sooner or later...
        Util.addDefaultSelectors(options, cha);

        CallGraphBuilder<InstanceKey> callGraphBuilder = ZeroXCFABuilder.make(cha, options, cache, null, null, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.CONSTANT_SPECIFIC);
        try {
			callGraph = callGraphBuilder.makeCallGraph(options, null);
		} catch (IllegalArgumentException | CallGraphBuilderCancelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        ShrikeIRFactory shrikeIRFactory = new ShrikeIRFactory();
        shrikeCfg = (ShrikeCFG) shrikeIRFactory.makeCFG((IBytecodeMethod) m);
        
        pointerAnalysis = callGraphBuilder.getPointerAnalysis();
			
		// Build the IR and cache it.
		ir = (((AnalysisCache) cache).getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions()));
	}
	
	/**
     * Iterates through all Application modules to collect the Entrypoints
     * @param classHierarchy
     * @return the Entrypoints of the Class Hierarchy
     */
	private List<Entrypoint> getEntrypoints(ClassHierarchy classHierarchy){
    		IClassLoader classLoader = classHierarchy.getLoader(ClassLoaderReference.Application);
    		List<Entrypoint> entrypoints = new ArrayList<>();

        for (Iterator<IClass> classIterator = classLoader.iterateAllClasses(); classIterator.hasNext(); ) {

            IClass klass = classIterator.next();

            for (IMethod method : klass.getDeclaredMethods()) {
                Entrypoint entrypoint = new SubtypesEntrypoint(method, classHierarchy);
                //System.out.println("Entrypoints " + entrypoint.getMethod().getName());
                entrypoints.add(entrypoint);
            }
        }
        return entrypoints;
    }
    
    /**
     * Prints out all the SSA Instructions from the call graph 
     * @param Callgraph 
     * @throws InvalidClassFileException
     */
    private void debugCallgraphSSA(CallGraph callgraph) throws InvalidClassFileException {
    		for (CGNode cgEntryoint : callgraph.getEntrypointNodes()) {
            IR ir = cgEntryoint.getIR();
            System.err.println(CONSOLE_LINE);
            System.err.println("IR to String:");
            System.err.println(ir.toString());

            System.err.println(CONSOLE_LINE);
            System.err.println(cgEntryoint.getMethod().toString() + " <- current method");

            IBytecodeMethod bytecodeMethod = (IBytecodeMethod)ir.getMethod();
            
            for (Iterator<ISSABasicBlock> iterBlocks = ir.getControlFlowGraph().iterator(); iterBlocks.hasNext();) {
            	for (Iterator<SSAInstruction> iter = iterBlocks.next().iterator(); iter.hasNext();) {

            		SSAInstruction ssaInstruction = iter.next();
            		
            		if (ssaInstruction.iindex == SSAInstruction.NO_INDEX) {
            			System.err.println(String.format("SSA Instruction Index: -1, Instruction: %s", 
            					ssaInstruction.toString()));
            		} else {
            			int bcIndex = bytecodeMethod.getBytecodeIndex(ssaInstruction.iindex);
                        int lineNumber = bytecodeMethod.getLineNumber(bcIndex);
                        
            			System.err.println(String.format("SSA Instruction Index: %d, Line number: %d, Instruction: %s", 
            					ssaInstruction.iindex, lineNumber, ssaInstruction.toString()));
            		}
            		// this demo shows how Wala aggregates multiple source lines into one SSAInstruction
            		if (ssaInstruction instanceof SSABinaryOpInstruction) {
            			SSABinaryOpInstruction test = (SSABinaryOpInstruction)ssaInstruction;
            			System.err.println("- Operator: " + test.getOperator() );
            		}
            	}
            }
            System.err.println(CONSOLE_LINE);
        }
    }
    
    /**
     * Prints out all the BC Instructions from the call graph 
     * @param Callgraph 
     * @throws InvalidClassFileException
     */
    private void debugCallgraphBC(CallGraph callgraph) throws InvalidClassFileException {
    		for (CGNode cgEntryoint : callgraph.getEntrypointNodes()) {
            IR ir = cgEntryoint.getIR();

            System.err.println(CONSOLE_LINE);
            System.err.println(cgEntryoint.getMethod().toString() + " <- current method");

            IBytecodeMethod bytecodeMethod = (IBytecodeMethod)ir.getMethod();
            
            int bcInstructionIndex = -1;
            for (IInstruction instruction : bytecodeMethod.getInstructions()) {
            	
                int bcIndex = bytecodeMethod.getBytecodeIndex(++bcInstructionIndex);

                if (-1 == bcIndex) {
                    System.err.println("Na, damn it!");
                }
                else {
                    System.err.println(String.format("BC Instruction Index: %d, Line number: %d, Instruction: %s", 
                    		bcIndex, bytecodeMethod.getLineNumber(bcIndex), instruction.toString()));
                }
                
            }
            System.err.println(CONSOLE_LINE);
        }
    }
    
    /**
     * Prints out all the BC Instructions from the shrike cfg and full IR
     * @throws InvalidClassFileException
     */
    private void debugIRSSACfgBC() throws InvalidClassFileException {
    	System.err.println(CONSOLE_LINE);
        System.err.println("Full IR to String:");
        System.err.println(ir.toString());
        
    	System.err.println(CONSOLE_LINE);
        System.err.println("Full CFG to String:");
        System.err.println(shrikeCfg.toString());

        System.err.println(CONSOLE_LINE);
        IBytecodeMethod bytecodeMethod = shrikeCfg.getMethod();
		int bcInstructionIndex = -1;
    	for (IInstruction instruction : shrikeCfg.getInstructions()) {

    		int bcIndex = bytecodeMethod.getBytecodeIndex(++bcInstructionIndex);

    		if (-1 == bcIndex) {
    			System.err.println("Na, damn it!");
    		}
    		else {
    			System.err.println(String.format("BC Instruction Index: %d, Line number: %d, Instruction: %s", 
    					bcIndex, bytecodeMethod.getLineNumber(bcIndex), instruction.toString()));
    		}
    	}
    	System.err.println(CONSOLE_LINE);
    }
    
	//String representing what will be written into the build.xml
	private static String begin = "<project>\n "
			+ "<target name=\"clean\"> \n"
			+ "<delete file=\"TestForIR.jar\"/> \n"
			+ "</target> \n"
			+ "<target name=\"cleanAfterwards\"> \n"
			+ "<delete file=\"TestForIR.jar\"/> \n"
			+ "<delete file=\"";
	private static String middle = ".class\"/> \n"
			+ "</target> \n"
			+ "<target name=\"jar\"> \n"
			+ "<jar destfile=\"TestForIR.jar\" basedir=\"\" includes=\"**.class\"> \n"
			+ "<manifest> \n"
			+ "<attribute name=\"Main-Class\" value=\"";
	private static String end = "\"/> \n"
			+ "</manifest> \n"
			+ "</jar>"
			+ "</target>"
			+ "</project>";
	
	//sets up the ant script at de.hu-berlin.slice.tests/dat/build.xml, 
	//including the targets 'clean' to delete TestForIR.jar and 'jar' to generate the new jar 
	private void setUpAntScript(String srcFileName) {
		
		BufferedWriter out = null;
		try  
		{
		    FileWriter fstream = new FileWriter(Paths.get(ANT_BUILD_FILE).toAbsolutePath().toString(), false); 
		    out = new BufferedWriter(fstream);

		    out.write(begin+ srcFileName + middle + srcFileName +end);
		}
		catch (IOException e)
		{
		    System.err.println("Error: " + e.getMessage());
		}
		finally
		{
		    if(out != null) {
		        try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		}
	}
	
	//runs the ant script
	private void runAntScript(String[] target) {
		String fullPath = Paths.get(ANT_BUILD_FILE).toAbsolutePath().toString();

		 File buildFile = new File(fullPath);
	     Project antProject = new Project();
	     antProject.setUserProperty("ant.file", buildFile.getAbsolutePath());
	     antProject.init();
	     ProjectHelper helper = ProjectHelper.getProjectHelper();
	     antProject.addReference("ant.ProjectHelper", helper);
	     helper.parse(antProject, buildFile);
	     for(String t : target) {
	    	 	antProject.executeTarget(t);
	     }
	}
}
