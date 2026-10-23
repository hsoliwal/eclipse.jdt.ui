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
 *     Sebastian Davids <sdavids@gmx.de> bug 38692
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.refactoring.IRefactoringSaveModes;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OpenBrowserUtil;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;


public class JavadocWizard extends Wizard implements IExportWizard {

	private JavadocTreeWizardPage fTreeWizardPage;
	private JavadocSpecificsWizardPage fLastWizardPage;
	private JavadocStandardWizardPage fStandardDocletWizardPage;
	private ContributedJavadocWizardPage[] fContributedJavadocWizardPages;

	private IPath fDestination;

	private boolean fOpenInBrowser;

	private static final String TREE_PAGE_DESC= "JavadocTreePage"; //$NON-NLS-1$
	private static final String SPECIFICS_PAGE_DESC= "JavadocSpecificsPage"; //$NON-NLS-1$
	private static final String STANDARD_PAGE_DESC= "JavadocStandardPage"; //$NON-NLS-1$

	private static final int YES= 0;
	private static final int YES_TO_ALL= 1;
	private static final int NO= 2;
	private static final int NO_TO_ALL= 3;
	private static final String JAVADOC_ANT_INFORMATION_DIALOG= "javadocAntInformationDialog";//$NON-NLS-1$


	private JavadocOptionsManager fStore;
	private IWorkspaceRoot fRoot;

	private IFile fXmlJavadocFile;

	private static final String ID_JAVADOC_PROCESS_TYPE= "org.eclipse.jdt.ui.javadocProcess"; //$NON-NLS-1$

	private static final String ENCODING_ARGUMENT_PREFIX= "-J-Dfile.encoding="; //$NON-NLS-1$

