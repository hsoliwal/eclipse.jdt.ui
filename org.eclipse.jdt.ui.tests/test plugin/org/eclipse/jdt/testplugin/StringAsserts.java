/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.testplugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;


public class StringAsserts {
	public StringAsserts() {
		super();
	}

	private static int getDiffPos(String str1, String str2) {
		int len1= Math.min(str1.length(), str2.length());

		int diffPos= -1;
		for (int i= 0; i < len1; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (str1.charAt(i) != str2.charAt(i)) {
				diffPos= i;
				break;
			}
		}
		if (diffPos == -1 && str1.length() != str2.length()) {
			diffPos= len1;
		}
		return diffPos;
	}

	private static final int printRange= 6;

	public static void assertEqualString(String actual, String expected) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (actual == null || expected == null) {
			if (actual == expected) {
				return;
			}
			if (actual == null) {
				Assertions.fail("Content not as expected: is 'null' expected: " + expected); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			} else {
				Assertions.fail("Content not as expected: expected 'null' is: " + actual); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			}
		}

		int diffPos= getDiffPos(actual, expected);
		if (diffPos != -1) {
			int diffAhead= Math.max(0, diffPos - printRange);
			int diffAfter= Math.min(actual.length(), diffPos + printRange);

			String diffStr= actual.substring(diffAhead, diffPos) + '^' + actual.substring(diffPos, diffAfter); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT

			// use detailed message
			String message= "Content not as expected: is\n" + actual + "\nDiffers at pos " + diffPos + ": " + diffStr + "\nexpected:\n" + expected;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT

			Assertions.assertEquals(expected, actual, message);
		}
	}

	public static void assertEqualStringIgnoreDelim(String actual, String expected) throws IOException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (actual == null || expected == null) {
			if (actual == expected) {
				return;
			}
			if (actual == null) {
				Assertions.fail("Content not as expected: is 'null' expected: " + expected); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			} else {
				Assertions.fail("Content not as expected: expected 'null' is: " + actual); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			}
		}

		BufferedReader read1= new BufferedReader(new StringReader(actual));
		BufferedReader read2= new BufferedReader(new StringReader(expected));

		int line= 1;
		do { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String s1= read1.readLine();
			String s2= read2.readLine();

			if (s1 == null || !s1.equals(s2)) {
				if (s1 == null && s2 == null) {
					return;
				}
				String diffStr= (s1 == null) ? s2 : s1;

				String message= "Content not as expected: Content is: \n" + actual + "\nDiffers at line " + line + ": " + diffStr + "\nExpected contents: \n" + expected; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
				Assertions.assertEquals(expected, actual, message);
			}
			line++;
		} while (true);
	}

	public static void assertEqualStringsIgnoreOrder(String[] actuals, String[] expecteds) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		ArrayList<String> list1= new ArrayList<>(Arrays.asList(actuals)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		ArrayList<String> list2= new ArrayList<>(Arrays.asList(expecteds)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

		for (int i= list1.size() - 1; i >= 0; i--) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (list2.remove(list1.get(i))) {
				list1.remove(i);
			}
		}

		int n1= list1.size();
		int n2= list2.size();

		if (n1 + n2 > 0) {
			if (n1 == 1 && n2 == 1) {
				assertEqualString(list1.get(0), list2.get(0));
			}

			StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
			for (int i= 0; i < n1; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				String s1= list1.get(i);
				if (s1 != null) {
					buf.append(s1); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					buf.append("\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
			}
			String actual= buf.toString();

			buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
			for (int i= 0; i < n2; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				String s2= list2.get(i);
				if (s2 != null) {
					buf.append(s2); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					buf.append("\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
			}
			String expected= buf.toString();

			String message= "Content not as expected: Content is: \n" + actual + "\nExpected contents: \n" + expected; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			Assertions.assertEquals(expected, actual, message);
		}
	}

	public static void assertExpectedExistInProposals(String[] actuals, String[] expecteds) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		ArrayList<String> list1= new ArrayList<>(Arrays.asList(actuals)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		ArrayList<String> list2= new ArrayList<>(Arrays.asList(expecteds)); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

		for (int i= list1.size() - 1; i >= 0; i--) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (list2.remove(list1.get(i))) {
				list1.remove(i);
			}
		}

		int n1= list1.size();
		int n2= list2.size();

		if (n2 > 0) {
			if (n1 == 1 && n2 == 1) {
				assertEqualString(list1.get(0), list2.get(0));
			}

			StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
			for (int i= 0; i < n1; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				String s1= list1.get(i);
				if (s1 != null) {
					buf.append(s1); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					buf.append("\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
			}
			String actual= buf.toString();

			buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
			for (int i= 0; i < n2; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				String s2= list2.get(i);
				if (s2 != null) {
					buf.append(s2); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					buf.append("\n"); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
			}
			String expected= buf.toString();

			String message= "Content not as expected: Content is: \n" + actual + "\nExpected contents: \n" + expected; //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			Assertions.assertEquals(expected, actual, message);
		}
	}

}
