package de.hu_berlin.slice.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;

public class TestLineOutput {

    TestForIR firstTest;

    // Put the .java files to be tested into the sourceDirectory
    static File sourceDirectory = new File(Paths.get("../de.hu-berlin.slice.plugin/dat/testfiles").toAbsolutePath().toString());
    static File classDirectory = new File(Paths.get("../de.hu-berlin.slice.plugin/dat/testfiles/classes").toAbsolutePath().toString());
    static String jarDirectory = Paths.get("../de.hu-berlin.slice.plugin/dat/testfiles/classes/classes.jar").toAbsolutePath().toString();


    @BeforeClass
    public static void before() throws IOException, InterruptedException {
	Runtime.getRuntime().exec("mkdir classes ", null, sourceDirectory);
	Runtime.getRuntime().exec("javac -d classes *.java", null, sourceDirectory).waitFor();
	Runtime.getRuntime().exec("jar -cfv classes.jar *.class", null, classDirectory).waitFor();
	}


    @AfterClass public static void after() throws IOException, InterruptedException {
	Arrays.stream(new File("../de.hu-berlin.slice.plugin/dat/testfiles/classes/").listFiles()).forEach(File::delete);
  }

    @Test
    public void testThinBackwardSlice() throws Exception {

	Map<String, List<Integer>> actual = doSlice("HelloWorld", "demo1", 28, "thinBackward");
	Map<String, List<Integer>> expected = new HashMap<>();
        List<Integer> list = new ArrayList<Integer>();
        list.addAll(Arrays.asList(27,28));	//the expected lines being part of the slice
        expected.put("HelloWorld", list);	//class name and list of the expected lines
        assertTrue(Maps.difference(expected, actual).areEqual());
    }


    @Test
    public void testBackwardSlice() throws Exception {

	Map<String, List<Integer>> actual = doSlice("HelloWorld", "main", 6, "backward");
	Map<String, List<Integer>> expected = new HashMap<>();
        List<Integer> list = new ArrayList<Integer>();
        list.addAll(Arrays.asList(4,5,6));
        expected.put("HelloWorld", list);
        assertTrue(Maps.difference(expected, actual).areEqual());
    }

    @Test
    public void testFullBackwardSlice() throws Exception {

	Map<String, List<Integer>> actual = doSlice("HelloWorld", "demo1", 28, "fullBackward");
	Map<String, List<Integer>> expected = new HashMap<>();
        List<Integer> list = new ArrayList<Integer>();
        list.addAll(Arrays.asList(9,27,28));
        expected.put("HelloWorld", list);
        assertTrue(Maps.difference(expected, actual).areEqual());
    }

    @Test
    public void testForwardSlice() throws Exception {

	Map<String, List<Integer>> actual = doSlice("HelloWorld", "main", 6, "forwardSlice");
	Map<String, List<Integer>> expected = new HashMap<>();
        List<Integer> list = new ArrayList<Integer>();
        list.addAll(Arrays.asList(6,10,13,15));
        expected.put("HelloWorld", list);
        assertTrue(Maps.difference(expected, actual).areEqual());
    }



    /**
     * Calls the method which slices the statement and then sorts the line numbers.
     * The slice output line numbers will be in ascending order which is important for the test success/failure.
     * @param className Class name the statement belongs to
     * @param methodName Method name the statement belongs to
     * @param lineNumber Line number of the statement
     * @param sliceType Type of slice to be done. Pick one of backward/forward/thinBackward/fullBackward
     * @return
     * @throws ClassHierarchyException
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws InvalidClassFileException
     * @throws CancelException
     */
    private Map<String, List<Integer>> doSlice(String className, String methodName,
	    int lineNumber, String sliceType) throws ClassHierarchyException, IllegalArgumentException, IOException, InvalidClassFileException, CancelException{

	ContextSetup cs = new ContextSetup();
	Map<String, List<Integer>> actual = (cs.doSlicing(className, methodName, lineNumber, sliceType));
	if (actual == null)
	    return Collections.<String, List<Integer>>emptyMap();
	for (List<Integer> list : actual.values()) {
	    list.sort(Comparator.nullsLast(Comparator.naturalOrder()));
	    }
	return actual;
    }



}
