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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.formatter.IndentManipulation;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;


public class JavaElementLine {


	private final ITypeRoot fElement;
	private final String fLineContents;
	private final int fLineNumber;
	private final int fLineStartOffset;

	private int fFlags;

	/**
	 * @param element either an ICompilationUnit or an IClassFile
	 * @param lineNumber the line number, starting at 0
	 * @param lineStartOffset the start offset of the line
	 * @throws CoreException thrown when accessing of the buffer failed
	 */
	public JavaElementLine(ITypeRoot element, int lineNumber, int lineStartOffset) throws CoreException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		fElement= element;
		fFlags= 0;

		IBuffer buffer= element.getBuffer();
		if (buffer == null) {
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, Messages.format( SearchMessages.JavaElementLine_error_nobuffer, BasicElementLabels.getFileName(element))));
		}

		int length= buffer.getLength();
		int i= lineStartOffset;

		char ch= buffer.getChar(i);
		while (lineStartOffset < length && IndentManipulation.isIndentChar(ch)) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			ch= buffer.getChar(++i);
		}
		fLineStartOffset= i;

		StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW

		while (i < length && !IndentManipulation.isLineDelimiterChar(ch)) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (Character.isISOControl(ch)) {
				buf.append(' '); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			} else {
				buf.append(ch); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			}
			i++;
			if (i < length)
				ch= buffer.getChar(i);
		}
		fLineContents= buf.toString();
		fLineNumber= lineNumber;
	}

	public void setFlags(int flags) {
		fFlags= flags;
	}

	public int getFlags() {
		return fFlags;
	}

	public ITypeRoot getJavaElement() {
		return fElement;
	}

	/**
	 * Returns the line number.
	 *
	 * @return the line number, starting at 0
	 */
	public int getLineNumber() {
		return fLineNumber;
	}

	public String getLineContents() {
		return fLineContents;
	}

	public int getLineStartOffset() {
		return fLineStartOffset;
	}
}
