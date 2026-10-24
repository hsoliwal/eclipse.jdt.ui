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

import org.eclipse.jdt.ui.leaktest.reftracker.ReferencedArrayElement;
import org.eclipse.jdt.ui.leaktest.reftracker.ReferencedFieldElement;
import org.eclipse.jdt.ui.leaktest.reftracker.ReferencedObject;

/**
 * Result element with information to the link to the root element
 */

public class ReferenceInfo {

	private final Object fReference;
	private final BacklinkNode[] fBacklinkNodes;
	private boolean fIsPosibleLeak;

	public ReferenceInfo(ReferencedObject ref) {
		fReference= ref.getValue();
		fBacklinkNodes= getBacklinkNodes(ref);
	}

	public boolean isPosibleLeak() {
		return fIsPosibleLeak;
	}

	public void setPosibleLeak(boolean isPosibleLeak) {
		fIsPosibleLeak= isPosibleLeak;
	}

	private BacklinkNode[] getBacklinkNodes(ReferencedObject curr) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		ArrayList<BacklinkNode> res= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

		while (curr != null) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String str;
			if (curr instanceof ReferencedArrayElement) {
				ReferencedArrayElement ref= (ReferencedArrayElement) curr;
				String name= ref.getReferenceHolder().getValue().getClass().getComponentType().getName();
				str= name + String.valueOf('[') + ref.getIndex() + String.valueOf(']'); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			} else if (curr instanceof ReferencedFieldElement) {
				ReferencedFieldElement ref= (ReferencedFieldElement) curr;
				String name= ref.getField().getDeclaringClass().getName();
				str= name + "#" + ref.getField().getName(); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			} else {
				str= curr.getValue().getClass().getName();
			}
			res.add(new BacklinkNode(str, curr.getValue()));
			curr= curr.getReferenceHolder();
		}
		return res.toArray(new BacklinkNode[res.size()]);
	}

	public Object getReference() {
		return fReference;
	}

	public BacklinkNode[] getBacklinkNodes() {
		return fBacklinkNodes;
	}

	@Override
	public String toString() {
		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		buf.append(getReference().getClass().getName()).append('\n'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		for (BacklinkNode backlinkNode : getBacklinkNodes()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buf.append("  ").append(backlinkNode.getBackLinkString()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			String value= backlinkNode.getValue().toString();
			if (value.length() > 100)
				value= value.substring(0, 99);
			buf.append(" -> ").append(value).append('\n'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		return buf.toString();
	}

	private static class BacklinkNode {

		private final Object fValue;
		private final String fBackStrings;

		public BacklinkNode(String str, Object value) {
			fBackStrings= str;
			fValue= value;
		}

		public String getBackLinkString() {
			return fBackStrings;
		}

		public Object getValue() {
			return fValue;
		}

	}

}


