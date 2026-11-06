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
package org.eclipse.jdt.ui.tests.refactoring.typeconstraints;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class PrettySignatures {

	public static String get(ITypeBinding binding) {
		if (binding.isTypeVariable()) {
			return getTypeVariable(binding);
		}
		return getPlain(binding);
	}

	private static String getPlain(ITypeBinding binding) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (binding.isPrimitive()) {
			return binding.getName();
		} else if (binding.isArray()) {
			return getPlainArrayType(binding);
		} else if (binding.isRawType()) {
			return getPlainRawType(binding);
		} else if (binding.isGenericType()) {
			return getPlainGenericType(binding);
		} else if (binding.isParameterizedType()) {
			return getPlainParameterizedType(binding);
		} else if (binding.isTypeVariable()) {
			return getPlainTypeVariable(binding);
		} else if (binding.isWildcardType()) {
			if (binding.isUpperbound()) {
				return getPlainExtendsWildCardType(binding);
			} else {
				return getPlainSuperWildCardType(binding);
			}
		} else if (binding.isCapture()) {
			return getPlainCaptureType(binding);
		}
		return getPlainStandardType(binding);
	}

	private static String getPlainCaptureType(ITypeBinding binding) {
		StringBuilder result= new StringBuilder("capture-of "); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		result.append(PrettySignatures.getPlain(binding.getWildcard())); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		return result.toString();
	}

	private static String getPlainSuperWildCardType(ITypeBinding binding) {
		StringBuilder result= new StringBuilder("?"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		ITypeBinding bound= binding.getBound();
		if (bound != null) {
			result.append(" super "); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append(PrettySignatures.getPlain(bound)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		return result.toString();
	}

	private static String getPlainExtendsWildCardType(ITypeBinding binding) {
		StringBuilder result= new StringBuilder("?"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		ITypeBinding bound= binding.getBound();
		if (bound != null) {
			result.append(" extends "); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append(PrettySignatures.getPlain(bound)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		return result.toString();
	}

	private static String getPlainParameterizedType(ITypeBinding binding) {
		StringBuilder result= new StringBuilder(getQualifiedName(binding)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		ITypeBinding[] typeArguments= binding.getTypeArguments();
		result.append("<"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		result.append(PrettySignatures.getPlain(typeArguments[0])); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		for (int i= 1; i < typeArguments.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			result.append(", "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append(PrettySignatures.getPlain(typeArguments[i])); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		result.append(">"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		return result.toString();
	}

	private static String getPlainGenericType(ITypeBinding binding) {
		StringBuilder result= new StringBuilder(getQualifiedName(binding)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		ITypeBinding[] typeParameters= binding.getTypeParameters();
		result.append("<"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		result.append(PrettySignatures.get(typeParameters[0])); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		for (int i= 1; i < typeParameters.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			result.append(", "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append(PrettySignatures.get(typeParameters[i])); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		result.append(">"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		return result.toString();
	}

	private static String getTypeVariable(ITypeBinding binding) {
		ITypeBinding[] bounds= binding.getTypeBounds();
		if (bounds.length == 1 && "java.lang.Object".equals(bounds[0].getQualifiedName()))
			return binding.getName();

		StringBuilder result= new StringBuilder(binding.getName()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		if (bounds.length > 0) {
			result.append(" extends "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append(PrettySignatures.getPlain(bounds[0])); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			for (int i= 1; i < bounds.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				result.append(" & "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				result.append(PrettySignatures.getPlain(bounds[i])); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			}
		}
		return result.toString();
	}

	private static String getPlainTypeVariable(ITypeBinding binding) {
		return binding.getName();
	}

	private static String getPlainRawType(ITypeBinding binding) {
		return getQualifiedName(binding);
	}

	private static String getPlainArrayType(ITypeBinding binding) {
		StringBuilder result= new StringBuilder(PrettySignatures.getPlain(binding.getElementType())); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		for (int i= 0; i < binding.getDimensions(); i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			result.append("[]"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		return result.toString();
	}

	private static String getPlainStandardType(ITypeBinding binding) {
		return getQualifiedName(binding);
	}

	private static String getQualifiedName(ITypeBinding binding) {
		if (binding.isLocal())
			return ((IType)binding.getJavaElement()).getFullyQualifiedName('.');
		String result= binding.getQualifiedName();
		if (binding.isParameterizedType()) {
			int index= result.indexOf('<');
			if (index != -1)
				return result.substring(0, index);
		}
		return result;
	}
}
