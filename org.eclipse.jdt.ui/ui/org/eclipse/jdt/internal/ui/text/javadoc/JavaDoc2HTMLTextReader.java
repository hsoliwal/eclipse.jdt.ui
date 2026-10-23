/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Brock Janiczak (brockj_eclipse@ihug.com.au) - https://bugs.eclipse.org/bugs/show_bug.cgi?id=20644
 *     Brock Janiczak (brockj_eclipse@ihug.com.au) - https://bugs.eclipse.org/bugs/show_bug.cgi?id=83607
 *     Benjamin Muskalla <b.muskalla@gmx.net> - [navigation][hovering] Javadoc view cannot find URL with anchor - https://bugs.eclipse.org/bugs/show_bug.cgi?id=70870
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.javadoc;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.readers.SubstitutionReader;

import org.eclipse.jface.internal.text.html.HTMLPrinter;

import org.eclipse.jdt.core.dom.TagElement;


/**
 * Processes JavaDoc tags.
 */
public class JavaDoc2HTMLTextReader extends SubstitutionReader {


	static private class Pair {
		String fTag;
		String fContent;

		Pair(String tag, String content) {
			fTag= tag;
			fContent= content;
		}
	}

	private List<String> fParameters;
	private String fReturn;
	private List<String> fExceptions;
	private List<String> fAuthors;
	private List<String> fSees;
	private List<String> fSince;
	private List<Pair> fRest; // list of Pair objects

	public JavaDoc2HTMLTextReader(Reader reader) {
		super(reader);
		setSkipWhitespace(false);
	}

	private int getTag(StringBuilder buffer) throws IOException {
		int c= nextChar();
		while (c == '.' || c != -1 && Character.isLetter((char) c)) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buffer.append((char) c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			c= nextChar();
		}
		return c;
	}

	private int getContent(StringBuilder buffer, char stopChar) throws IOException {
		int c= nextChar();
		while (c != -1 && c != stopChar) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			buffer.append((char) c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			c= nextChar();
		}
		return c;
	}

