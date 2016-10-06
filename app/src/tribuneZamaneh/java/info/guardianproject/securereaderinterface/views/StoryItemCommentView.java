package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.RelativeLayout;
import android.widget.TextView;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.R;

import com.tinymission.rss.Comment;

public class StoryItemCommentView extends RelativeLayout 
{
	protected Comment mComment;
	protected TextView mTvAuthor;
	protected TextView mTvContent;
	protected TextView mTvTime;
	
	public StoryItemCommentView(Context context)
	{
		super(context);
		init(null);
	}

	public StoryItemCommentView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs);
	}

	public StoryItemCommentView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs);
	}

	private void init(AttributeSet attrs)
	{
	}

	protected void findViews()
	{
		mTvContent = (TextView) findViewById(R.id.tvContent);
		mTvTime = (TextView) findViewById(R.id.tvTime);
		mTvAuthor = (TextView) findViewById(R.id.tvAuthor);
		if (mTvContent != null)
			mTvContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTvContent.getTextSize() + App.getSettings().getContentFontSizeAdjustment());
		if (mTvTime != null)
			mTvTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTvTime.getTextSize() + App.getSettings().getContentFontSizeAdjustment());
		if (mTvAuthor != null)
			mTvAuthor.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTvAuthor.getTextSize() + App.getSettings().getContentFontSizeAdjustment());
	}

	public void populateWithComment(Comment comment)
	{
		if (mTvContent == null)
			findViews();
		
		if (mComment == null || comment == null || mComment.getDatabaseId() != comment.getDatabaseId())
		{
			mComment = comment;

			if (mTvContent != null)
				mTvContent.setText(mComment.getCleanMainContent());
			if (mTvAuthor != null)
				mTvAuthor.setText(mComment.getAuthor());
		}
		populateTime();
	}
	
	protected void populateTime()
	{
		if (mTvTime != null)
			mTvTime.setText(UIHelpers.dateDiffDisplayString(mComment.getPublicationTime(), getContext(), R.string.story_item_short_published_never,
						R.string.story_item_short_published_recently, R.string.story_item_short_published_minutes, R.string.story_item_short_published_minute,
						R.string.story_item_short_published_hours, R.string.story_item_short_published_hour, R.string.story_item_short_published_days,
						R.string.story_item_short_published_day));
	}

	private final Runnable mUpdateTimestamp = new Runnable()
	{
		@Override
		public void run()
		{
			// Every minute
			populateTime();

			Handler handler = getHandler();
			if (handler != null)
			{
				handler.postDelayed(mUpdateTimestamp, 1000 * 60); // Every
																	// minute
			}
		}
	};

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();

		Handler handler = getHandler();
		if (handler != null)
		{
			handler.removeCallbacks(mUpdateTimestamp);
			handler.postDelayed(mUpdateTimestamp, 1000 * 60); // Every minute
		}
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();

		Handler handler = getHandler();
		if (handler != null)
		{
			handler.removeCallbacks(mUpdateTimestamp);
		}
	}
}
