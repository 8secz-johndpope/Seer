 /*
  *  soapUI, copyright (C) 2004-2011 eviware.com 
  *
  *  soapUI is free software; you can redistribute it and/or modify it under the 
  *  terms of version 2.1 of the GNU Lesser General Public License as published by 
  *  the Free Software Foundation.
  *
  *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
  *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
  *  See the GNU Lesser General Public License for more details at gnu.org.
  */
 package com.eviware.soapui.impl.wsdl.panels.assertions;
 
 import java.util.LinkedHashMap;
 import java.util.SortedSet;
 import java.util.TreeSet;
 
 import com.eviware.soapui.impl.wsdl.teststeps.assertions.TestAssertionRegistry;
 import com.eviware.soapui.impl.wsdl.teststeps.assertions.recent.RecentAssertionHandler;
 import com.eviware.soapui.model.testsuite.Assertable;
 
 public class AssertionCategoryMapping
 {
 	public final static String VALIDATE_RESPONSE_CONTENT_CATEGORY = "Validate Response Content";
 	public final static String STATUS_CATEGORY = "Compliance, Status and Standards";
 	public final static String SCRIPT_CATEGORY = "Script";
 	public final static String SLA_CATEGORY = "SLA";
 	public final static String JMS_CATEGORY = "JMS";
 	public final static String SECURITY_CATEGORY = "Security";
 	public final static String RECENTLY_USED = "Recently used";
 	public static final String JDBC_CATEGORY = "JDBC";
 	public static final String GROUPING = "GROUPING";
 
 	public static String[] getAssertionCategories()
 	{
 		return new String[] { VALIDATE_RESPONSE_CONTENT_CATEGORY, STATUS_CATEGORY, SCRIPT_CATEGORY, SLA_CATEGORY,
				JMS_CATEGORY, JDBC_CATEGORY, SECURITY_CATEGORY, GROUPING };
 	}
 
 	/**
 	 * 
 	 * @param assertable
 	 * @param recentAssertionHandler
 	 * @return Set of Recently used assertion if @param assertable is not null
 	 *         only recently used assertions applicable to the @param assertable
 	 *         will be included if @param assertable is null all recently used
 	 *         assertions will be included
 	 */
 	private static SortedSet<AssertionListEntry> createRecentlyUsedSet( Assertable assertable,
 			RecentAssertionHandler recentAssertionHandler )
 	{
 		SortedSet<AssertionListEntry> recentlyUsedSet = new TreeSet<AssertionListEntry>();
 
 		for( String name : recentAssertionHandler.get() )
 		{
 			String type = recentAssertionHandler.getAssertionTypeByName( name );
 
 			if( type != null )
 			{
 				if( assertable == null || recentAssertionHandler.canAssert( type, assertable ) )
 				{
 					recentlyUsedSet.add( recentAssertionHandler.getAssertionListEntry( type ) );
 				}
 			}
 		}
 		return recentlyUsedSet;
 	}
 
 	/**
 	 * 
 	 * @param assertable
 	 * @param recentAssertionHandler
 	 * @return assertion categories mapped with assertions in exact category if @param
 	 *         assertable is not null only assertions for specific @param
 	 *         assertable will be included if @param assertable is null all
 	 *         assertions are included
 	 */
 	public static LinkedHashMap<String, SortedSet<AssertionListEntry>> getCategoriesAssertionsMap(
 			Assertable assertable, RecentAssertionHandler recentAssertionHandler )
 	{
 		LinkedHashMap<String, SortedSet<AssertionListEntry>> categoriesAssertionsMap = new LinkedHashMap<String, SortedSet<AssertionListEntry>>();
 
 		SortedSet<AssertionListEntry> recentlyUsedSet = createRecentlyUsedSet( assertable, recentAssertionHandler );
 
 		if( recentlyUsedSet.size() > 0 )
 			categoriesAssertionsMap.put( RECENTLY_USED, recentlyUsedSet );
 		TestAssertionRegistry.getInstance().addCategoriesAssertionsMap( assertable, categoriesAssertionsMap );
 		return categoriesAssertionsMap;
 	}
 
 }
