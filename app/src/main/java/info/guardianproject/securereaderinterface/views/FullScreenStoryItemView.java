package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.BuildConfig;
import info.guardianproject.securereaderinterface.adapters.DownloadsAdapter;
import info.guardianproject.securereaderinterface.adapters.ShareSpinnerAdapter;
import info.guardianproject.securereaderinterface.adapters.StoryListAdapter;
import info.guardianproject.securereaderinterface.installer.SecureBluetoothSenderFragment;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import info.guardianproject.securereaderinterface.widgets.CheckableImageView;
import info.guardianproject.securereaderinterface.widgets.compat.Spinner;
import info.guardianproject.securereaderinterface.R;

import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

public class FullScreenStoryItemView extends FrameLayout
{
	protected static final String LOGTAG = "FullScreenStoryItemView";
	public static final boolean LOGGING = false;
	
	private View mBtnComments;
	private View mBtnTextSize;
	private CheckableImageView mBtnFavorite;
	private ShareSpinnerAdapter mShareAdapter;
	private ViewPager mContentPager;
	private ContentPagerAdapter mContentPagerAdapter;
	private View mPanelTextSize;
	private SeekBar mSeekBarTextSize;
	
	private StoryListAdapter mItemAdapter;
	private int mCurrentIndex;
	private SparseArray<Rect> mInitialViewPositions;
	private SparseArray<Rect> mFinalViewPositions;
	

	public FullScreenStoryItemView(Context context)
	{
		super(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.story_item, this);
		initialize();
	}

