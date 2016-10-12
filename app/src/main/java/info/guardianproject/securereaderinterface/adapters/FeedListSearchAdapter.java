package info.guardianproject.securereaderinterface.adapters;

import info.guardianproject.securereaderinterface.R;

import java.util.ArrayList;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tinymission.rss.Feed;

public class FeedListSearchAdapter extends BaseAdapter
{
	public final static String LOGTAG = "FeedListSearchAdapter";
	public static final boolean LOGGING = false;

	private final Context mContext;
	private final LayoutInflater mInflater;

	private final ArrayList<Feed> mItems;
	private final ArrayList<Feed> mItemsLocal;
	private final ArrayList<Feed> mItemsRemote;
	private ArrayList<Feed> mSelectedItems;

	public FeedListSearchAdapter(Context context)
	{
		super();
		mContext = context;
		mItems = new ArrayList<Feed>();
		mItemsLocal = new ArrayList<Feed>();
		mItemsRemote = new ArrayList<Feed>();
		mSelectedItems = new ArrayList<Feed>();
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void clear()
	{
		mItemsLocal.clear();
		mItemsRemote.clear();
		mItems.clear();
		notifyDataSetChanged();
	}

	public void setLocalMatches(ArrayList<Feed> localFeeds)
	{
		mItemsLocal.clear();
		if (localFeeds != null)
			mItemsLocal.addAll(localFeeds);
		mergeLocalAndRemote();
	}
	
	public void setRemoteMatches(ArrayList<Feed> remoteFeeds)
	{
		mItemsRemote.clear();
		if (remoteFeeds != null)
			mItemsRemote.addAll(remoteFeeds);
		mergeLocalAndRemote();
	}
	
	private void mergeLocalAndRemote()
	{
		mItems.clear();
		mItems.addAll(mItemsLocal);
		ArrayList<String> addedUrls = new ArrayList<String>();
		for (Feed feed : mItemsLocal)
			addedUrls.add(feed.getFeedURL());
		for (Feed feed : mItemsRemote)
		{
			if (!addedUrls.contains(feed.getFeedURL()))
			{
				mItems.add(feed);
				addedUrls.add(feed.getFeedURL());
			}
		}
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount()
	{
		return mItems.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Feed feed = (Feed)getItem(position);

		View view;
		if (convertView == null)
		{
			view = mInflater.inflate(R.layout.add_feed_list_search_item, parent, false);
		}
		else
		{
			view = convertView;
		}

		// ImageView iv = (ImageView) view.findViewById(R.id.ivFeedIcon);
		// if (feedModel.feed.getImageManager() != null)
		// feedModel.feed.getImageManager().download(feedModel.feed.,
		// imageView)
		// App.getInstance().socialReader.loadDisplayImageMediaContent(feedModel.feed.getImageManager(),
		// iv);

		// Name
		TextView tv = (TextView) view.findViewById(R.id.tvFeedName);
		tv.setText(feed.getTitle());
		tv.setTextColor(mContext.getResources().getColor(R.color.feed_list_title_normal));

		int feedStatus = feed.getStatus();
		if (feedStatus == Feed.STATUS_LAST_SYNC_FAILED_404 || feedStatus == Feed.STATUS_LAST_SYNC_FAILED_BAD_URL
				|| feedStatus == Feed.STATUS_LAST_SYNC_FAILED_UNKNOWN)
		{
			tv.setText(R.string.add_feed_error_loading);
			tv.setTextColor(mContext.getResources().getColor(R.color.feed_list_title_error));
		}
		else if (TextUtils.isEmpty(feed.getTitle()))
			tv.setText(R.string.add_feed_not_loaded);

		// Description
		tv = (TextView) view.findViewById(R.id.tvFeedDescription);
		tv.setText(feed.getDescription());
		if (TextUtils.isEmpty(feed.getTitle()) || tv.getText().length() == 0)
			tv.setText(feed.getFeedURL());

		// Operation?
		View btnAdd = view.findViewById(R.id.btnAdd);
		View btnRemove = view.findViewById(R.id.btnRemove);

		btnAdd.setVisibility(mSelectedItems.contains(feed) ? View.GONE : View.VISIBLE);
		btnRemove.setVisibility(mSelectedItems.contains(feed) ? View.VISIBLE : View.GONE);
		btnAdd.setTag(feed);
		btnAdd.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Feed feed = (Feed) v.getTag();
				if (!mSelectedItems.contains(feed))
				{
					mSelectedItems.add(feed);
					FeedListSearchAdapter.this.notifyDataSetChanged();
				}
			}
		});
		btnRemove.setTag(feed);
		btnRemove.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Feed feed = (Feed) v.getTag();
				if (mSelectedItems.contains(feed))
				{
					mSelectedItems.remove(feed);
					FeedListSearchAdapter.this.notifyDataSetChanged();
				}
			}
		});
		view.setOnClickListener(null);
		return view;
	}

	public ArrayList<Feed> getSelectedFeeds() {
		return mSelectedItems;
	}
}
