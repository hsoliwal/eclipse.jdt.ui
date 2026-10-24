/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
package org.eclipse.jsp;

import java.io.IOException;
import java.io.Reader;

public abstract class AbstractJspParser {

	private Reader fReader;
	private boolean fHasUnread;
	private int fUnread;
	private int fPos;
	protected int fLines= 1;

	AbstractJspParser() {
		super();
	}

	private int getc() throws IOException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		fPos++;
		if (fHasUnread) {
			fHasUnread= false;
			return fUnread;
		}

		int ch= fReader.read();

		if (ch == '\n') // LF
			fLines++;
		else if (ch == '\r')  { // CR
			int nextCh= getc();
			if (nextCh != '\n')
				fLines++;
			return nextCh;
		}
		return ch;
	}

	private void ungetc(int c) {
		fHasUnread= true;
		fUnread= c;
		fPos--;
	}

	private void parseDirective() throws IOException {
		StringBuilder sb= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		int pos= fPos;
		while (true) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			int c = getc();
			if (c == '%') {
				c= getc();
				if (c == '>') {
					// get attributes
					parseAttributes(pos, sb.toString());
					return;
				}
			}
			sb.append((char)c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
	}

	private void parseTag(boolean endTag) throws IOException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		StringBuilder sb= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		int pos= fPos;
		while (true) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			int c= getc();
			if (c == '/') {
				c= getc();
				if (c == '>') {
					// get attributes
					parseAttributes(pos, sb.toString());
					return;
				} else {
					ungetc(c);
				}
			} else if (c == '>') {
				// get attributes
				parseAttributes(pos, sb.toString());
				return;
			}
			sb.append((char)c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
	}

	private void parseComment() throws IOException {
		while (true) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			int c = getc();
			if (c == '-') {
				c= getc();
				if (c == '-') {
					c= getc();
					if (c == '%') {
						c= getc();
						if (c == '>') {
							return;
						}
					}
				}
			}
		}
	}

	private void parseJava(char type) throws IOException {
		StringBuilder sb= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		int line= fLines;
		while (true) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			int c = getc();
			if (c == '%') {
				c= getc();
				if (c == '>') {
					java(type, sb.toString(), line);
					return;
				}
			}
			sb.append((char)c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
	}

	protected void java(char tagType, String contents, int line) {
		// empty implementation
	}

	private void parseAttributes(int pos, String s) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD

		boolean hasValue= false;
		StringBuilder name= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		StringBuilder value= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		String startTag= ""; //$NON-NLS-1$
		int i= 0;
		int ix= 0;
		int startName= 0;
		int startValue= 0;
		char c= s.charAt(i++);

		try {
			while (true) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP

				// whitespace
				while (Character.isWhitespace(c)) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					c= s.charAt(i++);

				startName= i;
				while (Character.isLetterOrDigit(c) || c == ':') { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					name.append(c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					c= s.charAt(i++);
				}
				if (startName == i) {
					// avoid endless loop, e.g. for <!DOCTYPE html>
					return;
				}

				// whitespace
				while (Character.isWhitespace(c)) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					c= s.charAt(i++);

				hasValue= false;
				if (c == '=') {
					c= s.charAt(i++);

					// value
					while (Character.isWhitespace(c)) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
						c= s.charAt(i++);

					startValue= i;

					// Special handling for this taglib tag
					if ("c:out".equals(startTag))  { //$NON-NLS-1$
						value= value.append(s.substring(startValue, Math.max(startValue, s.length() - 2))); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
						name.setLength(0);
						tagAttribute(name.toString(), value.toString(), startName+pos, startValue+pos);
						break;
					} else if (c == '"') {
						c= s.charAt(i++);
						while (c != '"') { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
							value.append(c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
							c= s.charAt(i++);
						}
						c= s.charAt(i++);

					} else {
						while (Character.isLetterOrDigit(c)) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
							value.append(c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
							c= s.charAt(i++);
						}
					}
					hasValue= true;
				}

				if (ix == 0)  {
					startTag= name.toString();
					startTag(false, startTag, startName+pos);
				} else
					tagAttribute(name.toString(), hasValue ? value.toString() : null, startName+pos, startValue+pos);
				ix++;

				name.setLength(0);
				value.setLength(0);
			}
		} catch (StringIndexOutOfBoundsException e) {
			// we don't log this exception because it is used
			// as one way to exit the scanning loop
		}

		if (name.length() > 0) {
			if (ix == 0)
				startTag(false, name.toString(), startName+pos);
			else
				tagAttribute(name.toString(), hasValue ? value.toString() : null, startName+pos, startValue+pos);
		}

		endTag(false);
	}

	protected void startTag(boolean endTag, String name, int startName) {
		// empty implementation
	}

	protected void tagAttribute(String attrName, String value, int startName, int startValue) {
		// empty implementation
	}

	protected void endTag(boolean end) {
		// empty implementation
	}

	protected void text(String t, int line) {
		// empty implementation
	}

	void parse(Reader reader) throws IOException { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		int c;
		StringBuilder buffer= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		fPos= 0;
		fLines= 1;
		int line= fLines;
		fReader= reader;

		while (true) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			c= getc();
			switch (c) {
			case -1:
				if (buffer.length() > 0)
					text(buffer.toString(), line);
				return;
			case '<':
				c= getc();
				switch (c) {
				case '%':
					// flush buffer
					if (buffer.length() > 0) {
						text(buffer.toString(), line);
						buffer.setLength(0);
						line= fLines;
					}
					c= getc();
					switch (c) {
					case '-':
						c= getc();
						if (c == '-') {
							parseComment();
						} else {
							ungetc(c);
							continue;
						}
						break;
					case '@':
						parseDirective();
						break;
					case ' ':
					case '!':
					case '=':
						parseJava((char)c);
						break;
					default:
						parseComment();
						break;
					}
					break;
				case '/':
					parseTag(true);
					break;
				default:
					ungetc(c);
					parseTag(false);
					break;
				}
				break;

			default:
				buffer.append((char)c); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				break;
			}
		}
	}
}
