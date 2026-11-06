/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation.IChooseImportQuery;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java16ProjectTestSetup;

public class ImportOrganizeTest16 extends ImportOrganizeTest {
	@Rule
	public Java16ProjectTestSetup j16p= new Java16ProjectTestSetup(false);

	private IJavaProject fJProject1;

	@Before
	public void before() throws Exception {
		fJProject1= j16p.getProject();

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		JavaCore.setOptions(options);
	}

	@After
	public void after() throws Exception {
		setOrganizeImportSettings(null, 99, 99, fJProject1);
		JavaProjectHelper.clear(fJProject1, j16p.getDefaultClasspath());
	}

	@Test //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public void testRecordParameters() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			record E(List<?> list, Map<?,?> map){
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("E", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
			"java.util.List",
			"java.util.Map"
		});
	}

	@Test //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public void testNestedRecordIssue890() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/890
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			record E(E1 b){
			  class E1 {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("E", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, false, query);
		op.run(null);

		assertImports(cu, new String[] {
		});
	}

	@Test //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public void testOrganizeImportsModuleInfo() throws Exception {
		IEclipsePreferences node= new ProjectScope(fJProject1.getProject()).getNode(JavaManipulation.getPreferenceNodeId());
		node.put(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "org.junit.Assert.*");
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		String str= """
			import foo.bar.MyDriverAction;
			import java.sql.DriverAction;
			import java.sql.SQLException;
			
			module mymodule.nine {
			    requires java.sql;
			    exports foo.bar;
			    provides DriverAction with MyDriverAction;
			}
			""";
		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", str, false, null);

		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		buf.append("import foo.bar.MyDriverAction;\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("import java.sql.DriverAction;\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("module mymodule.nine {\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("    requires java.sql;\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("    exports foo.bar;\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("    provides DriverAction with MyDriverAction;\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append("}\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("E", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);
		assertImports(cu, new String[] {
				"foo.bar.MyDriverAction",
				"java.sql.DriverAction"
		});
	}
}
