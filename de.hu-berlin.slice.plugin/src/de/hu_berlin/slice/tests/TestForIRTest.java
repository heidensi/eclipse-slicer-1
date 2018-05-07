package de.hu_berlin.slice.tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class TestForIRTest {
	TestForIR firstTest;
	private TestForIROwnSSABuilder firstTestOwn;
	
	@Before
	public void before() {
		firstTest = new TestForIR();
		firstTestOwn = new TestForIROwnSSABuilder();
	}
	
	@Test
	public void test() {
		assertEquals(3, firstTest.test("TestingSomeStuff", "TestingSomeStuff.testing()V"));
	}
	
	@Test
	public void testOwn() {
		assertEquals(3, firstTestOwn.test("TestingSomeStuffOwn", "TestingSomeStuffOwn.testing()V"));
	}

}