package info.guardianproject.securereaderinterface;


import java.util.ArrayList;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.views.ExpandingFrameLayout;
import info.guardianproject.securereaderinterface.views.FullScreenStoryItemView;
import info.guardianproject.securereaderinterface.views.ExpandingFrameLayout.ExpansionListener;
import info.guardianproject.securereaderinterface.views.StoryListView.StoryListListener;
import info.guardianproject.securereaderinterface.R;

import com.tinymission.rss.Item;

public class ItemExpandActivity extends FragmentActivityWithMenu implements StoryListListener
{
	public static final String LOGTAG = "ItemExpandActivity";
	public static final boolean LOGGING = false;

	private ExpandingFrameLayout mFullStoryView;
	private FullScreenStoryItemView mFullView;
	private ListView mFullListStories;
	private int mFullOpeningOffset;
	private boolean mInFullScreenMode;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		configureActionBarForFullscreen(isInFullScreenMode());
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		boolean ret = super.onPrepareOptionsMenu(menu);
		configureActionBarForFullscreen(isInFullScreenMode());
		return ret;
	}

	@Override
	public void onStoryClicked(ArrayList<Item> stories, int index, View storyView)
	{
		if (storyView != null)
			openStoryFullscreen(stories, index, (ListView) storyView.getParent(), storyView);
	}

	public void openStoryFullscreen(ArrayList<Item> stories, int index, ListView listStories, View storyView)
	{
		FrameLayout screenFrame = getTopFrame();

		if (stories != null && screenFrame != null)
		{
			// Remove old view (if set) from view tree
			//
			removeFullStoryView();

			// Disable drag of the left side menu
			//
			mLeftSideMenu.setDragEnabled(false);

			mFullView = new FullScreenStoryItemView(this);
			mFullStoryView = new ExpandingFrameLayout(this, mFullView, getSupportActionBar().getHeight());
			mInFullScreenMode = true;
			
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
					Gravity.LEFT | Gravity.TOP);
			// params.topMargin = UIHelpers.getStatusBarHeight(this);
			mFullStoryView.setLayoutParams(params);

			screenFrame.addView(mFullStoryView);

			mFullView.setStory(stories, index, getStoredPositions((ViewGroup) storyView));
			this.prepareFullScreenView(mFullView);

			mFullListStories = listStories;
			mFullStoryView.setExpansionListener(new ExpansionListener()
			{
				@Override
				public void onExpanded()
				{
					configureActionBarForFullscreen(true);

					// Minimize overdraw by hiding list
					mFullListStories.setVisibility(View.INVISIBLE);
				}

				@Override
				public void onCollapsed()
				{
					removeFullStoryView();
					mLeftSideMenu.setDragEnabled(true);
					refreshMenu();
				}
			});

			setCollapsedSizeToStoryViewSize(storyView);
		}
	}

	private void setCollapsedSizeToStoryViewSize(View storyView)
	{
		// // Get screen position of the story view
		int[] locationLv = new int[2];
		mFullListStories.getLocationOnScreen(locationLv);

		int[] location = new int[2];
		if (storyView != null)
			storyView.getLocationOnScreen(location);
		else
			location = locationLv;

		// Get from top and bottom
		int[] locationTopFrame = new int[2];
		getTopFrame().getLocationOnScreen(locationTopFrame);

		int fromClip = Math.max(0, locationLv[1] - location[1]);
		int fromTop = location[1] - locationTopFrame[1];
		int fromHeight = (storyView != null) ? storyView.getHeight() : mFullListStories.getHeight();

		mFullOpeningOffset = location[1] - locationLv[1];
		mFullStoryView.setCollapsedSize(fromClip, fromTop, fromHeight);
	}
	
	private void getStoredPositionForViewWithId(ViewGroup parent, int viewId, SparseArray<Rect> positions)
	{
		View view = parent.findViewById(viewId);
		if (view != null)
		{
			Rect rect = UIHelpers.getRectRelativeToView(parent, view);
			rect.offset(view.getPaddingLeft(), view.getPaddingTop());
			rect.right -= (view.getPaddingRight() + view.getPaddingLeft());
			rect.bottom -= (view.getPaddingBottom() + view.getPaddingTop());
			positions.put(view.getId(), rect);
		}
	}
	
	private SparseArray<Rect> getStoredPositions(ViewGroup viewGroup)
	{
		if (viewGroup == null || viewGroup.getChildCount() == 0)
			return null;

		SparseArray<Rect> positions = new SparseArray<Rect>();

		getStoredPositionForViewWithId(viewGroup, R.id.layout_media, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.tvTitle, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.tvContent, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.layout_source, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.layout_author, positions);
		return positions;
	}

	protected void prepareFullScreenView(FullScreenStoryItemView fullView)
	{
	}

	private FrameLayout getTopFrame()
	{
		try
		{
			FrameLayout parent = (FrameLayout) (((FrameLayout) getWindow().getDecorView()).getChildAt(0));
			return parent;
		}
		catch (Exception ex)
		{
			if (LOGGING)
				Log.e(LOGTAG, "Failed to get top level frame: " + ex.toString());
		}
		return null;
	}

	private void removeFullStoryView()
	{
		if (mFullStoryView != null)
		{
			try
			{
				AnimationHelpers.fadeOut(mFullStoryView, 500, 0, true);
				mFullStoryView = null;
			}
			catch (Exception ex)
			{
				if (LOGGING)
					Log.e(LOGTAG, "Failed to remove full story view from view tree: " + ex.toString());
			}
		}
	}

	@Override
	public void onBackPressed()
	{
		if (isInFullScreenMode())
		{
			exitFullScreenMode();
		}
		else
		{
			// If the user is not currently in full screen story mode, allow the
			// system to handle the
			// Back button. This calls finish() on this activity and pops the
			// back stack.
			super.onBackPressed();
		}
	}

	protected void configureActionBarForFullscreen(boolean b)
	{
	}

	private boolean isInFullScreenMode()
	{
		return mInFullScreenMode;
	}

	@Override
	public void onResync()
	{
	}

	@Override
	public void onHeaderCreated(View headerView, int resIdHeader)
	{
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			if (isInFullScreenMode())
			{
				exitFullScreenMode();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	private void exitFullScreenMode()
	{
		mInFullScreenMode = false;
		configureActionBarForFullscreen(false);
		// getSupportActionBar().hide();
		// getSupportActionBar().show();
		mFullStoryView.post(new Runnable()
		{
			// Reason for post? We need to give the call above
			// (configureActionBar...) a chance
			// to do all layout changes it needs to do. This is
			// because in collapse() below we
			// take a snapshot of the screen and need to have valid
			// data.
			@Override
			public void run()
			{
				updateList();
				scrollToCurrentItem();
				mFullListStories.post(new Runnable()
				{
					@Override
					public void run()
					{
						mFullListStories.setVisibility(View.VISIBLE);
						if (mFullView != null)
							mFullView.onBeforeCollapse();
						if (mFullStoryView != null)
							mFullStoryView.collapse();
					}
				});
			}
		});
	}

	boolean bWaitingForCallToScroll = false;
	
	private void scrollToCurrentItem()
	{		
		// Try to find index of current item, so that we can
		// scroll the
		// list to the actual story the user was reading
		// while in full screen mode.
		Item currentItem = mFullView.getCurrentStory();
		if (currentItem != null && mFullListStories.getAdapter() != null)
		{
			ListAdapter adapter = mFullListStories.getAdapter();
			for (int iItem = 0; iItem < adapter.getCount(); iItem++)
			{
				Item item = (Item) adapter.getItem(iItem);
				if (item.getDatabaseId() == currentItem.getDatabaseId())
				{
					bWaitingForCallToScroll = true;
					mFullListStories.setOnScrollListener(new OnScrollListener()
					{
						@Override
						public void onScrollStateChanged(AbsListView view,
								int scrollState) {
						}

						@Override
						public void onScroll(AbsListView view,
								int firstVisibleItem, int visibleItemCount,
								int totalItemCount) {
							if (!bWaitingForCallToScroll)
								mFullListStories.setOnScrollListener(null);
							
					        Item currentItem = mFullView.getCurrentStory();
					        for (int i = firstVisibleItem; i < totalItemCount && i < (firstVisibleItem + visibleItemCount); i++)
							{
								Item item = (Item) mFullListStories.getItemAtPosition(i);
								if (item.getDatabaseId() == currentItem.getDatabaseId()) 
								{
									View storyView = mFullListStories.getChildAt(i - mFullListStories.getFirstVisiblePosition());
									if (storyView != null)
										setCollapsedSizeToStoryViewSize(storyView);
									break;
								}
							}
						}
					});
					mFullListStories.setSelectionFromTop(iItem, mFullOpeningOffset);
					bWaitingForCallToScroll = false;
					break;
				}
			}
		}
	}

	/**
	 * Called before we collapse the full screen view. This is to update all
	 * list view items that are visible. We might have loaded media etc while in
	 * full screen mode, so we need to pick that up in the list.
	 */
	private void updateList()
	{
		if (mFullListStories == null || mFullListStories.getCount() == 0)
			return;

		if (mFullListStories.getAdapter() != null && mFullListStories.getAdapter() instanceof BaseAdapter)
		{
			((BaseAdapter) mFullListStories.getAdapter()).notifyDataSetChanged();
		}
	}

	@Override
	public void onListViewUpdated(ListView newList)
	{
		// List view has been recreated (probably due to orientation change).
		// Remember the new one!
		mFullListStories = newList;
		if (isInFullScreenMode() && mFullListStories != null)
			mFullListStories.setVisibility(View.INVISIBLE);
	}

}
