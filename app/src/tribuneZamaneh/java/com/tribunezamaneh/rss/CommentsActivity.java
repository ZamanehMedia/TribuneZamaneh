package com.tribunezamaneh.rss;

import java.util.Date;
import java.util.UUID;

import com.tinymission.rss.Comment;
import com.tinymission.rss.Item;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereader.XMLRPCCommentPublisher;
import info.guardianproject.securereader.XMLRPCCommentPublisher.XMLRPCCommentPublisherCallback;
import info.guardianproject.securereaderinterface.adapters.StoryItemCommentsAdapter;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.views.CreateAccountView;
import info.guardianproject.securereaderinterface.views.CreateAccountView.OnActionListener;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.LayoutInflaterCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CommentsActivity extends Activity implements OnClickListener, OnActionListener, FadeInFadeOutListener
{
	public static final String LOGTAG = "CommentsActivity";
	public static final boolean LOGGING = false;
	
	private LayoutInflater mInflater;
	private Item mItem;
	private ImageView mBtnSend;
	private Drawable mBtnSendDrawableEnabled;
	private ListView mListComments;
	private StoryItemCommentsAdapter mListCommentsAdapter;
	private EditText mEditComment;
	private CreateAccountView mViewCreateAccount;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = LayoutInflater.from(this);
		mInflater = inflater.cloneInContext(this);
		LayoutInflaterCompat.setFactory(mInflater, new LayoutFactoryWrapper(inflater.getFactory()));
		getWindow().setBackgroundDrawable(null);
		setContentView(R.layout.activity_comments);

		mBtnSend = (ImageView) findViewById(R.id.btnSend);
		mBtnSendDrawableEnabled = mBtnSend.getDrawable().mutate();
		UIHelpers.colorizeDrawable(this, R.attr.colorPrimary, mBtnSendDrawableEnabled);
		mBtnSend.setImageDrawable(mBtnSendDrawableEnabled);
		mBtnSend.setOnClickListener(this);
		mBtnSend.setEnabled(false);
		
		mEditComment = (EditText) findViewById(R.id.editComment);
		mEditComment.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				mBtnSend.setEnabled(s.length() > 0);
			}
		});
		
		mListComments = (ListView) findViewById(R.id.listComments);
		
		View btnCancel = findViewById(R.id.btnCancel);
		btnCancel.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				CommentsActivity.this.finish();
			}
		});
		
		mItem = (Item)getIntent().getSerializableExtra("item");
		if (mItem == null)
		{
			// Called without an item, show Paik Talk. We do this by getting special item from the SocialReader.
			//
			mItem = App.getInstance().socialReader.getTalkItem();
			((TextView)findViewById(R.id.title)).setText(R.string.menu_chat);
		}
		
		if (mItem != null) {
			mListCommentsAdapter = new StoryItemCommentsAdapter(this, App.getInstance().socialReader.getItemComments(mItem));
			mListComments.setAdapter(mListCommentsAdapter);
		}
		
		mViewCreateAccount = (CreateAccountView) findViewById(R.id.createAccount);
		mViewCreateAccount.setActionListener(this);
		showHideCreateAccount(false);
	}

	private void showHideCreateAccount(boolean animate)
	{
		if (App.getInstance().socialReporter.getAuthorName() != null)
		{
			if (animate)
			{
				AnimationHelpers.fadeOut(mViewCreateAccount, 500, 0, false, this);
			}
			else
			{
				mViewCreateAccount.setVisibility(View.GONE);
			}
		}
		else
		{
			mViewCreateAccount.setVisibility(View.VISIBLE);
			//mViewCreateAccount.focusTextField();
		}
	}

	@Override
	public void onCreateIdentity(String authorName)
	{
		App.getInstance().socialReporter.createAuthorName(authorName);
		showHideCreateAccount(true);
	}
	
	@Override
	public Object getSystemService(String name)
	{
		if (LAYOUT_INFLATER_SERVICE.equals(name))
		{
			if (mInflater != null)
				return mInflater;
		}
		return super.getSystemService(name);
	}

	@Override
	public void onClick(View v) {
		if (v == mBtnSend)
		{
			Comment comment = new Comment(UUID.randomUUID().toString(), "", new Date(), mEditComment.getEditableText().toString() , mItem.getDatabaseId());
			comment.setAuthor(App.getInstance().socialReporter.getAuthorName());
			XMLRPCCommentPublisherCallback callback = new XMLRPCCommentPublisherCallback() {
				
				@Override
				public void commentPublished(int commentId) {
					Toast.makeText(CommentsActivity.this,"Posted", Toast.LENGTH_SHORT).show();
				}
				
				@Override
				public void commentPublishFailure(int reason) {
					// Should add in reasons
					if (reason == XMLRPCCommentPublisher.FAILURE_REASON_NO_PRIVACY_PROXY) {
						Toast.makeText(CommentsActivity.this, "Posting Failed, Tor or Psiphon Required for Posting", Toast.LENGTH_SHORT).show();
					} else if (reason == XMLRPCCommentPublisher.FAILURE_REASON_NO_CONNECTION) {
						Toast.makeText(CommentsActivity.this, "Posting Failed, No Network Connection", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(CommentsActivity.this, "Posting Failed, Failure Unknown", Toast.LENGTH_SHORT).show();
					}
				}
			};
			
			App.getInstance().socialReporter.postComment(comment, callback);
			
			//App.getInstance().socialReader.addCommentToItem(mItem, comment);
			UIHelpers.hideSoftKeyboard(this);							
			mEditComment.setText("Published");

		}
	}
	
	@Override
	public void onFadeInStarted(View view)
	{
	}

	@Override
	public void onFadeInEnded(View view)
	{
	}

	@Override
	public void onFadeOutStarted(View view)
	{
	}

	@Override
	public void onFadeOutEnded(View view)
	{
		view.setVisibility(View.GONE);
		// To avoid old device bug, see
		// http://stackoverflow.com/questions/4728908/android-view-with-view-gone-still-receives-ontouch-and-onclick
		view.clearAnimation();
	}
}
