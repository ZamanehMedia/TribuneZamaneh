package info.guardianproject.securereaderinterface.adapters;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.views.StoryItemCommentView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.tinymission.rss.Comment;
import com.tinymission.rss.Item;

public class StoryItemCommentsAdapter extends BaseAdapter
{
	public final static String LOGTAG = "StoryItemCommentsAdapter";
	public static final boolean LOGGING = false;

	private final LayoutInflater mInflater;

	private final ArrayList<Comment> mItems;

	public StoryItemCommentsAdapter(Context context, ArrayList<Comment> comments)
	{
		super();
		mItems = sortCommentsOnPublicationTime(comments);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private ArrayList<Comment> sortCommentsOnPublicationTime(ArrayList<Comment> unsortedComments)
	{
		if (unsortedComments == null)
			return null;

		ArrayList<Comment> items = new ArrayList<Comment>(unsortedComments);
		Collections.sort(items, new Comparator<Comment>()
		{
			@Override
			public int compare(Comment i1, Comment i2)
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
		View view = (convertView != null) ? convertView : createView(position, parent);
		bindView(view, position, (Comment) getItem(position));
		return view;
	}
	
	protected View createView(int position, ViewGroup parent)
	{
		StoryItemCommentView view = (StoryItemCommentView) mInflater.inflate(R.layout.story_item_comment, parent, false);
		return view;
	}

	protected void bindView(View view, int position, Comment comment)
	{
		StoryItemCommentView pv = (StoryItemCommentView) view;
		pv.populateWithComment(comment);
	}
}
