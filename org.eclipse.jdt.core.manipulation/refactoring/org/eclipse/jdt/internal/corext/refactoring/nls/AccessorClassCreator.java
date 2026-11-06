/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.manipulation.CodeGeneration;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.MembersOrderPreferenceCacheCommon;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManagerCore;

public class AccessorClassCreator {

	private final ICompilationUnit fCu;
	private final String fAccessorClassName;
	private final IPath fAccessorPath;
	private final IPath fResourceBundlePath;
	private final IPackageFragment fAccessorPackage;
	private final boolean fIsEclipseNLS;
	private final NLSSubstitution[] fNLSSubstitutions;
	private final String fSubstitutionPattern;

	private AccessorClassCreator(ICompilationUnit cu, String accessorClassname, IPath accessorPath, IPackageFragment accessorPackage, IPath resourceBundlePath, boolean isEclipseNLS, NLSSubstitution[] nlsSubstitutions, String substitutionPattern) {
		fCu= cu;
		fAccessorClassName= accessorClassname;
		fAccessorPath= accessorPath;
		fAccessorPackage= accessorPackage;
		fResourceBundlePath= resourceBundlePath;
		fIsEclipseNLS= isEclipseNLS;
		fNLSSubstitutions= nlsSubstitutions;
		fSubstitutionPattern= substitutionPattern;
	}

	public static Change create(ICompilationUnit cu, String accessorClassname, IPath accessorPath, IPackageFragment accessorPackage, IPath resourceBundlePath, boolean isEclipseNLS, NLSSubstitution[] nlsSubstitutions, String substitutionPattern, IProgressMonitor pm) throws CoreException {
		AccessorClassCreator accessorClass= new AccessorClassCreator(cu, accessorClassname, accessorPath, accessorPackage, resourceBundlePath, isEclipseNLS, nlsSubstitutions, substitutionPattern);

		return new CreateTextFileChange(accessorPath, accessorClass.createAccessorCUSource(pm), null, "java"); //$NON-NLS-1$
	}

	private String createAccessorCUSource(IProgressMonitor pm) throws CoreException {
		IProject project= getFileHandle(fAccessorPath).getProject();
		String lineDelimiter= StubUtility.getLineDelimiterPreference(project);
		return CodeFormatterUtil.format(CodeFormatter.K_COMPILATION_UNIT, getUnformattedSource(pm), 0, lineDelimiter, FormatterProfileManagerCore.getProjectSettings(fCu.getJavaProject()));
	}

	private static IFile getFileHandle(IPath filePath) {
		if (filePath == null)
			return null;
		return ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
	}

