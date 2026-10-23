/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.jeview.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;


public class TreeCopyAction extends Action {

	private static class TreeObject {
		private final TreeItem fTreeItem;
		private boolean fSelected;
		private final List<TreeObject> fChildren;
		public TreeObject(TreeItem element, boolean selected) {
			fTreeItem= element;
			fSelected= selected;
			fChildren= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		}
		public void setSelected() {
			fSelected= true;
		}
		public void addChild(TreeObject child) {
			fChildren.add(child);
		}
		public boolean isSelected() {
			return fSelected;
		}
		public TreeItem getTreeItem() {
			return fTreeItem;
		}
		public List<TreeObject> getChildren() {
			return fChildren;
		}
		@Override
		public String toString() {
			StringBuilder buf= new StringBuilder(); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
			if (fSelected)
				buf.append("* "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			buf.append(trim(fTreeItem.getText())).append(" ["); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			for (int i= 0; i < fChildren.size(); i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				TreeObject child= fChildren.get(i);
				buf.append(trim(child.getTreeItem().getText())); //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
				if (i > 0)
					buf.append(", "); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
			}
			return buf.append("]").toString(); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE CALL
		}
		private String trim(String string) {
			if (string.length() > 60)
				return string.substring(0, 60) + "..."; //$NON-NLS-1$ //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
			else
				return string;
		}
	}

	private final Tree[] fTrees;

	public TreeCopyAction(Tree[] trees) {
		fTrees= trees;
		setText("&Copy"); //$NON-NLS-1$
		setToolTipText("Copy to Clipboard"); //$NON-NLS-1$
		setEnabled(false);
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setId(ActionFactory.COPY.getId());
		setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
	}

	@Override //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
	public void run() {
		Tree tree= null;
		for (Tree fTree : fTrees) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (fTree.isFocusControl()) {
				tree= fTree;
				break;
			}
		}
		if (tree == null)
			return;

		TreeItem[] selection= tree.getSelection();
		if (selection.length == 0)
			return;

		Clipboard clipboard= null;
		try {
			clipboard= new Clipboard(tree.getDisplay());
			if (selection.length == 1) {
				clipboard.setContents(new Object[]{selection[0].getText()}, new Transfer[]{TextTransfer.getInstance()});
			} else if (selection.length > 1) {
				copyTree(selection, clipboard);
			}
		} finally {
			if (clipboard != null)
				clipboard.dispose();
		}
	}

	private void copyTree(TreeItem[] selection, Clipboard clipboard) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		HashMap<TreeItem, TreeObject> elementToTreeObj= new HashMap<>(); //FLLDCIM: MARK — M01 M3_HASH_LANES NEW
		List<TreeObject> roots= new ArrayList<>(); //FLLDCIM: MARK — M02 M3_ARRAY_LANES NEW
		int indent= Integer.MIN_VALUE;

		for (TreeItem item : selection) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			TreeObject treeObj= elementToTreeObj.get(item);
			if (treeObj == null) {
				treeObj= new TreeObject(item, true);
				elementToTreeObj.put(item, treeObj);
			} else {
				treeObj.setSelected();
				continue;
			}
			// walk up to roots:
			int level= 0;
			item= item.getParentItem();
			while (item != null) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
				TreeObject parentTreeObj= elementToTreeObj.get(item);
				if (parentTreeObj == null) {
					parentTreeObj= new TreeObject(item, false);
					elementToTreeObj.put(item, parentTreeObj);
					parentTreeObj.addChild(treeObj);
					treeObj= parentTreeObj;
					item= item.getParentItem();
					level--;
				} else {
					parentTreeObj.addChild(treeObj);
					treeObj= parentTreeObj;
					break;
				}
			}
			if (item == null) {
				roots.add(treeObj);
				indent= Math.max(indent, level);
			}
		}
		elementToTreeObj= null;
		StringBuffer buf= new StringBuffer();
		appendSelectionObjects(buf, indent, roots);
		String result= buf.length() > 0 ? buf.substring(1) : buf.toString();
		clipboard.setContents(new Object[] { result }, new Transfer[] { TextTransfer.getInstance() });
	}

	private void appendSelectionObjects(StringBuffer buffer, int indent, List<TreeObject> selObjs) {
		for (TreeObject selObj : selObjs) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			if (selObj.isSelected()) {
				buffer.append('\n');
				for (int d= 0; d < indent; d++) //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
					buffer.append('\t');
				buffer.append(selObj.getTreeItem().getText());
			}
			appendSelectionObjects(buffer, indent + 1, selObj.getChildren());
		}
	}
}
