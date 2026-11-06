/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.CopyArguments;
import org.eclipse.ltk.core.refactoring.participants.CopyParticipant;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;

public class TestCopyParticipantSingle extends CopyParticipant {

	static List<TestCopyParticipantSingle> fgInstances= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

	private Object fElement;
	private String fHandle;

	@Override
	public boolean initialize(Object element) {
		fgInstances.add(this);
		fElement= element;
		if (fElement instanceof IJavaElement) {
			fHandle= ((IJavaElement)fElement).getHandleIdentifier();
		} else if (fElement instanceof IResource) {
			fHandle= ((IResource)fElement).getFullPath().toString();
		} else if (element instanceof JavaElementResourceMapping) {
			fHandle= ((JavaElementResourceMapping)element). //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
				getJavaElement().getHandleIdentifier() + "_mapping";
		}
		return true;
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) {
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		return null;
	}

	public static void testNumberOfInstances(int instances) {
		Assert.assertEquals(instances, fgInstances.size());
	}

	public static void testElements(String[] handles) {
		testNumberOfInstances(handles.length);
		List<String> l1= new ArrayList<>(Arrays.asList(handles)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		for (int i= 0; i < l1.size(); i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			Assert.assertTrue(l1.contains(getInstance(i).fHandle));
		}
	}

	public static void testArguments(CopyArguments[] args) {
		testNumberOfInstances(args.length);
		for (int i= 0; i < args.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			CopyArguments expected= args[i];
			CopyArguments actual= getInstance(i).getArguments();
			TestCopyParticipantShared.compareArguments(expected, actual);
		}
	}

	public static void reset() {
		fgInstances= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
	}

	private static TestCopyParticipantSingle getInstance(int i) {
		return fgInstances.get(i);
	}
}
