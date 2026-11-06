/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - moved to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;


/**
 * Rewrite helper for {@link Statement}s. Ensures that
 * the replacement nodes are enclosed in a Block if necessary.
 *
 * see JDTUIHelperClasses
 */
public class StatementRewrite extends ReplaceRewrite {

	public StatementRewrite(ASTRewrite rewrite, ASTNode[] nodes) {
		super(rewrite, nodes);
	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	protected void handleOneMany(ASTNode[] replacements, TextEditGroup description) {
		AST ast= fToReplace[0].getAST();
		// to replace == 1, but more than one replacement. Have to check if we
		// need to insert a block to not change structure
		if (ASTNodes.isControlStatementBody(fDescriptor)) {
			Block block= ast.newBlock();
			ListRewrite statements= fRewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
			for (ASTNode replacement : replacements) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				statements.insertLast(replacement, description);
			}
			fRewrite.replace(fToReplace[0], block, description);
		} else {
			ListRewrite container= fRewrite.getListRewrite(fToReplace[0].getParent(), (ChildListPropertyDescriptor)fDescriptor);
			container.replace(fToReplace[0], replacements[0], description);
			for (int i= 1; i < replacements.length; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				container.insertAfter(replacements[i], replacements[i - 1], description);
			}
		}
	}
}
