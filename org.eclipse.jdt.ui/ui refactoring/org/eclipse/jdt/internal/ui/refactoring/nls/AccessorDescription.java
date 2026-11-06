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
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;


public class AccessorDescription {

	private final static String KEY_ACCESSOR_NAME= "accessorName"; //$NON-NLS-1$
	private final static String KEY_ACCESSOR_PACK= "accessorPackage"; //$NON-NLS-1$
	private final static String KEY_RESOURCE_BUNDLE_NAME= "bundleName"; //$NON-NLS-1$
	private final static String KEY_RESOURCE_BUNDLE_PACK= "bundlePackage"; //$NON-NLS-1$


	private final IPackageFragment fResourceBundlePackage;
	private final String fAccessorClassName;
	private final IPackageFragment fAccessorClassPackage;
	private final String fResourceBundleName;

	public AccessorDescription(String accessorClassName, IPackageFragment accessorClassPackage, String propertyFileName, IPackageFragment propertyFilePackage) {
		super();
		fAccessorClassName= accessorClassName;
		fAccessorClassPackage= accessorClassPackage;
		fResourceBundleName= propertyFileName;
		fResourceBundlePackage= propertyFilePackage;
	}

	public String getLabel() {
		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		buf.append(getAccessorClassPackage().getElementName()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		if (buf.length() > 0) {
			buf.append('.'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		buf.append(BasicElementLabels.getResourceName(getAccessorClassName())); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		buf.append(JavaElementLabels.CONCAT_STRING); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		IPath propertyFilePath= getResourceBundlePackage().getPath().append(getResourceBundleName());
		buf.append(BasicElementLabels.getPathLabel(propertyFilePath, false)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		return buf.toString();
	}

	public void serialize(IDialogSettings settings) {
		settings.put(KEY_ACCESSOR_NAME, getAccessorClassName());
		settings.put(KEY_ACCESSOR_PACK, getAccessorClassPackage().getHandleIdentifier());
		settings.put(KEY_RESOURCE_BUNDLE_NAME, getResourceBundleName());
		settings.put(KEY_RESOURCE_BUNDLE_PACK, getResourceBundlePackage().getHandleIdentifier());
	}

	/**
	 * @return Returns the accessor class name.
	 */
	public String getAccessorClassName() {
		return fAccessorClassName;
	}
	/**
	 * @return Returns the accessor class package.
	 */
	public IPackageFragment getAccessorClassPackage() {
		return fAccessorClassPackage;
	}
	/**
	 * @return Returns the resource bundle name.
	 */
	public String getResourceBundleName() {
		return fResourceBundleName;
	}
	/**
	 * @return Returns the resource bundle package
	 */
	public IPackageFragment getResourceBundlePackage() {
		return fResourceBundlePackage;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass().equals(getClass())) {
			AccessorDescription other= (AccessorDescription) obj;
			return other == this ||
			  (other.fAccessorClassName.equals(fAccessorClassName)
			  && other.fAccessorClassPackage.equals(fAccessorClassPackage)
			  && other.fResourceBundleName.equals(fResourceBundleName)
			  && other.fResourceBundlePackage.equals(fResourceBundlePackage));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fAccessorClassName.hashCode() + fAccessorClassPackage.hashCode() + fResourceBundleName.hashCode() + fResourceBundlePackage.hashCode();
	}

	public static AccessorDescription deserialize(IDialogSettings settings) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		String accessorName= settings.get(KEY_ACCESSOR_NAME);
		if (accessorName == null) {
			return null;
		}

		String accessorPackHandle= settings.get(KEY_ACCESSOR_PACK);
		if (accessorPackHandle == null) {
			return null;
		}
		IJavaElement accessorPack= JavaCore.create(accessorPackHandle);
		if (!(accessorPack instanceof IPackageFragment) || !accessorPack.exists()) {
			return null;
		}

		String bundleName= settings.get(KEY_RESOURCE_BUNDLE_NAME);
		if (bundleName == null) {
			return null;
		}

		String bundlePackHandle= settings.get(KEY_RESOURCE_BUNDLE_PACK);
		if (bundlePackHandle == null) {
			return null;
		}
		IJavaElement bundlePack= JavaCore.create(bundlePackHandle);
		if (!(bundlePack instanceof IPackageFragment) || !bundlePack.exists()) {
			return null;
		}

		return new AccessorDescription(accessorName, (IPackageFragment) accessorPack, bundleName, (IPackageFragment) bundlePack);
	}
}
