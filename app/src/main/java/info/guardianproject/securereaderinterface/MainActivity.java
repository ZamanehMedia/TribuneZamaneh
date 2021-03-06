package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.Settings.SyncMode;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SyncService;
import info.guardianproject.securereader.FeedFetcher.FeedFetchedCallback;
import info.guardianproject.securereaderinterface.adapters.StoryListAdapter;
import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.ActionProviderShare;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.views.StoryItemView;
import info.guardianproject.securereaderinterface.views.StoryListHintProxyView;
import info.guardianproject.securereaderinterface.views.StoryListView;
import info.guardianproject.securereaderinterface.views.StoryListHintProxyView.OnButtonClickedListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.PopupWindowCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

// HockeyApp SDK
//import net.hockeyapp.android.CrashManager;
//import net.hockeyapp.android.UpdateManager;

public class MainActivity extends ItemExpandActivity implements SyncService.SyncServiceListener
{
	public static String INTENT_EXTRA_SHOW_THIS_TYPE = "info.guardianproject.securereaderinterface.showThisFeedType";
	public static String INTENT_EXTRA_SHOW_THIS_FEED = "info.guardianproject.securereaderinterface.showThisFeedId";
	public static String INTENT_EXTRA_SHOW_THIS_ITEM = "info.guardianproject.securereaderinterface.showThisItemId";

	public static final boolean LOGGING = false;
	public static final String LOGTAG = "MainActivity";
	
	// HockeyApp SDK
	//public static String APP_ID = "3fa04d8b0a135d7f3bf58026cb125866";

	private boolean mIsInitialized;
	private long mShowItemId;
	private long mShowFeedId;
	private FeedFilterType mShowFeedFilterType;
	SocialReader socialReader;

	/*
	 * The action bar menu item for the "TAG" option. Only show this when a feed
	 * filter is set.
	 */
	MenuItem mMenuItemTag;
	boolean mShowTagMenuItem;
	MenuItem mMenuItemShare;
	MenuItem mMenuItemFeed;

	ActionProviderShare mShareActionProvider;

	StoryListView mStoryListView;

	boolean mIsLoading;
	private SyncMode mCurrentSyncMode;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		mCurrentSyncMode = App.getSettings().syncMode();
		super.onCreate(savedInstanceState);
	
		//getSupportActionBar().hide();

		setContentView(R.layout.activity_main);
		setMenuIdentifier(R.menu.activity_main);

		mStoryListView = (StoryListView) findViewById(R.id.storyList);
		mStoryListView.setListener(this);

		socialReader = ((App) getApplicationContext()).socialReader;
		App.getInstance().addSyncServiceListener(this);

		// Saved what we were looking at?
		if (savedInstanceState != null && savedInstanceState.containsKey("FeedFilterType"))
		{
			FeedFilterType type = Enum.valueOf(FeedFilterType.class, savedInstanceState.getString("FeedFilterType"));
			long feedId = savedInstanceState.getLong("FeedId", 0);
			UICallbacks.setFeedFilter(type, feedId, this);
		}
		else
		{
			UICallbacks.setFeedFilter(App.getInstance().getCurrentFeedFilterType(), App.getInstance().getCurrentFeedId(), MainActivity.this);
		}
		
