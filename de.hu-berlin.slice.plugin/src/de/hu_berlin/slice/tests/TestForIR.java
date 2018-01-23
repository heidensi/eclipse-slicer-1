package de.hu_berlin.slice.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

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
	ArrayList<IR> IRs = new ArrayList<IR>();

	@Before
	public void before() throws IOException, ClassHierarchyException {
		addIR(1);
		addIR(2);
	}


	@Test
	public void test() {
		//First text case
		int counter  = 0;
		SSAInstruction[] instructions = IRs.get(0).getInstructions();
		for(int i = 0; i < instructions.length; i++) {
			if(instructions[i] != null) {
				System.out.println(instructions[i].toString());
				counter++;
			}
		}
		assertEquals(4, counter);
		//second test case, TODO: set assertEquals()
		counter  = 0;
		instructions = IRs.get(1).getInstructions();
		for(int i = 0; i < instructions.length; i++) {
			if(instructions[i] != null) {
				System.out.println(instructions[i].toString());
				counter++;
			}
		}
		//TODO
		//assertEquals(4, counter);

	}

	public void addIR(int i) {
		if(i < 1 || i > 2)
			return;
		String appJar = Paths.get("../de.hu-berlin.slice.plugin/dat/TestForIR"+i+".jar").toAbsolutePath().toString();
		String exclusionsFilePath = Paths.get("../de.hu-berlin.slice.plugin/dat/Java60RegressionExclusions.txt").toAbsolutePath().toString();

		String methodSig = "TestingSomeStuff.testing()V";

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
		IRs.add(((AnalysisCache) cache).getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions()));
	}

}