	private String getUnformattedSource(IProgressMonitor pm) throws CoreException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		ICompilationUnit newCu= null;
		try {
			newCu= fAccessorPackage.getCompilationUnit(fAccessorPath.lastSegment()).getWorkingCopy(null);

			String typeComment= null, fileComment= null;
			final IJavaProject project= newCu.getJavaProject();
			final String lineDelim= StubUtility.getLineDelimiterUsed(project);
			if (StubUtility.doAddComments(project)) {
				typeComment= CodeGeneration.getTypeComment(newCu, fAccessorClassName, lineDelim);
				fileComment= CodeGeneration.getFileComment(newCu, lineDelim);
			}
			String classContent= createClass(lineDelim);
			String cuContent= CodeGeneration.getCompilationUnitContent(newCu, fileComment, typeComment, classContent, lineDelim);
			if (cuContent == null) {
				StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
				if (fileComment != null) {
					buf.append(fileComment).append(lineDelim); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
				if (!fAccessorPackage.isDefaultPackage()) {
					buf.append("package ").append(fAccessorPackage.getElementName()).append(';'); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
				buf.append(lineDelim).append(lineDelim); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				if (typeComment != null) {
					buf.append(typeComment).append(lineDelim); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
				buf.append(classContent); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				cuContent= buf.toString();
			}

			newCu.getBuffer().setContents(cuContent);
			addImportsToAccessorCu(newCu, pm);
			return newCu.getSource();
		} finally {
			if (newCu != null) {
				newCu.discardWorkingCopy();
			}
		}
	}

	private void addImportsToAccessorCu(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		ImportRewrite is= StubUtility.createImportRewrite(newCu, true);
		if (fIsEclipseNLS) {
			is.addImport("org.eclipse.osgi.util.NLS"); //$NON-NLS-1$
		} else {
			is.addImport("java.util.MissingResourceException"); //$NON-NLS-1$
			is.addImport("java.util.ResourceBundle"); //$NON-NLS-1$
		}
		TextEdit edit= is.rewriteImports(pm);
		JavaModelUtil.applyEdit(newCu, edit, false, null);
	}

	private String createClass(String lineDelim) throws CoreException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (fIsEclipseNLS) {
			MembersOrderPreferenceCacheCommon sortOrder= JavaManipulationPlugin.getDefault().getMembersOrderPreferenceCacheCommon();
			int constructorIdx= sortOrder.getCategoryIndex(MembersOrderPreferenceCacheCommon.CONSTRUCTORS_INDEX);
			int fieldIdx= sortOrder.getCategoryIndex(MembersOrderPreferenceCacheCommon.STATIC_FIELDS_INDEX);
			int initIdx= sortOrder.getCategoryIndex(MembersOrderPreferenceCacheCommon.STATIC_INIT_INDEX);

			String constructor= createConstructor(lineDelim) + lineDelim; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			String initializer= createStaticInitializer(lineDelim) + lineDelim; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			String fields= createStaticFields() + lineDelim; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT

			StringBuilder result= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
			result.append("public class ").append(fAccessorClassName).append(" extends NLS {"); //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append("private static final String ").append(NLSRefactoring.BUNDLE_NAME_FIELD).append(" = ").append(getBundleNameFieldValue()).append("; "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append(NLSElement.createTagText(1)).append(lineDelim); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

			if (constructorIdx < fieldIdx) {
				if (fieldIdx < initIdx) {
					result.append(constructor); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					result.append(fields); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					result.append(initializer); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				} else {
					result.append(constructor); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					result.append(initializer); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					result.append(fields); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
			} else {
				if (constructorIdx < initIdx) {
					result.append(fields); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					result.append(constructor); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					result.append(initializer); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				} else {
					result.append(fields); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					result.append(initializer); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					result.append(constructor); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
			}

			result.append('}').append(lineDelim); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

			return result.toString();
		} else {
			MembersOrderPreferenceCacheCommon sortOrder= JavaManipulationPlugin.getDefault().getMembersOrderPreferenceCacheCommon();
			int constructorIdx= sortOrder.getCategoryIndex(MembersOrderPreferenceCacheCommon.CONSTRUCTORS_INDEX);
			int methodIdx= sortOrder.getCategoryIndex(MembersOrderPreferenceCacheCommon.METHOD_INDEX);

			String constructor= lineDelim	+ createConstructor(lineDelim); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			String method= lineDelim + createGetStringMethod(lineDelim); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT

			StringBuilder result= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
			result.append("public class ").append(fAccessorClassName).append(" {"); //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append("private static final String ").append(NLSRefactoring.BUNDLE_NAME_FIELD); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append(" = ").append(getBundleNameFieldValue()).append("; ").append(NLSElement.createTagText(1)).append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

			result.append(lineDelim).append("private static final ResourceBundle ").append(getResourceBundleConstantName()); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append("= ResourceBundle.getBundle(").append(NLSRefactoring.BUNDLE_NAME_FIELD).append(");").append(lineDelim); //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

			if (constructorIdx < methodIdx) {
				result.append(constructor); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				result.append(method); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			} else {
				result.append(method); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				result.append(constructor); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			}

			result.append(lineDelim).append('}').append(lineDelim); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

			return result.toString();
		}
	}

	private String getBundleNameFieldValue() throws CoreException {
		IPath resourceBundleContainerPath= fResourceBundlePath.removeLastSegments(1);
		IPath accessorContainerPath= fAccessorPath.removeLastSegments(1);
		if (JavaModelUtil.is9OrHigher(fCu.getJavaProject()) && Objects.equals(accessorContainerPath, resourceBundleContainerPath)) {
			return fAccessorClassName + ".class.getPackageName() + \"." + getPropertyFileNameWithoutExtension() + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		}
		return "\"" + getResourceBundleName() + "\""; //$NON-NLS-1$//$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	}

	private String getResourceBundleConstantName() {
		return "RESOURCE_BUNDLE";//$NON-NLS-1$
	}

	private String createStaticFields() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		HashSet<String> added= new HashSet<>(); //FLLDCIM: MARK — M03 M3_SET_LANES NEW
		List<NLSSubstitution> subs= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		for (NLSSubstitution substitution : fNLSSubstitutions) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			int newState= substitution.getState();
			if ((substitution.hasStateChanged() || substitution.isAccessorRename())&& newState == NLSSubstitution.EXTERNALIZED) {
				if (added.add(substitution.getKey()))
					subs.add(substitution);
			}
		}
		Collections.sort(subs, new Comparator<NLSSubstitution>() { //FLLDCIM: MARK — M16 SORT_LANE CALL
			private Collator fCollator= Collator.getInstance();
			@Override
			public int compare(NLSSubstitution s0, NLSSubstitution s1) {
				return fCollator.compare(s0.getKey(), s1.getKey());
			}
		});
		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		for (NLSSubstitution element : subs) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			appendStaticField(buf, element);
		}
		return buf.toString();
	}

	private void appendStaticField(StringBuilder buf, NLSSubstitution substitution) {
		buf.append("public static String "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append(substitution.getKey()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append(';'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
	}

	private String createGetStringMethod(String lineDelim) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		StringBuilder result= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW

		result.append("public static String "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		int i= fSubstitutionPattern.indexOf(NLSRefactoring.KEY);
		if (i != -1) {
			result.append(fSubstitutionPattern.substring(0, i)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append("String key"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append(fSubstitutionPattern.substring(i + NLSRefactoring.KEY.length())); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		} else {
			//fallback
			result.append("getString(String key)"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		result.append('{').append(lineDelim); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

		result.append("try {").append(lineDelim) //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				.append("return ") //$NON-NLS-1$
				.append(getResourceBundleConstantName()).append(".getString(key);").append(lineDelim) //$NON-NLS-1$
				.append("} catch (MissingResourceException e) {").append(lineDelim) //$NON-NLS-1$
				.append("return '!' + key + '!';").append(lineDelim) //$NON-NLS-1$
				.append("}"); //$NON-NLS-1$

		result.append(lineDelim).append('}'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		return result.toString();
	}

	private String createStaticInitializer(String lineDelim) {
		return "static {" //$NON-NLS-1$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		+ lineDelim
		+ "// initialize resource bundle" //$NON-NLS-1$
		+ lineDelim
		+ "NLS.initializeMessages(BUNDLE_NAME, " + fAccessorClassName + ".class);" //$NON-NLS-1$ //$NON-NLS-2$
		+ lineDelim
		+ "}"; //$NON-NLS-1$
	}

	private String createConstructor(String lineDelim) {
		return "private " + fAccessorClassName + "(){" + //$NON-NLS-2$//$NON-NLS-1$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
				lineDelim + '}';
	}

	/* Currently not used.
	 private String createGetStringMethodComment() throws CoreException {
	 if (fCodeGenerationSettings.createComments) {
	 String comment= CodeGeneration.getMethodComment(fCu, fAccessorClassName, "getString", //$NON-NLS-1$
	 new String[]{"key"}, //$NON-NLS-1$
	 new String[0], "QString;", //$NON-NLS-1$
	 null, lineDelim);
	 if (comment == null) {
	 return "";//$NON-NLS-1$
	 }

	 return comment + lineDelim;
	 } else {
	 return "";//$NON-NLS-1$
	 }
	 }
	 */

	private String getPropertyFileName() {
		return fResourceBundlePath.lastSegment();
	}

	private String getPropertyFileNameWithoutExtension() {
		String fileName= getPropertyFileName();
		return fileName.substring(0, fileName.indexOf(NLSRefactoring.PROPERTY_FILE_EXT));
	}

	private String getResourceBundleName() throws CoreException {
		IResource res= ResourcesPlugin.getWorkspace().getRoot().findMember(fResourceBundlePath.removeLastSegments(1));
		if (res != null && res.exists()) {
			IJavaElement el= JavaCore.create(res);
			if (el instanceof IPackageFragment) {
				IPackageFragment p= (IPackageFragment) el;
				return p.getElementName() + '.' + getPropertyFileNameWithoutExtension(); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			} else
				if ((el instanceof IPackageFragmentRoot) || (el instanceof IJavaProject)) {
					return getPropertyFileNameWithoutExtension();
				}
		}
		throw new CoreException(new StatusInfo(IStatus.ERROR, "Resourcebundle not specified")); //$NON-NLS-1$
	}
}
