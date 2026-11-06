/*******************************************************************************
 * Copyright (c) 2021, 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.StringConcatToTextBlockFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class StringConcatToTextBlockCleanUpCore extends AbstractCleanUp {

	public StringConcatToTextBlockCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public StringConcatToTextBlockCleanUpCore() {
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK);
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK)) {
			return null;
		}

		return StringConcatToTextBlockFixCore.createCleanUp(compilationUnit, isEnabled(CleanUpConstants.STRINGCONCAT_STRINGBUFFER_STRINGBUILDER));
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK)) {
			if (isEnabled(CleanUpConstants.STRINGCONCAT_STRINGBUFFER_STRINGBUILDER)) {
				return new String[] {MultiFixMessages.StringConcatToTextBlockStringBuffer_description};
			}
			return new String[] {MultiFixMessages.StringConcatToTextBlockCleanUp_description};
		}
		return new String[0];
	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public String getPreview() {
		StringBuilder bld= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW

		if (isEnabled(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK)) {
			bld.append("String buf = \"\"\"\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("    public class A {\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("        public void foo() {\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("        }\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("    }\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("    \"\"\";\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		} else {
			bld.append("String buf = \"\" +\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("    \"public class A {\" +\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("    \"    public void foo() {\" +\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("    \"    }\" + \n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("    \"}\";\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		bld.append("\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		if (isEnabled(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK) && isEnabled(CleanUpConstants.STRINGCONCAT_STRINGBUFFER_STRINGBUILDER)) {
			bld.append("String k = \"\"\"\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("    public void foo() {\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("        return null;\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("    }\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("    \"\"\";\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		} else {
			bld.append("StringBuffer buf= new StringBuffer();\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("buf.append(\"public void foo() {\\n\");\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("buf.append(\"    return null;\\n\");\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("buf.append(\"}\\n\");\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			bld.append("String k = buf.toString();\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}

		return bld.toString();
	}
}
