package com.tribunezamaneh.rss.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.tinymission.rss.Item;
import com.tribunezamaneh.rss.views.StoryItemDraftPageView;
import com.tribunezamaneh.rss.views.StoryItemPostedPageView;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.adapters.StoryListAdapter;
import info.guardianproject.securereaderinterface.views.StoryItemPageView;

public class PostPublishedListAdapter extends StoryListAdapter
{
	public static final String LOGTAG = "PostPublishedListAdapter";
	public static final boolean LOGGING = false;

	public interface PostPublishedListAdapterListener
	{
		void onDeletePost(Item item);
	}

	private PostPublishedListAdapterListener mPostPublishedListAdapterListener;

	public PostPublishedListAdapter(Context context, ArrayList<Item> posts)
	{
		super(context, posts);
		mPostPublishedListAdapterListener = null;
		setShowTags(true);
	}

	public void setPostPublishedListAdapterListener(PostPublishedListAdapterListener listener)
	{
		mPostPublishedListAdapterListener = listener;
	}

	protected View createView(int position, ViewGroup parent)
	{
		StoryItemPostedPageView view = new StoryItemPostedPageView(parent.getContext());
		view.createViews(StoryItemPageView.ViewType.LANDSCAPE_PHOTO);
		return view;
	}

	@Override
	protected void bindView(View view, int position, Item item)
	{
		super.bindView(view, position, item);
		if (view instanceof StoryItemPostedPageView) {
			StoryItemPostedPageView pv = (StoryItemPostedPageView) view;
			pv.setButtonClickListeners(new PostPublishedListAdapter.DeleteButtonClickListener(item));
		}
	}

	private class DeleteButtonClickListener implements View.OnClickListener
	{
		private final Item mItem;

		public DeleteButtonClickListener(Item item)
		{
			mItem = item;
		}

		@Override
		public void onClick(View v)
		{
			if (mPostPublishedListAdapterListener != null)
				mPostPublishedListAdapterListener.onDeletePost(mItem);
		}
	}
}
