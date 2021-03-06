 /**
  *
  Copyright 2012 Vineet Semwal
 
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  */
 package com.aplombee;
 
 /**
  * reuse strategy that is used with {@link QuickView}
  *
  * @author Vineet Semwal
  */
 public enum ReUse {
 
     /**
      * all children are removed and children of last page visited are created ,used for paging
      * say with {@link org.apache.wicket.markup.html.navigation.paging.PagingNavigator} or
      * {@link org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator}  ,
      * this is the preferred strategy for paging navigation
      * <p/>
      * earlier it was known as DEFAULT_PAGING
      */
     PAGING,
 
     /**
      * mostly used and preferred for {@link com.aplombee.navigator.AjaxItemsNavigator}
     *  or {@link com.aplombee.navigator.AjaxScrollEventBehaviorBase}
      * <p/>
     * 1)all children are removed and children of first page are created again on re-render <br/>
      * 2) new children for next page is created in  {@link com.aplombee.QuickViewBase#addItemsForNextPage()}
      * <p/>
     *
      * earlier it was known as DEFAULT_ITEMSNAVIGATION
      */
     ITEMSNAVIGATION,
 
 
     /**
      * reuse the items whose models are equal ,to use this model should implement equals,
      * used with {@link org.apache.wicket.markup.html.navigation.paging.PagingNavigator} ,
      * not supported with @{@link com.aplombee.navigator.AjaxItemsNavigator} or
      * {@link com.aplombee.navigator.AjaxScrollEventBehaviorBase}
      */
     CURRENTPAGE,
 
 
     /**
      *  used with {@link com.aplombee.navigator.AjaxItemsNavigator}
      *  or
      * {@link com.aplombee.navigator.AjaxScrollEventBehaviorBase}
     * <br/>
     * 1) no child is removed or recreated on re-render <br/>
      * 2) new children for next page is created in  {@link com.aplombee.QuickViewBase#addItemsForNextPage()}
      * <p/>
      * all children are reused,no child gets removed  or recreated on re re-render ,
      * the usecase for this can be a user has  itemsperequest 3 times after the initial render ,
      * on page reload you want to show him all the rows he created so that he doesn't have to start again
      */
     ALL,
 
 
     /**
      * dont use it,this is internally used in QuickView
      */
     NOT_INITIALIZED
 }
