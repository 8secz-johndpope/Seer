 /*******************************************************************************
 * Copyright (c) 2010, 2013 Tasktop Technologies and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Tasktop Technologies - initial API and implementation
  *     Jan Lohre (SAP) - improvements
  *     Sascha Scholz (SAP) - improvements
  *******************************************************************************/
 
 package org.eclipse.mylyn.reviews.ui.spi.editor;
 
import java.util.ArrayList;
 import java.util.List;
 import java.util.Map.Entry;
 
 import org.apache.commons.lang.StringUtils;
 import org.eclipse.jface.layout.GridDataFactory;
 import org.eclipse.jface.layout.GridLayoutFactory;
 import org.eclipse.mylyn.commons.ui.CommonImages;
 import org.eclipse.mylyn.internal.reviews.ui.ReviewsImages;
 import org.eclipse.mylyn.reviews.core.model.IApprovalType;
 import org.eclipse.mylyn.reviews.core.model.IChange;
 import org.eclipse.mylyn.reviews.core.model.IRequirementEntry;
 import org.eclipse.mylyn.reviews.core.model.IReview;
 import org.eclipse.mylyn.reviews.core.model.IReviewerEntry;
 import org.eclipse.mylyn.reviews.core.model.IUser;
 import org.eclipse.mylyn.reviews.core.model.RequirementStatus;
 import org.eclipse.mylyn.reviews.ui.spi.factories.AbstractUiFactoryProvider;
 import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
 import org.eclipse.osgi.util.NLS;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.custom.CLabel;
 import org.eclipse.swt.events.SelectionAdapter;
 import org.eclipse.swt.events.SelectionEvent;
 import org.eclipse.swt.widgets.Composite;
 import org.eclipse.swt.widgets.Display;
 import org.eclipse.swt.widgets.Label;
 import org.eclipse.swt.widgets.Link;
 import org.eclipse.ui.forms.IFormColors;
 import org.eclipse.ui.forms.widgets.ExpandableComposite;
 import org.eclipse.ui.forms.widgets.FormToolkit;
 import org.eclipse.ui.forms.widgets.Section;
 
 /**
  * Displays basic information about a given review corresponding to top sections of Gerrit web interface.
  * 
  * @author Steffen Pingel
  * @author Miles Parker
  */
 public abstract class ReviewDetailSection extends AbstractReviewSection {
 
 	public ReviewDetailSection() {
 		setPartName("Review");
 	}
 
 	@Override
 	public void createModelContent() {
 		createReviewersSubSection(composite);
 		createDependenciesSubSection(toolkit, composite, "Depends On", getReview().getParents());
 		createDependenciesSubSection(toolkit, composite, "Needed By", getReview().getChildren());
 	}
 
 	protected void createReviewersSubSection(Composite parent) {
 		if (getReview().getReviewerApprovals().isEmpty() && !canAddReviewers()) {
 			return;
 		}
 
 		int style = ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT
 				| ExpandableComposite.LEFT_TEXT_CLIENT_ALIGNMENT | ExpandableComposite.EXPANDED;
 
 		final Section subSection = toolkit.createSection(parent, style);
 		GridDataFactory.fillDefaults().grab(true, false).applyTo(subSection);
 		subSection.setTitleBarForeground(toolkit.getColors().getColor(IFormColors.TITLE));
 		subSection.setText("Reviewers");
 
 		Composite composite = toolkit.createComposite(subSection);
		List<IApprovalType> approvalTypes = getModelRepository().getApprovalTypes();
		List<IApprovalType> approvalTypesWithLabel = new ArrayList<IApprovalType>(approvalTypes.size());
		for (IApprovalType approvalType : approvalTypes) {
			if (!approvalType.getKey().equals(approvalType.getName())) {
				approvalTypesWithLabel.add(approvalType);
			}
		}

		int numColumns = approvalTypesWithLabel.size() + 1;
 		GridLayoutFactory.fillDefaults()
 				.numColumns(numColumns)
 				.extendedMargins(0, 0, 0, 5)
 				.equalWidth(true)
 				.spacing(4, 5)
 				.applyTo(composite);
 		subSection.setClient(composite);
 
		if (!approvalTypesWithLabel.isEmpty()) {
 			StringBuilder names = new StringBuilder();
 
 			Label headerLabel = new Label(composite, SWT.NONE);
 			headerLabel.setText(" "); //$NON-NLS-1$
 			StringBuilder needs = new StringBuilder();
 
			for (IApprovalType approvalType : approvalTypesWithLabel) {
 				IRequirementEntry requirementEntry = getReview().getRequirements().get(approvalType);
 				Composite headerContainer = new Composite(composite, SWT.NONE);
 				headerContainer.setForeground(toolkit.getColors().getColor(IFormColors.TB_BG));
 				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(headerContainer);
 				GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(headerContainer);
 				CLabel approvalHeaderLabel = new CLabel(headerContainer, SWT.NONE);
 				approvalHeaderLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
 				approvalHeaderLabel.setText(approvalType.getName());
 				GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(approvalHeaderLabel);
 				RequirementStatus status = null;
 				if (requirementEntry != null) {
 					status = requirementEntry.getStatus();
 					switch (status) {
 					case SATISFIED:
 						approvalHeaderLabel.setImage(CommonImages.getImage(ReviewsImages.APPROVED));
 						break;
 					case NOT_SATISFIED:
 						approvalHeaderLabel.setImage(CommonImages.getImage(ReviewsImages.UNKNOWN));
 						break;
 					case REJECTED:
 						approvalHeaderLabel.setImage(CommonImages.getImage(ReviewsImages.REJECTED));
 						break;
 					default:
 						//To ensure that label is aligned properly
 						approvalHeaderLabel.setImage(CommonImages.getImage(ReviewsImages.BLANK));
 						break;
 					}
 				}
 				if (status != null && (status == RequirementStatus.UNKNOWN || status == RequirementStatus.REJECTED)) {
 					if (needs.length() > 0) {
 						needs.append(", "); //$NON-NLS-1$
 					}
 					needs.append(approvalType.getName());
 				}
 			}
 
 			for (Entry<IUser, IReviewerEntry> entry : getReview().getReviewerApprovals().entrySet()) {
 
 				Label reviewerRowLabel = new Label(composite, SWT.NONE);
 				reviewerRowLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
 				reviewerRowLabel.setText(entry.getKey().getDisplayName());
 
				for (IApprovalType approvalType : approvalTypesWithLabel) {
 					Integer value = entry.getValue().getApprovals().get(approvalType);
 					Label approvalValueLabel = new Label(composite, SWT.NONE);
 					GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(approvalValueLabel);
 					String rankingText = " ";
 					if (value != null && value != 0) {
 						if (value > 0) {
 							rankingText += "+";
 							approvalValueLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
 						} else if (value < 0) {
 							approvalValueLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
 						}
 						rankingText += value;
 						approvalValueLabel.setToolTipText(value + "  " + approvalType.getName());
 					}
 					approvalValueLabel.setText(rankingText);
 				}
 				if (names.length() > 0) {
 					names.append(", "); //$NON-NLS-1$
 				}
 				names.append(entry.getKey().getDisplayName());
 			}
 
 			String headerText = names.toString();
 			if (needs.length() > 0) {
 				headerText += " (needs " + needs.toString() + ")";
 			}
 			if (headerText.length() > 0) {
 				addTextClient(toolkit, subSection, headerText);
 			}
 		}
 		if (getUiFactoryProvider() != null) {
 			Composite actionComposite = getUiFactoryProvider().createButtons(this, composite, getToolkit(), getReview());
 			GridDataFactory.fillDefaults().span(2, 1).applyTo(actionComposite);
 		}
 	}
 
 	protected boolean canAddReviewers() {
 		return true;
 	}
 
 	protected void createDependenciesSubSection(final FormToolkit toolkit, final Composite parent, String title,
 			List<IChange> changes) {
 		if (changes.isEmpty()) {
 			return;
 		}
 
 		int style = ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT;
 
 		final Section subSection = toolkit.createSection(parent, style);
 		GridDataFactory.fillDefaults().grab(true, false).applyTo(subSection);
 		subSection.setTitleBarForeground(toolkit.getColors().getColor(IFormColors.TITLE));
 		subSection.setText(title);
 
 		Composite composite = toolkit.createComposite(subSection);
 		GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 5).applyTo(composite);
 		subSection.setClient(composite);
 
 		for (final IChange change : changes) {
 			Link link = new Link(composite, SWT.NONE);
 			String changeStatus = change.getState() != null ? NLS.bind(" ({0})",
 					String.valueOf(change.getState().getName())) : " ";
 			String ownerName = change.getOwner().getDisplayName();
 			link.setText(NLS.bind("<a>{0}</a>: {1} {3} by {2}", new String[] { StringUtils.left(change.getKey(), 9),
 					change.getSubject(), ownerName, changeStatus }));
 			link.addSelectionListener(new SelectionAdapter() {
 				@Override
 				public void widgetSelected(SelectionEvent e) {
 					TasksUiUtil.openTask(getTaskEditorPage().getTaskRepository(), change.getId() + ""); //$NON-NLS-1$
 				}
 			});
 		}
 	}
 
 	protected abstract AbstractUiFactoryProvider<IReview> getUiFactoryProvider();
 
 	@Override
 	protected boolean shouldExpandOnCreate() {
 		return true;
 	}
 }
