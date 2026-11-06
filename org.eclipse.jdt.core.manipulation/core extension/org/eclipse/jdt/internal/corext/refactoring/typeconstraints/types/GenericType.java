/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ITypeBinding;


public final class GenericType extends HierarchyType {

	private TypeVariable[] fTypeParameters;

	protected GenericType(TypeEnvironment environment) {
		super(environment);
	}

	@Override
	protected void initialize(ITypeBinding binding, IType javaElementType) {
		Assert.isTrue(binding.isGenericType());
		super.initialize(binding, javaElementType);
		TypeEnvironment environment= getEnvironment();
		ITypeBinding[] typeParameters= binding.getTypeParameters();
		fTypeParameters= new TypeVariable[typeParameters.length];
		for (int i= 0; i < typeParameters.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			fTypeParameters[i]= (TypeVariable) environment.create(typeParameters[i]);
		}
	}

	@Override
	public int getKind() {
		return GENERIC_TYPE;
	}

	public TypeVariable[] getTypeParameters() {
		return fTypeParameters.clone();
	}

	@Override
	public boolean doEquals(TType type) {
		return getJavaElementType().equals(((GenericType)type).getJavaElementType());
	}

	@Override
	public int hashCode() {
		return getJavaElementType().hashCode();
	}

	@Override
	protected boolean doCanAssignTo(TType type) {
		return false;
	}

	@Override
	protected boolean isTypeEquivalentTo(TType other) {
		int otherElementType= other.getKind();
		if (otherElementType == RAW_TYPE || otherElementType == PARAMETERIZED_TYPE)
			return getErasure().isTypeEquivalentTo(other.getErasure());
		return super.isTypeEquivalentTo(other);
	}

	@Override
	public String getName() {
		return getJavaElementType().getElementName();
	}

	@Override
	protected String getPlainPrettySignature() {
		StringBuilder result= new StringBuilder(getJavaElementType().getFullyQualifiedName('.')); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		result.append("<"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		result.append(fTypeParameters[0].getPrettySignature()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		for (int i= 1; i < fTypeParameters.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			result.append(", "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			result.append(fTypeParameters[i].getPrettySignature()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		result.append(">"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		return result.toString();
	}
}
