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
package org.eclipse.jdt.internal.ui.packageview;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFolder;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledString;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * Provides the labels for the Package Explorer.
 * <p>
 * It provides labels for the packages in hierarchical layout and in all
 * other cases delegates it to its super class.
 * </p>
 * @since 2.1
 */
public class PackageExplorerLabelProvider extends AppearanceAwareLabelProvider { //FLLDCIM: MARK — T06  IMPLEMENTS

	private PackageExplorerContentProvider fContentProvider;
	private Map<ImageDescriptor, Image> fWorkingSetImages;

	private boolean fIsFlatLayout;
	private PackageExplorerProblemsDecorator fProblemDecorator;

	public PackageExplorerLabelProvider(PackageExplorerContentProvider cp) {
		super(DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED | JavaElementLabels.ALL_CATEGORY2, DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS);

		fProblemDecorator= new PackageExplorerProblemsDecorator();
		addLabelDecorator(fProblemDecorator);
		Assert.isNotNull(cp);
		fContentProvider= cp;
		fWorkingSetImages= null;
	}

	@Override
	public StyledString getStyledText(Object element) {
		String text= getSpecificText(element);
		if (text != null) {
			return new StyledString(decorateText(text, element));
		}
		return super.getStyledText(element);
	}

	private String getSpecificText(Object element) {
		if (!fIsFlatLayout && element instanceof IPackageFragment) {
			IPackageFragment fragment = (IPackageFragment) element;
			Object parent= fContentProvider.getHierarchicalPackageParent(fragment);
			if (parent instanceof IPackageFragment) {
				return getNameDelta((IPackageFragment) parent, fragment);
			} else if (parent instanceof IFolder) { // bug 152735
				return getNameDelta((IFolder) parent, fragment);
			}
		} else if (element instanceof IWorkingSet) {
			return ((IWorkingSet) element).getLabel();
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		String text= getSpecificText(element);
		if (text != null) {
			return decorateText(text, element);
		}
		return super.getText(element);
	}

	private String getNameDelta(IPackageFragment parent, IPackageFragment fragment) {
		String prefix= parent.getElementName() + '.'; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		String fullName= fragment.getElementName();
		if (fullName.startsWith(prefix)) {
			return fullName.substring(prefix.length());
		}
		return fullName;
	}

	private String getNameDelta(IFolder parent, IPackageFragment fragment) {
		IPath prefix= parent.getFullPath();
		IPath fullPath= fragment.getPath();
		if (prefix.isPrefixOf(fullPath)) {
			StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
			for (int i= prefix.segmentCount(); i < fullPath.segmentCount(); i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				if (buf.length() > 0)
					buf.append('.'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				buf.append(fullPath.segment(i)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			}
			return buf.toString();
		}
		return fragment.getElementName();
	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public Image getImage(Object element) {
		if (element instanceof IWorkingSet) {
			ImageDescriptor image= ((IWorkingSet)element).getImageDescriptor();
			if (image == null) {
				return null;
			}
			if (fWorkingSetImages == null) {
				fWorkingSetImages= new HashMap<>(); //FLLDCIM: MARK — M01 M3_HASH_LANES NEW
			}

			Image result= fWorkingSetImages.get(image);
			if (result == null) {
				result= image.createImage();
				fWorkingSetImages.put(image, result);
			}
			return decorateImage(result, element);
		}
		return super.getImage(element);
	}

	public void setIsFlatLayout(boolean state) {
		fIsFlatLayout= state;
		fProblemDecorator.setIsFlatLayout(state);
	}

	@Override
	public void dispose() {
		if (fWorkingSetImages != null) {
			for (Image image : fWorkingSetImages.values()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				image.dispose();
			}
		}
		super.dispose();
	}
}
