/*******************************************************************************
 * Copyright (c) 2023, 2025 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.objectweb.asm.util.Printer;

import org.eclipse.jdt.core.dom.AST;

/**
 * Fetches latest supported JLS spec and provides help for bytecode opcodes.
 */
public class HelpUtils {
	static final String SPECS_HTML = "https://docs.oracle.com/javase/specs/jvms/se" + AST.getJLSLatest() + "/html/jvms-6.html"; //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT

	private static String fullSpec;

	private static String htmlHead;

	private static String checkOpcodeName(String opcodeName) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		opcodeName = opcodeName.toLowerCase();
		/*
		 * we need an additional check for DCONST_1...5, FCONST_1...5 etc case
		 * to convert it to DCONST_D etc
		 */
		int sepIndex = opcodeName.indexOf('_');
		if (sepIndex > 0) {
			if (Character.isDigit(opcodeName.charAt(sepIndex + 1))) {
				opcodeName = opcodeName.substring(0, sepIndex);
				switch (opcodeName.charAt(0)) {
					case 'd':
						opcodeName += "_d"; //$NON-NLS-1$
						break;
					case 'f':
						opcodeName += "_f"; //$NON-NLS-1$
						break;
					case 'l':
						opcodeName += "_l"; //$NON-NLS-1$
						break;
					default:
						// ICONST uses "n"
						opcodeName += "_n"; //$NON-NLS-1$
						break;
				}
			}
			if (opcodeName.startsWith("if_acmp")) { //$NON-NLS-1$
				opcodeName = "if_acmp_cond"; //$NON-NLS-1$
			} else if (opcodeName.startsWith("if_icmp")) { //$NON-NLS-1$
				opcodeName = "if_icmp_cond"; //$NON-NLS-1$
			} else if (opcodeName.startsWith("if_")) { //$NON-NLS-1$
				opcodeName = "if_cond"; //$NON-NLS-1$
			} else if (opcodeName.startsWith("aload_")) { //$NON-NLS-1$
				opcodeName = "aload_n"; //$NON-NLS-1$
			} else if (opcodeName.startsWith("iconst_")) { //$NON-NLS-1$
				opcodeName = "iconst_i"; //$NON-NLS-1$
			}
		} else if (opcodeName.startsWith("if")) { //$NON-NLS-1$
			opcodeName = "if_cond"; //$NON-NLS-1$
		}
		return opcodeName;
	}

	private static String getOpcodeName(int opcode) {
		if (opcode < 0 || opcode >= Printer.OPCODES.length) {
			return null;
		}
		String opcodeName = Printer.OPCODES[opcode];
		if (opcodeName == null) {
			return null;
		}
		return opcodeName;
	}

	private static URL toUrl(String href) {
		try {
			return URI.create(href).toURL();
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public static URL getHelpIndex() {
		return toUrl(SPECS_HTML);
	}

	private static String readFullSpec() { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD

		// The anchor where the real content starts. We have to ignore content before
		// that contains TOC with same anchors we want search later in content
		String firstInterestingLine = "<a name=\"jvms-6-100\">"; //$NON-NLS-1$
		URL helpResource = toUrl(SPECS_HTML);
		StringBuilder sb = new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		boolean foundContentStart = false;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(helpResource.openStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = in.readLine()) != null) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				if (!foundContentStart) {
					if (line.contains(firstInterestingLine)) {
						foundContentStart = true;
						// that was enough from head
						htmlHead = readHtmlHead(sb.toString());
						// from here on we read opcodes
						sb = new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
					}
				}
				sb.append(line).append('\n'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			}
		} catch (IOException e) {
			sb.append("Error trying access JVM specification at ").append(SPECS_HTML); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			sb.append(":"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			sb.append(e); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		return sb.toString();
	}

	public static StringBuilder getOpcodeHelpFor(int opcode) {
		if (fullSpec == null) {
			fullSpec = readFullSpec();
		}
		StringBuilder sb = new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		String opcodeName = getOpcodeName(opcode);
		if (opcodeName == null) {
			return sb;
		}
		return getOpcodeHelpFor(opcodeName);
	}

	public static StringBuilder getOpcodeHelpFor(String opcodeName) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (fullSpec == null) {
			fullSpec = readFullSpec();
		}
		StringBuilder sb = new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		if (opcodeName != null) {
			opcodeName = checkOpcodeName(opcodeName);
		}
		sb.append(htmlHead); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

		// Extract only important part related to the given opcode
		String patternStart = "jvms-6.5." + opcodeName + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		String patternEnd = "<div class=\"section-execution\""; //$NON-NLS-1$
		String startSection = "<div class=\"section-execution\"><div class=\"titlepage\"><div><div>"; //$NON-NLS-1$
		try (Scanner in = new Scanner(fullSpec)) {
			String line;
			boolean foundStart = false;
			boolean checkEnd = false;
			while (in.hasNextLine()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				line = in.nextLine();
				if (checkEnd && line.contains(patternEnd)) {
					break;
				}
				if (!foundStart && line.contains(patternStart)) {
					foundStart = true;
					checkEnd = true;
					sb.append(startSection); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
				if (foundStart) {
					sb.append(line); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
			}
		}

		// Allow navigation relative to the document
		int endHeadIdx = sb.indexOf("</head>"); //$NON-NLS-1$
		if (endHeadIdx > 0) {
			sb.insert(endHeadIdx, "\n<base href='" + SPECS_HTML + "'>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		}
		sb.append("</body></html>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		return sb;
	}

	private static String readHtmlHead(String head) {
		StringBuilder sb = new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		try (Scanner in = new Scanner(head)) {
			String line;
			while (in.hasNextLine()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				line = in.nextLine();
				if (line.contains("<body")) { //$NON-NLS-1$
					sb.append(line.substring(0, line.indexOf("<body"))); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					break;
				}
				sb.append(line); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			}
		}
		return sb.toString();
	}

}
