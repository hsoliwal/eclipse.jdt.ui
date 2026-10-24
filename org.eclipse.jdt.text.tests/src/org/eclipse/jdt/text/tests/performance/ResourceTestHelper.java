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
package org.eclipse.jdt.text.tests.performance;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipException;

import org.junit.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.ui.tests.performance.FileTool;


public class ResourceTestHelper {

	public static final int FAIL_IF_EXISTS= 0;

	public static final int OVERWRITE_IF_EXISTS= 1;

	public static final int SKIP_IF_EXISTS= 2;

	private static final int DELETE_MAX_RETRY= 5;

	private static final long DELETE_RETRY_DELAY= 1000;

	public static void replicate(String src, String destPrefix, String destSuffix, int n, int ifExists) throws CoreException {
		for (int i= 0; i < n; i++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			copy(src, destPrefix + i + destSuffix, ifExists); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	}

	public static void copy(String src, String dest) throws CoreException {
		copy(src, dest, FAIL_IF_EXISTS);
	}

	public static void copy(String src, String dest, int ifExists) throws CoreException {
		if (handleExisting(dest, ifExists))
			getFile(src).copy(new Path(dest), true, null);
	}

	private static boolean handleExisting(String dest, int ifExists) throws CoreException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		IFile destFile= getFile(dest);
		switch (ifExists) {
			case FAIL_IF_EXISTS:
				if (destFile.exists())
					throw new IllegalArgumentException("Destination file exists: " + dest); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
				return true;
			case OVERWRITE_IF_EXISTS:
				if (destFile.exists())
					delete(destFile);
				return true;
			case SKIP_IF_EXISTS:
				if (destFile.exists())
					return false;
				return true;
			default:
				throw new IllegalArgumentException();
		}
	}

	private static IFile getFile(String path) {
		return getRoot().getFile(new Path(path));
	}

	public static void delete(String file) throws CoreException {
		delete(getFile(file));
	}

	private static void delete(IFile file) throws CoreException {
		for (int i= 0; ; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			try {
				file.delete(true, null);
				return;
			} catch (CoreException x0) {
				if (i == DELETE_MAX_RETRY) {
					throw x0;
				}
				try {
					Thread.sleep(DELETE_RETRY_DELAY);
				} catch (InterruptedException x1) {
					// should not happen
				}
			}
		}
	}

	public static void delete(String prefix, String suffix, int n) throws CoreException {
		for (int i= 0; i < n; i++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			delete(prefix + i + suffix); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	}

	public static IFile findFile(String pathStr) {
		IFile file= getFile(pathStr);
		Assert.assertTrue(file != null && file.exists());
		return file;
	}

	public static IFile[] findFiles(String prefix, String suffix, int i, int n) {
		List<IFile> files= new ArrayList<>(n); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		for (int j= i; j < i + n; j++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String path= prefix + j + suffix; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			files.add(findFile(path));
		}
		return files.toArray(new IFile[files.size()]);
	}

	public static StringBuffer read(String src) throws IOException, CoreException {
		try (InputStream contents= getFile(src).getContents()){
			return new StringBuffer(new String(contents.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	public static void write(String dest, final String content) throws CoreException {
		ByteArrayInputStream stream= new ByteArrayInputStream(content.getBytes());
		getFile(dest).create(stream, true, null);
	}


	public static void replicate(String src, String destPrefix, String destSuffix, int n, String srcName, String destNamePrefix, int ifExists) throws IOException, CoreException {
		StringBuffer s= read(src);
		List<Integer> positions= identifierPositions(s, srcName); //FLLDCIM: MARK — M10 M3_PRIMITIVE_LANES LOCAL_VARIABLE
		for (int j= 0; j < n; j++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String dest= destPrefix + j + destSuffix; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			if (handleExisting(dest, ifExists)) {
				StringBuffer c= new StringBuffer(s.toString());
				replacePositions(c, srcName.length(), destNamePrefix + j, positions); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
				write(dest, c.toString());
			}
		}
	}

	public static void copy(String src, String dest, String srcName, String destName, int ifExists) throws IOException, CoreException {
		if (handleExisting(dest, ifExists)) {
			StringBuffer buf= read(src);
			List<Integer> positions= identifierPositions(buf, srcName); //FLLDCIM: MARK — M10 M3_PRIMITIVE_LANES LOCAL_VARIABLE
			replacePositions(buf, srcName.length(), destName, positions);
			write(dest, buf.toString());
		}
	}

	private static void replacePositions(StringBuffer c, int origLength, String string, List<Integer> positions) { //FLLDCIM: MARK — M10 M3_PRIMITIVE_LANES PARAMETER
		int offset= 0;
		for (Integer integer : positions) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			int position= integer;
			c.replace(offset + position, offset + position + origLength, string);
			offset += string.length() - origLength;
		}
	}

	private static List<Integer> identifierPositions(StringBuffer buffer, String identifier) {
		List<Integer> positions= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW //FLLDCIM: MARK — M10 M3_PRIMITIVE_LANES LOCAL_VARIABLE
		int i= -1;
		while (true) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			i= buffer.indexOf(identifier, i + 1);
			if (i == -1)
				break;
			if (i > 0 && Character.isJavaIdentifierPart(buffer.charAt(i - 1)))
				continue;
			if (i < buffer.length() - 1 && Character.isJavaIdentifierPart(buffer.charAt(i + identifier.length())))
				continue;
			positions.add(i);
		}
		return positions;
	}

	private static IWorkspaceRoot getRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	public static void incrementalBuild() throws CoreException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
	}

	public static void fullBuild() throws CoreException {
		ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
	}

	public static IProject createExistingProject(String projectName) throws CoreException {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IProject project= workspace.getRoot().getProject(projectName);
		IProjectDescription description= workspace.newProjectDescription(projectName);
		description.setLocation(null);

		project.create(description, null);
		project.open(null);
		return project;
	}

	public static IProject createProjectFromZip(Plugin installationPlugin, String projectZip, String projectName) throws IOException, ZipException, CoreException {
		String workspacePath= ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/"; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		FileTool.unzip(FileTool.getFileInPlugin(installationPlugin, new Path(projectZip)), new File(workspacePath));
		return createExistingProject(projectName);
	}

	public static IProject getProject(String projectName) {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		return workspace.getRoot().getProject(projectName);
	}

	public static boolean projectExists(String projectName) {
		return getProject(projectName).exists();
	}
}
