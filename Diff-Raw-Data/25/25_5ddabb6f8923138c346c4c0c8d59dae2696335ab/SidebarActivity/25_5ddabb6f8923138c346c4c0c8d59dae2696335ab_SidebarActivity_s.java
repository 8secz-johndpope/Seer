 /*
  * Copyright (C) 2012 Brian Muramatsu
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.btmura.android.reddit.app;
 
 import android.app.ActionBar;
 import android.app.Activity;
 import android.os.Bundle;
 import android.support.v4.view.ViewPager;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.ViewStub;
 import android.widget.Button;
 
 import com.btmura.android.reddit.R;
 import com.btmura.android.reddit.database.Subreddits;
 import com.btmura.android.reddit.widget.SidebarPagerAdapter;
 
 public class SidebarActivity extends Activity implements OnClickListener, SubredditNameHolder {
 
     public static final String EXTRA_SUBREDDIT = "subreddit";
 
     private SidebarPagerAdapter adapter;
     private ViewPager pager;
 
     private Button addSubreddit;
     private View cancel;
 
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.sidebar);
         setupViews();
     }
 
     private void setupViews() {
         ActionBar bar = getActionBar();
         if (bar != null) {
             bar.setDisplayHomeAsUpEnabled(true);
         } else {
             ViewStub vs = (ViewStub) findViewById(R.id.button_bar_stub);
             View buttonBar = vs.inflate();
 
             addSubreddit = (Button) buttonBar.findViewById(R.id.ok);
             addSubreddit.setText(R.string.add_subreddit);
             addSubreddit.setOnClickListener(this);
 
             cancel = findViewById(R.id.cancel);
             cancel.setOnClickListener(this);
         }
 
         String subreddit = getIntent().getStringExtra(EXTRA_SUBREDDIT);
         setTitle(Subreddits.getTitle(this, subreddit));
 
         String[] subreddits = subreddit.split("\\+");
         adapter = new SidebarPagerAdapter(getFragmentManager(), subreddits);
         pager = (ViewPager) findViewById(R.id.pager);
         pager.setAdapter(adapter);
 
         View pagerStrip = findViewById(R.id.pager_strip);
         pagerStrip.setVisibility(subreddits.length > 1 ? View.VISIBLE : View.GONE);
     }
 
     public void onClick(View v) {
         if (v == addSubreddit) {
             handleAddSubreddit();
         } else {
             finish();
         }
     }
 
    public CharSequence getSubredditName() {
        return adapter.getPageTitle(pager.getCurrentItem());
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         super.onCreateOptionsMenu(menu);
         getMenuInflater().inflate(R.menu.sidebar_menu, menu);
         return true;
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case android.R.id.home:
                 finish();
                 return true;
 
             case R.id.menu_add_subreddit:
                 handleAddSubreddit();
                 return true;
 
             default:
                 return super.onOptionsItemSelected(item);
         }
     }
 
     private void handleAddSubreddit() {
         AddSubredditFragment.newInstance().show(getFragmentManager(), null);
     }
 }
