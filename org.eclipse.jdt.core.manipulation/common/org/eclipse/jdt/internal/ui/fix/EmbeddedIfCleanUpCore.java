/*******************************************************************************
 * Copyright (c) 2020 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperationWithSourceRange;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that raises embedded if into parent if.
 */
public class EmbeddedIfCleanUpCore extends AbstractMultiFix implements ICleanUpFix {
	public EmbeddedIfCleanUpCore() {
		this(Collections.emptyMap());
	}

	public EmbeddedIfCleanUpCore(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.RAISE_EMBEDDED_IF);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.RAISE_EMBEDDED_IF)) {
			return new String[] { MultiFixMessages.EmbeddedIfCleanup_description };
		}

		return new String[0];
	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public String getPreview() {
		if (isEnabled(CleanUpConstants.RAISE_EMBEDDED_IF)) {
			return """
				if (isActive && isValid) {
				  int i = 0;
				}


				"""; //$NON-NLS-1$
		}

		return """
			if (isActive) {
			  if (isValid) {
			    int i = 0;
			  }
			}
			"""; //$NON-NLS-1$
	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.RAISE_EMBEDDED_IF)) {
			return null;
		}

		final List<CompilationUnitRewriteOperationWithSourceRange> rewriteOperations= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW

		unit.accept(new ASTVisitor() {
			@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
			public boolean visit(final IfStatement visited) {
				if (visited.getElseStatement() == null) {
					IfStatement innerIf= ASTNodes.as(visited.getThenStatement(), IfStatement.class);

					if (innerIf != null
							&& innerIf.getElseStatement() == null
							&& ASTNodes.getNbOperands(visited.getExpression()) + ASTNodes.getNbOperands(innerIf.getExpression()) < ASTNodes.EXCESSIVE_OPERAND_NUMBER) {
						// The parsing crashes when there are two embedded lone ifs with an end of line comment at the right of the statement
						// So we disable the rule on double lone if
						if (!(visited.getThenStatement() instanceof Block)
								&& !(innerIf.getThenStatement() instanceof Block)) {
							return true;
						}

						rewriteOperations.add(new EmbeddedIfOperation(visited, innerIf));
						return false;
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFixCore(MultiFixMessages.EmbeddedIfCleanup_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperationWithSourceRange[0]));
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		return null;
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class EmbeddedIfOperation extends CompilationUnitRewriteOperationWithSourceRange {
		private final IfStatement visited;
		private final IfStatement innerIf;

		public EmbeddedIfOperation(final IfStatement visited, final IfStatement innerIf) {
			this.visited= visited;
			this.innerIf= innerIf;
		}

		@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		public void rewriteASTInternal(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.EmbeddedIfCleanup_description, cuRewrite);

			InfixExpression infixExpression= ast.newInfixExpression();
			infixExpression.setLeftOperand(ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodes.createMoveTarget(rewrite, visited.getExpression())));
			infixExpression.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
			infixExpression.setRightOperand(ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodes.createMoveTarget(rewrite, innerIf.getExpression())));
			ASTNode visitedNode= visited.getThenStatement() instanceof Block ? visited.getThenStatement() : visited.getExpression();
			int offset= visited.getThenStatement() instanceof Block ? 1 : visited.getExpression().getLength();
			List<Comment> visitedLineComments= ASTNodes.getTrailingLineComments(visitedNode, offset);
			if (!visitedLineComments.isEmpty()) {
				ASTNode innerNode= innerIf.getThenStatement() instanceof Block ? innerIf.getThenStatement() : innerIf.getExpression();
				int innerOffset= innerIf.getThenStatement() instanceof Block ? 1 : visited.getExpression().getLength();
				List<Comment> innerComments= ASTNodes.getTrailingLineComments(innerNode, innerOffset);
				StringBuilder blockBuilder= new StringBuilder("{"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
				CompilationUnit root= cuRewrite.getRoot();
				IBuffer buffer= cuRewrite.getCu().getBuffer();
				appendComments(innerComments, blockBuilder, buffer);
				appendComments(visitedLineComments, blockBuilder, buffer);
				blockBuilder.append("\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				String fIndent= "\t"; //$NON-NLS-1$
				IJavaElement rootElement= root.getJavaElement();
				if (rootElement != null) {
					IJavaProject project= rootElement.getJavaProject();
					if (project != null) {
						String tab_option= project.getOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, true);
						if (JavaCore.SPACE.equals(tab_option)) {
							fIndent= ""; //$NON-NLS-1$
							for (int i= 0; i < CodeFormatterUtil.getTabWidth(project); ++i) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
								fIndent += " "; //$NON-NLS-1$
							}
						}
					}
				}
				if (innerIf.getThenStatement() instanceof Block innerBlock) {
					ListRewrite innerBlockStmts= rewrite.getListRewrite(innerBlock, Block.STATEMENTS_PROPERTY);
					List<Statement> originalList= innerBlockStmts.getOriginalList();
					for (Statement stmt : originalList) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
						int startOffset= root.getExtendedStartPosition(stmt);
						int endLength= root.getExtendedLength(stmt);
						String text= buffer.getText(startOffset,  endLength);
						String[] textLines= text.split("\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M12 REGEX_PRECOMPUTE CALL
						for (String textLine : textLines) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
							blockBuilder.append(fIndent + textLine.trim() + "\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
						}
					}
				} else {
					int startOffset= root.getExtendedStartPosition(innerIf.getThenStatement());
					int endLength= root.getExtendedLength(innerIf.getThenStatement());
					String text= buffer.getText(startOffset,  endLength);
					String[] textLines= text.split("\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M12 REGEX_PRECOMPUTE CALL
					for (String textLine : textLines) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
						blockBuilder.append(fIndent + textLine.trim() + "\n"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					}
				}
				blockBuilder.append("}"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				Block newBlock= (Block) rewrite.createStringPlaceholder(blockBuilder.toString(), ASTNode.BLOCK);
				rewrite.replace(innerIf.getThenStatement(), newBlock, group);
			}
			ASTNodes.replaceButKeepComment(rewrite, innerIf.getExpression(), infixExpression, group);
			ASTNodes.replaceButKeepComment(rewrite, visited, ASTNodes.createMoveTarget(rewrite, innerIf), group);
		}

		private void appendComments(List<Comment> commentList, StringBuilder buffer, IBuffer cuBuffer) {
			for (Comment innerComment : commentList) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				if (innerComment instanceof LineComment lineComment) {
					buffer.append(" "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					buffer.append(cuBuffer.getText(lineComment.getStartPosition(), lineComment.getLength())); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				} else if (innerComment instanceof BlockComment blockComment) {
					buffer.append(" //"); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
					buffer.append(cuBuffer.getText(blockComment.getStartPosition(), blockComment.getLength() - 2)); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				}
			}
		}
	}
}
