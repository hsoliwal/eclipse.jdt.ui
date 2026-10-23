/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageProcessor;

public abstract class AbstractRenamePackagePerfTest extends RepeatingRefactoringPerformanceTestCaseCommon {

	@Override
	protected void doExecuteRefactoring(int numberOfCus, int numberOfRefs, boolean measure) throws Exception {
		IPackageFragment pack= generateSources(numberOfCus, numberOfRefs);
		RenamePackageProcessor processor= new RenamePackageProcessor(pack);
		processor.setNewElementName("pack2");
		executeRefactoring(new RenameRefactoring(processor), measure);
	}

	private IPackageFragment generateSources(int numberOfCus, int numberOfRefs) throws Exception {
		IPackageFragment pack= fTestProject.getSourceFolder().createPackageFragment("pack", false, null);
		for (int i= 0; i < numberOfRefs; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
			buf.append("package pack;\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buf.append("public class A" + i + " {\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buf.append("}\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			pack.createCompilationUnit("A" + i + ".java", buf.toString(), false, null); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		}

		IPackageFragment references= fTestProject.getSourceFolder().createPackageFragment("ref", false, null);
		for(int i= 0; i < numberOfCus; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			createReferenceCu(references, i, numberOfRefs);
		}
		return pack;
	}

	private static void createReferenceCu(IPackageFragment pack, int index, int numberOfRefs) throws Exception {
		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		buf.append("package " + pack.getElementName() + ";\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		for (int i= 0; i < numberOfRefs; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buf.append("import pack.A" + i + ";\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		buf.append("public class Ref" + index + " {\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("}\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	}

}
