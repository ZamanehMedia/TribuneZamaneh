package com.tribunezamaneh.rss;

import java.util.ArrayList;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.ItemExpandActivity;

import com.tribunezamaneh.rss.PostListFragment.PostListType;
import info.guardianproject.securereaderinterface.adapters.StoryListAdapter.OnTagClickedListener;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import com.tribunezamaneh.rss.views.CreateAccountView;
import info.guardianproject.securereaderinterface.views.FullScreenStoryItemView;
import com.tribunezamaneh.rss.views.PostSignInView;
import com.tribunezamaneh.rss.views.CreateAccountView.OnActionListener;
import com.tribunezamaneh.rss.views.PostSignInView.OnAgreeListener;
import com.tribunezamaneh.rss.views.WPSignInView;

import info.guardianproject.securereaderinterface.widgets.CustomFontCheckableButton;
import info.guardianproject.securereaderinterface.R;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;


public class PostActivity extends ItemExpandActivity implements OnTagClickedListener
{
	public static final String LOGTAG = "PostActivity";
	public static final boolean LOGGING = false;
	
	PostPagerAdapter mPostPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	private String mCurrentSearchTag;

	private MenuItem mMenuItemTag;

	private MenuItem mMenuAddPost;
	private ArrayList<View> mTabs;
	private ViewGroup mTabContainer;

