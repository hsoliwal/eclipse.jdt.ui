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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.views.contentoutline.ContentOutline;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.JavaElementDelta;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaOutlinePage;

/**
 * Tests Java-model callbacks queued across changes to the native outline input.
 */
public class JavaOutlineDisplayBindingTest {
	@Rule
	public ProjectTestSetup project= new ProjectTestSetup();

	private IWorkbenchPage workbenchPage;
	private IEditorPart editor;
	private ContentOutline outlineView;
	private boolean openedOutlineView;
	private JavaOutlinePage outline;
	private TreeViewer viewer;
	private IElementChangedListener listener;

	@After
	public void closeEditor() throws Exception {
		if (editor != null)
			workbenchPage.closeEditor(editor, false);
		if (openedOutlineView)
			workbenchPage.hideView(outlineView);
		JavaProjectHelper.clear(project.getProject(), project.getDefaultClasspath());
	}

    @Test
    public void sourceOwnerPublishesAnExplicitVolatileDisplayBinding() throws Exception {
        Field binding = JavaOutlinePage.class.getDeclaredField("fM3OutlineDisplay"); //$NON-NLS-1$
        assertEquals(Display.class, binding.getType());
        assertTrue("a Java-model worker must see UI create/dispose publication", //$NON-NLS-1$
                Modifier.isVolatile(binding.getModifiers()));
        Class<?> callback = Class.forName(JavaOutlinePage.class.getName() + "$ElementChangedListener"); //$NON-NLS-1$
        assertTrue(IElementChangedListener.class.isAssignableFrom(callback));
    }

	@Test
	public void currentInputModelChangeRefreshesOutlineRows() throws Exception {
		openOutline();
		ICompilationUnit unit= (ICompilationUnit) viewer.getInput();
		viewer.expandAll();
		// Isolate the notification below from the model operation's own events.
		JavaCore.removeElementChangedListener(listener);
		try {
			IField added= unit.getType("OutlineInput").createField("int added;", null, false, null); //$NON-NLS-1$ //$NON-NLS-2$
			assertFalse(containsElement(viewer.getTree().getItems(), added));
			JavaElementDelta delta= new JavaElementDelta(unit);
			delta.changed(IJavaElementDelta.F_CHILDREN);
			postModelChange(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));
			drainCallbacks(viewer.getControl().getDisplay());
			assertTrue("the current input's new member must be rendered", //$NON-NLS-1$
					containsElement(viewer.getTree().getItems(), added));
		} finally {
			JavaCore.addElementChangedListener(listener);
		}
	}

	@Test
	public void queuedModelChangeIgnoresReplacementClassFile() throws Exception {
		openOutline();
		IJavaElement classFile= project.getProject().findType("java.lang.Object").getClassFile(); //$NON-NLS-1$
		assertNotNull(classFile);
		checkInputChange(classFile, true);
	}

	@Test
	public void queuedModelChangeIgnoresClearedInput() throws Exception {
		openOutline();
		Field topLevelOnly= JavaOutlinePage.class.getDeclaredField("fTopLevelTypeOnly"); //$NON-NLS-1$
		topLevelOnly.setAccessible(true);
		topLevelOnly.setBoolean(outline, true);
		checkInputChange(null, true);
	}

	@Test
	public void removedListenerIgnoresAnInFlightModelNotification() throws Exception {
		openOutline();
		IJavaElement classFile= project.getProject().findType("java.lang.Object").getClassFile(); //$NON-NLS-1$
		assertNotNull(classFile);
		checkInputChange(classFile, false);
	}

	private void openOutline() throws Exception {
		IPackageFragmentRoot source= JavaProjectHelper.addSourceContainer(project.getProject(), "src"); //$NON-NLS-1$
		ICompilationUnit unit= source.getPackageFragment("").createCompilationUnit( //$NON-NLS-1$
				"OutlineInput.java", "class OutlineInput {}", false, null); //$NON-NLS-1$ //$NON-NLS-2$
		workbenchPage= JavaPlugin.getActivePage();
		openedOutlineView= workbenchPage.findView(IPageLayout.ID_OUTLINE) == null;
		editor= JavaUI.openInEditor(unit);
		workbenchPage.activate(editor);
		outlineView= (ContentOutline) workbenchPage.showView(IPageLayout.ID_OUTLINE);
		assertTrue(outlineView.getCurrentPage() instanceof JavaOutlinePage);
		outline= (JavaOutlinePage) outlineView.getCurrentPage();
		Field viewerField= JavaOutlinePage.class.getDeclaredField("fOutlineViewer"); //$NON-NLS-1$
		viewerField.setAccessible(true);
		viewer= (TreeViewer) viewerField.get(outline);
		Object provider= viewer.getContentProvider();
		Field listenerField= provider.getClass().getDeclaredField("fListener"); //$NON-NLS-1$
		listenerField.setAccessible(true);
		listener= (IElementChangedListener) listenerField.get(provider);
		assertNotNull(listener);
	}

	private void checkInputChange(IJavaElement replacement, boolean queueBeforeInputChange) throws Exception {
		Display display= viewer.getControl().getDisplay();
		List<RuntimeException> failures= new ArrayList<>();
		Consumer<RuntimeException> previousHandler= display.getRuntimeExceptionHandler();
		JavaElementDelta delta= new JavaElementDelta((IJavaElement) viewer.getInput());
		delta.changed(IJavaElementDelta.F_CONTENT);
		ElementChangedEvent event= new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE);
		display.setRuntimeExceptionHandler(failures::add);
		try {
			if (!queueBeforeInputChange)
				outline.setInput(replacement);
			postModelChange(event);
			if (queueBeforeInputChange)
				outline.setInput(replacement);
			drainCallbacks(display);
			assertTrue("stale outline callback failed: " + failures, failures.isEmpty()); //$NON-NLS-1$
			assertSame(replacement, viewer.getInput());
		} finally {
			display.setRuntimeExceptionHandler(previousHandler);
		}
	}

	private void postModelChange(ElementChangedEvent event) throws InterruptedException {
		AtomicReference<Throwable> workerFailure= new AtomicReference<>();
		Thread worker= new Thread(() -> {
			try {
				listener.elementChanged(event);
			} catch (Throwable failure) {
				workerFailure.set(failure);
			}
		}, "outline-model-notification"); //$NON-NLS-1$
		worker.setDaemon(true);
		worker.start();
		worker.join(10000);
		assertFalse("model callback must not block the UI thread", worker.isAlive()); //$NON-NLS-1$
		if (workerFailure.get() != null)
			throw new AssertionError(workerFailure.get());
	}

	private static void drainCallbacks(Display display) {
		AtomicBoolean drained= new AtomicBoolean();
		display.asyncExec(() -> drained.set(true));
		while (!drained.get()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	private static boolean containsElement(TreeItem[] items, IJavaElement expected) {
		for (TreeItem item : items) {
			if (expected.equals(item.getData()) || containsElement(item.getItems(), expected))
				return true;
		}
		return false;
	}
}