	public static void openJavadocWizard(JavadocWizard wizard, Shell shell, IStructuredSelection selection ) {
		wizard.init(PlatformUI.getWorkbench(), selection);

		WizardDialog dialog= new WizardDialog(shell, wizard) {
			@Override
			protected IDialogSettings getDialogBoundsSettings() {
				// added so that the wizard can remember the last used size
				return JavaPlugin.getDefault().getDialogSettingsSection("JavadocWizardDialog"); //$NON-NLS-1$
			}
		};
		PixelConverter converter= new PixelConverter(JFaceResources.getDialogFont());
		dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(100), converter.convertHeightInCharsToPixels(20));
		dialog.open();
	}


	public JavadocWizard() {
		this(null);
	}

	public JavadocWizard(IFile xmlJavadocFile) {
		super();
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_EXPORT_JAVADOC);
		setWindowTitle(JavadocExportMessages.JavadocWizard_javadocwizard_title);

		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());

		fRoot= ResourcesPlugin.getWorkspace().getRoot();
		fXmlJavadocFile= xmlJavadocFile;
	}

	/*
	 * @see IWizard#performFinish()
	 */
	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public boolean performFinish() {
		updateStore();

		IJavaProject[] checkedProjects= fTreeWizardPage.getCheckedProjects();
		fStore.updateDialogSettings(getDialogSettings(), checkedProjects);

		// Wizard should not run with dirty editors
		if (!new RefactoringSaveHelper(IRefactoringSaveModes.SAVE_ALL_ALWAYS_ASK).saveEditors(getShell())) {
			return false;
		}

		fDestination= Path.fromOSString(fStore.getDestination());
		fDestination.toFile().mkdirs(); //FLLDCIM: MARK — M31 M3FILE_IO CALL

		fOpenInBrowser= fStore.doOpenInBrowser();

		//Ask if you wish to set the javadoc location for the projects (all) to
		//the location of the newly generated javadoc
		if (fStore.isFromStandard()) {
			try {

				URL newURL= fDestination.toFile().toURI().toURL();
				String newExternalForm= newURL.toExternalForm();
				List<IJavaProject> projs= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
				//get javadoc locations for all projects
				for (IJavaProject curr : checkedProjects) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					URL currURL= JavaUI.getProjectJavadocLocation(curr);
					if (currURL == null || !newExternalForm.equals(currURL.toExternalForm())) {
						//if not all projects have the same javadoc location ask if you want to change
						//them to have the same javadoc location
						projs.add(curr);
					}
				}
				if (!projs.isEmpty()) {
					setAllJavadocLocations(projs.toArray(new IJavaProject[projs.size()]), newURL);
				}
			} catch (MalformedURLException e) {
				JavaPlugin.log(e);
			}
		}

		if (fLastWizardPage.generateAnt()) {
			//@Improve: make a better message
			OptionalMessageDialog.open(JAVADOC_ANT_INFORMATION_DIALOG, getShell(), JavadocExportMessages.JavadocWizard_antInformationDialog_title, null, JavadocExportMessages.JavadocWizard_antInformationDialog_message, MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0);
			try {
				Element javadocXMLElement= fStore.createXML(checkedProjects);
				if (javadocXMLElement != null) {

					if (!fTreeWizardPage.getCustom()) {
						for (ContributedJavadocWizardPage contributedJavadocWizardPage : fContributedJavadocWizardPages) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
							contributedJavadocWizardPage.updateAntScript(javadocXMLElement);
						}
					}
					File file= fStore.writeXML(javadocXMLElement);
					IFile[] files= fRoot.findFilesForLocationURI(file.toURI());
					if (files != null) {
						for (IFile f : files) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
							f.refreshLocal(IResource.DEPTH_ONE, null);
						}
					}
				}

			} catch (CoreException e) {
				ExceptionHandler.handle(e, getShell(),JavadocExportMessages.JavadocWizard_error_writeANT_title, JavadocExportMessages.JavadocWizard_error_writeANT_message);
			}
		}

		if (!executeJavadocGeneration())
			return false;

		return true;
	}

	private void updateStore() {
		fTreeWizardPage.updateStore();
		if (!fTreeWizardPage.getCustom())
			fStandardDocletWizardPage.updateStore();
		fLastWizardPage.updateStore();
	}

	@Override
	public boolean performCancel() {
		updateStore();

		//If the wizard was not launched from an ant file store the settings
		if (fXmlJavadocFile == null) {
			IJavaProject[] checkedProjects= fTreeWizardPage.getCheckedProjects();
			fStore.updateDialogSettings(getDialogSettings(), checkedProjects);
		}
		return super.performCancel();
	}

	private void setAllJavadocLocations(IJavaProject[] projects, URL newURL) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		Shell shell= getShell();
		String[] buttonlabels= new String[] { IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.NO_TO_ALL_LABEL };

		for (int j= 0; j < projects.length; j++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			IJavaProject iJavaProject= projects[j];
			String message= Messages.format(JavadocExportMessages.JavadocWizard_updatejavadoclocation_message, new String[] { BasicElementLabels.getJavaElementName(iJavaProject.getElementName()), BasicElementLabels.getPathLabel(fDestination, true) });
			MessageDialog dialog= new MessageDialog(shell, JavadocExportMessages.JavadocWizard_updatejavadocdialog_label, null, message, MessageDialog.QUESTION, buttonlabels, 1);

			switch (dialog.open()) {
				case YES :
					JavaUI.setProjectJavadocLocation(iJavaProject, newURL);
					break;
				case YES_TO_ALL :
					for (int i= j; i < projects.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
						iJavaProject= projects[i];
						JavaUI.setProjectJavadocLocation(iJavaProject, newURL);
						j++;
					}
					break;
				case NO_TO_ALL :
					j= projects.length;
					break;
				case NO :
				default :
					break;
			}
		}
	}

	private boolean executeJavadocGeneration() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		Process process= null;
		try {
			ArrayList<String> vmArgs= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
			ArrayList<String> progArgs= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

			IStatus status= fStore.getArgumentArray(vmArgs, progArgs);
			if (!status.isOK()) {
				ErrorDialog.openError(getShell(), JavadocExportMessages.JavadocWizard_error_title, JavadocExportMessages.JavadocWizard_warning_starting_message, status);
			}

			if (!fTreeWizardPage.getCustom()) {
				for (ContributedJavadocWizardPage fContributedJavadocWizardPage : fContributedJavadocWizardPages) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					fContributedJavadocWizardPage.updateArguments(vmArgs, progArgs);
				}
			}

			File file= File.createTempFile("javadoc-arguments", ".tmp");  //$NON-NLS-1$//$NON-NLS-2$
			vmArgs.add('@' + file.getAbsolutePath()); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT

			try (BufferedWriter writer= new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), getEncoding(vmArgs)))) { //FLLDCIM: MARK — M31 M3FILE_IO NEW
				for (String progArg : progArgs) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					String curr= progArg;
					curr= checkForSpaces(curr);

					writer.write(curr);
					writer.write(' ');
				}
			}
			String[] args= vmArgs.toArray(new String[vmArgs.size()]);
			process= Runtime.getRuntime().exec(args);
			if (process != null) {
				// construct a formatted command line for the process properties
				StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
				for (String arg : args) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					buf.append(arg); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					buf.append(' '); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}

				try {
					// Usually the output from Javadoc command is shown in Console View. The console is automatically created from the
					// debug framework but only if org.eclipse.debug.ui plug-in is loaded. It is unlikely but possible that this plug-in
					// is not loaded at this point which does not interfere the command execution but its output will be lost.
					// Hence the Console View is not part of org.eclipse.debug.ui plug-in but in a separate plug-in it is even possible
					// that Console View is open but the output of Javadoc command does not appear. For a user it is incomprehensible
					// why the Javadoc command output does not appear in this uncommon situation.
					// The easy fix for this is to simply 'touch' a class from org.eclipse.debug.ui plug-in to be sure it is loaded before
					// starting the Javadoc command. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=239217
					// Note: you might notice the use of IDebugUIConstants below and feel the urge to remove this workaround because it
					// seem unnecessary. It is not! IDebugUIConstants.ATTR_PRIVATE is resolved at compile time and will not access the
					// constants class and will not trigger plug-in activation.
					DebugUITools.class.getClass();

					ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
					ILaunchConfigurationType lcType= launchManager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);

					String name= JavadocExportMessages.JavadocWizard_launchconfig_name;
					ILaunchConfigurationWorkingCopy wc= lcType.newInstance(null, name);
					wc.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);

					ILaunch newLaunch= new Launch(wc, ILaunchManager.RUN_MODE, null);
					IProcess iprocess= DebugPlugin.newProcess(newLaunch, process, JavadocExportMessages.JavadocWizard_javadocprocess_label);
					iprocess.setAttribute(IProcess.ATTR_CMDLINE, buf.toString());
					iprocess.setAttribute(IProcess.ATTR_PROCESS_TYPE, ID_JAVADOC_PROCESS_TYPE);

					launchManager.addLaunch(newLaunch);
					JavadocLaunchListener listener= new JavadocLaunchListener(getShell().getDisplay(), newLaunch, file);
					launchManager.addLaunchListener(listener);
					if (newLaunch.isTerminated()) {
						listener.onTerminated();
					}

				} catch (CoreException e) {
					String title= JavadocExportMessages.JavadocWizard_error_title;
					String message= JavadocExportMessages.JavadocWizard_launch_error_message;
					ExceptionHandler.handle(e, getShell(), title, message);
				}

				return true;

			}
		} catch (IOException e) {
			String title= JavadocExportMessages.JavadocWizard_error_title;
			String message= JavadocExportMessages.JavadocWizard_exec_error_message;

			IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, e.getMessage(), e);
			ExceptionHandler.handle(new CoreException(status), getShell(), title, message);
			return false;
		}
		return false;

	}

	private static String getEncoding(ArrayList<String> vmArgs) {
		Iterator<String> iter= vmArgs.iterator();
		while (iter.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String argument= iter.next();
			if (argument.length() > ENCODING_ARGUMENT_PREFIX.length() && argument.startsWith(ENCODING_ARGUMENT_PREFIX)) {
				String encoding= argument.substring(ENCODING_ARGUMENT_PREFIX.length());
				if (Charset.isSupported(encoding))
					return encoding;
				break;
			}
		}
		return Charset.defaultCharset().displayName();

	}

	private String checkForSpaces(String curr) {
		if (curr.indexOf(' ') == -1) {
			return curr;
		}
		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		buf.append('\''); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		for (char ch : curr.toCharArray()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (ch == '\\' || ch == '\'') {
				buf.append('\\'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			}
			buf.append(ch); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		buf.append('\''); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		return buf.toString();
	}

	/*
	 * @see IWizard#addPages()
	 */
	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public void addPages() {
		fContributedJavadocWizardPages= ContributedJavadocWizardPage.getContributedPages(fStore);

		fTreeWizardPage= new JavadocTreeWizardPage(TREE_PAGE_DESC, fStore);
		fLastWizardPage= new JavadocSpecificsWizardPage(SPECIFICS_PAGE_DESC, fTreeWizardPage, fStore);
		fStandardDocletWizardPage= new JavadocStandardWizardPage(STANDARD_PAGE_DESC, fTreeWizardPage, fStore);

		super.addPage(fTreeWizardPage);
		super.addPage(fStandardDocletWizardPage);

		for (ContributedJavadocWizardPage fContributedJavadocWizardPage : fContributedJavadocWizardPages) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			super.addPage(fContributedJavadocWizardPage);
		}
		super.addPage(fLastWizardPage);

		fTreeWizardPage.init();
		fStandardDocletWizardPage.init();
		fLastWizardPage.init();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection structuredSelection) {
		IWorkbenchWindow window= workbench.getActiveWorkbenchWindow();
		List<?> selected= Collections.EMPTY_LIST;
		if (window != null) {
			ISelection selection= window.getSelectionService().getSelection();
			if (selection instanceof IStructuredSelection) {
				selected= ((IStructuredSelection) selection).toList();
			} else {
				IJavaElement element= EditorUtility.getActiveEditorJavaInput();
				if (element != null) {
					selected= Arrays.asList(element);
				}
			}
		}
		fStore= new JavadocOptionsManager(fXmlJavadocFile, getDialogSettings(), selected);
	}

	private void refresh(IPath path) {
		IContainer[] containers= fRoot.findContainersForLocationURI(path.toFile().toURI());
		try {
			for (IContainer container : containers) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				container.refreshLocal(IResource.DEPTH_INFINITE, null);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		}
	}

	private void spawnInBrowser(Display display) {
		if (fOpenInBrowser) {
			try {
				IPath indexFile= fDestination.append("index.html"); //$NON-NLS-1$
				URL url= indexFile.toFile().toURI().toURL();
				OpenBrowserUtil.open(url, display);
			} catch (MalformedURLException e) {
				JavaPlugin.log(e);
			}
		}
	}

	private class JavadocLaunchListener implements ILaunchesListener2 { //FLLDCIM: MARK — T27  IMPLEMENTS
		private Display fDisplay;
		private volatile ILaunch fLaunch;
		private File fFile;

		public JavadocLaunchListener(Display display, ILaunch launch, File file) {
			fDisplay= display;
			fLaunch= launch;
			fFile= file;
		}

		@Override
		public void launchesTerminated(ILaunch[] launches) {
			if (containsJavadocLaunch(launches)) {
				onTerminated();
			}
		}

		private boolean containsJavadocLaunch(ILaunch[] launches) {
			for (ILaunch launch : launches) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				if (launch == fLaunch) {
					return true;
				}
			}
			return false;
		}

		public void onTerminated() {
			if (fLaunch != null) {
				fFile.deleteOnExit(); // Just to be safe. #launchesRemoved(..) is not called on shutdown.
				spawnInBrowser(fDisplay);
				refresh(fDestination);
			}
		}

		@Override
		public void launchesRemoved(ILaunch[] launches) {
			if (containsJavadocLaunch(launches)) {
				try {
					fFile.delete(); //FLLDCIM: MARK — M31 M3FILE_IO CALL
					fLaunch= null;
				} finally {
					DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
				}
			}
		}

		@Override
		public void launchesAdded(ILaunch[] launches) { }
		@Override
		public void launchesChanged(ILaunch[] launches) { }
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == fTreeWizardPage && fTreeWizardPage.getCustom()) {
			return fLastWizardPage;
		}
		return super.getNextPage(page);
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page == fLastWizardPage && fTreeWizardPage.getCustom()) {
			return fTreeWizardPage;
		}
		return super.getPreviousPage(page);
	}

}