		// HockeyApp SDK
		//checkForUpdates();
	}

	@Override
	protected void onDestroy() {
		App.getInstance().removeSyncServiceListener(this);
		super.onDestroy();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// If we are in the process of displaying the lock screen isFinishing is
		// actually
		// true, so avoid extra work!
		if (!isFinishing())
		{
			// If we have not shown help yet, open that on top
			if (!App.getSettings().hasShownHelp())
			{
				if (!App.getInstance().isActivityLocked())
				{
					Intent intent = new Intent(this, HelpActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					intent.putExtra("useLeftSideMenu", false);
					startActivity(intent);
				}
			}
			else
			{
				if (!mIsInitialized)
				{
					mIsInitialized = true;
					UICallbacks.setFeedFilter(App.getInstance().getCurrentFeedFilterType(), App.getInstance().getCurrentFeedId(), MainActivity.this);
					//getSupportActionBar().show();
				}
			}

		}

		// Called with flags of which item to show?
		Intent intent = getIntent();
		if (intent.hasExtra(INTENT_EXTRA_SHOW_THIS_ITEM) && intent.hasExtra(INTENT_EXTRA_SHOW_THIS_FEED))
		{
			this.mShowFeedId = intent.getLongExtra(INTENT_EXTRA_SHOW_THIS_FEED, 0);
			this.mShowItemId = intent.getLongExtra(INTENT_EXTRA_SHOW_THIS_ITEM, 0);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_FEED);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_ITEM);
		}
		if (intent.hasExtra(INTENT_EXTRA_SHOW_THIS_TYPE))
		{
			this.mShowFeedFilterType = (FeedFilterType) intent.getSerializableExtra(INTENT_EXTRA_SHOW_THIS_TYPE);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_TYPE);
		}
		else if (socialReader.getDefaultFeedId() >= 0) 
		{
			this.mShowFeedId = socialReader.getDefaultFeedId();
		}
		else
		{
			this.mShowFeedFilterType = null;
		}

		if (this.mShowFeedFilterType != null)
		{
			if (LOGGING) 
				Log.d(LOGTAG, "INTENT_EXTRA_SHOW_THIS_TYPE was set, show type " + this.mShowFeedFilterType.toString());
			
			UICallbacks.setFeedFilter(this.mShowFeedFilterType, -1, MainActivity.this);
			this.mShowFeedFilterType = null;
		}
		else if (this.mShowFeedId != 0)
		{
			if (LOGGING)
				Log.d(LOGTAG, "INTENT_EXTRA_SHOW_THIS_FEED was set, show feed id " + this.mShowFeedId);
			
			UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, this.mShowFeedId, MainActivity.this);
		}

		// Resume sync if we are back from Orbot
		updateProxyView();
		
		// HockeyApp SDK
		//checkForCrashes();
	}
	
	// HockeyApp SDK
	/*private void checkForCrashes() {
		CrashManager.register(this, APP_ID);
	}*/	
	
	// HockeyApp SDK
	/*private void checkForUpdates() {
		//Remove this for store builds!
		UpdateManager.register(this, APP_ID);
	}*/	

	@Override
	protected void onAfterResumeAnimation()
	{
		super.onAfterResumeAnimation();
		if (!isFinishing() && App.getSettings().hasShownHelp())
		{
			boolean willShowMenuHint = false;
			//if (mLeftSideMenu != null)
			//	willShowMenuHint = mLeftSideMenu.showMenuHintIfNotShown();
			if (socialReader.getFeedsList().size() > 0)
			{
				if (willShowMenuHint)
				{
					// Allow the menu animation some time before we start the
					// heavy work!
					mStoryListView.postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							refreshList();
						}
					}, 6000);
				}
				else
				{
					refreshList();
				}
			}
		}

	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		
		// save index and top position
		Point scrollPosition = mStoryListView.saveScrollPosition();
		
		setContentView(R.layout.activity_main);
		mStoryListView = (StoryListView) findViewById(R.id.storyList);
		mStoryListView.setListener(this);
		mStoryListView.setAdapter(mAdapter);

		// restore index and position
		mStoryListView.restoreScrollPosition(scrollPosition);
	}
	
	private void syncSpinnerToCurrentItem()
	{
		if (getCurrentFeedFilterType() == FeedFilterType.ALL_FEEDS)
			setActionBarTitle(getString(R.string.feed_filter_all_feeds));
		else if (getCurrentFeedFilterType() == FeedFilterType.POPULAR)
			setActionBarTitle(getString(R.string.feed_filter_popular));
		else if (getCurrentFeedFilterType() == FeedFilterType.SHARED)
			setActionBarTitle(getString(R.string.feed_filter_shared_stories));
		else if (getCurrentFeedFilterType() == FeedFilterType.FAVORITES)
			setActionBarTitle(getString(R.string.feed_filter_favorites));
		else if (getCurrentFeed() != null)
			setActionBarTitle(getCurrentFeed().getTitle());
		else
			setActionBarTitle(getString(R.string.feed_filter_all_feeds));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		boolean ret = super.onCreateOptionsMenu(menu);

		// Find the tag menu item. Only to be shown when feed filter is set!
		mMenuItemTag = menu.findItem(R.id.menu_tag);
		mMenuItemTag.setVisible(mShowTagMenuItem);

		// Locate MenuItem with ShareActionProvider
		mMenuItemShare = menu.findItem(R.id.menu_share);
		if (mMenuItemShare != null)
		{
			mShareActionProvider = new ActionProviderShare(getSupportActionBar().getThemedContext());
			mShareActionProvider.setFeed(getCurrentFeed());
			MenuItemCompat.setActionProvider(mMenuItemShare, mShareActionProvider);
		}
		// Locate MenuItem with ShareActionProvider
		// mMenuItemFeed = menu.findItem(R.id.menu_feed);
		// if (mMenuItemFeed != null)
		// {
		// mMenuItemFeed.setActionProvider(new ActionProviderFeedFilter(this));
		// }

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

		return super.onOptionsItemSelected(item);
	}

	@SuppressLint("NewApi")
	private void setTagItemVisible(boolean bVisible)
	{
		mShowTagMenuItem = bVisible && BuildConfig.UI_ENABLE_TAGS;
		if (mMenuItemTag != null)
		{
			mMenuItemTag.setVisible(mShowTagMenuItem);
			if (Build.VERSION.SDK_INT >= 11)
				invalidateOptionsMenu();
		}
	}

	private void showTagSearchPopup(View anchorView)
	{
		try
		{
			LayoutInflater inflater = getLayoutInflater();
			final PopupWindow mMenuPopup = new PopupWindow(inflater.inflate(R.layout.story_search_by_tag, null, false), this.mStoryListView.getWidth(),
					this.mStoryListView.getHeight(), true);

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
					UICallbacks.setTagFilter(tag, null);
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
						UICallbacks.setTagFilter(v.getText().toString(), null);
						mMenuPopup.dismiss();
						return true;
					}
					return false;
				}
			});

			mMenuPopup.setOutsideTouchable(true);
			mMenuPopup.setBackgroundDrawable(new ColorDrawable(0x80ffffff));
			PopupWindowCompat.showAsDropDown(mMenuPopup, anchorView, 0, 0, Gravity.TOP);
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
	public void onResync()
	{
		if (getCurrentFeedFilterType() == FeedFilterType.SHARED)
			refreshList();
		else
			onResync(getCurrentFeed(), true);
	}

	private void onResync(Feed feed, boolean showLoadingSpinner)
	{
		if (socialReader.isOnline() == SocialReader.NOT_ONLINE_NO_PROXY)
		{
			socialReader.connectProxy(this);
		}

		if (socialReader.isOnline() == SocialReader.ONLINE)
		{
			setIsLoading(showLoadingSpinner);
			if (feed == null)
				socialReader.manualSyncSubscribedFeeds(mFeedFetchedCallback);
			else
				socialReader.manualSyncFeed(feed, mFeedFetchedCallback);
		}
	}

	@Override
	protected void configureActionBarForFullscreen(boolean fullscreen)
	{
		super.configureActionBarForFullscreen(fullscreen);
		if (mMenuItemFeed != null)
			mMenuItemFeed.setVisible(!fullscreen);
		if (mMenuItemShare != null)
			mMenuItemShare.setVisible(!fullscreen);

		if (!fullscreen)
		{
			getSupportActionBar().setDisplayShowCustomEnabled(true);
			setDisplayHomeAsUp(false);
		}
		else
		{
			getSupportActionBar().setDisplayShowCustomEnabled(false);
			setDisplayHomeAsUp(true);
		}
	}

	private void setIsLoading(boolean isLoading)
	{
		// Are we sure we are not loading anything?
		if (!isLoading && mAdapter != null && mAdapter.getCount() == 0) {
			if (getCurrentFeedFilterType() == FeedFilterType.ALL_FEEDS ||
					getCurrentFeedFilterType() == FeedFilterType.SINGLE_FEED) {
				if (socialReader.getSyncService() != null) {
					for (SyncService.SyncTask task : socialReader.getSyncService().syncList) {
						if (task.type == SyncService.SyncTask.TYPE_FEED) {
							if (task.status == SyncService.SyncTask.CREATED ||
									task.status == SyncService.SyncTask.QUEUED ||
									task.status == SyncService.SyncTask.STARTED) {
								if (getCurrentFeedFilterType() == FeedFilterType.ALL_FEEDS ||
										(getCurrentFeedFilterType() == FeedFilterType.SINGLE_FEED && getCurrentFeed() != null && task.feed != null && getCurrentFeed().getDatabaseId() == task.feed.getDatabaseId())) {
									isLoading = true; // No, one is actually syncing!
									break;
								}
							}
						}
					}
				}
			}
		}

		// Is all initialized?
		if (!isLoading &&
				getCurrentFeedFilterType() == FeedFilterType.ALL_FEEDS &&
				mStoryListView != null && mStoryListView.getListView() != null && mStoryListView.getListView().getAdapter().isEmpty()) {
			ArrayList<Feed> allFeeds = socialReader.getFeedsList();
			if (allFeeds == null || allFeeds.isEmpty() || allFeeds.get(0).getNetworkPullDate() == null) {
				// No feeds, so still loading!
				isLoading = true;
			}
		}

		mIsLoading = isLoading;
		if (mStoryListView != null)
			mStoryListView.setIsLoading(mIsLoading);
		updateProxyView();
		refreshLeftSideMenu();
	}

	private void showError(String error)
	{
		if (mStoryListView != null)
		{
			if (TextUtils.isEmpty(error))
				mStoryListView.hideError();
			else
				mStoryListView.showError(error);
		}
	}
	
	private UpdateFeedListTask mUpdateListTask;
	
	@SuppressLint({ "InlinedApi", "NewApi" })
	private void updateList(boolean isUpdate)
	{
		if (!isUpdate)
			setIsLoading(true);
		
		if (mUpdateListTask == null || !mUpdateListTask.isForTypeAndFeed(getCurrentFeedFilterType(), getCurrentFeed()))
		{
			if (mUpdateListTask != null)
			{
				mUpdateListTask.cancel(true);
			}
			mUpdateListTask = new UpdateFeedListTask(this, getCurrentFeedFilterType(), getCurrentFeed(), isUpdate);
			mUpdateListTask.execute();
		}
		else
		{
			if (LOGGING) 
				Log.v(LOGTAG, "Already updating feed list, ignore event for same feed");
		}
		
		syncSpinnerToCurrentItem();
		if (mShareActionProvider != null)
			mShareActionProvider.setFeed(getCurrentFeed());
		mStoryListView.setCurrentFeed(getCurrentFeed());
	}

	private void refreshList()
	{
		updateList(true);
	}
	
	private void refreshListIfCurrent(Feed feed)
	{
		if (getCurrentFeedFilterType() == FeedFilterType.ALL_FEEDS)
	{
		refreshList();
	}
	else if (getCurrentFeedFilterType() == FeedFilterType.SINGLE_FEED &&
	getCurrentFeed() != null && getCurrentFeed().getDatabaseId() == feed.getDatabaseId())
	{
		//TODO - this seems a little ugly
		App.getInstance().updateCurrentFeed(feed);
		refreshList();
	}
}

	private void checkShowStoryFullScreen()
	{
		if (mShowItemId != 0 && mAdapter != null)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Loaded feed and INTENT_EXTRA_SHOW_THIS_ITEM was set to " + mShowItemId + ". Try to show it");

			for (int itemIndex = 0; itemIndex < mAdapter.getItems().size(); itemIndex++)
			{
				Item item = mAdapter.getItems().get(itemIndex);
				if (item.getDatabaseId() == mShowItemId)
				{
					if (LOGGING)
						Log.v(LOGTAG, "Found item at index " + itemIndex);

					this.openStoryFullscreen(mAdapter, itemIndex, mStoryListView.getListView(), null);
				}
			}
			mShowFeedId = 0;
			mShowItemId = 0;
		}
	}

	private ArrayList<Item> flattenFeedArray(ArrayList<Feed> listOfFeeds)
	{
		ArrayList<Item> items = new ArrayList<Item>();
		if (listOfFeeds != null)
		{
			Iterator<Feed> itFeed = listOfFeeds.iterator();
			while (itFeed.hasNext())
			{
				Feed feed = itFeed.next();
				if (LOGGING)
					Log.v(LOGTAG, "Adding " + feed.getItemCount() + " items");
				items.addAll(feed.getItems());
			}
		}
		if (LOGGING)
			Log.v(LOGTAG, "There are " + items.size() + " items total");
		return items;
	}

	private boolean showErrorForFeed(Feed feed, boolean onlyRemoveIfAllOk)
	{
		if (feed.getStatus() == Feed.STATUS_LAST_SYNC_FAILED_404)
		{
			if (!onlyRemoveIfAllOk)
				this.showError(getString(R.string.error_feed_404));
		}
		else if (feed.getStatus() == Feed.STATUS_LAST_SYNC_FAILED_BAD_URL)
		{
			if (!onlyRemoveIfAllOk)
				this.showError(getString(R.string.error_feed_bad_url));
		}
		else if (feed.getStatus() == Feed.STATUS_LAST_SYNC_FAILED_UNKNOWN)
		{
			if (!onlyRemoveIfAllOk)
				this.showError(getString(R.string.error_feed_unknown));
		}
		else
		{
			this.showError(null);
			return false;
		}
		return true;
	}

	private final FeedFetchedCallback mFeedFetchedCallback = new FeedFetchedCallback()
	{
		@Override
		public void feedFetched(Feed _feed)
		{
			if (LOGGING)
				Log.v(LOGTAG, "feedFetched Callback");
			refreshListIfCurrent(_feed);
			refreshLeftSideMenu();
		}
	};
	private StoryListHintProxyView mProxyView;
	private FeedFilterType mCurrentShownFeedFilterType = null;
	private Feed mCurrentShownFeed = null;
	private boolean mBackShouldOpenAllFeeds;
	private StoryListAdapter mAdapter;

	@Override
	public boolean onCommand(Context context, int command, Bundle commandParameters)
	{
		if (command == R.integer.command_add_feed_manual)
		{
			// First add it to reader!
			App.getInstance().socialReader.addFeedByURL(commandParameters.getString("uri"), MainActivity.this.mFeedFetchedCallback);
			refreshList();
		}
		return super.onCommand(context, command, commandParameters);
	}

	@Override
	public void onHeaderCreated(View headerView, int resIdHeader)
	{
		if (resIdHeader == R.layout.story_list_hint_proxy)
		{
			mProxyView = (StoryListHintProxyView) headerView;
			updateProxyView();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);

		// Probably opening a popup (Feed Spinner). Remember what sync mode was
		// set to when we open.
		if (!hasFocus)
		{
			mCurrentSyncMode = App.getSettings().syncMode();
		}
		else
		{
			if (mCurrentSyncMode != App.getSettings().syncMode())
			{
				mCurrentSyncMode = App.getSettings().syncMode();
				refreshList();
			}
		}
	}

	private void updateProxyView()
	{
		if (mProxyView == null)
			return;

		if (!App.getSettings().requireProxy() || mIsLoading || !isActivityResumed())
		{
			mProxyView.setVisibility(View.GONE);
		}
		else
		{
			mProxyView.setOnButtonClickedListener(new OnButtonClickedListener()
			{
				private StoryListHintProxyView mView;

				@Override
				public void onNoNetClicked()
				{
					int onlineMode = App.getInstance().socialReader.isOnline();
					mView.setIsOnline(!(onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK),
							onlineMode == SocialReader.ONLINE);
				}

				@Override
				public void onGoOnlineClicked()
				{
					onResync();
				}

				public OnButtonClickedListener init(StoryListHintProxyView view)
				{
					mView = view;
					return this;
				}
			}.init(mProxyView));
			int onlineMode = App.getInstance().socialReader.isOnline();
			mProxyView.setIsOnline(!(onlineMode == SocialReader.NOT_ONLINE_NO_WIFI || onlineMode == SocialReader.NOT_ONLINE_NO_WIFI_OR_NETWORK),
					onlineMode == SocialReader.ONLINE);
			mProxyView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();
		UICallbacks.setFeedFilter(FeedFilterType.SINGLE_FEED, -1, this);
	}

	@Override
	public void syncEvent(SyncService.SyncTask syncTask) {
		if (LOGGING)
			Log.v(LOGTAG, "Got a syncEvent");

		if (syncTask.type == SyncService.SyncTask.TYPE_FEED && syncTask.status == SyncService.SyncTask.FINISHED) {
			refreshListIfCurrent(syncTask.feed);
			refreshLeftSideMenu();
		}
	}

	class UpdateFeedListTask extends ThreadedTask<Void, Void, ArrayList<Feed>>
	{
		private Context mContext;
		private FeedFilterType mFeedFilterType;
		private Feed mFeed;
		private boolean mIsUpdate;

		public UpdateFeedListTask(Context context, FeedFilterType feedFilterType, Feed feed, boolean isUpdate)
		{
			mContext = context;
			mFeedFilterType = feedFilterType;
			mFeed = (feed != null) ? new Feed(feed) : null;
			mIsUpdate = isUpdate;
		}

		public boolean isForTypeAndFeed(FeedFilterType feedFilterType, Feed feed)
		{
			return mFeedFilterType == feedFilterType &&
					((mFeed == null && feed == null) ||
					 (mFeed != null && feed != null && mFeed.getDatabaseId() == feed.getDatabaseId()));
		}
		
		@Override
		protected ArrayList<Feed> doInBackground(Void... values)
		{
			if (LOGGING)
				Log.v(LOGTAG, "UpdateFeedListTask: doInBackground");

			ArrayList<Feed> listOfFeeds = null;

			if (mFeedFilterType == FeedFilterType.SHARED)
			{
				listOfFeeds = new ArrayList<Feed>();
				listOfFeeds.add(socialReader.getAllShared());
			}
			else if (mFeedFilterType == FeedFilterType.FAVORITES)
			{
				listOfFeeds = new ArrayList<Feed>();
				listOfFeeds.add(socialReader.getAllFavorites());
			}
			else if (mFeedFilterType == FeedFilterType.ALL_FEEDS || mFeed == null)
			{
				if (LOGGING) 
					Log.v(LOGTAG, "UpdateFeedsTask: all subscribed");
				listOfFeeds = new ArrayList<Feed>();
				listOfFeeds.add(socialReader.getSubscribedFeedItems());
			}
			else
			{
				if (LOGGING)
					Log.v(LOGTAG, "UpdateFeedsTask");
				listOfFeeds = new ArrayList<Feed>();
				listOfFeeds.add(socialReader.getFeed(mFeed));
			}

			return listOfFeeds;
		}

		@Override
		protected void onPostExecute(ArrayList<Feed> result)
		{
			if (LOGGING)
				Log.v(LOGTAG, "RefreshFeedsTask: finished");

			switch (mFeedFilterType)
			{
			case ALL_FEEDS:
			case SINGLE_FEED:
			{
				Feed _feed = result.get(0);
				if (mFeed != null && mFeed.getDatabaseId() == _feed.getDatabaseId())
					mFeed = _feed; // need to update to get NetworkPullDate

				// Any errors to show?
				showError(null);
				for (Feed feed : result)
				{
					if (feed != null)
					{
						if (showErrorForFeed(feed, !mIsLoading))
							break;
					}
				}

				int headerViewId = 0;
				ArrayList<Item> items = result.get(0).getItems();
				if (BuildConfig.UI_ENABLE_PROXY_VIEW) {
					if (items == null || items.size() == 0) {
						headerViewId = R.layout.story_list_hint_proxy;
					}
				}
				updateStoryListItems(mContext, items, headerViewId, mIsUpdate);
				checkShowStoryFullScreen();
			}
				break;

			case FAVORITES:
			{
				ArrayList<Item> favoriteItems = new ArrayList<Item>();
				if (result != null)
				{
					Iterator<Feed> itFeed = result.iterator();
					while (itFeed.hasNext())
					{
						Feed feed = itFeed.next();
						favoriteItems.addAll(feed.getItems());
					}
				}

				boolean fakedItems = false;
				if (favoriteItems.size() == 0)
				{
					// No real favorites, so we fake some by randomly picking
					// Items from
					// the "all items" feed
					fakedItems = true;

					Feed allSubscribed = App.getInstance().socialReader.getSubscribedFeedItems();
					if (allSubscribed != null)
					{
						ArrayList<Item> allSubscribedItems = allSubscribed.getItems();

						// Truncate to random 5 items
						Collections.shuffle(allSubscribedItems);
						favoriteItems.addAll(allSubscribedItems.subList(0, Math.min(5, allSubscribedItems.size())));
					}
				}

				boolean shouldShowAddFavoriteHint = fakedItems || favoriteItems == null || favoriteItems.size() == 0;
				updateStoryListItems(mContext, favoriteItems, shouldShowAddFavoriteHint ? R.layout.story_list_hint_add_favorite : 0, mIsUpdate);
				break;
			}

			case POPULAR:
				// TODO
				break;

			case SHARED:
			{
				ArrayList<Item> items = flattenFeedArray(result);
				boolean shouldShowNoSharedHint = (items == null || items.size() == 0);
				updateStoryListItems(mContext, items, shouldShowNoSharedHint ? R.layout.story_list_hint_no_shared : 0, mIsUpdate);
				checkShowStoryFullScreen();
			}
				break;
			}
			setIsLoading(false);
			mUpdateListTask = null;
		}
	}

	@Override
	protected void onUnlocked() {
		super.onUnlocked();
		socialReader = ((App) getApplicationContext()).socialReader;
		UICallbacks.setFeedFilter(App.getInstance().getCurrentFeedFilterType(), App.getInstance().getCurrentFeedId(), MainActivity.this);
	}

	@Override
	public void onFeedSelect(FeedFilterType type, long feedId, Object source)
	{
		super.onFeedSelect(type, feedId, source);
		boolean visibleTags = false;
		if (getCurrentFeedFilterType() == FeedFilterType.SINGLE_FEED && getCurrentFeed() != null)
		{
			visibleTags = true;
		}
		setTagItemVisible(visibleTags);
		
		boolean isUpdate = true;
		if (mCurrentShownFeedFilterType != getCurrentFeedFilterType())
			isUpdate = false;
		else if (getCurrentFeedFilterType() == FeedFilterType.SINGLE_FEED
				&& (mCurrentShownFeed == null || getCurrentFeed() == null || mCurrentShownFeed.getDatabaseId() != getCurrentFeed().getDatabaseId()))
			isUpdate = false;
		
		mCurrentShownFeedFilterType = getCurrentFeedFilterType();
		mCurrentShownFeed = getCurrentFeed();

		// If clicking story source to filter feed, "back" should not close app but get us back to "ALL FEEDS" (task #4292)
		//
		mBackShouldOpenAllFeeds = false;
		if (mCurrentShownFeedFilterType == FeedFilterType.SINGLE_FEED)
		{
			if (source instanceof StoryListAdapter || source instanceof StoryItemView)
				mBackShouldOpenAllFeeds = true;
		}

		updateList(isUpdate);
	}
	
	@Override
	public void onItemFavoriteStatusChanged(Item item)
	{
		// An item has been marked/unmarked as favorite. Update the list
		// of favorites to pick
		// up this change!
		if (getCurrentFeedFilterType() == FeedFilterType.FAVORITES)
			refreshList();
	}

	@Override
	public void onRequestResync(Feed feed)
	{
		// Only show spinner if updating current feed
		boolean showLoader = ((getCurrentFeedFilterType() == FeedFilterType.ALL_FEEDS && feed == null) ||
							  (getCurrentFeedFilterType() == FeedFilterType.SINGLE_FEED && getCurrentFeed() != null && feed != null && getCurrentFeed().getDatabaseId() == feed.getDatabaseId()));
		onResync(feed, showLoader);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		// Save what we are currently looking at, so we can restore that later
		//
		outState.putString("FeedFilterType", App.getInstance().getCurrentFeedFilterType().name());
		outState.putLong("FeedId", App.getInstance().getCurrentFeedId());
	}
	
	@Override
	public void onBackPressed()
	{
		if (mBackShouldOpenAllFeeds)
		{
			mBackShouldOpenAllFeeds = false;
			UICallbacks.setFeedFilter(FeedFilterType.ALL_FEEDS, -1, this);
		}
		else
		{
			super.onBackPressed();
		}
	}
	
	private void updateStoryListItems(Context context, ArrayList<Item> items, int headerView, boolean isUpdate)
	{
		ArrayList<Item> sortedItems = sortItemsOnPublicationTime(items);
		if (mAdapter == null)
		{
			mAdapter = new StoryListAdapter(context, sortedItems);
		}
		else if (!isUpdate)
		{
			mStoryListView.setAdapter(null);
		}
		this.mStoryListView.setAdapter(mAdapter);
		mAdapter.setHeaderView(headerView, false);
		mAdapter.updateItems(sortedItems, isUpdate);
	}

	private ArrayList<Item> sortItemsOnPublicationTime(ArrayList<Item> unsortedItems)
	{
		if (unsortedItems == null)
			return null;

		ArrayList<Item> items = new ArrayList<Item>(unsortedItems);
		Collections.sort(items, new Comparator<Item>()
		{
			@Override
			public int compare(Item i1, Item i2)
			{
				if (i1.equals(i2))
					return 0;
				else if (i1.getPublicationTime() == null && i2.getPublicationTime() == null)
					return 0;
				else if (i1.getPublicationTime() == null)
					return 1;
				else if (i2.getPublicationTime() == null)
					return -1;
				return i2.getPublicationTime().compareTo(i1.getPublicationTime());
			}
		});
		return items;
	}
}
