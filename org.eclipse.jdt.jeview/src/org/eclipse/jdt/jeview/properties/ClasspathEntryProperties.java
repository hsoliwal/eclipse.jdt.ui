/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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

package org.eclipse.jdt.jeview.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.jeview.JEViewPlugin;

public class ClasspathEntryProperties implements IPropertySource {

	private static abstract class Property extends GenericProperty<IClasspathEntry> {
		public Property(String name) {
			super(IClasspathEntry.class, name);
		}
	}

	private static HashMap<String, Property> fgIdToProperty= new HashMap<>(); //FLLDCIM: MARK — M01 M3_HASH_LANES NEW
	private static LinkedHashMap<Class<?>, List<Property>> fgTypeToProperty= new LinkedHashMap<>(); //FLLDCIM: MARK — M04 M3_ORDERED_LANES NEW

	static {
		addProperty(new Property("combineAccessRules") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.combineAccessRules();
			}
		});
		addProperty(new Property("getContentKind") {
			@Override public Object compute(IClasspathEntry entry) {
				return getContentKindString(entry.getContentKind());
			}
		});
		addProperty(new Property("getEntryKind") {
			@Override public Object compute(IClasspathEntry entry) {
				return getEntryKindString(entry.getEntryKind());
			}
		});
		addProperty(new Property("getOutputLocation") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.getOutputLocation();
			}
		});
		addProperty(new Property("getPath") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.getPath();
			}
		});
		addProperty(new Property("getSourceAttachmentPath") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.getSourceAttachmentPath();
			}
		});
		addProperty(new Property("getSourceAttachmentRootPath") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.getSourceAttachmentRootPath();
			}
		});
		addProperty(new Property("isExported") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.isExported();
			}
		});
	}

	private static void addProperty(Property property) {
		fgIdToProperty.put(property.getId(), property);
		List<Property> properties= fgTypeToProperty.get(property.getType());
		if (properties == null) {
			properties= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			fgTypeToProperty.put(property.getType(), properties);
		}
		properties.add(property);
	}

	static String getContentKindString(int kind) {
		String name;
		switch (kind) {
			case IPackageFragmentRoot.K_SOURCE :
				name= "K_SOURCE";
				break;
			case IPackageFragmentRoot.K_BINARY :
				name= "K_BINARY";
				break;
			default :
				name= "UNKNOWN";
				break;
		}
		return kind + " (" + name + ")"; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	}

	static String getEntryKindString(int kind) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		String name;
		switch (kind) {
			case IClasspathEntry.CPE_CONTAINER :
				name= "CPE_CONTAINER";
				break;
			case IClasspathEntry.CPE_LIBRARY :
				name= "CPE_LIBRARY";
				break;
			case IClasspathEntry.CPE_PROJECT :
				name= "CPE_PROJECT";
				break;
			case IClasspathEntry.CPE_SOURCE :
				name= "CPE_SOURCE";
				break;
			case IClasspathEntry.CPE_VARIABLE :
				name= "CPE_VARIABLE";
				break;
			default :
				name= "UNKNOWN";
				break;
		}
		return kind + " (" + name + ")"; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	}


	protected IClasspathEntry fEntry;

	public ClasspathEntryProperties(IClasspathEntry entry) {
		fEntry= entry;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor> result= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		for (Entry<Class<?>, List<Property>> entry : fgTypeToProperty.entrySet()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (entry.getKey().isAssignableFrom(fEntry.getClass())) {
				for (Property property : entry.getValue()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					result.add(property.getDescriptor());
				}
			}
		}
		return result.toArray(new IPropertyDescriptor[result.size()]);
	}

	@Override
	public Object getPropertyValue(Object id) {
		Property property= fgIdToProperty.get(id);
		if (property == null) {
			return null;
		} else {
			try {
				return property.compute(fEntry);
			} catch (JavaModelException e) {
				if (e.isDoesNotExist()) {
					return "JavaModelException: " + e.getLocalizedMessage(); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
				} else {
					JEViewPlugin.log("error calculating property '" + property.getType().getSimpleName() + '#' + property.getName() + '\'', e); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
					return "Error: " + e.getLocalizedMessage(); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
				}
			}
		}
	}

	@Override
	public void setPropertyValue(Object name, Object value) {
		// do nothing
	}

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public boolean isPropertySet(Object property) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object property) {
		// do nothing
	}
}
