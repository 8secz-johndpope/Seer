 /*******************************************************************************
  * Copyright (c) 2004 - 2006 University Of British Columbia and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     University Of British Columbia - initial API and implementation
  *******************************************************************************/
 /*
  * Created on Apr 20, 2004
  */
 package org.eclipse.mylar.internal.tasks.ui;
 
 import java.net.MalformedURLException;
 import java.net.URL;
 
 import org.eclipse.jface.resource.ImageDescriptor;
 import org.eclipse.jface.resource.ImageRegistry;
 import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.graphics.Point;
 
 /**
  * @author Mik Kersten
  */
 public class TasksUiImages {
 
 	private static ImageRegistry imageRegistry;
 
 	private static final String T_ELCL = "elcl16";
 
 	private static final String T_EVIEW = "eview16";
 
 	private static final String T_TOOL = "etool16";
 
 	private static final String T_OBJ = "obj16";
 
 	private static final String T_WIZBAN = "wizban";
 
 	private static final String T_OVR = "ovr16";
 
 	public static final Point SIZE = new Point(16, 16);
 
	public static final Point SIZE_WIDE = new Point(26, 16);
 
 	private static final URL baseURL = TasksUiPlugin.getDefault().getBundle().getEntry("/icons/");
 
 	public static final ImageDescriptor TASK_ACTIVE = createSize(create(T_TOOL, "task-active.gif"), SIZE);
 
 	public static final ImageDescriptor TASK_ACTIVE_CENTERED = createSize(create(T_TOOL, "task-active-centered.gif"),
 			SIZE);
 
 	public static final ImageDescriptor TASK_INACTIVE = createSize(create(T_TOOL, "task-inactive.gif"), SIZE);
 
 	public static final ImageDescriptor TASK_INACTIVE_CONTEXT = createSize(create(T_TOOL, "task-context.gif"), SIZE);
 
 	public static final ImageDescriptor TASK_COMPLETE = create(T_TOOL, "task-complete.gif");
 
 	public static final ImageDescriptor TASK_INCOMPLETE = create(T_TOOL, "task-incomplete.gif");
 
 	public static final ImageDescriptor TASK = createSize(create(T_TOOL, "task.gif"), SIZE);
 
 	public static final ImageDescriptor TASK_COMPLETED = createSize(create(T_TOOL, "task-completed.gif"), SIZE);
 
 	public static final ImageDescriptor TASK_NOTES = createSize(create(T_TOOL, "task-notes.gif"), SIZE);
 
 	public static final ImageDescriptor TASK_NEW = create(T_TOOL, "task-new.gif");
 
 	public static final ImageDescriptor TASK_REPOSITORY_HISTORY = createSize(create(T_TOOL,
 			"task-repository-history.gif"), SIZE);
 
 	public static final ImageDescriptor TASK_REPOSITORY_NOTES = createSize(create(T_TOOL, "task-repository-notes.gif"),
 			SIZE);
 
 	public static final ImageDescriptor TASK_REPOSITORY_COMPLETED = createSize(create(T_TOOL,
 			"task-repository-completed.gif"), SIZE);
 
 	public static final ImageDescriptor TASK_REMOTE = createSize(create(T_TOOL, "task-remote.gif"), SIZE);
 
 	public static final ImageDescriptor TASKLIST = create("eview16", "task-list.gif");
 
 	public static final ImageDescriptor REPOSITORY = create("eview16", "repository.gif");
 
 	public static final ImageDescriptor REPOSITORY_SMALL = create(T_OBJ, "repository-small.gif");
 
 	public static final ImageDescriptor REPOSITORY_NEW = create("etool16", "repository-new.gif");
 
 	public static final ImageDescriptor REPOSITORIES = create("eview16", "repositories.gif");
 
 	public static final ImageDescriptor REMOVE = create(T_ELCL, "remove.gif");
 
 	public static final ImageDescriptor FILTER_COMPLETE = create(T_ELCL, "filter-complete.gif");
 
 	public static final ImageDescriptor FILTER_ARCHIVE = create(T_ELCL, "filter-archive.gif");
 
 	public static final ImageDescriptor FILTER_PRIORITY = create(T_ELCL, "filter-priority.gif");
 
 	public static final ImageDescriptor COLOR_PALETTE = create(T_ELCL, "color-palette.gif");
 
 	public static final ImageDescriptor FILTER = create(T_TOOL, "view-filter.gif");
 
 	public static final ImageDescriptor WARNING = create(T_ELCL, "warning.gif");
 
 	public static final ImageDescriptor OVERLAY_WEB = create(T_TOOL, "overlay-web.gif");
 
 	public static final ImageDescriptor BROWSER_SMALL = create(T_OBJ, "browser-small.gif");
 
 	public static final ImageDescriptor OVERLAY_SYNCHRONIZING = create(T_EVIEW, "overlay-synchronizing.gif");
 
 	public static final ImageDescriptor OVERLAY_HAS_DUE = create(T_EVIEW, "overlay-has-due.gif");
 
 	public static final ImageDescriptor OVERLAY_OVER_DUE = create(T_EVIEW, "overlay-over-due.gif");
 
 	public static final ImageDescriptor TASK_WEB = createWithOverlay(TASK, OVERLAY_WEB, false, true);
 
 	public static final ImageDescriptor TASK_WEB_REMOTE = create(T_TOOL, "overlay-web.gif");
 
 	public static final ImageDescriptor CATEGORY = createSize(create(T_TOOL, "category.gif"), SIZE);
 
 	public static final ImageDescriptor CATEGORY_NEW = create(T_TOOL, "category-new.gif");
 
 	public static final ImageDescriptor CATEGORY_ARCHIVE = createSize(create(T_TOOL, "category-archive.gif"), SIZE);
 
 	public static final ImageDescriptor TASK_REPOSITORY = createSize(create(T_TOOL, "task-repository.gif"), SIZE);
 
 	public static final ImageDescriptor TASK_REPOSITORY_NEW = create(T_TOOL, "task-repository-new.gif");
 
 	public static final ImageDescriptor COMMENT = create(T_TOOL, "comment.gif");
 
 	public static final ImageDescriptor PERSON = create(T_TOOL, "person.gif");
 
 	public static final ImageDescriptor CONTEXT_ATTACH = create(T_TOOL, "context-attach.gif");
 
 	public static final ImageDescriptor CONTEXT_RETRIEVE = create(T_TOOL, "context-retrieve.gif");
 
 	public static final ImageDescriptor CONTEXT_TRANSFER = create(T_TOOL, "context-transfer.gif");
 
 	public static final ImageDescriptor ATTACHMENT_PATCH = create(T_OBJ, "attachment-patch.gif");
 
 	public static final ImageDescriptor OVERLAY_INCOMMING = create(T_EVIEW, "overlay-incoming.gif");
 
 	public static final ImageDescriptor OVERLAY_INCOMMING_NEW = create(T_EVIEW, "overlay-incoming-new.gif");
 
 	public static final ImageDescriptor OVERLAY_OUTGOING = create(T_EVIEW, "overlay-outgoing.gif");
 
 	public static final ImageDescriptor OVERLAY_CONFLICT = create(T_EVIEW, "overlay-conflicting.gif");
 
 	public static final ImageDescriptor OVERLAY_REPOSITORY = create(T_EVIEW, "overlay-repository.gif");
 
 	public static final ImageDescriptor OVERLAY_REPOSITORY_CONTEXT = create(T_EVIEW, "overlay-repository-context.gif");
 
 	public static final ImageDescriptor OVERLAY_WARNING = create(T_OVR, "overlay-warning.gif");
 
 	public static final ImageDescriptor STATUS_NORMAL = create(T_EVIEW, "status-normal.gif");
 
 	public static final ImageDescriptor STATUS_CONTEXT = create(T_EVIEW, "status-server-context.gif");
 
 	public static final ImageDescriptor STATUS_NORMAL_INCOMING = createWithOverlay(STATUS_NORMAL, OVERLAY_INCOMMING,
 			true, true);
 
 	public static final ImageDescriptor STATUS_NORMAL_OUTGOING = createWithOverlay(STATUS_NORMAL, OVERLAY_OUTGOING,
 			true, true);
 
 	public static final ImageDescriptor STATUS_NORMAL_CONFLICT = createWithOverlay(STATUS_NORMAL, OVERLAY_CONFLICT,
 			true, true);
 
 	public static final ImageDescriptor STATUS_CONTEXT_INCOMING = createWithOverlay(STATUS_CONTEXT, OVERLAY_INCOMMING,
 			true, true);
 
 	public static final ImageDescriptor STATUS_CONTEXT_OUTGOING = createWithOverlay(STATUS_CONTEXT, OVERLAY_OUTGOING,
 			true, true);
 
 	public static final ImageDescriptor STATUS_CONTEXT_CONFLICT = createWithOverlay(STATUS_CONTEXT, OVERLAY_CONFLICT,
 			true, true);
 
 	public static final ImageDescriptor QUERY = createSize(create(T_TOOL, "query.gif"), SIZE);
 
 	public static final ImageDescriptor QUERY_NEW = create(T_TOOL, "query-new.gif");
 
 	public static final ImageDescriptor REPOSITORY_SYNCHRONIZE = create(T_TOOL, "repository-synchronize.gif");
 
 	public static final ImageDescriptor NAVIGATE_PREVIOUS = create(T_TOOL, "navigate-previous.gif");
 
 	public static final ImageDescriptor NAVIGATE_NEXT = create(T_TOOL, "navigate-next.gif");
 
 	public static final ImageDescriptor TASKLIST_MODE = create(T_TOOL, "tasklist-mode.gif");
 
 	public static final ImageDescriptor COPY = create(T_TOOL, "copy.png");
 
 	public static final ImageDescriptor GO_UP = create(T_TOOL, "go-up.gif");
 
 	public static final ImageDescriptor GO_INTO = create(T_TOOL, "go-into.gif");
 
 	public static final ImageDescriptor REFRESH = create(T_ELCL, "refresh.gif");
 
 	public static final ImageDescriptor REFRESH_SMALL = create(T_ELCL, "refresh-small.gif");
 
 	public static final ImageDescriptor COLLAPSE_ALL = create(T_ELCL, "collapseall.png");
 
 	public static final ImageDescriptor NOTIFICATION_CLOSE = create(T_EVIEW, "notification-close.gif");
 
 	public static final ImageDescriptor EXPAND_ALL = create(T_ELCL, "expandall.gif");
 
 	public static final ImageDescriptor REPLY = create(T_ELCL, "reply.gif");
 
 	public static final ImageDescriptor PRIORITY_1 = create(T_EVIEW, "priority-1.gif");
 
 	public static final ImageDescriptor PRIORITY_2 = create(T_EVIEW, "priority-2.gif");
 
 	public static final ImageDescriptor PRIORITY_3 = create(T_EVIEW, "priority-3.gif");
 
 	public static final ImageDescriptor PRIORITY_4 = create(T_EVIEW, "priority-4.gif");
 
 	public static final ImageDescriptor PRIORITY_5 = create(T_EVIEW, "priority-5.gif");
 
 	public static final ImageDescriptor CALENDAR = createSize(create(T_TOOL, "calendar.gif"), SIZE);
 
 	public static final ImageDescriptor CALENDAR_SMALL = create(T_OBJ, "calendar-small.gif");
 
 	public static final ImageDescriptor BANNER_REPOSITORY = create(T_WIZBAN, "banner-repository.gif");
 
 	public static final ImageDescriptor BANNER_REPOSITORY_SETTINGS = create(T_WIZBAN, "banner-repository-settings.gif");
 
 	public static final ImageDescriptor BANNER_REPOSITORY_CONTEXT = create(T_WIZBAN, "banner-repository-context.gif");
 
 	public static final ImageDescriptor BANNER_IMPORT = create(T_WIZBAN, "banner-import.gif");
 
 	public static final ImageDescriptor STATUS_WARNING = createWithOverlay(TasksUiImages.STATUS_NORMAL,
 			TasksUiImages.OVERLAY_WARNING, false, true);
 
 	public static final ImageDescriptor BLANK = create(T_ELCL, "blank.gif");
 
 	private static ImageDescriptor create(String prefix, String name) {
 		try {
 			return ImageDescriptor.createFromURL(makeIconFileURL(prefix, name));
 		} catch (MalformedURLException e) {
 			return ImageDescriptor.getMissingImageDescriptor();
 		}
 	}
 
 	private static ImageDescriptor createSize(ImageDescriptor base, Point size) {
 		return new TaskListImageDescriptor(base, size);
 	}
 
 	public static ImageDescriptor createWithOverlay(ImageDescriptor base, ImageDescriptor overlay, boolean top,
 			boolean left) {
 		return new TaskListImageDescriptor(base, overlay, top, left);
 	}
 
 	public static Image getImage(ImageDescriptor imageDescriptor) {
 		return getImage(imageDescriptor, false);
 	}
 
 	/**
 	 * Lazily initializes image map.
 	 */
 	public static Image getImage(ImageDescriptor imageDescriptor, boolean wide) {
 		String suffix = "";
 		if (wide) {
 			suffix = ".wide";
 		}
 
 		ImageRegistry imageRegistry = getImageRegistry();
 		Image image = imageRegistry.get(imageDescriptor.hashCode() + suffix);
 
 		if (image == null) {
 			if (wide && imageDescriptor instanceof TaskListImageDescriptor) {
 				((TaskListImageDescriptor) imageDescriptor).setImageSize(SIZE_WIDE);
 			}
 			image = imageDescriptor.createImage(true);
 			imageRegistry.put(imageDescriptor.hashCode() + suffix, image);
 		}
 		return image;
 	}
 
 	private static ImageRegistry getImageRegistry() {
 		if (imageRegistry == null) {
 			imageRegistry = new ImageRegistry();
 		}
 
 		return imageRegistry;
 	}
 
 	private static URL makeIconFileURL(String prefix, String name) throws MalformedURLException {
 		if (baseURL == null)
 			throw new MalformedURLException();
 
 		StringBuffer buffer = new StringBuffer(prefix);
 		buffer.append('/');
 		buffer.append(name);
 		return new URL(baseURL, buffer.toString());
 	}
 
 	private static Image[] progressImages;
 
 	public static Image[] getProgressImages() {
 
 		if (progressImages != null)
 			return progressImages;
 
 		progressImages = new Image[8];
 
 		for (int i = 1; i <= 8; i++) {
 			ImageDescriptor imageDescriptor = create(T_EVIEW + "/progress", i + ".png");
 			progressImages[i - 1] = getImage(imageDescriptor);
 		}
 
 		return progressImages;
 
 	}
 }
