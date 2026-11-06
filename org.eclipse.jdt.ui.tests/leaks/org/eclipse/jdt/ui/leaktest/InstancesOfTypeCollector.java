/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

package org.eclipse.jdt.ui.leaktest;

import java.util.ArrayList;

import org.eclipse.jdt.ui.leaktest.reftracker.ReferenceVisitor;
import org.eclipse.jdt.ui.leaktest.reftracker.ReferencedObject;

/**
 * An implementation of a {@link ReferenceVisitor} that collects all instances of a given type.
 * All matches are stored as {@link ReferenceInfo}
 */
public class InstancesOfTypeCollector extends ReferenceVisitor {

	private final ArrayList<ReferenceInfo> fResults;
	private final boolean fIncludeSubtypes;
	private final String fRequestedTypeName;

	public InstancesOfTypeCollector(String requestedTypeName, boolean includeSubtypes) {
		fIncludeSubtypes= includeSubtypes;
		fResults= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		fRequestedTypeName= requestedTypeName;
	}

	public ReferenceInfo[] getResults() {
		return fResults.toArray(new ReferenceInfo[fResults.size()]);
	}

	public String getResultString() {
		int i= 0;
		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		for (ReferenceInfo element : fResults) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buf.append("Element ").append(i++).append('\n'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buf.append(element.toString()).append('\n'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		return buf.toString();
	}

	public int getNumberOfResults() {
		return fResults.size();
	}

	@Override
	public boolean visit(ReferencedObject reference, Class<?> clazz, boolean firstVisit) {
		if (firstVisit) {
			if (isMatchingType(clazz)) {
				fResults.add(new ReferenceInfo(reference));
			}
		}
		return true;
	}

	private boolean isMatchingType(Class<?> clazz) {
		if (clazz.getName().equals(fRequestedTypeName)) {
			return true;
		}
		if (fIncludeSubtypes) {
			Class<?> superclass= clazz.getSuperclass();
			if (superclass != null && isMatchingType(superclass)) {
				return true;
			}
			for (Class<?> intf : clazz.getInterfaces()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				if (isMatchingType(intf)) {
					return true;
				}
			}
		}
		return false;
	}

}
