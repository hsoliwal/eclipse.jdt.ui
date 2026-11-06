/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jdt.core.IElementChangedListener;

import org.eclipse.jdt.internal.ui.javaeditor.JavaOutlinePage;

/**
 * Qualifies the source-level Java-model/SWT handoff contract. Full UI thread,
 * input generation, native disposal and editor lifecycle remain separate tests.
 */
public class JavaOutlineDisplayBindingTest {
    @Test
    public void sourceOwnerPublishesAnExplicitVolatileDisplayBinding() throws Exception {
        Field binding = JavaOutlinePage.class.getDeclaredField("fM3OutlineDisplay"); //$NON-NLS-1$
        assertEquals(Display.class, binding.getType());
        assertTrue("a Java-model worker must see UI create/dispose publication", //$NON-NLS-1$
                Modifier.isVolatile(binding.getModifiers()));
        Class<?> callback = Class.forName(JavaOutlinePage.class.getName() + "$ElementChangedListener"); //$NON-NLS-1$
        assertTrue(IElementChangedListener.class.isAssignableFrom(callback));
    }
}
