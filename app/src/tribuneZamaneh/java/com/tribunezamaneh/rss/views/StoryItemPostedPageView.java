package com.tribunezamaneh.rss.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.tinymission.rss.Item;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.views.StoryItemPageView;

public class StoryItemPostedPageView extends StoryItemPageView
{
	private View mBtnDelete;

	public StoryItemPostedPageView(Context context)
	{
		super(context);
	}

	public StoryItemPostedPageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	protected int getViewResourceByType(ViewType type)
	{
		return R.layout.post_item_posted_landscape_photo;
	}

	@Override
	protected void findViews(View view)
	{
		super.findViews(view);
		mBtnDelete = view.findViewById(R.id.btnDelete);
	}

	public void setButtonClickListeners(OnClickListener deleteListener)
	{
		if (mBtnDelete != null)
			mBtnDelete.setOnClickListener(deleteListener);
	}
}
