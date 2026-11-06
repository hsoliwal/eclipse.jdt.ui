/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.JavaElementLabels;

public class PatternStrings {

	public static String getSignature(IJavaElement element) {
		if (element == null)
			return null;
		else
			switch (element.getElementType()) {
				case IJavaElement.METHOD:
					return getMethodSignature((IMethod)element);
				case IJavaElement.TYPE:
					return getTypeSignature((IType) element);
				case IJavaElement.FIELD:
					return getFieldSignature((IField) element);
				default:
					return element.getElementName();
			}
	}

	public static String getMethodSignature(IMethod method) {
		StringBuilder buffer= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		buffer.append(JavaElementLabels.getElementLabel( //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			method.getDeclaringType(),
			JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.USE_RESOLVED));
		boolean isConstructor= method.getElementName().equals(method.getDeclaringType().getElementName());
		if (!isConstructor) {
			buffer.append('.'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		buffer.append(getUnqualifiedMethodSignature(method, !isConstructor)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

		return buffer.toString();
	}

	private static String getUnqualifiedMethodSignature(IMethod method, boolean includeName) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		StringBuilder buffer= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		if (includeName) {
			buffer.append(method.getElementName()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		buffer.append('('); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

		String[] types= method.getParameterTypes();
		for (int i= 0; i < types.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (i > 0)
				buffer.append(", "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			String typeSig= Signature.toString(types[i]);
			buffer.append(typeSig); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		buffer.append(')'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

		return buffer.toString();
	}

	public static String getUnqualifiedMethodSignature(IMethod method) {
		return getUnqualifiedMethodSignature(method, true);
	}

	public static String getTypeSignature(IType field) {
		return JavaElementLabels.getElementLabel(field,
			JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.T_TYPE_PARAMETERS | JavaElementLabels.USE_RESOLVED);
	}

	public static String getFieldSignature(IField field) {
		return JavaElementLabels.getElementLabel(field, JavaElementLabels.F_FULLY_QUALIFIED);
	}

	private PatternStrings() {
	}
}
