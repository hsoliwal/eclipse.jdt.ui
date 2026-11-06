/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
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
package org.eclipse.core.indexsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * A <code>SearchEngine</code> searches for java elements following a search pattern.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 */
public class SearchEngine {

	static class MyIndex implements IIndex {

		private final HashMap fMap= new HashMap(); //FLLDCIM: MARK — M01 M3_HASH_LANES NEW

		@Override
		public void addRef(String word, String path) {
			System.err.println("Index.add: " + path + " " + word); //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			HashMap words= (HashMap) fMap.get(path);
			if (words == null) {
				words= new HashMap(); //FLLDCIM: MARK — M01 M3_HASH_LANES NEW
				fMap.put(path, words);
			}
			words.put(word, word);
		}

		@Override
		public void remove(String path) {
			System.err.println("Index.remove: " + path); //$NON-NLS-1$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			fMap.remove(path);
		}

		@Override
		public void queryPrefix(HashSet results, String w) {
			Iterator iter= fMap.keySet().iterator();
			while (iter.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				String path= (String) iter.next();
				HashMap words= (HashMap) fMap.get(path);
				if (words.containsKey(w))
					results.add(path);
			}
		}
	}

	/* Waiting policies */
	/**
	 * The search operation throws an <code>org.eclipse.core.runtime.OperationCanceledException</code>
	 * if the underlying indexer has not finished indexing the workspace.
	 */
	public static int CANCEL_IF_NOT_READY_TO_SEARCH = 0;
	/**
	 * The search operation waits for the underlying indexer to finish indexing
	 * the workspace before starting the search.
	 */
	public static int WAIT_UNTIL_READY_TO_SEARCH = 1;


	private static SearchEngine fgSearchEngine;

	private final HashMap fIndexes= new HashMap(); //FLLDCIM: MARK — M01 M3_HASH_LANES NEW

	private SearchEngine() {
	}

	public static SearchEngine getSearchEngine() {
		if (fgSearchEngine == null)
			fgSearchEngine= new SearchEngine();
		return fgSearchEngine;
	}

	/**
	 * Trigger removal of a resource to an index
	 * Note: the actual operation is performed in background
	 */
	public void remove(String resourceName, IPath indexedContainer) {
		IIndex index= getIndex(indexedContainer, false);
		if (index != null)
			index.remove(resourceName);
	}

	public void add(IPath indexedContainer, IIndexer indexer) {
		IIndex index= getIndex(indexedContainer, true);
		try {
			indexer.index(index);
		} catch (IOException e) {
		}
	}

	/**
	 * Returns the files that correspond to the paths that have been collected.
	 * TODO_SEARCH
	 */
	private IFile[] getFiles(HashSet pc, IWorkspace workspace) {
		IFile[] result= new IFile[pc.size()];
		int i = 0;
		for (Iterator iter= pc.iterator(); iter.hasNext();) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String resourcePath= (String)iter.next();
			IPath path= new Path(resourcePath);
			result[i++]= workspace.getRoot().getFile(path);
		}
		return result;
	}

	/**
	 * Perform the given query against the index and return results via the resultCollector.
	 */
	public void search(IIndexQuery search, ISearchResultCollector resultCollector,
						IProgressMonitor progressMonitor, int waitingPolicy) {

		HashSet pathCollector= new HashSet(); //FLLDCIM: MARK — M03 M3_SET_LANES NEW
		IProgressMonitor pm= SubMonitor.convert(progressMonitor, 5);
		execute(search, pathCollector, pm);

		/* TODO_SEARCH */
		for (IFile file : getFiles(pathCollector, ResourcesPlugin.getWorkspace())) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			search.locateMatches(file, resultCollector);
		}
	}

	public IIndex getIndex(IPath indexPath, boolean create) {
		IIndex ix= (IIndex) fIndexes.get(indexPath);
		if (create && ix == null) {
			ix= new MyIndex();
			fIndexes.put(indexPath, ix);
		}
		return ix;
	}

	private boolean execute(IIndexQuery search, HashSet pathCollector, IProgressMonitor progressMonitor) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD

		if (progressMonitor != null && progressMonitor.isCanceled())
			throw new OperationCanceledException();
		boolean isComplete= true;
		IIndex[] searchIndexes= getIndexes(search);
		try {
			int max= searchIndexes.length;
			if (progressMonitor != null) {
				progressMonitor.beginTask("", max); //$NON-NLS-1$
			}
			for (int i = 0; i < max; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				isComplete &= search(search, searchIndexes[i], pathCollector, progressMonitor);
				if (progressMonitor != null) {
					if (progressMonitor.isCanceled()) {
						throw new OperationCanceledException();
					} else {
						progressMonitor.worked(1);
					}
				}
			}
			return isComplete;
		} finally {
			if (progressMonitor != null) {
				progressMonitor.done();
			}
		}
	}

	private IIndex[] getIndexes(IIndexQuery search) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		ArrayList requiredIndexKeys= new ArrayList(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		search.computePathsKeyingIndexFiles(requiredIndexKeys);
		IPath[] fIndexKeys= new IPath[requiredIndexKeys.size()]; // cache of the keys for looking index up
		requiredIndexKeys.toArray(fIndexKeys);

		// acquire the in-memory indexes on the fly
		int length = fIndexKeys.length;
		IIndex[] indexes = new IIndex[length];
		int count = 0;
		for (int i = 0; i < length; i++){ //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			// may trigger some index recreation work
			IIndex index = getIndex(fIndexKeys[i], false);
			if (index != null) indexes[count++] = index; // only consider indexes which are ready yet
		}
		if (count != length) {
			System.arraycopy(indexes, 0, indexes= new IIndex[count], 0, count);
		}
		return indexes;
	}

	private boolean search(IIndexQuery search, IIndex index, HashSet pathCollector, IProgressMonitor progressMonitor) {

		if (progressMonitor != null && progressMonitor.isCanceled())
			throw new OperationCanceledException();

		if (index == null)
			return true;
		try {
			search.findIndexMatches(index, pathCollector, progressMonitor);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