	public FullScreenStoryItemView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.story_item, this);
		initialize();
	}



	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_UP && this.mPanelTextSize.getVisibility() == View.VISIBLE)
		{
			hideTextSizePanel();
		}
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		super.onTouchEvent(event);
		return true;
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig)
	{
		this.removeAllViews();
		super.onConfigurationChanged(newConfig);
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(R.layout.story_item, this);
		initialize();
		setCurrentStoryIndex(mCurrentIndex);
		refresh();
	}

	private void initialize()
	{
		setBackgroundResource(R.drawable.background_detail);
		mContentPager = (ViewPager) findViewById(R.id.horizontalPagerContent);
		mContentPagerAdapter = new ContentPagerAdapter();
		mContentPager.setAdapter(mContentPagerAdapter);
		
		View toolbar = findViewById(R.id.storyToolbar);

		// Read comments
		//
		mBtnComments = toolbar.findViewById(R.id.btnComments);
		mBtnComments.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showComments();
			}
		});

		// Disable comments?
		if (!BuildConfig.UI_ENABLE_COMMENTS)
		{
			mBtnComments.setVisibility(View.GONE);
			// toolbar.findViewById(R.id.separatorComments).setVisibility(View.GONE);
		}

		// Text Size
		//
		mBtnTextSize = toolbar.findViewById(R.id.btnTextSize);
		mBtnTextSize.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (mPanelTextSize.getVisibility() == View.GONE)
				{
					int adjustment = App.getSettings().getContentFontSizeAdjustment();
					mSeekBarTextSize.setProgress(adjustment + 8);
					mPanelTextSize.setVisibility(View.VISIBLE);
					AnimationHelpers.fadeIn(mPanelTextSize, 500, 0, false);
				}
				else
				{
					hideTextSizePanel();
				}
			}
		});
		
		mPanelTextSize = findViewById(R.id.textSizePanel);
		mSeekBarTextSize = (SeekBar) mPanelTextSize.findViewById(R.id.seekBarTextSize);
		mSeekBarTextSize.setMax(16);
		mSeekBarTextSize.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser)
				{
					int adjustment = App.getSettings().getContentFontSizeAdjustment() + 8;
					if (adjustment != progress)
					{
						App.getSettings().setContentFontSizeAdjustment(progress - 8);
						FullScreenStoryItemView.this.removeCallbacks(mUpdateTextSizeInChildren);
						FullScreenStoryItemView.this.post(mUpdateTextSizeInChildren);
					}
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		AnimationHelpers.fadeOut(mPanelTextSize, 0, 0, false);
		mPanelTextSize.setVisibility(View.GONE);

		// Favorite
		//
		mBtnFavorite = (CheckableImageView) toolbar.findViewById(R.id.chkFavorite);
		mBtnFavorite.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (getCurrentStory() != null)
				{
					CheckableImageView view = (CheckableImageView) v;
					view.toggle();
					App.getInstance().socialReader.markItemAsFavorite(getCurrentStory(), view.isChecked());
					UICallbacks.itemFavoriteStatusChanged(getCurrentStory());
				}
			}
		});

		// Share
		//
		Spinner spinnerShare = (Spinner) toolbar.findViewById(R.id.spinnerShare);
		mShareAdapter = new ShareSpinnerAdapter(spinnerShare, getContext(), R.string.story_item_share_popup_title, R.layout.share_story_item_button);
		spinnerShare.setAdapter(mShareAdapter);
		spinnerShare.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				ShareSpinnerAdapter adapter = (ShareSpinnerAdapter) parent.getAdapter();
				Intent shareIntent = adapter.getIntentAtPosition(position);
				if (adapter.isSecureBTShareIntent(shareIntent))
				{
					// BT Share is a dialog popup, so handle that here.
			        FragmentManager fm = ((FragmentActivity)getContext()).getSupportFragmentManager();
			        SecureBluetoothSenderFragment dialogSendShare = new SecureBluetoothSenderFragment(); 
			        
			        // Get the share intent and make sure to forward it on to our fragment
			        Bundle args = new Bundle();
			        args.putParcelable("intent", shareIntent.getParcelableExtra("intent"));
			        dialogSendShare.setArguments(args);
			        
			        dialogSendShare.show(fm, App.FRAGMENT_TAG_SEND_BT_SHARE);
					return;
				}
				if (adapter.isSecureChatIntent(shareIntent))
					shareIntent = App.getInstance().socialReader.getSecureShareIntent(getCurrentStory(), false);
				if (shareIntent != null)
				{
					/*
					if (!App.getSettings().chatUsernamePasswordSet() 
							&& App.getInstance().socialReader.ssettings.getChatSecureUsername() != null
							&& App.getInstance().socialReader.ssettings.getChatSecurePassword() != null) {
					*/	
						/*
						ima://foo:pass@bar.com/
						action = android.intent.action.INSERT 
						 */
						/*
						Intent usernamePasswordIntent = new Intent(Intent.ACTION_INSERT, 
								Uri.parse("ima://"+App.getInstance().socialReader.ssettings.getChatSecureUsername()+":"
										+App.getInstance().socialReader.ssettings.getChatSecurePassword()+"@dukgo.com/"));
						*/
/*
 * 						Possible Example:
 * 						if (context instanceof FragmentActivityWithMenu)
 *							((FragmentActivityWithMenu) context).startActivityForResultAsInternal(intent, -1);
 *						else
 *							context.startActivity(intent);						
 */
						//((Activity)getContext()).startActivityForResult(usernamePasswordIntent, UICallbacks.RequestCode.CREATE_CHAT_ACCOUNT); 
						//getContext().startActivity(usernamePasswordIntent);
						
						// How to tell if it worked?
						//((Activity)context).startActivityForResult(usernamePasswordIntent,REGISTER_CHAT_USERNAME_PASSWORD);
						// if it is OK then App.getSettings().setChatUsernamePasswordSet();
					/*
					} else if (App.getInstance().socialReader.ssettings.getChatSecureUsername() == null ||
							App.getInstance().socialReader.ssettings.getChatSecurePassword() == null) {
						// Register Social Reporter username/password
						
					} else {
					*/
						getContext().startActivity(shareIntent);
					/*}*/
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0)
			{
			}
		});
	}

	protected void hideTextSizePanel() {
		AnimationHelpers.fadeOut(mPanelTextSize, 500, 0, false, new FadeInFadeOutListener()
		{
			@Override
			public void onFadeInStarted(View view) {
			}

			@Override
			public void onFadeInEnded(View view) {
			}

			@Override
			public void onFadeOutStarted(View view) {
			}

			@Override
			public void onFadeOutEnded(View view) {
				view.setVisibility(View.GONE);
			}
		}); 
	}

	private Runnable mUpdateTextSizeInChildren = new Runnable()
	{
		@Override
		public void run() {
			for (int iChild = 0; iChild < mContentPager.getChildCount(); iChild++)
			{
				StoryItemView storyItemView = (StoryItemView) mContentPager.getChildAt(iChild).getTag();
				if (storyItemView != null)
					storyItemView.updateTextSize();
			}
		}
	};
	
	public Item getCurrentStory()
	{
		if (mItemAdapter == null)
			return null;
		return mItemAdapter.getDataItem(mCurrentIndex);
	}

	public int getCurrentStoryIndex()
	{
		return mCurrentIndex;
	}
	
	public void setStory(StoryListAdapter itemAdapter, int currentIndex, SparseArray<Rect> initialViewPositions)
	{
		mItemAdapter = itemAdapter;
		setCurrentStoryIndex(currentIndex);
		mInitialViewPositions = initialViewPositions;
		mFinalViewPositions = initialViewPositions;
		//mContentPager.setCurrentItem(currentIndex);
		refresh();
	}

	private void setCurrentStoryIndex(int index)
	{
		mCurrentIndex = index;
		updateNumberOfComments();
		Item current = getCurrentStory();
		if (current != null)
		{
			mBtnFavorite.setChecked(current.getFavorite());
			mShareAdapter.clear();
			Intent shareIntent = App.getInstance().socialReader.getShareIntent(current);
			mShareAdapter.addSecureBTShareResolver(shareIntent);
			//mShareAdapter.addSecureChatShareResolver(App.getInstance().socialReader.getSecureShareIntent(getCurrentStory(), true));
			// mShareAdapter.addIntentResolvers(App.getInstance().socialReader.getSecureShareIntent(getCurrentStory()),
			// PackageHelper.URI_CHATSECURE,
			// R.string.share_via_secure_chat, R.drawable.ic_share_sharer);
			mShareAdapter.addIntentResolvers(shareIntent);
			DownloadsAdapter.viewed(current.getDatabaseId());
			
			Feed tempFeed = new Feed();
			tempFeed.setDatabaseId(current.getFeedId());
			if (!App.getInstance().socialReader.isFeedCommentable(tempFeed))
			{
				mBtnComments.setVisibility(View.GONE);
			} else {
				mBtnComments.setVisibility(View.VISIBLE);
			}
		}
	}

	public void refresh()
	{
		mContentPagerAdapter.notifyDataSetChanged();
	}

	private void showComments()
	{
		Item currentStory = getCurrentStory();
		if (currentStory != null)
		{
			Bundle params = new Bundle();
			params.putSerializable("item", currentStory);
			if (LOGGING)
				Log.v(LOGTAG, "Show comments for item: " + currentStory.getDatabaseId());
			UICallbacks.handleCommand(getContext(), R.integer.command_comment, params);
		}
	}

	public void showFavoriteButton(boolean bShow)
	{
		if (!bShow)
			this.mBtnFavorite.setVisibility(View.GONE);
		else
			this.mBtnFavorite.setVisibility(View.VISIBLE);
	}

	private void updateNumberOfComments()
	{
		// if (mBtnComments != null)
		// {
		// int numberOfComments = 0;
		// if (getCurrentStory() != null)
		// numberOfComments = getCurrentStory().getNumberOfComments();
		// ((TextView)
		// mBtnComments.findViewById(R.id.tvNumComments)).setText(String.valueOf(numberOfComments));
		// }
	}
	
	public void onBeforeCollapse()
	{
		StoryItemView storyItemView = mContentPagerAdapter.getCurrentView();
		if (storyItemView != null)
			storyItemView.resetToStoredPositions(mFinalViewPositions, ExpandingFrameLayout.DEFAULT_COLLAPSE_DURATION);
	}
	
	private class ContentPagerAdapter extends PagerAdapter
	{
		private StoryItemView mLeftView;
		private StoryItemView mCurrentView;
		private StoryItemView mRightView;
		private ArrayList<StoryItemView> mViews;
		
		public ContentPagerAdapter()
		{
			super();
			mViews = new ArrayList<StoryItemView>();
			updateViews();
		}

		public StoryItemView getCurrentView()
		{
			return mCurrentView;
		}
		
		@Override
		public void setPrimaryItem(ViewGroup container, int position,
				Object object) {
			int newIndex = mCurrentIndex;
			if (mCurrentView != null)
			{
				if (mCurrentView.usesReverseSwipe())
					newIndex = newIndex + getItemPosition(mCurrentView) - position;
				else
					newIndex = newIndex + position - getItemPosition(mCurrentView);
			}
			super.setPrimaryItem(container, position, object);
			if (newIndex != mCurrentIndex)
			{
				setCurrentStoryIndex(newIndex);
				notifyDataSetChanged();
			}
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1)
		{
			return arg0 == ((StoryItemView)arg1).getView(null);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			StoryItemView storyItemView = mViews.get(position);
			View view = storyItemView.getView(container);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);
			
			if (mInitialViewPositions != null && position == mContentPager.getCurrentItem())
			{
				storyItemView.setStoredPositions(mInitialViewPositions);
				mInitialViewPositions = null;
			}
			((ViewPager) container).addView(view);
			return storyItemView;
		}

		@Override
		public int getItemPosition(Object object)
		{
			StoryItemView storyItemView = (StoryItemView)object;
			int index = mViews.indexOf(storyItemView);
			if (index == -1)
				index = POSITION_NONE;
			return index;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object)
		{
			container.removeView(((StoryItemView)object).getView(container));
		}

		@Override
		public int getCount()
		{
			return mViews.size();
		}

		private StoryItemView getViewForItem(Item item, ArrayList<StoryItemView> reuseViews)
		{
			StoryItemView ret = null;
			for (StoryItemView storyItemView : reuseViews)
			{
				if (storyItemView.getItem().getDatabaseId() == item.getDatabaseId())
				{
					ret = storyItemView;
					reuseViews.remove(storyItemView);
					break;
				}
			}
			if (ret == null)
				ret = new StoryItemView(item);
			return ret;
		}
		
		private void updateViews()
		{
			mLeftView = null;
			mCurrentView = null;
			mRightView = null;
			
			if (mItemAdapter != null && mCurrentIndex >= 0 && mCurrentIndex < mItemAdapter.getDataItemCount())
				mCurrentView = getViewForItem(mItemAdapter.getDataItem(mCurrentIndex), mViews);
			
			if (mCurrentView != null)
			{
				if (mCurrentView.usesReverseSwipe())
				{
					if (mCurrentIndex > 0)
						mRightView = getViewForItem(mItemAdapter.getDataItem(mCurrentIndex - 1), mViews);
					if (mCurrentIndex < (mItemAdapter.getDataItemCount() - 1))
						mLeftView = getViewForItem(mItemAdapter.getDataItem(mCurrentIndex + 1), mViews);
				}
				else
				{
					if (mCurrentIndex > 0)
						mLeftView = getViewForItem(mItemAdapter.getDataItem(mCurrentIndex - 1), mViews);
					if (mCurrentIndex < (mItemAdapter.getDataItemCount() - 1))
						mRightView = getViewForItem(mItemAdapter.getDataItem(mCurrentIndex + 1), mViews);
				}
			}
						
			// Clean up views not used anymore
			//
			for (StoryItemView storyItemView : mViews)
			{
				storyItemView.recycle();
			}
			mViews.clear();
			
			if (mLeftView != null)
				mViews.add(mLeftView);
			if (mCurrentView != null)
				mViews.add(mCurrentView);
			if (mRightView != null)
				mViews.add(mRightView);
		}

		@Override
		public void notifyDataSetChanged()
		{
			updateViews();
			super.notifyDataSetChanged();
			if (mCurrentView != null)
				mContentPager.setCurrentItem(this.getItemPosition(mCurrentView));
		}
	}
}
