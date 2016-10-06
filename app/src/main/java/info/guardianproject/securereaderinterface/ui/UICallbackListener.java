package info.guardianproject.securereaderinterface.ui;

import info.guardianproject.securereaderinterface.models.FeedFilterType;
import info.guardianproject.securereaderinterface.ui.UICallbacks.OnCallbackListener;

import android.content.Context;
import android.os.Bundle;

import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

public class UICallbackListener implements OnCallbackListener
{
	/**
	 * Empty implementation of onFeedSelect.
	 * 
	 * @see OnCallbackListener#onFeedSelect(boolean, int, Object).
	 */
	@Override
	public void onFeedSelect(FeedFilterType type, long feedId, Object source)
	{
	}

	@Override
	public void onTagSelect(String tag)
	{
	}

	@Override
	public void onRequestResync(Feed feed)
	{
	}

	@Override
	public void onItemFavoriteStatusChanged(Item item)
	{
	}

	@Override
	public boolean onCommand(Context context, int command, Bundle commandParameters)
	{
		return false;
	}
}
