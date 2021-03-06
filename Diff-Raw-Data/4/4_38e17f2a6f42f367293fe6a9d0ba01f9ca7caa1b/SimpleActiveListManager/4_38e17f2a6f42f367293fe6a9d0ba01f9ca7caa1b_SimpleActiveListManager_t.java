 
 /*
  * Copyright 1999-2002 Carnegie Mellon University.
  * Portions Copyright 2002 Sun Microsystems, Inc.
  * Portions Copyright 2002 Mitsubishi Electronic Research Laboratories.
  * All Rights Reserved.  Use is subject to license terms.
  *
  * See the file "license.terms" for information on usage and
  * redistribution of this file, and for a DISCLAIMER OF ALL
  * WARRANTIES.
  *
  */
 
 package edu.cmu.sphinx.decoder.search;
 
 import edu.cmu.sphinx.util.SphinxProperties;
 import edu.cmu.sphinx.util.LogMath;
 import edu.cmu.sphinx.decoder.linguist.*;
 import edu.cmu.sphinx.decoder.linguist.simple.*;
 import edu.cmu.sphinx.decoder.linguist.lextree.LexTreeLinguist;
 
 import java.util.AbstractMap;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 
 /**
  * A list of ActiveLists. Different token types are placed in different lists.
  *
  * This class is not thread safe and should only be used by  a single
  * thread.
  *
  */
 public class SimpleActiveListManager implements ActiveListManager  {
 
     private static final String PROP_PREFIX
         = "edu.cmu.sphinx.decoder.search.SimpleActiveListManager.";
 
     /**
      * This property is used in the Iterator returned by the
      * getNonEmittingListIterator() method. When the Iterator.next() method
      * is called, this property determines whether the lists prior to
      * that returned by next() are empty (they should be empty).
      * If they are not empty, an Error will be thrown.
      */
     public static final String PROP_CHECK_PRIOR_LISTS_EMPTY
         = PROP_PREFIX + "checkPriorListsEmpty";
 
     /**
      * The default value of PROP_CHECK_PRIOR_LISTS_EMPTY.
      */
     public static final boolean PROP_CHECK_PRIOR_LISTS_EMPTY_DEFAULT = false;
 
 
     private Class[] searchStateOrder;
     private Class[] nonEmittingClasses;
     private Class emittingClass;
 
     private AbstractMap listMap = new HashMap();
     private ActiveList emittingActiveList;
 
     private SphinxProperties props;
     private boolean checkPriorLists;
 
 
     /**
      * Creates active lists with properties
      *
      * @param props the sphinx properties
      * @param searchStateOrder an array of classes that represents the order 
      *     in which the states will be returned
      */
     public SimpleActiveListManager(SphinxProperties props, 
                                    Class[] searchStateOrder) {
         this.props = props;
         this.searchStateOrder = searchStateOrder;
 
         checkPriorLists = props.getBoolean
             (PROP_CHECK_PRIOR_LISTS_EMPTY,
              PROP_CHECK_PRIOR_LISTS_EMPTY_DEFAULT);
 
         emittingClass = searchStateOrder[searchStateOrder.length - 1];
         nonEmittingClasses = new Class[searchStateOrder.length - 1];
 
         // assign the non-emitting classes
         for (int i = 0; i < nonEmittingClasses.length; i++) {
             nonEmittingClasses[i] = searchStateOrder[i];
         }
 
         // create the emitting and non-emitting active lists
         createActiveLists();
     }
 
     /**
      * Creates the emitting and non-emitting active lists.
      * When creating the non-emitting active lists, we will look
      * at their respective beam widths (eg, word beam, unit beam, state beam).
      */
     private void createActiveLists() {
     
         String activeListClass = props.getString
             (SimpleBreadthFirstSearchManager.PROP_ACTIVE_LIST_TYPE,
              SimpleBreadthFirstSearchManager.PROP_ACTIVE_LIST_TYPE_DEFAULT);
 
         int absoluteWordBeam = props.getInt
             (PROP_ABSOLUTE_WORD_BEAM_WIDTH,
              PROP_ABSOLUTE_WORD_BEAM_WIDTH_DEFAULT);
 
         double relativeWordBeam = 
             props.getDouble(PROP_RELATIVE_WORD_BEAM_WIDTH,
                             PROP_RELATIVE_WORD_BEAM_WIDTH_DEFAULT);
 
         try {
             Class wordSearchState = Class.forName
                 ("edu.cmu.sphinx.decoder.linguist.WordSearchState");
             
             emittingActiveList = 
                 (ActiveList)Class.forName(activeListClass).newInstance();
             emittingActiveList.setProperties(props);
             
             for (int i = 0; i < nonEmittingClasses.length; i++) {
                 
                 ActiveList list = (ActiveList)
                     Class.forName(activeListClass).newInstance();
                 list.setProperties(props);
 
                 // figure out what type of token (word, unit, hmm...)
                 // goes into this ActiveList
 
                 if (wordSearchState.isAssignableFrom(nonEmittingClasses[i])) {
                     list.setAbsoluteBeamWidth(absoluteWordBeam);
                     list.setRelativeBeamWidth(relativeWordBeam);
                 }
 
                 listMap.put(nonEmittingClasses[i], list);
             }
         } catch (ClassNotFoundException fe) {
             throw new Error("Can't create active list", fe);
         } catch (InstantiationException ie) {
             throw new Error("Can't create active list", ie);
         } catch (IllegalAccessException iea) {
             throw new Error("Can't create active list", iea);
         }
     }
 
 
 
     /**
      * Creates a new version of this active list with
      * the same general properties as this list
      *
      * @return the new active list
      */
     public ActiveListManager createNew() {
         return new SimpleActiveListManager(props, searchStateOrder);
     }
 
 
     /**
      * Adds the given token to the list
      *
      * @param token the token to add
      */
     public void add(Token token) {
         ActiveList activeList;
         if (token.isEmitting()) {
             activeList = emittingActiveList;
         } else {
             activeList = findListFor(token.getSearchState().getClass());
         }
        if (activeList == null) {
            throw new Error("Cannot find ActiveList for " + 
                            token.getSearchState().getClass());
        }
         activeList.add(token);
     }
 
     private ActiveList findListFor(Token token) {
         if (token.isEmitting()) {
             return emittingActiveList;
         } else {
             return findListFor(token.getSearchState().getClass());
         }
     }
 
     private ActiveList findListFor(Class type) {
         return (ActiveList)listMap.get(type);
     }
 
 
     /**
      * Replaces an old token with a new token
      *
      * @param oldToken the token to replace (or null in which case,
      * replace works like add).
      *
      * @param newToken the new token to be placed in the list.
      *
      */
     public void replace(Token oldToken, Token newToken) {
         ActiveList activeList = findListFor(oldToken);
         assert activeList != null;
         activeList.replace(oldToken, newToken);
     }
 
 
     /**
      * Returns the emitting ActiveList, and removes it from this manager.
      *
      * @return the emitting ActiveList
      */
     public ActiveList getEmittingList() {
         ActiveList list = emittingActiveList;
         emittingActiveList = list.createNew();
         return list;
     }
 
 
     /**
      * Returns an Iterator of all the non-emitting ActiveLists. The
      * iteration order is the same as the search state order.
      *
      * @return an Iterator of non-emitting ActiveLists
      */
     public Iterator getNonEmittingListIterator() {
         return (new NonEmittingListIterator());
     }
 
 
     private class NonEmittingListIterator implements Iterator {
         private int listPtr;
         private Class stateClass;
         private ActiveList list;
 
         public NonEmittingListIterator() {
             listPtr = 0;
         }
 
         public boolean hasNext() {
             return (listPtr < nonEmittingClasses.length);
         }
 
         public Object next() {
             if (checkPriorLists) {
                 checkPriorLists();
             }
             stateClass = nonEmittingClasses[listPtr++];
             list = (ActiveList) listMap.get(stateClass);
             return list;
         }
 
         /**
          * Check that all lists prior to listPtr is empty.
          */
         private void checkPriorLists() {
             for (int i = 0; i < listPtr; i++) {
                 ActiveList activeList = findListFor(nonEmittingClasses[i]);
                 if (activeList.size() > 0) {
                     throw new Error("At " + nonEmittingClasses[listPtr] +
                                     ". List for " + nonEmittingClasses[i] + 
                                     " should not have tokens.");
                 }
             }
         }
 
         public void remove() {
             listMap.put(stateClass, list.createNew());
         }
     }
 }
