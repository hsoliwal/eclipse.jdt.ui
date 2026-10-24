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

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.dom.IVariableBinding;

public class GenerateConstructorUsingFieldsContentProvider implements ITreeContentProvider { //FLLDCIM: MARK — M22  IMPLEMENTS

	private static final Object[] EMPTY= new Object[0];

	private List<IVariableBinding> fFields;
	private List<IVariableBinding> fSelected;

	public GenerateConstructorUsingFieldsContentProvider(List<IVariableBinding> fields, List<IVariableBinding> selected) {
		fFields= fields;
		fSelected= selected;
	}

	public boolean canMoveDown(List<?> selectedElements) {
		int nSelected= selectedElements.size();
		for (int index= fFields.size() - 1; index >= 0 && nSelected > 0; index--) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (!selectedElements.contains(fFields.get(index))) {
				return true;
			}
			nSelected--;
		}
		return false;
	}

	public boolean canMoveUp(List<?> selected) {
		int nSelected= selected.size();
		for (int index= 0; index < fFields.size() && nSelected > 0; index++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (!selected.contains(fFields.get(index))) {
				return true;
			}
			nSelected--;
		}
		return false;
	}

	/*
	 * @see IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
	}

	public void down(List<?> checked, CheckboxTreeViewer tree) {
		if (checked.size() > 0) {
			setElements(reverse(moveUp(reverse(fFields), checked)), tree);
			tree.reveal(checked.get(checked.size() - 1));
		}
		tree.setSelection(new StructuredSelection(checked));
	}

	/*
	 * @see ITreeContentProvider#getChildren(Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		return EMPTY;
	}

	/*
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		return fFields.toArray();
	}

	public List<IVariableBinding> getFieldsList() {
		return fFields;
	}

	public Object[] getInitiallySelectedElements() {
		if (fSelected.isEmpty())
			return getElements(null);
		return fSelected.toArray();
	}

	/*
	 * @see ITreeContentProvider#getParent(Object)
	 */
	@Override
	public Object getParent(Object element) {
		return null;
	}

	/*
	 * @see ITreeContentProvider#hasChildren(Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	/*
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	private List<IVariableBinding> moveUp(List<IVariableBinding> elements, List<?> move) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		List<IVariableBinding> result= new ArrayList<>(elements.size()); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		IVariableBinding floating= null;
		for (int index= 0; index < elements.size(); index++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			IVariableBinding current= elements.get(index);
			if (move.contains(current)) {
				result.add(current);
			} else {
				if (floating != null) {
					result.add(floating);
				}
				floating= current;
			}
		}
		if (floating != null) {
			result.add(floating);
		}
		return result;
	}

	private List<IVariableBinding> reverse(List<IVariableBinding> list) {
		List<IVariableBinding> reverse= new ArrayList<>(list.size()); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		for (int index= list.size() - 1; index >= 0; index--) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			reverse.add(list.get(index));
		}
		return reverse;
	}

	public void setElements(List<IVariableBinding> elements, CheckboxTreeViewer tree) {
		fFields= new ArrayList<>(elements); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		if (tree != null)
			tree.refresh(); //FLLDCIM: MARK — M21 FULL_REBUILD_TO_DELTA CALL
	}

	public void up(List<?> checked, CheckboxTreeViewer tree) {
		if (checked.size() > 0) {
			setElements(moveUp(fFields, checked), tree);
			tree.reveal(checked.get(0));
		}
		tree.setSelection(new StructuredSelection(checked));
	}
}
