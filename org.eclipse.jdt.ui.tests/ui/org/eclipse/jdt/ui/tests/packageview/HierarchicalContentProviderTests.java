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

package org.eclipse.jdt.ui.tests.packageview;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;

public class HierarchicalContentProviderTests {

	private static class MyTestSetup implements BeforeEachCallback, AfterEachCallback {

		public static IJavaProject fJProject;
		public static IPackageFragmentRoot fJAR;
		public static IPackageFragmentRoot fClassfolder;

		public static List<String> fExpectedInJAR, fExpectedInCF;

		@Override
		public void beforeEach(ExtensionContext context) throws Exception {
			fJProject= JavaProjectHelper.createJavaProject("Testing", "bin");
			JavaProjectHelper.addRTJar(fJProject);

			File jreArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.RT_STUBS_15);

			fJAR= JavaProjectHelper.addLibrary(fJProject, new Path(jreArchive.getPath()));
			fClassfolder= JavaProjectHelper.addClassFolderWithImport(fJProject, "jre", null, null, jreArchive);

			// access JAR to avoid measuring JAR initialization
			fExpectedInJAR= getExpected(fJAR);
			fExpectedInCF= getExpected(fClassfolder);
		}

		private static List<String> getExpected(IPackageFragmentRoot root) throws JavaModelException {
			ArrayList<String> res= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			for (IJavaElement p : root.getChildren()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				for (IJavaElement file : ((IPackageFragment) p).getChildren()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					testAndAdd(file, res);
				}
			}
			return res;
		}

		@Override
		public void afterEach(ExtensionContext context) {
			try {
				JavaProjectHelper.delete(fJProject);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			fExpectedInJAR= null;
			fExpectedInCF= null;
		}
	}


	@RegisterExtension
	public MyTestSetup mytestsetup=new MyTestSetup();

	private static void testAndAdd(Object curr, List<String> res) {
		if (curr instanceof ICompilationUnit || curr instanceof IClassFile) {
			IJavaElement par= (IJavaElement) curr;
			res.add(par.getParent().getElementName() + '/' + par.getElementName()); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		}
	}

	@Test
	public void testJARHierarchical() {
		List<String> res= collectChildren(MyTestSetup.fJAR, false, false);
		assertEquals(MyTestSetup.fExpectedInJAR, res);
	}

	@Test
	public void testJARFoldedHierarchical() {
		List<String> res= collectChildren(MyTestSetup.fJAR, true, false);
		assertEquals(MyTestSetup.fExpectedInJAR, res);
	}

	@Test
	public void testClassFolderHierarchical() {
		List<String> res= collectChildren(MyTestSetup.fClassfolder, false, false);
		assertEquals(MyTestSetup.fExpectedInCF, res);
	}

	@Test
	public void testClassFolderFoldedHierarchical() {
		List<String> res= collectChildren(MyTestSetup.fClassfolder, true, false);
		assertEquals(MyTestSetup.fExpectedInCF, res);
	}




	private void assertEquals(List<String> expected, List<String> current) {
		Assertions.assertEquals(getString(expected), getString(current));
	}


	private String getString(List<String> list) {
		Collections.sort(list, Collator.getInstance()); //FLLDCIM: MARK — M16 SORT_LANE CALL
		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		for (String element : list) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buf.append(element).append('\n'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		return buf.toString();
	}


	private List<String> collectChildren(Object elem, boolean fold, boolean flatLayout) {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER, fold);

		ArrayList<String> result= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

		PackageExplorerContentProvider provider= new PackageExplorerContentProvider(false);
		provider.setIsFlatLayout(flatLayout);

		collectChildren(provider, elem, result);
		return result;
	}


	private void collectChildren(PackageExplorerContentProvider provider, Object elem, List<String> result) {
		for (Object curr : provider.getChildren(elem)) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			testAndAdd(curr, result);
			if (curr instanceof IPackageFragment) {
				collectChildren(provider, curr, result);
			}
		}
	}
}
