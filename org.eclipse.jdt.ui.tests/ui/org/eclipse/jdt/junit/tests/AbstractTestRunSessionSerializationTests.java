/*******************************************************************************
 * Copyright (c) 2007, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.junit.model.ITestSuiteElement;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

import org.eclipse.jdt.ui.tests.core.rules.JUnitWorkspaceTestSetup;

public class AbstractTestRunSessionSerializationTests {

	private static final int TIMEOUT= 10 * 1000;


	private static class SerializationResult {
		TestRunSession fTestRunSession;
		String fSerialized;
	}

	private SerializationResult launchTest(IJavaElement elementToLaunch) throws Exception { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		final SerializationResult result= new SerializationResult();

		TestRunListener testRunListener= new TestRunListener() {
			@Override
			public void sessionFinished(ITestRunSession session) {
				assertNotNull(session);
				result.fTestRunSession= (TestRunSession) session;
			}
		};

		JUnitCore.addTestRunListener(testRunListener);
		try {
			new AbstractTestRunListenerTest().launchJUnit(elementToLaunch, (String) null);
			assertTrue(new DisplayHelper(){
				@Override
				protected boolean condition() {
					return result.fTestRunSession != null;
				}
			}.waitForCondition(Display.getCurrent(), TIMEOUT, 100));
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}

		ByteArrayOutputStream out= new ByteArrayOutputStream();
		JUnitModel.exportTestRunSession(result.fTestRunSession, out);

		result.fSerialized= out.toString("UTF-8");
		return result;
	}

	private void runExportImport(IJavaElement test, String expectedXML) throws Exception { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		SerializationResult serializationResult= launchTest(test);
		assertEqualXML(expectedXML, serializationResult.fSerialized);

		IFile resultFile= JUnitWorkspaceTestSetup.getJavaProject().getProject().getFile("testresult.xml");
		try {
			resultFile.create(new ByteArrayInputStream(serializationResult.fSerialized.getBytes()), true, null);
			TestRunSession imported= JUnitModel.importTestRunSession(resultFile.getLocation().toFile());
			// swap out the test run session because it may not have been done earlier
			// due to lingering TestRunnerViewPart$TestSessionListeners
			serializationResult.fTestRunSession.swapOut();
			assertEqualSessions(serializationResult.fTestRunSession, imported);
		} finally {
			if (resultFile.exists())
				try {
					resultFile.delete(true, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
		}
	}

	private void assertEqualXML(String expected, String actual) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		/*
		 * Strips &#13; and &#10;
		 */
		Pattern regex0= Pattern.compile("&#1[03];"); //FLLDCIM: MARK — M12 REGEX_PRECOMPUTE CALL
		/*
		 * Avoid comparing stack traces (which are VM-dependent)
		 */
		Pattern regex= Pattern.compile("(?m)^\\s*at\\s+[/\\w\\.\\:\\;\\$\\(\\)\\[ \\t]+$\r?\n?"); //FLLDCIM: MARK — M12 REGEX_PRECOMPUTE CALL
		/*
		 * Strips lines like " ... 18 more"
		 */
		Pattern regex2= Pattern.compile("(?m)^\\s*\\.{3}\\s+\\d+\\s+more\\s+$\r?\n?"); //FLLDCIM: MARK — M12 REGEX_PRECOMPUTE CALL
		/*
		 * Strips running times
		 */
		Pattern regex3= Pattern.compile("(?<=time=\\\")\\d+\\.\\d+(?=\\\")"); //FLLDCIM: MARK — M12 REGEX_PRECOMPUTE CALL
		/*
		 * CPU timings are optional and VM-dependent. Their persistence is covered by
		 * TestRunSessionHistoryTests, while these legacy golden files verify the rest
		 * of the exported XML independently of the availability and exact CPU values.
		 */
		Pattern regexCpuTimes= Pattern.compile("\\s+(?:cpuTime|userTime)=\\\"\\d+(?:\\.\\d+)?\\\""); //FLLDCIM: MARK — M12 REGEX_PRECOMPUTE CALL
		String replacement= "";
		expected= regexCpuTimes.matcher(regex3.matcher(regex2.matcher(regex.matcher(regex0.matcher(expected).replaceAll(replacement)).replaceAll(replacement)).replaceAll(replacement)).replaceAll(replacement)).replaceAll(replacement); //FLLDCIM: MARK — M12 REGEX_PRECOMPUTE CALL
		actual= regexCpuTimes.matcher(regex3.matcher(regex2.matcher(regex.matcher(regex0.matcher(actual).replaceAll(replacement)).replaceAll(replacement)).replaceAll(replacement)).replaceAll(replacement)).replaceAll(replacement); //FLLDCIM: MARK — M12 REGEX_PRECOMPUTE CALL
		int ibmJava6BugOffset= actual.indexOf("><");
		if (ibmJava6BugOffset > 0) // https://bugs.eclipse.org/bugs/show_bug.cgi?id=197842
			actual= new StringBuffer(actual).insert(ibmJava6BugOffset + 1, " ").toString();

		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=526754
		String regex4= "</actual>\\s*";
		String replacement4= "</actual>";
		String regex5= "\\s*</failure>";
		String replacement5= " </failure>";
		actual= actual.replaceAll(regex4, replacement4).replaceAll(regex5, replacement5); //FLLDCIM: MARK — M12 REGEX_PRECOMPUTE CALL

		/*
		 * Strip all whitespace
		 */
		StringTokenizer expTok= new StringTokenizer(expected);
		StringTokenizer actTok= new StringTokenizer(actual);
		if (expTok.countTokens() != actTok.countTokens())
			assertEquals(expected, actual);
		while (expTok.hasMoreElements()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String e= expTok.nextToken();
			String a= actTok.nextToken();
			if (! e.equals(a)) {
				assertEquals(expected, actual);
				fail(a);
			}
		}
	}

	private void assertEqualSessions(TestRunSession expected, TestRunSession actual) {
		assertEquals(expected.getTestRunName(), actual.getTestRunName());
		assertEquals(expected.getStartedCount(), actual.getStartedCount());
		assertEquals(expected.getTotalCount(), actual.getTotalCount());
		assertEquals(expected.getErrorCount(), actual.getErrorCount());
		assertEquals(expected.getFailureCount(), actual.getFailureCount());
		assertEquals(expected.getIgnoredCount(), actual.getIgnoredCount());

//		assertEquals(expected.getLaunchedProject(), actual.getLaunchedProject()); //TODO

		assertEqualSuite(expected.getTestRoot(), actual.getTestRoot());
	}

	private void assertEqualSuite(ITestSuiteElement expected, ITestSuiteElement actual) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		assertEquals(expected.getProgressState(), actual.getProgressState());
		assertEquals(expected.getTestResult(false), actual.getTestResult(false));
		if (actual instanceof TestSuiteElement) {
			TestSuiteElement act= (TestSuiteElement) actual;
			TestSuiteElement exp= (TestSuiteElement) expected;
			assertEquals(exp.getTestName(), act.getTestName());
			assertEquals(exp.getTrace(), act.getTrace());
			assertEquals(exp.getTestName(), act.getTestName());
			ITestElement[] expChildren= exp.getChildren();
			ITestElement[] actChildren= act.getChildren();
			assertEquals(expChildren.length, actChildren.length);
			for (int i= 0; i < expChildren.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				ITestElement expChild= expChildren[i];
				ITestElement actChild= actChildren[i];
				if (expChild instanceof ITestCaseElement) {
					assertEqualTestCase((ITestCaseElement) expChild, (ITestCaseElement) actChild);
				} else if (expChild instanceof ITestSuiteElement) {
					assertEqualSuite((ITestSuiteElement) expChild, (ITestSuiteElement) actChild);
				} else {
					fail(expChild.getClass().getName());
				}
			}
		}
	}

	private void assertEqualTestCase(ITestCaseElement expected, ITestCaseElement actual) {
		assertEquals(expected.getTestClassName(), actual.getTestClassName());
		assertEquals(expected.getTestMethodName(), actual.getTestMethodName());
		assertEquals(expected.getTestResult(false), actual.getTestResult(false));
		FailureTrace expFailure= expected.getFailureTrace();
		FailureTrace actFailure= actual.getFailureTrace();
		if (expFailure == null) {
			assertNull(actFailure);
		} else if (actFailure == null) {
			assertNull(expFailure);
		} else {
			assertEquals(expFailure.getActual(), actFailure.getActual());
			assertEquals(expFailure.getExpected(), actFailure.getExpected());
			// FailureTrace#getTrace() is VM-dependent; could only compare first line
		}
	}

	protected void runCUTest(String test) throws CoreException, IOException, FileNotFoundException, Exception {
		IPackageFragmentRoot root= JUnitWorkspaceTestSetup.getRoot();
		IPackageFragment pack= root.getPackageFragment("pack");
		ICompilationUnit cu= pack.getCompilationUnit(test + ".java"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		IType aTestCase= cu.findPrimaryType();

		Path expectedPath= new Path(JUnitWorkspaceTestSetup.getProjectPath() + "xml/" + test + ".xml"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		File expectedFile= JavaTestPlugin.getDefault().getFileInPlugin(expectedPath);
		String expected=  Files.readString(expectedFile.toPath()); //FLLDCIM: MARK — M31 M3FILE_IO CALL
		runExportImport(aTestCase, expected);

		runImportAntResult(test);
	}

	private void runImportAntResult(String test) throws CoreException {
		Path testPath= new Path(JUnitWorkspaceTestSetup.getProjectPath() + "ant/result/TEST-pack." + test + ".xml"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		File testFile= JavaTestPlugin.getDefault().getFileInPlugin(testPath);
		JUnitModel.importTestRunSession(testFile); // no contents check for now...
	}

	protected void runMethodTest(String testType, String method) throws Exception {
		IPackageFragmentRoot root= JUnitWorkspaceTestSetup.getRoot();
		IPackageFragment pack= root.getPackageFragment("pack");
		ICompilationUnit cu= pack.getCompilationUnit(testType + ".java"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		IType testCase= cu.findPrimaryType();
		IMethod testMethod= testCase.getMethod(method, new String[0]);

		Path expectedPath= new Path(JUnitWorkspaceTestSetup.getProjectPath() + "xml/" + testType + "_" + method + ".xml"); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		File expectedFile= JavaTestPlugin.getDefault().getFileInPlugin(expectedPath);
		String expected= Files.readString(expectedFile.toPath()); //FLLDCIM: MARK — M31 M3FILE_IO CALL
		runExportImport(testMethod, expected);

		//ant cannot run single test methods
	}

}
