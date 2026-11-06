/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

package org.eclipse.jdt.astview.views;

import java.util.Objects;

import org.eclipse.swt.graphics.Image;


public class GeneralAttribute extends ASTAttribute {

	private final Object fParent;
	private final String fLabel;
	private final Object[] fChildren;

	public GeneralAttribute(Object parent, String name, Object value) {
		fParent= parent;
		fLabel= name + ": " + String.valueOf(value); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		fChildren= EMPTY;
	}

	public GeneralAttribute(Object parent, String label) {
		fParent= parent;
		fLabel= label;
		fChildren= EMPTY;
	}

	public GeneralAttribute(Object parent, String name, Object[] children) {
		fParent= parent;
		if (children == null) {
			fLabel= name + ": null"; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			fChildren= EMPTY;
		} else if (children.length == 0) {
			fLabel= name + " (0)"; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			fChildren= EMPTY;
		} else {
			fChildren= createChildren(children);
			fLabel= name + " (" + String.valueOf(fChildren.length) + ')'; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		}
	}

	private Object[] createChildren(Object[] children) {
		ASTAttribute[] res= new ASTAttribute[children.length];
		for (int i= 0; i < res.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			Object child= children[i];
			String name= String.valueOf(i);
			res[i]= Binding.createValueAttribute(this, name, child);
		}
		return res;
	}

	@Override
	public Object getParent() {
		return fParent;
	}

	@Override
	public Object[] getChildren() {
		return fChildren;
	}

	@Override
	public String getLabel() {
		return fLabel;
	}

	@Override
	public Image getImage() {
		return null;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}

		GeneralAttribute other= (GeneralAttribute) obj;
		if (!Objects.equals(fParent, other.fParent)) {
			return false;
		}

		if (!Objects.equals(fLabel, other.fLabel)) {
			return false;
		}

		return true;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ (fLabel != null ? fLabel.hashCode() : 0);
	}
}
