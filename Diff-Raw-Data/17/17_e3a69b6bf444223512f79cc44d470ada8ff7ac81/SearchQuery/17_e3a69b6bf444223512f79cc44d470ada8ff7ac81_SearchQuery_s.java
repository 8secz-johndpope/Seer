 /*
  *  Weblounge: Web Content Management System
  *  Copyright (c) 2003 - 2011 The Weblounge Team
  *  http://entwinemedia.com/weblounge
  *
  *  This program is free software; you can redistribute it and/or
  *  modify it under the terms of the GNU Lesser General Public License
  *  as published by the Free Software Foundation; either version 2
  *  of the License, or (at your option) any later version.
  *
  *  This program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License
  *  along with this program; if not, write to the Free Software Foundation
  *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
 
 package ch.entwine.weblounge.common.content;
 
 import ch.entwine.weblounge.common.content.page.Pagelet;
 import ch.entwine.weblounge.common.language.Language;
 import ch.entwine.weblounge.common.security.User;
 import ch.entwine.weblounge.common.site.Site;
 
 import java.util.Date;
 import java.util.Map;
 
 /**
  * This interface defines a fluent api for a query object used to lookup
  * resources in the <code>ContentRepository</code>.
  */
 public interface SearchQuery {
 
   /**
    * Sort order definitions.
    */
   public enum Order {
     None, Ascending, Descending
   }
 
   /**
    * Returns the contextual site for this query.
    * 
    * @return the site
    */
   Site getSite();
 
   /**
    * Sets the number of results that are returned.
    * 
    * @param limit
    *          the number of results
    * @return the search query
    */
   SearchQuery withLimit(int limit);
 
   /**
    * Returns the number of results that are returned, starting at the offset
    * returned by <code>getOffset()</code>. If no limit was specified, this
    * method returns <code>-1</code>.
    * 
    * @return the maximum number of results
    */
   int getLimit();
 
   /**
    * Sets the starting offset. Search results will be returned starting at that
    * offset and until the limit is reached, as specified by
    * <code>getLimit()</code>.
    * 
    * @param offset
    *          the starting offset
    * @return the search query
    * @see
    */
   SearchQuery withOffset(int offset);
 
   /**
    * Returns the starting offset within the search result or <code>0</code> if
    * no offset was specified.
    * 
    * @return the offset
    */
   int getOffset();
 
   /**
    * Return the resources with the given identifier.
    * 
    * @param id
    *          the identifier to look up
    * @return the query extended by this criterion
    */
   SearchQuery withIdentifier(String id);
 
   /**
    * Returns the identifier or <code>null</code> if no identifier was specified.
    * 
    * @return the identifier
    */
   String getIdentifier();
 
   /**
    * Return the resources with the given path.
    * 
    * @param path
    *          the path to look up
    * @return the query extended by this criterion
    */
   SearchQuery withPath(String path);
 
   /**
    * Returns the path or <code>null</code> if no path was specified.
    * 
    * @return the path
    */
   String getPath();
 
   /**
    * Return the resources with the given template.
    * 
    * @param template
    *          the template to look up
    * @return the query extended by this criterion
    */
   SearchQuery withTemplate(String template);
 
   /**
    * Returns the template or <code>null</code> if no template was specified.
    * 
    * @return the template
    */
   String getTemplate();
 
   /**
    * Return the resources with the given layout.
    * 
    * @param layout
    *          the layout to look up
    * @return the query extended by this criterion
    */
   SearchQuery withLayout(String layout);
 
   /**
    * Returns the layout or <code>null</code> if no layout was specified.
    * 
    * @return the layout
    */
   String getLayout();
 
   /**
    * Return the resources with the given type.
    * 
    * @param type
    *          the resource type to look up
    * @return the query extended by this criterion
    */
   SearchQuery withType(String type);
 
   /**
    * Returns the resource type or <code>null</code> if no type was specified.
    * 
    * @return the type
    */
   String getType();
 
   /**
    * Return resources that contain the given text either in the page header or
    * in one of the pagelets.
    * 
    * @param text
    *          the text to look up
    * @return the query extended by this criterion
    */
   SearchQuery withText(String text);
 
   /**
    * Returns the search text or <code>null</code> if no text was specified.
    * 
    * @return the text
    */
   String getText();
 
   /**
    * Specifies an element within a pagelet.
    * 
    * @param element
    *          the element name
    * @param value
    *          the element value
    * @return the search query
    */
   SearchQuery withElement(String element, String value);
 
   /**
    * Returns the elements text or <code>null</code> if no elements were
    * specified.
    * 
    * @return the text
    */
   Map<String, String> getElements();
 
   /**
    * Sets the search text.
    * 
    * @param property
    *          the property name
    * @param value
    *          the property value
    * @return the search query
    */
   SearchQuery withProperty(String property, String value);
 
   /**
    * Returns the search text or <code>null</code> if no text was specified.
    * 
    * @return the text
    */
   Map<String, String> getProperties();
 
   /**
    * Return only resources with hits in the specified language.
    * 
    * @param language
    *          the language
    * @return the query extended by this criterion
    */
   SearchQuery withLanguage(Language language);
 
   /**
    * Returns the language or <code>null</code> if no language was specified.
    * 
    * @return the language
    */
   Language getLanguage();
 
   /**
    * Only returns resources that contain the specified subject. Note that this
    * method may be called multiple times in order to specify more than one
    * subject.
    * 
    * @param subject
    *          the subject
    * @return the query extended by this criterion
    */
   SearchQuery withSubject(String subject);
 
   /**
    * Returns the subjects or an empty array if no subjects have been specified.
    * 
    * @return the subjects
    */
   String[] getSubjects();
 
   /**
    * Return only resources that have been created or modified by the specified
    * author.
    * 
    * @param author
    *          the author
    * @return the query extended by this criterion
    */
   SearchQuery withAuthor(User author);
 
   /**
    * Returns the author or <code>null</code> if no author has been specified.
    * 
    * @return the author
    */
   User getAuthor();
 
   /**
    * Return only resources that have been created by the specified user.
    * 
    * @param creator
    *          the creator
    * @return the query extended by this criterion
    */
   SearchQuery withCreator(User creator);
 
   /**
    * Returns the creator or <code>null</code> if no creator has been specified.
    * 
    * @return the creator
    */
   User getCreator();
 
   /**
    * Return only resources that have been modified by the specified user.
    * 
    * @param modifier
    *          the modifier
    * @return the query extended by this criterion
    */
   SearchQuery withModifier(User modifier);
 
   /**
    * Returns the modifier or <code>null</code> if no modifier has been
    * specified.
    * 
    * @return the modifier
    */
   User getModifier();
 
   /**
    * Return only resources that have been published by the specified publisher.
    * 
    * @param publisher
    *          the publisher
    * @return the query extended by this criterion
    */
   SearchQuery withPublisher(User publisher);
 
   /**
    * Returns the publisher or <code>null</code> if no publisher has been
    * specified.
    * 
    * @return the publisher
    */
   User getPublisher();
 
   /**
    * Return resources that have been published on the given date.
    * <p>
    * Note that this method throws an <code>IllegalStateException</code> if used
    * in conjunction with {@link #withPublishingDateBetween(Date)} or
    * {@link #and(Date)}.
    * 
    * @param date
    *          the publishing date
    * @return the query extended by this criterion
    */
   SearchQuery withPublishingDate(Date date);
 
   /**
    * Return resources with a publishing date of <code>date</code> or later.
    * <p>
    * Note that this method cannot be used without a subsequent call to
    * {@link #and(Date)} in order to specify the end date.
    * 
    * @param date
    *          the publishing start date
    * @return the query extended by this criterion
    */
   SearchQuery withPublishingDateBetween(Date date);
 
   /**
    * Returns the publishing date or <code>null</code> if no publishing date has
    * been specified.
    * 
    * @return the publishing date
    */
   Date getPublishingDate();
 
   /**
    * Returns the end of the range for the publishing date or <code>null</code>
    * if no end date has been specified.
    * 
    * @return the publishing end date
    */
   Date getPublishingDateEnd();
 
   /**
    * Return resources that have been modified on the given date.
    * <p>
    * Note that this method throws an <code>IllegalStateException</code> if used
    * in conjunction with {@link #withModificationDateBetween(Date)} or
    * {@link #and(Date)}.
    * 
    * @param date
    *          the modification date
    * @return the query extended by this criterion
    */
   SearchQuery withModificationDate(Date date);
 
   /**
    * Return resources with a modification date of <code>date</code> or later.
    * <p>
    * Note that this method cannot be used without a subsequent call to
    * {@link #and(Date)} in order to specify the end date.
    * 
    * @param date
    *          the modification start date
    * @return the query extended by this criterion
    */
   SearchQuery withModificationDateBetween(Date date);
 
   /**
    * Returns the modification date or <code>null</code> if no modification date
    * has been specified.
    * 
    * @return the modification date
    */
   Date getModificationDate();
 
   /**
    * Returns the end of the range for the modification date or <code>null</code>
    * if no end date has been specified.
    * 
    * @return the modification end date
    */
   Date getModificationDateEnd();
 
   /**
    * Return resources that have been created on the given date.
    * <p>
    * Note that this method throws an <code>IllegalStateException</code> if used
    * in conjunction with {@link #withCreationDateBetween(Date)} or
    * {@link #and(Date)}.
    * 
    * @param date
    *          the Creation date
    * @return the query extended by this criterion
    */
   SearchQuery withCreationDate(Date date);
 
   /**
    * Return resources with a Creation date of <code>date</code> or later.
    * <p>
    * Note that this method cannot be used without a subsequent call to
    * {@link #and(Date)} in order to specify the end date.
    * 
    * @param date
    *          the Creation start date
    * @return the query extended by this criterion
    */
   SearchQuery withCreationDateBetween(Date date);
 
   /**
    * Returns the creation date or <code>null</code> if no creation date has been
    * specified.
    * 
    * @return the creation date
    */
   Date getCreationDate();
 
   /**
    * Returns the end of the range for the creation date or <code>null</code> if
    * no end date has been specified.
    * 
    * @return the creation end date
    */
   Date getCreationDateEnd();
 
   /**
    * This method is used to specify the end of a date range, started by a call
    * to either one of {@link #withPublishingDateBetween(Date)},
    * {@link #withModificationDateBetween(Date)} or
    * {@link #withCreationDateBetween(Date)}.
    * 
    * @param date
    *          the end date
    * @return the query extended by this criterion
    */
   SearchQuery and(Date date);
 
   /**
    * Only return resources that are located on or below the given path in the
    * page tree.
    * 
    * @param path
    *          the path in the site tree
    * @return the query extended by this criterion
    */
   SearchQuery withPathPrefix(String path);
 
   /**
    * Returns the path prefix.
    * 
    * @return the prefix
    */
   String getPathPrefix();
 
   /**
    * Return resources that contain the specified pagelet.
    * <p>
    * Note that you can specify the location where the pagelet needs to be as
    * additional elements or properties by a subsequent call to
    * {@link #inComposer(String)} {@link #atPosition(int)},
    * {@link #andElement(String, String)} and
    * {@link #andProperty(String, String)}.
    * 
    * @param module
    *          the module identifier
    * @param id
    *          the pagelet identifier
    * @return the query extended by this criterion
    */
   SearchQuery withPagelet(String module, String id);
 
   /**
    * Returns the list of required pagelets, along with their elements,
    * properties and location information.
    * 
    * @return the pagelets
    */
   Pagelet[] getPagelets();
 
   /**
    * This method may be called after a call to {@link #withPagelet(Pagelet)} in
    * order to specify the composer that the pagelet needs to be in.
    * 
    * @param composer
    *          the composer name
    * @return the query extended by this criterion
    * @throws IllegalStateException
    *           if no pagelet has been specified before
    */
   SearchQuery inComposer(String composer) throws IllegalStateException;
 
   /**
    * This method may be called after a call to {@link #withPagelet(Pagelet)} in
    * order to specify that the pagelet needs to be in the stage composer.
    * 
    * @return the query extended by this criterion
    * @throws IllegalStateException
    *           if no pagelet has been specified before
    */
   SearchQuery inStage() throws IllegalStateException;
 
   /**
    * This method may be called after a call to {@link #inComposer(String)} in
    * order to specify the pagelet's position within that composer.
    * 
    * @param position
    *          the pagelet position within the composer
    * @return the query extended by this criterion
    * @throws IllegalStateException
    *           if no composer has been specified before
    */
   SearchQuery atPosition(int position) throws IllegalStateException;
 
   /**
    * Only return resources that contain a certain pagelet (see
    * {@link #withPagelet(Pagelet)} that features the given property with the
    * indicated value.
    * <p>
    * Note that the property value needs to match exactly. Also, you need to
    * specify the pagelet right before calling this method, otherwise an
    * <code>IllegalStateException</code> will be thrown:
    * 
    * <pre>
    * SearchQuery q = new SearchQuery();
    * q.withPagelet(p).andProperty(&quot;hello&quot;, &quot;world&quot;);
    * </pre>
    * 
    * @param propertyName
    *          name of the property
    * @param propertyValue
    *          property value
    * @return the query extended by this criterion
    * @throws IllegalStateException
    *           if the pagelet has not been specified before
    */
   SearchQuery andProperty(String propertyName, String propertyValue)
       throws IllegalStateException;
 
   /**
    * Only return resources that contain a certain pagelet (see
    * {@link #withPagelet(Pagelet)} that features the given text with the
    * indicated value in any language.
    * <p>
    * Note that partial matches are considered a hit as well. Also, you need to
    * specify the pagelet right before calling this method, otherwise an
    * <code>IllegalStateException</code> will be thrown:
    * 
    * <pre>
    * SearchQuery q = new SearchQuery();
    * q.withPagelet(p).andProperty(&quot;hello&quot;, &quot;world&quot;);
    * </pre>
    * 
    * @param textName
    *          name of the text field
    * @param text
    *          the actual text
    * @return the query extended by this criterion
    * @throws IllegalStateException
    *           if the pagelet has not been specified before
    */
   SearchQuery andElement(String textName, String text)
       throws IllegalStateException;
 
   /**
    * Return the resources with the given filename in their content section.
    * 
    * @param filename
    *          the filename to look up
    * @return the query extended by this criterion
    */
   SearchQuery withFilename(String filename);
 
   /**
    * Returns the filename or <code>null</code> if no filename was specified.
    * 
    * @return the filename
    */
   String getFilename();
 
   /**
    * Return the resources with the given mime type in their content section.
    * 
    * @param mimetype
    *          the mime type to look up
    * @return the query extended by this criterion
    */
   SearchQuery withMimetype(String mimetype);
 
   /**
    * Returns the mime type or <code>null</code> if no mime type was specified.
    * 
    * @return the mime type
    */
   String getMimetype();
 
   /**
    * Asks the search index to order the results by creation date rather than by
    * relevance.
    * 
    * @param order
    *          the sort order
    * @return the search query
    */
   SearchQuery sortByCreationDate(Order order);
 
   /**
    * Returns the sort order for the date if specified. If this field is not to
    * be sorted by creation date, {@link Order#None} is returned.
    * 
    * @return the sort order
    */
   Order getCreationDateSortOrder();
 
   /**
    * Asks the search index to order the results by modification date rather than
    * by relevance.
    * 
    * @param order
    *          the sort order
    * @return the search query
    */
   SearchQuery sortByModificationDate(Order order);
 
   /**
    * Returns the sort order for the date if specified. If this field is not to
    * be sorted by modification date, {@link Order#None} is returned.
    * 
    * @return the sort order
    */
   Order getModificationDateSortOrder();
 
   /**
    * Asks the search index to order the results by publishing date rather than
    * by relevance.
    * 
    * @param order
    *          the sort order
    * @return the search query
    */
   SearchQuery sortByPublishingDate(Order order);
 
   /**
    * Returns the sort order for the date if specified. If this field is not to
    * be sorted by publishing date, {@link Order#None} is returned.
    * 
    * @return the sort order
    */
   Order getPublishingDateSortOrder();
 
   /**
    * Turns on faceting for subjects
    * 
    * @return the search query
    */
   SearchQuery withSubjectFacet();
 
   /**
    * Returns <code>true</code> if faceting on subjects is enabled.
    * 
    * @return <code>true</code> the subject facet is enabled
    */
   boolean isSubjectFacetEnabled();
 
 }