	private int getContentUntilNextTag(StringBuilder buffer) throws IOException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		int c= nextChar();
        boolean blockStartRead= false;
		while (c != -1) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (c == '@') {
				int index= buffer.length();
				while (--index >= 0 && Character.isWhitespace(buffer.charAt(index))) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					switch (buffer.charAt(index)) {
					case '\n':
					case '\r':
						return c;
					}
					if (index <= 0) {
						return c;
					}
				}
			}
            if (blockStartRead) {
                buffer.append(processBlockTag()); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
                blockStartRead= false;
            } else {
                buffer.append((char) c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
            }

			c= nextChar();
            blockStartRead= c == '{';
		}
		return c;
	}

	private String substituteQualification(String qualification) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		String result;
		if (!qualification.contains("<a")) { //$NON-NLS-1$
			// No tag at all, use smart way
			result= qualification.replace('#', '.');
		} else {
			// Handle tags
			int length= qualification.length();
			result= qualification;
			boolean insideTag= false;
			for (int i= 0; i < length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				char charAt= result.charAt(i);
				if (charAt == '<' && result.charAt(i + 1) == 'a')
					insideTag= true;
				if (charAt == '>')
					insideTag= false;
				if (charAt == '#' && !insideTag)
					result= result.substring(0, i) + "." + result.substring(i + 1); //$NON-NLS-1$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			}
		}

		if (result.startsWith(".")) //$NON-NLS-1$
			result= result.substring(1);
		return result;
	}

	private void printDefinitions(StringBuilder buffer, List<String> list, boolean firstword) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		Iterator<String> e= list.iterator();
		while (e.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			String s= e.next();
			buffer.append("<dd>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			if (!firstword)
				buffer.append(s); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			else {
				buffer.append("<b>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

				int i= getParamEndOffset(s);
				if (i <= s.length()) {
					buffer.append(HTMLPrinter.convertToHTMLContent(s.substring(0, i))); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					buffer.append("</b>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					buffer.append(s.substring(i)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				} else {
					buffer.append("</b>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
			}
			buffer.append("</dd>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
	}

	private int getParamEndOffset(String s) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		int i= 0;
		final int length= s.length();
		// \s*
		while (i < length && Character.isWhitespace(s.charAt(i))) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			++i;
		if (i < length && s.charAt(i) == '<') {
			// generic type parameter
			// read <\s*\w*\s*>
			while (i < length && Character.isWhitespace(s.charAt(i))) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				++i;
			while (i < length && Character.isJavaIdentifierPart(s.charAt(i))) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				++i;
			while (i < length && s.charAt(i) != '>') //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				++i;
		} else {
			// simply read an identifier
			while (i < length && Character.isJavaIdentifierPart(s.charAt(i))) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				++i;
		}

		return i;
	}

	private void print(StringBuilder buffer, String tag, List<String> elements, boolean firstword) {
		if ( !elements.isEmpty()) {
			buffer.append("<dt>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buffer.append(tag); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buffer.append("</dt>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			printDefinitions(buffer, elements, firstword);
		}
	}

	private void print(StringBuilder buffer, String tag, String content) {
		if  (content != null) {
			buffer.append("<dt>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buffer.append(tag); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buffer.append("</dt>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buffer.append("<dd>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buffer.append(content); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buffer.append("</dd>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
	}

	private void printRest(StringBuilder buffer) {
		if ( !fRest.isEmpty()) {
			Iterator<Pair> e= fRest.iterator();
			while (e.hasNext()) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				Pair p= e.next();
				buffer.append("<dt>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				if (p.fTag != null)
					buffer.append(p.fTag); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				buffer.append("</dt>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				buffer.append("<dd>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				if (p.fContent != null)
					buffer.append(p.fContent); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				buffer.append("</dd>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			}
		}
	}

	private String printSimpleTag() {
		StringBuilder buffer= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		buffer.append("<dl>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_see_section, fSees, false);
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_parameters_section, fParameters, true);
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_returns_section, fReturn);
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_throws_section, fExceptions, false);
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_author_section, fAuthors, false);
		print(buffer, JavaDocMessages.JavaDoc2HTMLTextReader_since_section, fSince, false);
		printRest(buffer);
		buffer.append("</dl>"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

		return buffer.toString();
	}

	private void handleTag(String tag, String tagContent) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		tagContent= tagContent.trim();

		if (tag != null) {
			switch (tag) {
				case TagElement.TAG_PARAM:
					fParameters.add(tagContent);
					return;
				case TagElement.TAG_RETURN:
					fReturn= tagContent;
					return;
				case TagElement.TAG_EXCEPTION:
				case TagElement.TAG_THROWS:
					fExceptions.add(tagContent);
					return;
				case TagElement.TAG_AUTHOR:
					fAuthors.add(substituteQualification(tagContent));
					return;
				case TagElement.TAG_SEE:
					fSees.add(substituteQualification(tagContent));
					return;
				case TagElement.TAG_SINCE:
					fSince.add(substituteQualification(tagContent));
					return;
				default:
					break;
			}
		}

		if (tagContent != null) {
			fRest.add(new Pair(tag, tagContent));
		}
	}

	/*
	 * A '@' has been read. Process a javadoc tag
	 */
	private String processSimpleTag() throws IOException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD

		fParameters= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		fExceptions= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		fAuthors= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		fSees= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		fSince= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		fRest= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

		StringBuilder buffer= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		int c= '@';
		while (c != -1) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP

			buffer.setLength(0);
			buffer.append((char) c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			c= getTag(buffer);
			String tag= buffer.toString();

			buffer.setLength(0);
			if (c != -1) {
				c= getContentUntilNextTag(buffer);
			}

			handleTag(tag, buffer.toString());
		}

		return printSimpleTag();
	}

	private String printBlockTag(String tag, String tagContent) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD

		if (tag != null) {
			switch (tag) {
				case TagElement.TAG_LINK:
				case TagElement.TAG_LINKPLAIN:
					char[] contentChars= tagContent.toCharArray();
					boolean inParentheses= false;
					int labelStart= 0;
					for (int i= 0; i < contentChars.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
						char nextChar= contentChars[i];

						// tagContent always has a leading space
						if (i == 0 && Character.isWhitespace(nextChar)) {
							labelStart= 1;
							continue;
						}

						if (nextChar == '(') {
							inParentheses= true;
							continue;
						}

						if (nextChar == ')') {
							inParentheses= false;
							continue;
						}

						// Stop at first whitespace that is not in parentheses
						if (!inParentheses && Character.isWhitespace(nextChar)) {
							labelStart= i+1;
							break;
						}
					}
					if (TagElement.TAG_LINK.equals(tag))
						return "<code>" + substituteQualification(tagContent.substring(labelStart)) + "</code>";  //$NON-NLS-1$//$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
					else
						return substituteQualification(tagContent.substring(labelStart));
				case TagElement.TAG_LITERAL:
					return printLiteral(tagContent);
				case TagElement.TAG_CODE:
					return "<code>" + printLiteral(tagContent) + "</code>"; //$NON-NLS-1$//$NON-NLS-2$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
				default:
					break;
			}
		}

		// If something went wrong at least replace the {} with the content
		return substituteQualification(tagContent);
	}

	private String printLiteral(String tagContent) {
		int contentStart= 0;
		for (int i= 0; i < tagContent.length(); i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (! Character.isWhitespace(tagContent.charAt(i))) {
				contentStart= i;
				break;
			}
		}
		return HTMLPrinter.convertToHTMLContent(tagContent.substring(contentStart));
	}

	/*
	 * A '{' has been read. Process a block tag
	 */
	private String processBlockTag() throws IOException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD

		int c= nextChar();

		if (c != '@') {
			StringBuilder buffer= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
			buffer.append('{'); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buffer.append((char) c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			return buffer.toString();
		}

		StringBuilder buffer= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		if (c != -1) {

			buffer.setLength(0);
			buffer.append((char) c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL

			c= getTag(buffer);
			String tag= buffer.toString();

			buffer.setLength(0);
			if (c != -1 && c != '}') {
				buffer.append((char) c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				c= getContent(buffer, '}');
			}

			return printBlockTag(tag, buffer.toString());
		}

		return null;
	}

	/*
	 * @see SubstitutionTextReaderr#computeSubstitution(int)
	 */
	@Override
	protected String computeSubstitution(int c) throws IOException {
		if (c == '@' && fWasWhiteSpace)
			return processSimpleTag();

		if (c == '{')
			return processBlockTag();

		return null;
	}
}
