 /*
  * Copyright (c) 2011 Roberto Tyley
  *
  * This file is part of 'Agit' - an Android Git client.
  *
  * Agit is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * Agit is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package com.madgag.agit;
 
import static com.madgag.agit.CloneLauncherActivity.EXTRA_SOURCE_URI;
import static com.madgag.agit.CloneLauncherActivity.EXTRA_TARGET_DIR;
 import static java.lang.System.currentTimeMillis;
import android.content.Intent;
 import android.os.Bundle;
 import android.view.View;
 import android.widget.Checkable;
 import android.widget.TextView;
 
 import com.github.calculon.CalculonUnitTest;
 import com.github.calculon.predicate.Predicate;
 
 public class CloneActivityUnitTest extends CalculonUnitTest<CloneLauncherActivity> {
 	
 	final String appleProjectSourceUri="/example/apple";
 	final String targetDir="/sdcard/tango";
 	
 	public CloneActivityUnitTest() {
 		super(CloneLauncherActivity.class);
 	}
 	
 	public void testUsesDefaultGitDirLocationIfOnlySourceUriIsProvidedInIntent() {
 		Bundle bundle = new Bundle();
 		bundle.putString(EXTRA_SOURCE_URI, appleProjectSourceUri);
 		
 		startActivity(bundle);
 		getInstrumentation().callActivityOnStart(getActivity());
 		
 		assertThat(R.id.CloneUrlEditText).satisfies(hasText(appleProjectSourceUri));
 		assertThat(R.id.UseDefaultGitDirLocation).satisfies(isChecked(true));
 	}
 	
 	public void testUsesSpecifiedRepoDirLocationFromIntentIfSupplied() {
 		Bundle bundle = new Bundle();
 		bundle.putString(EXTRA_SOURCE_URI, appleProjectSourceUri);
 		bundle.putString(EXTRA_TARGET_DIR, targetDir);
 		
 		startActivity(bundle);
 		getInstrumentation().callActivityOnStart(getActivity());
 		
 		assertThat(R.id.GitDirEditText).satisfies(hasText(targetDir));
 		assertThat(R.id.UseDefaultGitDirLocation).satisfies(isChecked(false));
 		assertThat(R.id.CloneUrlEditText).satisfies(hasText(appleProjectSourceUri));
 	}
 
 	public void testClickingCloneLaunchesTheGitOperationWithTheCorrectIntent() {
 		String littleTargetDir = targetDir+"/"+currentTimeMillis();
 
 		startActivity();
 		getInstrumentation().callActivityOnStart(getActivity());
 		
 		setUp(R.id.CloneUrlEditText).setText(appleProjectSourceUri);
 		setUp(R.id.UseDefaultGitDirLocation).setChecked(false);
 		setUp(R.id.GitDirEditText).setText(littleTargetDir);
 		
 		/*
 		 *  Unfortunately, assertions on the service-starting intent are not possible due to 
 		 *  http://code.google.com/p/android/issues/detail?id=12246
 
 		Intent intent = assertThat(R.id.GoCloneButton).click().starts(GitOperationsService.class);
 		assertEquals(intent.getStringExtra("gitdir"),littleTargetDir);
 		 */
 	}
 	
 	private Predicate<View> isChecked(final boolean checked) {
 		return new Predicate<View>() {
 			public boolean check(View target) {
 				return ((Checkable) target).isChecked()==checked;
 			}
 		};
 	}
 	
 	private static Predicate<View> hasText(final String text) {
 		return new Predicate<View>() {
 			public boolean check(View target) {
 				return ((TextView) target).getText().toString().equals(text);
 			}
 		};
 	}
 }
