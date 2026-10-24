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

import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;


public abstract class AbstractMoveStaticMemberPrefTest extends RepeatingRefactoringPerformanceTestCaseCommon {

	@Override
	protected void doExecuteRefactoring(int numberOfCus, int numberOfRefs, boolean measure) throws Exception {
		ICompilationUnit cunit= generateSources(numberOfCus, numberOfRefs);
		IType type= cunit.findPrimaryType();
		IMember member= type.getField("VALUE");
		IMember[] elements= new IMember[] {member};
		MoveStaticMembersProcessor processor= (RefactoringAvailabilityTester.isMoveStaticMembersAvailable(elements) ? new MoveStaticMembersProcessor(elements, JavaPreferencesSettings.getCodeGenerationSettings(cunit.getJavaProject())) : null);
		IPackageFragment destPack= fTestProject.getSourceFolder().createPackageFragment("destination", false, null);
		String str= """
			package destination;
			public class Dest {
			}
			""";
		ICompilationUnit destination= destPack.createCompilationUnit("Dest.java", str, false, null);

		processor.setDestinationTypeFullyQualifiedName(destination.findPrimaryType().getFullyQualifiedName());
		executeRefactoring(new MoveRefactoring(processor), measure);
	}

	private ICompilationUnit generateSources(int numberOfCus, int numberOfRefs) throws Exception {
		IPackageFragment source= fTestProject.getSourceFolder().createPackageFragment("source", false, null);
		String str= """
			package source;
			public class A {
			    public static final int VALUE= 10;
			}
			""";
		ICompilationUnit result= source.createCompilationUnit("A.java", str, false, null);

		IPackageFragment references= fTestProject.getSourceFolder().createPackageFragment("ref", false, null);
		for(int i= 0; i < numberOfCus; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			createReferenceCu(references, i, numberOfRefs);
		}
		return result;
	}

	private static void createReferenceCu(IPackageFragment pack, int index, int numberOfRefs) throws Exception {
		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		buf.append("package " + pack.getElementName() + ";\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("import source.A;\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("public class Ref" + index + " {\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		for (int i= 0; i < numberOfRefs - 1; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buf.append("    int field" + i +"= A.VALUE;\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		buf.append("}\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	}
}
