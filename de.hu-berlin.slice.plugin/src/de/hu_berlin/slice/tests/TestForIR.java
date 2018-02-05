package de.hu_berlin.slice.tests;




import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.StringStuff;




public class TestForIR {	
	//List of all the IRs that are being tested
	private IR ir;
	
	
	//This method expects the source file to be in the de.hu-berlin.tests.dat directory
	//for exemplary use: test("TestingSomeStuff", 4, "TestingSomeStuff.testing()V");
	/** method to test whether a source file is optimized during IR generation by WALA, only been tested with files that include only one method!
	 * 
	 * @param srcFile name of the source file under test
	 * @param numberOfTotalSSAInstructions number of the total instructions in the method, including a return statement!
	 * @param methodSig method signature of the method, something like TestingSomeStuff.testing()V ->  package.classname.method(parameter)methodType
	 * @return returns true iff the numberOfTotalSSAInstructions == the actual number of SSAInstructions in the IR representation that is made by WALA
	 */
	public boolean test(String srcFile, int numberOfTotalSSAInstructions, String methodSig) {
		setUpAntScript(srcFile);
		//compile .class
		compileSrc(srcFile);
		//create.jar
		String [] targets = {"clean", "jar"};
		runAntScript(targets);
		addIR(methodSig);
		//delete .class and .jar
		String [] target = {"cleanAfterwards"};
		runAntScript(target);
		
		File build = new File(Paths.get("../de.hu-berlin.slice.tests/dat/build.xml").toAbsolutePath().toString());
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
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		compiler.run(System.in, System.out, System.err, Paths.get("../de.hu-berlin.slice.tests/dat/"+srcFile+".java").toAbsolutePath().toString());
	}
		
	
	private void addIR(String methodSig) {
		
		String appJar = Paths.get("../de.hu-berlin.slice.tests/dat/TestForIR.jar").toAbsolutePath().toString();
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
		AnalysisOptions options = new AnalysisOptions();
		options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
			      
		// Create an object which caches IRs and related information, reconstructing them lazily on demand.
		IAnalysisCacheView cache = new AnalysisCacheImpl(); //TODO remove options?
		//AnalysisCache cache = new AnalysisCache(null, null, null); 
			
		// Build the IR and cache it.
		ir = (((AnalysisCache) cache).getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions()));
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
			+ "<jar destfile=\"TestForIR.jar\" basedir=\"\"> \n"
			+ "<manifest> \n"
			+ "<attribute name=\"Main-Class\" value=\"";
	private static String end = "\"/> \n"
			+ "</manifest> \n"
			+ "</jar>"
			+ "</target>"
			+ "</project>";
	
	//sets up the ant script at de.hu-berlin.slice.tests/dat/build.xml, including the targets 'clean' to delete TestForIR.jar and 'jar' to generate the new jar 
	private void setUpAntScript(String srcFileName) {
		
		BufferedWriter out = null;
		try  
		{
		    FileWriter fstream = new FileWriter(Paths.get("../de.hu-berlin.slice.tests/dat/build.xml").toAbsolutePath().toString(), false); 
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
		String fullPath = Paths.get("../de.hu-berlin.slice.tests/dat/build.xml").toAbsolutePath().toString();

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
