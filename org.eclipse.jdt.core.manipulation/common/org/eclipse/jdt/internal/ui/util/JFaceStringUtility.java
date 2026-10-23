/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - copied from org.eclipse.jface.util.Util
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

/**
 * Copied from org.eclipse.jface.util.Util
 * @since 1.20
 */
public class JFaceStringUtility {

	/**
	 * Foundation replacement for <code>String#replaceAll(String,
	 * String)</code>, but <strong>without support for regular
	 * expressions</strong>.
	 *
	 * @param src the original string
	 * @param find the string to find
	 * @param replacement the replacement string
	 * @return the new string, with all occurrences of <code>find</code>
	 *         replaced by <code>replacement</code> (not using regular
	 *         expressions)
	 * @since 3.4
	 */
	public static String replaceAll(String src, String find, String replacement) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		final int len = src.length();
		final int findLen = find.length();

		int idx = src.indexOf(find);
		if (idx < 0) {
			return src;
		}

		StringBuilder buf = new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		int beginIndex = 0;
		while (idx != -1 && idx < len) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buf.append(src.substring(beginIndex, idx)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buf.append(replacement); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

			beginIndex = idx + findLen;
			if (beginIndex < len) {
				idx = src.indexOf(find, beginIndex);
			} else {
				idx = -1;
			}
		}
		if (beginIndex<len) {
			buf.append(src.substring(beginIndex, (idx==-1?len:idx))); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		return buf.toString();
	}
}
