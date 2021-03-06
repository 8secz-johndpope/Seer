 /*
  * MessageToadlet.java
  * This file is part of Freemail
  * Copyright (C) 2011 Martin Nyhus
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License along
  * with this program; if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 
 package freemail.ui.web;
 
 import java.io.IOException;
 import java.net.URI;
 
 import freemail.AccountManager;
 import freemail.FreemailAccount;
 import freemail.MailMessage;
 import freemail.MessageBank;
 import freenet.client.HighLevelSimpleClient;
 import freenet.clients.http.PageMaker;
 import freenet.clients.http.PageNode;
 import freenet.clients.http.SessionManager;
 import freenet.clients.http.ToadletContext;
 import freenet.clients.http.ToadletContextClosedException;
 import freenet.support.HTMLNode;
 import freenet.support.api.HTTPRequest;
 
 public class MessageToadlet extends WebPage {
 	private final AccountManager accountManager;
 
 	MessageToadlet(HighLevelSimpleClient client, SessionManager sessionManager, PageMaker pageMaker, AccountManager accountManager) {
 		super(client, pageMaker, sessionManager);
 		this.accountManager = accountManager;
 	}
 
 	@Override
 	public void makeWebPage(URI uri, HTTPRequest req, ToadletContext ctx, HTTPMethod method, PageNode page) throws ToadletContextClosedException, IOException {
 		HTMLNode pageNode = page.outer;
 		HTMLNode contentNode = page.content;
 
 		HTMLNode container = contentNode.addChild("div", "class", "container");
 
 		//Add the list of folders
 		HTMLNode folderList = container.addChild("div", "class", "folderlist");
 
 		String identity = sessionManager.useSession(ctx).getUserID();
 		FreemailAccount account = accountManager.getAccount(identity);
 		MessageBank topLevelMessageBank = account.getMessageBank();
 		addMessageBank(folderList, topLevelMessageBank, "inbox");
 
 		//Add the message
 		String folderName = req.getParam("folder", "inbox");
 		MessageBank messageBank = getMessageBank(account, folderName);
 		String messageId = req.getParam("message", null);
 		MailMessage msg = getMessage(messageBank, messageId);
 
 		HTMLNode messageNode = container.addChild("div", "class", "message");
 
 		addMessageHeaders(messageNode, msg);
 
 		writeHTMLReply(ctx, 200, "OK", pageNode.generate());
 	}
 
 	//TODO: Handle cases where folderName doesn't start with inbox
 	private MessageBank getMessageBank(FreemailAccount account, String folderName) {
 		if(folderName.equalsIgnoreCase("inbox")) {
 			return account.getMessageBank();
 		}
 
 		if(!folderName.startsWith("inbox")) {
 			return null;
 		}
 
 		//Find the correct subfolder. The account message bank is inbox, so strip it
 		MessageBank messageBank = account.getMessageBank();
 		for(String name : folderName.substring("index.".length()).split("\\.")) {
 			messageBank = messageBank.getSubFolder(name);
 		}
 		return messageBank;
 	}
 
 	private MailMessage getMessage(MessageBank messageBank, String messageId) {
 		for(MailMessage msg : messageBank.listMessages().values()) {
			if(msg.getFirstHeader("message-id").equalsIgnoreCase(messageId)) {
 				return msg;
 			}
 		}
 
 		return null;
 	}
 
 	private void addMessageHeaders(HTMLNode messageNode, MailMessage message) {
 		HTMLNode headerBox = messageNode.addChild("div", "class", "message-headers");
 		headerBox.addChild("p", "To: " + message.getFirstHeader("to"));
 		headerBox.addChild("p", "From: " + message.getFirstHeader("from"));
 
 		if(message.getFirstHeader("cc") != null) {
 			headerBox.addChild("p", "CC: " + message.getFirstHeader("cc"));
 		}
 		if(message.getFirstHeader("bcc") != null) {
 			headerBox.addChild("p", "BCC: " + message.getFirstHeader("bcc"));
 		}
 
 		headerBox.addChild("p", "Subject: " + message.getFirstHeader("subject"));
 	}
 
 	private HTMLNode addMessageBank(HTMLNode parent, MessageBank messageBank, String link) {
 		//First add this message bank
 		HTMLNode folderDiv = parent.addChild("div", "class", "folder");
 		HTMLNode folderPara = folderDiv.addChild("p");
 		folderPara.addChild("a", "href", "?folder=" + link, messageBank.getName());
 
 		//Then add all the children recursively
 		for(MessageBank child : messageBank.listSubFolders()) {
 			addMessageBank(folderDiv, child, link + "." + child.getName());
 		}
 
 		return folderDiv;
 	}
 
 	@Override
 	public boolean isEnabled(ToadletContext ctx) {
 		return sessionManager.sessionExists(ctx);
 	}
 
 	@Override
 	public String path() {
 		return "/Freemail/Message";
 	}
 
 	@Override
 	boolean requiresValidSession() {
 		return true;
 	}
 }
