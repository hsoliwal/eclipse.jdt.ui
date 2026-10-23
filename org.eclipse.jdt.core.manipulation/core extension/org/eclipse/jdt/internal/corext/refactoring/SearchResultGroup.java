/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;

import org.eclipse.jdt.internal.corext.util.SearchUtils;

public class SearchResultGroup {

	private final IResource fResouce;
	private final List<SearchMatch> fSearchMatches;

	public SearchResultGroup(IResource res, SearchMatch[] matches){
		Assert.isNotNull(matches);
		fResouce= res;
		fSearchMatches= new ArrayList<>(Arrays.asList(matches)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
	}

	public void add(SearchMatch match) {
		Assert.isNotNull(match);
		fSearchMatches.add(match);
	}

	public IResource getResource() {
		return fResouce;
	}

	public SearchMatch[] getSearchResults() {
		return fSearchMatches.toArray(new SearchMatch[fSearchMatches.size()]);
	}

	public static IResource[] getResources(SearchResultGroup[] searchResultGroups){
		Set<IResource> resourceSet= new HashSet<>(searchResultGroups.length); //FLLDCIM: MARK — M03 M3_SET_LANES NEW
		for (SearchResultGroup searchResultGroup : searchResultGroups) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			resourceSet.add(searchResultGroup.getResource());
		}
		return resourceSet.toArray(new IResource[resourceSet.size()]);
	}

	public ICompilationUnit getCompilationUnit(){
		if (getSearchResults() == null || getSearchResults().length == 0)
			return null;
		return SearchUtils.getCompilationUnit(getSearchResults()[0]);
	}

	@Override
	public String toString() {
		StringBuilder buf= new StringBuilder(fResouce.getFullPath().toString()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		buf.append('\n'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		for (SearchMatch match : fSearchMatches) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buf.append("  ").append(match.getOffset()).append(", ").append(match.getLength()); //$NON-NLS-1$//$NON-NLS-2$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buf.append(match.getAccuracy() == SearchMatch.A_ACCURATE ? "; acc" : "; inacc"); //$NON-NLS-1$//$NON-NLS-2$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			if (match.isInsideDocComment())
				buf.append("; inDoc"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			if (match.getElement() instanceof IJavaElement)
				buf.append("; in: ").append(((IJavaElement) match.getElement()).getElementName()); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buf.append('\n'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		return buf.toString();
	}
}
