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

import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.swt.graphics.Image;


public abstract class DynamicBindingProperty extends ExceptionAttribute {

	protected static final String N_A= "N/A"; //$NON-NLS-1$
	private final Binding fParent;

	private Binding fViewerElement;
	private String fLabel= "<unknown>";

	public DynamicBindingProperty(Binding parent) {
		fParent= parent;
	}

	@Override
	public Object getParent() {
		return fParent;
	}

	@Override
	public Object[] getChildren() {
		return EMPTY;
	}

	public void setViewerElement(Binding viewerElement) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (fViewerElement == viewerElement)
			return;

		fViewerElement= viewerElement;
		fException= null;
		IBinding trayBinding= fParent.getBinding();
		StringBuilder buf= new StringBuilder(getName()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		if (viewerElement != null) {
			IBinding viewerBinding= viewerElement.getBinding();
			try {
				String queryResult= executeQuery(viewerBinding, trayBinding);
				buf.append(queryResult); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			} catch (RuntimeException e) {
				fException= e;
				buf.append(e.getClass().getName()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				buf.append(" for \""); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				if (viewerBinding == null)
					buf.append("null"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				else
					buf.append('"').append(viewerBinding.getKey()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				buf.append("\" and "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				buf.append(trayBinding.getKey()).append('"'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			}
		} else {
			buf.append(N_A); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		fLabel= buf.toString();
	}

	/**
	 * Executes this dynamic binding property's query in a protected environment.
	 * A {@link RuntimeException} thrown by this method is made available via
	 * {@link #getException()}.
	 *
	 * @param viewerBinding the binding of the element selected in the AST viewer, or <code>null</code> iff none
	 * @param trayBinding the binding of the element selected in the comparison tray, or <code>null</code> iff none
	 * @return this property's result
	 */
	protected abstract String executeQuery(IBinding viewerBinding, IBinding trayBinding);

	/**
	 * @return a description of the dynamic property
	 */
	protected abstract String getName();

	@Override
	public String getLabel() {
		return fLabel;
	}

	@Override
	public Image getImage() {
		return null;
	}
}