	@SuppressLint("NewApi") @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post);

		setMenuIdentifier(R.menu.activity_post);

		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mPostPagerAdapter = new PostPagerAdapter(getSupportFragmentManager());

		setActionBarTitle(getString(R.string.title_activity_post));

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOffscreenPageLimit(3);
		mViewPager.setAdapter(mPostPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab.
		// We can also use ActionBar.Tab#select() to do this if we have a
		// reference to the
		// Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
		{
			@Override
			public void onPageSelected(int position)
			{
				setSelectedTab(position);
			}
		});

		mTabContainer = (ViewGroup) findViewById(R.id.tab_container);
		mTabs = new ArrayList<View>();
		
		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mPostPagerAdapter.getCount(); i++)
		{
			// Create a tab with text corresponding to the page title defined by
			// the adapter.
			// Also specify this Activity object, which implements the
			// TabListener interface, as the
			// listener for when this tab is selected.
			CharSequence title = mPostPagerAdapter.getPageTitle(i);
			CustomFontCheckableButton tab = (CustomFontCheckableButton) LayoutInflater.from(this).inflate(R.layout.actionbar_tab, mTabContainer, false);
			tab.setText(title);
			mTabs.add(tab);
			tab.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int index = mTabs.indexOf(v);
					mViewPager.setCurrentItem(index, false);
				}
			});
			mTabContainer.addView(tab);
		}
		setSelectedTab(0);
		
		// Turn off toolbar shadow, it would overlay our tabs
		findViewById(R.id.toolbar_shadow).setVisibility(View.GONE);
		if (Build.VERSION.SDK_INT >= 21)
		{
			mToolbar.setElevation(0);
		}
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		boolean ret = super.onCreateOptionsMenu(menu);
		mMenuAddPost = menu.findItem(R.id.menu_add_post);
		mMenuItemTag = menu.findItem(R.id.menu_tag);
		return ret;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_tag:
		{
			View view = mToolbar.findViewById(item.getItemId());
			showTagSearchPopup(view);
			return true;
		}
		}

		boolean ret = super.onOptionsItemSelected(item);
		if (item.getItemId() == R.id.menu_logout) {
			// If we logged out, update views
			showHideCreateAccount(true);
		}
		return ret;
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class PostPagerAdapter extends FragmentPagerAdapter
	{
		PostListFragment mFragmentPublished;
		PostListFragment mFragmentOutgoing;
		PostListFragment mFragmentDrafts;

		public PostPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int i)
		{
			Bundle args = new Bundle();
			if (i == 0)
			{
				if (mFragmentPublished == null)
				{
					mFragmentPublished = new PostListFragment();
					args.putString(PostListFragment.ARG_POST_LIST_TYPE, PostListType.PUBLISHED.toString());
				}
				mFragmentPublished.setStoryListListener(PostActivity.this);
				mFragmentPublished.setOnTagClickedListener(PostActivity.this);
				if (mCurrentSearchTag != null)
					mFragmentPublished.setTagFilter(mCurrentSearchTag);
				mFragmentPublished.setArguments(args);
				return mFragmentPublished;
			}
/*
			else if (i == 1)
			{
				if (mFragmentOutgoing == null)
				{
					mFragmentOutgoing = new PostListFragment();
					args.putString(PostListFragment.ARG_POST_LIST_TYPE, PostListType.OUTGOING.toString());
					mFragmentOutgoing.setStoryListListener(PostActivity.this);
					mFragmentOutgoing.setOnTagClickedListener(PostActivity.this);
					if (mCurrentSearchTag != null)
						mFragmentOutgoing.setTagFilter(mCurrentSearchTag);
					mFragmentOutgoing.setArguments(args);
				}
				return mFragmentOutgoing;
			}
*/
			else if (i == 1)
			{
				if (mFragmentDrafts == null)
				{
					mFragmentDrafts = new PostListFragment();
					args.putString(PostListFragment.ARG_POST_LIST_TYPE, PostListType.DRAFTS.toString());
					mFragmentDrafts.setStoryListListener(PostActivity.this);
					mFragmentDrafts.setOnTagClickedListener(PostActivity.this);
					if (mCurrentSearchTag != null)
						mFragmentDrafts.setTagFilter(mCurrentSearchTag);
					mFragmentDrafts.setArguments(args);
				}
				return mFragmentDrafts;
			}
			return null;
		}

		@Override
		public int getCount()
		{
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			switch (position)
			{
			case 0:
				return getString(R.string.post_title_published).toUpperCase();
			//case 1:
			//	return getString(R.string.post_title_outgoing).toUpperCase();
			case 1:
				return getString(R.string.post_title_drafts).toUpperCase();
			}
			return null;
		}

		public void setTagFilter(String tag)
		{
			if (mFragmentPublished != null)
				mFragmentPublished.setTagFilter(tag);
			if (mFragmentOutgoing != null)
				mFragmentOutgoing.setTagFilter(tag);
			if (mFragmentDrafts != null)
				mFragmentDrafts.setTagFilter(tag);
		}

		public void updateAdapter()
		{
			if (mFragmentPublished != null)
				mFragmentPublished.updateListAdapter();
			if (mFragmentOutgoing != null)
				mFragmentOutgoing.updateListAdapter();
			if (mFragmentDrafts != null)
				mFragmentDrafts.updateListAdapter();
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		showHideCreateAccount(false);

		if (mPostPagerAdapter != null)
			mPostPagerAdapter.updateAdapter();

		// Anyone telling us where to go?
		if (getIntent().hasExtra("go_to_tab"))
		{
			int tab = getIntent().getIntExtra("go_to_tab", -1);
			getIntent().removeExtra("go_to_tab");
			if (tab >= 0 && tab < 2)
				mViewPager.setCurrentItem(tab, false);
		}
	}

	@Override
	public void onTagClicked(String tag)
	{
		mCurrentSearchTag = tag;
		mPostPagerAdapter.setTagFilter(tag);
		if (tag != null)
		{
			mTabContainer.setVisibility(View.GONE);
		}
		else
		{
			// Clear the tag search. Show the tabs again!
			//
			int currentIndex = mViewPager.getCurrentItem();
			setSelectedTab(currentIndex);
			mTabContainer.setVisibility(View.VISIBLE);
		}
	}

	private void showTagSearchPopup(View anchorView)
	{
		try
		{
			LayoutInflater inflater = getLayoutInflater();
			final PopupWindow mMenuPopup = new PopupWindow(inflater.inflate(R.layout.story_search_by_tag, null, false), this.mViewPager.getWidth(),
					this.mViewPager.getHeight(), true);

			ListView lvTags = (ListView) mMenuPopup.getContentView().findViewById(R.id.lvTags);

			String[] rgTags = new String[0];
			// rgTags[0] = "#one";
			// rgTags[1] = "#two";
			// rgTags[2] = "#three";
			// rgTags[3] = "#four";

			ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.story_search_by_tag_item, R.id.tvTag, rgTags);
			lvTags.setAdapter(adapter);
			lvTags.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
				{
					String tag = (String) arg0.getAdapter().getItem(position);
					onTagClicked(tag);
					mMenuPopup.dismiss();
				}
			});

			EditText editTag = (EditText) mMenuPopup.getContentView().findViewById(R.id.editTag);
			editTag.setOnEditorActionListener(new OnEditorActionListener()
			{
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
				{
					if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_SEARCH)
					{
						onTagClicked(v.getText().toString());
						mMenuPopup.dismiss();
						return true;
					}
					return false;
				}
			});

			mMenuPopup.setOutsideTouchable(true);
			mMenuPopup.setBackgroundDrawable(new ColorDrawable(0x80ffffff));
			mMenuPopup.showAsDropDown(anchorView);
			mMenuPopup.getContentView().setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mMenuPopup.dismiss();
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	protected void prepareFullScreenView(FullScreenStoryItemView fullView)
	{
		super.prepareFullScreenView(fullView);
		fullView.showFavoriteButton(false);
	}

	@Override
	protected void configureActionBarForFullscreen(boolean fullscreen)
	{
		super.configureActionBarForFullscreen(fullscreen);
		if (mMenuAddPost != null)
			mMenuAddPost.setVisible(!fullscreen);
		if (mMenuItemTag != null)
			mMenuItemTag.setVisible(!fullscreen);

		if (!fullscreen)
		{
			getSupportActionBar().setDisplayShowCustomEnabled(true);
			setDisplayHomeAsUp(false);
			toggleActionBarTabs(true);
		}
		else
		{
			getSupportActionBar().setDisplayShowCustomEnabled(false);
			setDisplayHomeAsUp(true);
			toggleActionBarTabs(false);
		}
	}

	private void toggleActionBarTabs(boolean showTabs)
	{
		if (showTabs)
		{
			// Clear the tag search. Show the tabs again!
			//
			int currentIndex = mViewPager.getCurrentItem();
			setSelectedTab(currentIndex);
			mTabContainer.setVisibility(View.VISIBLE);
		}
		else
		{
			// Dont show tabs when we are searching for a tag
			mTabContainer.setVisibility(View.GONE);
		}
	}

	private void showHideCreateAccount(boolean animate)
	{
		if (!com.tribunezamaneh.rss.App.isSignedIn()) {
			if (mMenuAddPost != null)
				mMenuAddPost.setVisible(false);
			final WPSignInView signIn = new WPSignInView(this);
			signIn.setListener(new WPSignInView.OnLoginListener() {
				@Override
				public void onLoggedIn(String username, String password) {
					((ViewGroup)signIn.getParent()).removeView(signIn);
					com.tribunezamaneh.rss.App.getInstance().socialReader.ssettings.setXMLRPCUsername(username);
					com.tribunezamaneh.rss.App.getInstance().socialReader.ssettings.setXMLRPCPassword(password);
					if (mMenuAddPost != null)
						mMenuAddPost.setVisible(true);
					showHideCreateAccount(true);
					UIHelpers.hideSoftKeyboard(PostActivity.this);
				}
			});
			((ViewGroup)findViewById(R.id.post_root)).addView(signIn, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		}
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();

		// Reload the adapters after the the wipe!
		if (mPostPagerAdapter != null)
			mPostPagerAdapter.updateAdapter();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		if (mPostPagerAdapter != null)
			mPostPagerAdapter.updateAdapter();
	}
	
	private void setSelectedTab(int index)
	{
		for (int i = 0; i < mTabs.size(); i++)
		{
			View view = mTabs.get(i);
			view.setSelected(i == index);
		}
	}
}
