 package com.xpn.xwiki.watch.client.ui.dialog;
 
 import com.google.gwt.user.client.ui.*;
 import com.google.gwt.user.client.rpc.AsyncCallback;
 import com.google.gwt.core.client.GWT;
 import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;
 import com.xpn.xwiki.watch.client.Feed;
 
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * See the NOTICE file distributed with this work for additional
  * information regarding copyright ownership.
  * <p/>
  * This is free software;you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation;either version2.1of
  * the License,or(at your option)any later version.
  * <p/>
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY;without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
  * Lesser General Public License for more details.
  * <p/>
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software;if not,write to the Free
  * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
  * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
  *
  * @author ldubost
  */
 
 public class SearchEngineFeedDialog extends FeedDialog {
     protected TextBox feedNameTextBox;
     protected TextBox searchTermTextBox;
     protected ListBox searchLanguageListBox;
     protected String baseURL = "";
 
     /**
      * Choice dialog
      * @param app  XWiki GWT App object to access translations and css prefix names
      * @param name dialog name
      * @param buttonModes button modes Dialog.BUTTON_CANCEL|Dialog.BUTTON_NEXT for Cancel / Next
      * @param baseURL base url of the search engine
      */
     public SearchEngineFeedDialog(XWikiGWTApp app, String name, int buttonModes, Feed feed, String baseURL, String[] languages) {
         super(app, name, buttonModes, feed, languages);
         this.baseURL = baseURL;
     }
 
     protected void updateFeed() {
         feed.setName(feedNameTextBox.getText());
         String query = searchTermTextBox.getText();
         String language = (searchLanguageListBox==null) ? null 
             : searchLanguageListBox.getValue(searchLanguageListBox.getSelectedIndex());
         String url = getURL(query, language);
         feed.setUrl(url);
         List groups = new ArrayList();
         for (int i=0;i<groupsListBox.getItemCount();i++) {
             if (groupsListBox.isItemSelected(i))
              groups.add(groupsListBox.getValue(i));
         }
         feed.setGroups(groups);
 
     }
 
     protected void validateDialogData(final AsyncCallback cb)
     {
         String feedName = this.feedNameTextBox.getText().trim();
         final DialogValidationResponse response = new DialogValidationResponse();        
         
         if (searchTermTextBox.getText().equals("")) {
             response.setValid(false);
             response.setMessage(app.getTranslation(getDialogTranslationName() + ".noquery"));
             cb.onSuccess(response);
             return;
         }
         if (feedName.equals("")) {
             response.setValid(false);
             response.setMessage(app.getTranslation(getDialogTranslationName() + ".nofeedname"));
             cb.onSuccess(response);
             return;
         }
 
         if (this.feed.getName().trim().equalsIgnoreCase(feedName)) {
             response.setValid(true);
             cb.onSuccess(response);
             return;
         } else {
             //do the unicity check, the feedname was changed
             this.checkUniqueFeedName(feedName, cb);
             return;
         }
     }
 
     public String getURL(String queryterm, String language) {
         String oStr = baseURL;
         String oStr2;
         String[] args = new String[(language==null) ? 1 : 2];
         args[0] = encode(queryterm);
         if (language !=null) {
             args[1] = language;
         }
 
         for (int i = 0; i<args.length; i++){
             if (GWT.isScript()) {
                 oStr2 = oStr.replaceAll("\\{"+i+"\\}", args[i]);
             } else {
                 oStr2 = oStr.replaceAll("\\{"+i+"\\}", args[i]);
             }
             oStr = oStr2;
         }
 
         return oStr;
     }
 
     private String encode(String text) {
         return text.replaceAll(" ", "+").replaceAll("\"", "%22");
     }
 
     protected Widget getParametersPanel() {
         FlowPanel paramsPanel = new FlowPanel();
         Label feedNameLabel = new Label();
        feedNameLabel.setStyleName("mailsubject-label");
         feedNameLabel.setText(app.getTranslation(getDialogTranslationName() + ".feedname"));
         paramsPanel.add(feedNameLabel);
 
         feedNameTextBox = new TextBox();
         if ((feed!=null)&&(feed.getName()!=null))
             feedNameTextBox.setText(feed.getName());
         feedNameTextBox.setVisibleLength(60);
         feedNameTextBox.setName("feedname");
         feedNameTextBox.setStyleName(getCSSName("feedname"));
         paramsPanel.add(feedNameTextBox);
         Label searchTermLabel = new Label();
         searchTermLabel.setStyleName("query-label");
         searchTermLabel.setText(app.getTranslation(getDialogTranslationName() + ".query"));
         paramsPanel.add(searchTermLabel);
         searchTermTextBox = new TextBox();
         searchTermTextBox.setVisibleLength(40);
         searchTermTextBox.setName("query");
         searchTermTextBox.setStyleName(getCSSName("query"));
         paramsPanel.add(searchTermTextBox);
         if ((languages!=null)&&(languages.length>0)) {
             Label searchLanguageLabel = new Label();
             searchLanguageLabel.setStyleName("language-label");
             searchLanguageLabel.setText(app.getTranslation(getDialogTranslationName() + ".language"));
             paramsPanel.add(searchLanguageLabel);
             searchLanguageListBox = new ListBox();
             searchLanguageListBox.setName("language");
             searchLanguageListBox.setMultipleSelect(false);
             for (int i=0;i<languages.length;i++) {
                 String language = languages[i];
                 String languageText = app.getTranslation("language." + language);
                 searchLanguageListBox.addItem(languageText, language);
             }
             paramsPanel.add(searchLanguageListBox);
         }
         paramsPanel.add(getGroupsFields());
         return paramsPanel;
     }
 
 }
