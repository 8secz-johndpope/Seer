 /*******************************************************************************
  * Copyright (c) 2010 SAP AG.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Mathias Kinzler (SAP AG) - initial implementation
  *******************************************************************************/
 package org.eclipse.egit.ui.internal.repository;
 
 import java.io.IOException;
 import java.util.Map.Entry;
 
 import org.eclipse.core.runtime.CoreException;
 import org.eclipse.core.runtime.IProgressMonitor;
 import org.eclipse.egit.core.op.BranchOperation;
 import org.eclipse.egit.ui.UIText;
 import org.eclipse.egit.ui.internal.ValidationUtils;
 import org.eclipse.jface.dialogs.Dialog;
 import org.eclipse.jface.dialogs.IInputValidator;
 import org.eclipse.jface.layout.GridDataFactory;
 import org.eclipse.jface.wizard.WizardPage;
 import org.eclipse.jgit.lib.Constants;
 import org.eclipse.jgit.lib.ObjectId;
 import org.eclipse.jgit.lib.Ref;
 import org.eclipse.jgit.lib.RefUpdate;
 import org.eclipse.jgit.lib.Repository;
 import org.eclipse.jgit.revwalk.RevCommit;
 import org.eclipse.jgit.revwalk.RevWalk;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.events.ModifyEvent;
 import org.eclipse.swt.events.ModifyListener;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.layout.GridLayout;
 import org.eclipse.swt.widgets.Button;
 import org.eclipse.swt.widgets.Combo;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Text;
 
 /**
  * Allows to create a new local branch based on another branch or commit.
  * <p>
  * If the base is a branch, the source branch can be selected using a drop down.
  */
 class CreateBranchPage extends WizardPage {
 
 	private final boolean commitMode;
 
 	private final Repository myRepository;
 
 	private final IInputValidator myValidator;
 
 	private final Ref myBaseBranch;
 
 	private final RevCommit myBaseCommit;
 
 	private Text nameText;
 
 	private Button checkout;
 
 	private Combo branchCombo;
 
 	/**
 	 * Constructs this page.
 	 * <p>
 	 * If a base branch is provided, the drop down will be selected accordingly
 	 *
 	 * @param repo
 	 *            the repository
 	 * @param baseBranch
 	 *            the branch to base the new branch on, may be null
 	 */
 	public CreateBranchPage(Repository repo, Ref baseBranch) {
 		super(CreateBranchPage.class.getName());
 		commitMode = false;
 		this.myRepository = repo;
 		this.myBaseBranch = baseBranch;
 		this.myBaseCommit = null;
 		this.myValidator = ValidationUtils.getRefNameInputValidator(
 				myRepository, Constants.R_HEADS, true);
 		setTitle(UIText.CreateBranchPage_Title);
 		setMessage(UIText.CreateBranchPage_ChooseBranchAndNameMessage);
 	}
 
 	/**
 	 * Constructs this page.
 	 * <p>
 	 * If a base branch is provided, the drop down will be selected accordingly
 	 *
 	 * @param repo
 	 *            the repository
 	 * @param baseCommit
 	 *            the commit to base the new branch on, must not be null
 	 */
 	public CreateBranchPage(Repository repo, RevCommit baseCommit) {
 		super(CreateBranchPage.class.getName());
 		commitMode = true;
 		this.myRepository = repo;
 		this.myBaseBranch = null;
 		this.myBaseCommit = baseCommit;
 		this.myValidator = ValidationUtils.getRefNameInputValidator(
 				myRepository, Constants.R_HEADS, true);
 		setTitle(UIText.CreateBranchPage_Title);
 		setMessage(UIText.CreateBranchPage_ChooseNameMessage);
 	}
 
 	public void createControl(Composite parent) {
 		Composite main = new Composite(parent, SWT.NONE);
 		main.setLayout(new GridLayout(3, false));
 
 		Label sourceLabel = new Label(main, SWT.NONE);
 		if (commitMode) {
 			sourceLabel.setText(UIText.CreateBranchPage_SourceCommitLabel);
 			sourceLabel
 					.setToolTipText(UIText.CreateBranchPage_SourceCommitTooltip);
 
 		} else {
 			sourceLabel.setText(UIText.CreateBranchPage_SourceBranchLabel);
 			sourceLabel
 					.setToolTipText(UIText.CreateBranchPage_SourceBranchTooltip);
 		}
 		this.branchCombo = new Combo(main, SWT.READ_ONLY | SWT.DROP_DOWN);
 
 		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(
 				this.branchCombo);
 
 		if (commitMode) {
 			this.branchCombo.add(myBaseCommit.name());
 			this.branchCombo.setText(myBaseCommit.name());
 			this.branchCombo.setEnabled(false);
 		} else {
 			try {
 				for (Entry<String, Ref> ref : myRepository.getRefDatabase()
 						.getRefs(Constants.R_HEADS).entrySet()) {
 					if (!ref.getValue().isSymbolic())
 						this.branchCombo.add(ref.getValue().getName());
 				}
 				for (Entry<String, Ref> ref : myRepository.getRefDatabase()
 						.getRefs(Constants.R_REMOTES).entrySet()) {
 					if (!ref.getValue().isSymbolic())
 						this.branchCombo.add(ref.getValue().getName());
 				}
 				for (Entry<String, Ref> ref : myRepository.getRefDatabase()
 						.getRefs(Constants.R_TAGS).entrySet()) {
 					if (!ref.getValue().isSymbolic())
 						this.branchCombo.add(ref.getValue().getName());
 				}
 
 			} catch (IOException e1) {
 				// ignore here
 			}
 
 			this.branchCombo.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent e) {
 					checkPage();
 				}
 			});
 			// select the current branch in the drop down
 			if (myBaseBranch != null) {
 				this.branchCombo.setText(myBaseBranch.getName());
 			}
 		}
 
 		Label nameLabel = new Label(main, SWT.NONE);
 		nameLabel.setText(UIText.CreateBranchPage_BranchNameLabel);
 
 		// we visualize the prefix here
 		Text prefix = new Text(main, SWT.NONE);
 		prefix.setText(Constants.R_HEADS);
 		prefix.setEnabled(false);
 
 		nameText = new Text(main, SWT.BORDER);
 		// enable testing with SWTBot
 		nameText.setData("org.eclipse.swtbot.widget.key", "BranchName"); //$NON-NLS-1$ //$NON-NLS-2$
 		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameText);
 
 		boolean isBare = myRepository.isBare();
 		checkout = new Button(main, SWT.CHECK);
 		checkout.setText(UIText.CreateBranchPage_CheckoutButton);
 		// most of the time, we probably will check this out
 		// unless we have a bare repository which doesn't allow
 		// check out at all
 		checkout.setSelection(!isBare);
 		checkout.setEnabled(!isBare);
 		checkout.setVisible(!isBare);
 		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(
 				checkout);
 		checkout.addSelectionListener(new SelectionAdapter() {
 			@Override
 			public void widgetSelected(SelectionEvent e) {
 				checkPage();
 			}
 		});
 
 		Dialog.applyDialogFont(main);
 		setControl(main);
 		nameText.setFocus();
 		if (myBaseBranch != null
 				&& (myBaseBranch.getName().startsWith(Constants.R_REMOTES) || myBaseBranch
 						.getName().startsWith(Constants.R_TAGS))) {
 			// additional convenience: the last part of the name is suggested
 			// as name for the local branch
 			nameText.setText(myBaseBranch.getName().substring(
 					myBaseBranch.getName().lastIndexOf('/') + 1));
 			checkPage();
 		} else {
 			// in any case, we will have to enter the name
 			setPageComplete(false);
 		}
 		// add the listener just now to avoid unneeded checkPage()
 		nameText.addModifyListener(new ModifyListener() {
 			public void modifyText(ModifyEvent e) {
 				checkPage();
 			}
 		});
 	}
 
 	private void checkPage() {
 		setErrorMessage(null);
 		try {
 			if (branchCombo.getText().length() == 0) {
 				setErrorMessage(UIText.CreateBranchPage_MissingSourceMessage);
 				return;
 			}
 			if (nameText.getText().length() == 0) {
 				setErrorMessage(UIText.CreateBranchPage_ChooseNameMessage);
 				return;
 			}
 			String message = this.myValidator.isValid(nameText.getText());
 			if (message != null) {
 				setErrorMessage(message);
 				return;
 			}
 		} finally {
 			setPageComplete(getErrorMessage() == null);
 		}
 	}
 
 	private String getBranchName() {
 		return Constants.R_HEADS + nameText.getText();
 	}
 
 	private String getSourceBranchName() {
 		if (commitMode)
 			return myBaseCommit.name();
		if (myBaseBranch != null)
			return myBaseBranch.getName();
		else if (this.branchCombo != null)
 			return this.branchCombo.getText();
 		else
 			return null;
 	}
 
 	/**
 	 * @param monitor
 	 * @throws CoreException
 	 * @throws IOException
 	 */
 	public void createBranch(IProgressMonitor monitor) throws CoreException,
 			IOException {
 		monitor.beginTask(UIText.CreateBranchPage_CreatingBranchMessage,
 				IProgressMonitor.UNKNOWN);
 
 		String newRefName = getBranchName();
 
 		RefUpdate updateRef = myRepository.updateRef(newRefName);
 		ObjectId startAt;
 		if (commitMode)
 			startAt = myBaseCommit.getId();
 		else
 			startAt = new RevWalk(myRepository).parseCommit(myRepository
 					.resolve(getSourceBranchName()));
 
 		updateRef.setNewObjectId(startAt);
 		updateRef.setRefLogMessage(
 				"branch: Created from " + getSourceBranchName(), false); //$NON-NLS-1$
 		updateRef.update();
 
 		if (checkout.getSelection()) {
 			if (monitor.isCanceled())
 				return;
 			monitor.beginTask(UIText.CreateBranchPage_CheckingOutMessage,
 					IProgressMonitor.UNKNOWN);
 			new BranchOperation(myRepository, getBranchName()).execute(monitor);
 		}
 	}
 }
