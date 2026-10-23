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
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class GenerateConstructorUsingFieldsValidator implements ISelectionStatusValidator {

	private GenerateConstructorUsingFieldsSelectionDialog fDialog;

	private final int fEntries;

	private List<String> fSignatures;

	private ITypeBinding fType= null;

	public GenerateConstructorUsingFieldsValidator(GenerateConstructorUsingFieldsSelectionDialog dialog, ITypeBinding type, int entries) {
		fEntries= entries;
		fDialog= dialog;
		fType= type;
		fSignatures= getExistingConstructorSignatures();
	}

	public GenerateConstructorUsingFieldsValidator(int entries) {
		fEntries= entries;
		fType= null;
	}

	private int countSelectedFields(Object[] selection) {
		int count= 0;
		for (Object s : selection) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (s instanceof IVariableBinding) {
				count++;
			}
		}
		return count;
	}

	private void createSignature(final IMethodBinding constructor, StringBuilder buffer, Object[] selection) {
		for (ITypeBinding type : constructor.getParameterTypes()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buffer.append(type.getName()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		if (selection != null) {
			for (Object s : selection) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				if (s instanceof IVariableBinding) {
					buffer.append(((IVariableBinding) s).getType().getErasure().getName()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
			}
		}
	}

	private List<String> getExistingConstructorSignatures() {
		List<String> existing= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		for (IMethodBinding method : fType.getDeclaredMethods()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (method.isConstructor()) {
				StringBuilder buffer= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
				createSignature(method, buffer, null);
				existing.add(buffer.toString());
			}
		}
		return existing;
	}

	@Override
	public IStatus validate(Object[] selection) {
		StringBuilder buffer= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		final IMethodBinding constructor= fDialog.getSuperConstructorChoice();
		createSignature(constructor, buffer, selection);
		if (fSignatures.contains(buffer.toString()))
			return new StatusInfo(IStatus.WARNING, ActionMessages.GenerateConstructorUsingFieldsAction_error_duplicate_constructor);
		return new StatusInfo(IStatus.INFO, Messages.format(ActionMessages.GenerateConstructorUsingFieldsAction_fields_selected, new Object[] { String.valueOf(countSelectedFields(selection)), String.valueOf(fEntries)}));
	}
}
