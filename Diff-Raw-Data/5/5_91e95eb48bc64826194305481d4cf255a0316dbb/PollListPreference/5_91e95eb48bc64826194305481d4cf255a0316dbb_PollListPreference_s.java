 /* -*- compile-command: "cd ../../../../../; ant install"; -*- */
 /*
  * Copyright 2009-2010 by Eric House (xwords@eehouse.org).  All
  * rights reserved.
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License as
  * published by the Free Software Foundation; either version 2 of the
  * License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
  */
 
 package org.eehouse.android.xw4;
 
 import android.preference.Preference;
 import android.preference.ListPreference;
 import android.content.Context;
 import android.util.AttributeSet;
 import junit.framework.Assert;
 
 
 public class PollListPreference extends ListPreference
     implements Preference.OnPreferenceChangeListener {
 
     private Context m_context;
 
     public PollListPreference( Context context, AttributeSet attrs )
     {
         super( context, attrs );
         m_context = context;
         setOnPreferenceChangeListener( this );
 
     }
 
     @Override
     protected void onAttachedToActivity()
     {
         // String key = getString( R.string.key_connect_frequency );
         String val = getPersistedString( "0" );
        setSummary( String.format( "%s minutes", val ) );
     }
 
     // Preference.OnPreferenceChangeListener interface
     public boolean onPreferenceChange( Preference preference, Object newValue )
     {
         int val = Integer.parseInt((String)newValue);
        RelayActivity.RestartTimer( m_context, val * 1000 );
         setSummary( String.format( "%d seconds", val ) );
         return true;
     }
 }
