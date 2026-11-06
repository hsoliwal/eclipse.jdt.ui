/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.eclipse.test.performance.Dimension;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringPerformanceTestSetup;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RenameMethodWithOverloadPerfTests extends RepeatingRefactoringPerformanceTestCaseCommon {

	@Rule
	public RefactoringPerformanceTestSetup rpts= new RefactoringPerformanceTestSetup();

	@Test
	public void testACold_10_10() throws Exception {
		executeRefactoring(10, 10, false, 10);
	}

	@Test
	public void testB_10_10() throws Exception {
		executeRefactoring(10, 10, true, 10);
	}

	@Test
	public void testC_100_10() throws Exception {
		tagAsSummary("Rename method with overloading", Dimension.ELAPSED_PROCESS);
		executeRefactoring(100, 10, true, 10);
	}

	@Test
	public void testD_1000_10() throws Exception {
		executeRefactoring(1000, 10, true, 10);
	}

	@Override
	protected void doExecuteRefactoring(int numberOfCus, int numberOfRefs, boolean measure) throws Exception {
		ICompilationUnit cunit= generateSources(numberOfCus, numberOfRefs);
		IMethod method= cunit.findPrimaryType().getMethod("setString", new String[] {"QString;"});
		RenameVirtualMethodProcessor processor= new RenameVirtualMethodProcessor(method);
		processor.setNewElementName("set");
		executeRefactoring(new RenameRefactoring(processor), measure, RefactoringStatus.FATAL);
	}

	private ICompilationUnit generateSources(int numberOfCus, int numberOfRefs) throws Exception { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		IPackageFragment definition= getTestProject().getSourceFolder().createPackageFragment("def", false, null);
		String str= """
			package def;
			public class A {
			    public void set(Object s) {
			    }
			    public void setString(String s) {
			    }
			}
			""";
		ICompilationUnit result= definition.createCompilationUnit("A.java", str, false, null);

		IPackageFragment references= getTestProject().getSourceFolder().createPackageFragment("ref", false, null);
		for(int i= 0; i < numberOfCus; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			createReferenceCu(references, i, numberOfRefs);
		}
		return result;
	}

	private void createReferenceCu(IPackageFragment pack, int index, int numberOfRefs) throws Exception {
		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		buf.append("package " + pack.getElementName() + ";\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("import def.A;\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("public class Ref" + index + " {\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("    public void ref(A a) {\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("        String s= \"Eclipse\";\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		for (int i= 0; i < numberOfRefs; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buf.append("        a.set(s);\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buf.append("        a.setString(s);\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		buf.append("    }\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("}\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	}

	@Override
	protected void assertMeasurements() {
		assertPerformanceInRelativeBand(Dimension.CPU_TIME, -100, +10);
	}

}
