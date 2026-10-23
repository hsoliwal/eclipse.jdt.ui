/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Matt Chapman, mpchapman@gmail.com - 89977 Make JDT .java agnostic
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import java.text.Collator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.core.util.ISourceAttribute;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.util.JavaConventionsUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ClassPathDetector implements IResourceProxyVisitor { //FLLDCIM: MARK — T07  IMPLEMENTS

	private HashMap<IPath, List<IPath>> fSourceFolders;
	private List<IResource> fClassFiles;
	private HashSet<IPath> fJARFiles;

	private IProject fProject;

	private IPath fResultOutputFolder;
	private IClasspathEntry[] fResultClasspath;

	private IProgressMonitor fMonitor;

	private static class CPSorter implements Comparator<IClasspathEntry> {
		private Collator fCollator= Collator.getInstance();
		@Override
		public int compare(IClasspathEntry e1, IClasspathEntry e2) {
			return fCollator.compare(e1.getPath().toString(), e2.getPath().toString());
		}
	}


	public ClassPathDetector(IProject project, IProgressMonitor monitor) throws CoreException {
		fSourceFolders= new HashMap<>(); //FLLDCIM: MARK — M01 M3_HASH_LANES NEW
		fJARFiles= new HashSet<>(10); //FLLDCIM: MARK — M03 M3_SET_LANES NEW
		fClassFiles= new ArrayList<>(100); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		fProject= project;

		fResultClasspath= null;
		fResultOutputFolder= null;

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		detectClasspath(monitor);
	}


	private boolean isNested(IPath path, Iterator<IPath> iter) {
		while (iter.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			IPath other= iter.next();
			if (other.isPrefixOf(path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method detectClasspath.
	 *
	 * @param monitor The progress monitor (not null)
	 * @throws CoreException in case of any failure
	 */
	private void detectClasspath(IProgressMonitor monitor) throws CoreException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		try {
			monitor.beginTask(NewWizardMessages.ClassPathDetector_operation_description, 4);

			fMonitor= monitor;
			fProject.accept(this, IResource.NONE);
			monitor.worked(1);

			ArrayList<IClasspathEntry> cpEntries= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

			detectSourceFolders(cpEntries);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			monitor.worked(1);


			IPath outputLocation= detectOutputFolder();
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			monitor.worked(1);

			detectLibraries(cpEntries, outputLocation);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			monitor.worked(1);

			if (cpEntries.isEmpty() && fClassFiles.isEmpty()) {
				return;
			}
			IClasspathEntry[] jreEntries= PreferenceConstants.getDefaultJRELibrary();
			cpEntries.addAll(0, Arrays.asList(jreEntries));

			IClasspathEntry[] entries= cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
			if (!JavaConventions.validateClasspath(JavaCore.create(fProject), entries, outputLocation).isOK()) {
				return;
			}

			fResultClasspath= entries;
			fResultOutputFolder= outputLocation;
		} finally {
			monitor.done();
		}
	}

	private IPath findInSourceFolders(IPath path) {
		Iterator<IPath> iter= fSourceFolders.keySet().iterator();
		while (iter.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			Object key= iter.next();
			List<IPath> cus= fSourceFolders.get(key);
			if (cus.contains(path)) {
				return (IPath) key;
			}
		}
		return null;
	}

	private IPath detectOutputFolder() throws CoreException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		HashSet<IPath> classFolders= new HashSet<>(); //FLLDCIM: MARK — M03 M3_SET_LANES NEW

		for (IResource iResource : fClassFiles) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			IFile file= (IFile) iResource;
			IClassFileReader reader= null;
			try (InputStream content= file.getContents()) {
				reader= ToolFactory.createDefaultClassFileReader(content, IClassFileReader.CLASSFILE_ATTRIBUTES);
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR,
					Messages.format(NewWizardMessages.ClassPathDetector_error_closing_file, BasicElementLabels.getPathLabel(file.getFullPath(), false)),
					e));
			}
			if (reader == null) {
				continue; // problematic class file
			}
			char[] className= reader.getClassName();
			ISourceAttribute sourceAttribute= reader.getSourceFileAttribute();
			if (className != null && sourceAttribute != null && sourceAttribute.getSourceFileName() != null) {
				IPath packPath= file.getParent().getFullPath();
				int idx= CharOperation.lastIndexOf('/', className) + 1;
				IPath relPath= new Path(new String(className, 0, idx));
				IPath cuPath= relPath.append(new String(sourceAttribute.getSourceFileName()));

				IPath resPath= null;
				if (idx == 0) {
					resPath= packPath;
				} else {
					IPath folderPath= getFolderPath(packPath, relPath);
					if (folderPath != null) {
						resPath= folderPath;
					}
				}
				if (resPath != null) {
					IPath path= findInSourceFolders(cuPath);
					if (path != null) {
						return resPath;
					} else {
						classFolders.add(resPath);
					}
				}
			}
		}
		IPath projPath= fProject.getFullPath();
		if (fSourceFolders.size() == 1 && classFolders.isEmpty() && fSourceFolders.get(projPath) != null) {
			return projPath;
		} else {
			IPath path= projPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME));
			while (classFolders.contains(path)) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				path= new Path(path.toString() + '1'); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			}
			return path;
		}
	}


	private void detectLibraries(ArrayList<IClasspathEntry> cpEntries, IPath outputLocation) {
		ArrayList<IClasspathEntry> res= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		Set<IPath> sourceFolderSet= fSourceFolders.keySet();
		for (IPath path : fJARFiles) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (isNested(path, sourceFolderSet.iterator())) {
				continue;
			}
			if (outputLocation != null && outputLocation.isPrefixOf(path)) {
				continue;
			}
			IClasspathEntry entry= JavaCore.newLibraryEntry(path, null, null);
			res.add(entry);
		}
		Collections.sort(res, new CPSorter()); //FLLDCIM: MARK — M16 SORT_LANE CALL
		cpEntries.addAll(res);
	}


	private void detectSourceFolders(ArrayList<IClasspathEntry> resEntries) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		ArrayList<IClasspathEntry> res= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		Set<IPath> sourceFolderSet= fSourceFolders.keySet();
		for (IPath path : sourceFolderSet) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			ArrayList<IPath> excluded= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			for (IPath other : sourceFolderSet) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				if (!path.equals(other) && path.isPrefixOf(other)) {
					IPath pathToExclude= other.removeFirstSegments(path.segmentCount()).addTrailingSeparator();
					excluded.add(pathToExclude);
				}
			}
			IPath[] excludedPaths= excluded.toArray(new IPath[excluded.size()]);
			IClasspathEntry entry= JavaCore.newSourceEntry(path, excludedPaths);
			res.add(entry);
		}
		Collections.sort(res, new CPSorter()); //FLLDCIM: MARK — M16 SORT_LANE CALL
		resEntries.addAll(res);
	}

	private void visitCompilationUnit(IFile file) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		ICompilationUnit cu= JavaCore.createCompilationUnitFrom(file);
		if (cu != null) {
			ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
			parser.setSource(cu);
			parser.setFocalPosition(0);
			CompilationUnit root= (CompilationUnit)parser.createAST(null);
			PackageDeclaration packDecl= root.getPackage();

			IPath packPath= file.getParent().getFullPath();
			String cuName= file.getName();
			if (packDecl == null) {
				addToMap(fSourceFolders, packPath, new Path(cuName));
			} else {
				IPath relPath= new Path(packDecl.getName().getFullyQualifiedName().replace('.', '/'));
				IPath folderPath= getFolderPath(packPath, relPath);
				if (folderPath != null) {
					addToMap(fSourceFolders, folderPath, relPath.append(cuName));
				}
			}
		}
	}

	private void addToMap(HashMap<IPath, List<IPath>> map, IPath folderPath, IPath relPath) {
		List<IPath> list= map.get(folderPath);
		if (list == null) {
			list= new ArrayList<>(50); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			map.put(folderPath, list);
		}
		list.add(relPath);
	}

	private IPath getFolderPath(IPath packPath, IPath relpath) {
		int remainingSegments= packPath.segmentCount() - relpath.segmentCount();
		if (remainingSegments >= 0) {
			IPath common= packPath.removeFirstSegments(remainingSegments);
			if (common.equals(relpath)) {
				return packPath.uptoSegment(remainingSegments);
			}
		}
		return null;
	}

	private boolean hasExtension(String name, String ext) {
		return name.endsWith(ext) && (ext.length() != name.length());
	}

	private boolean isValidCUName(String name) {
		return !JavaConventionsUtil.validateCompilationUnitName(name, JavaCore.create(fProject)).matches(IStatus.ERROR);
	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public boolean visit(IResourceProxy proxy) {
		if (fMonitor.isCanceled()) {
			throw new OperationCanceledException();
		}

		if (proxy.getType() == IResource.FILE) {
			String name= proxy.getName();
			if (isValidCUName(name)) {
				visitCompilationUnit((IFile) proxy.requestResource());
			} else if (hasExtension(name, ".class")) { //$NON-NLS-1$
				fClassFiles.add(proxy.requestResource());
			} else if (hasExtension(name, ".jar")) { //$NON-NLS-1$
				fJARFiles.add(proxy.requestFullPath());
			}
			return false;
		}
		return true;
	}


	public IPath getOutputLocation() {
		return fResultOutputFolder;
	}

	public IClasspathEntry[] getClasspath() {
		if (fResultClasspath == null)
			return new IClasspathEntry[0];
		return fResultClasspath;
	}
}
