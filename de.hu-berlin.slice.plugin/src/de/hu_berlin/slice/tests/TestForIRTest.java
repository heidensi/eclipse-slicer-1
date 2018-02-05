package de.hu_berlin.slice.tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class TestForIRTest {
	TestForIR firstTest;
	
	@Before
	public void before() {
		firstTest = new TestForIR();
	}
	
	@Test
	public void test() {
		assertTrue(firstTest.test("TestingSomeStuff", 4, "TestingSomeStuff.testing()V"));
	}

}