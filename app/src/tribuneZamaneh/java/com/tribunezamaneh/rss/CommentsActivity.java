package com.tribunezamaneh.rss;

import java.util.Date;
import java.util.UUID;

import com.tinymission.rss.Comment;
import com.tinymission.rss.Item;

import info.guardianproject.securereader.SyncService;
import info.guardianproject.securereaderinterface.FragmentActivityWithMenu;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereader.XMLRPCCommentPublisher;
import info.guardianproject.securereader.XMLRPCCommentPublisher.XMLRPCCommentPublisherCallback;
import com.tribunezamaneh.rss.adapters.StoryItemCommentsAdapter;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import com.tribunezamaneh.rss.views.CreateAccountView;
import com.tribunezamaneh.rss.views.CreateAccountView.OnActionListener;
import com.tribunezamaneh.rss.views.WPSignInView;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.LayoutInflaterCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CommentsActivity extends FragmentActivityWithMenu implements SyncService.SyncServiceListener, OnClickListener, OnActionListener, FadeInFadeOutListener
{
	public static final String LOGTAG = "CommentsActivity";
	public static final boolean LOGGING = false;
	
	private Item mItem;
	private ImageView mBtnSend;
	private Drawable mBtnSendDrawableEnabled;
	private ListView mListComments;
	private StoryItemCommentsAdapter mListCommentsAdapter;
	private EditText mEditComment;
	private CreateAccountView mViewCreateAccount;
	private WPSignInView mSignInView;

	@Override
	protected boolean useLeftSideMenu() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(null);
		setContentView(R.layout.activity_comments);

		mToolbar.setVisibility(View.GONE);
		findViewById(R.id.root).requestFocus();

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
		mEditComment.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean b) {
				if (b) {
					// If we have not logged in yet, this is the time to do it!
					if (!App.isSignedIn()) {
						mEditComment.clearFocus();
						signIn();
					}
				}
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
			// Called without an item, show Tribune Zamaneh Talk. We do this by getting special item from the SocialReader.
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
		App.getInstance().addSyncServiceListener(this);
	}

	@Override
	protected void onDestroy() {
		App.getInstance().removeSyncServiceListener(this);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_comments, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.menu_logout);
		if (item != null) {
			item.setVisible(App.isSignedIn());
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				finish();
				return true;
			case R.id.menu_logout:
				App.getInstance().socialReader.ssettings.setXMLRPCUsername("");
				App.getInstance().socialReader.ssettings.setXMLRPCPassword("");
				showHideCreateAccount(true);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showHideCreateAccount(boolean animate)
	{
		if (true || App.getInstance().socialReporter.getAuthorName() != null)
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
	public void onClick(View v) {
		if (v == mBtnSend) {
			// Try to get remote post id of item
			if (mItem.getRemotePostId() != 0) {
				Comment comment = new Comment(UUID.randomUUID().toString(), "", new Date(), mEditComment.getEditableText().toString(), mItem.getDatabaseId());
				//comment.setAuthor(App.getInstance().socialReporter.getAuthorName());
				comment.setAuthor(App.getInstance().socialReader.ssettings.getXMLRPCUsername());
				XMLRPCCommentPublisherCallback callback = new XMLRPCCommentPublisherCallback() {

					@Override
					public void commentPublished(int commentId) {
						Toast.makeText(CommentsActivity.this, "Posted", Toast.LENGTH_SHORT).show();
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								refresh();
							}
						});
					}

					@Override
					public void commentPublishFailure(int reason) {
						// Should add in reasons
						if (reason == XMLRPCCommentPublisher.FAILURE_REASON_NO_PRIVACY_PROXY) {
							Toast.makeText(CommentsActivity.this, getString(R.string.comment_error_proxy), Toast.LENGTH_SHORT).show();
						} else if (reason == XMLRPCCommentPublisher.FAILURE_REASON_NO_CONNECTION) {
							Toast.makeText(CommentsActivity.this, getString(R.string.comment_error_network), Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(CommentsActivity.this, getString(R.string.comment_error_unknown), Toast.LENGTH_SHORT).show();
						}
					}
				};

				App.getInstance().socialReporter.postComment(comment, callback);

				//App.getInstance().socialReader.addCommentToItem(mItem, comment);
				UIHelpers.hideSoftKeyboard(this);
				mEditComment.setText("");
			} else {
				Toast.makeText(CommentsActivity.this, getString(R.string.comment_error_unknown), Toast.LENGTH_SHORT).show();
			}
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

	private void signIn() {
		mSignInView = new WPSignInView(this);
		mSignInView.setListener(new WPSignInView.OnLoginListener() {
			@Override
			public void onLoggedIn(String username, String password) {
				((ViewGroup)mSignInView.getParent()).removeView(mSignInView);
				mSignInView = null;
				App.getInstance().socialReader.ssettings.setXMLRPCUsername(username);
				App.getInstance().socialReader.ssettings.setXMLRPCPassword(password);
				showHideCreateAccount(true);
				UIHelpers.hideSoftKeyboard(CommentsActivity.this);
			}
		});
		((ViewGroup)findViewById(R.id.root)).addView(mSignInView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
	}

	@Override
	public void onBackPressed() {
		if (mSignInView != null) {
			((ViewGroup)mSignInView.getParent()).removeView(mSignInView);
			mSignInView = null;
			return;
		}
		super.onBackPressed();
	}

	private void refresh() {
		if (App.getInstance().socialReader.getSyncService() != null) {
			App.getInstance().socialReader.getSyncService().addCommentsSyncTask(mItem);
		}
	}

	@Override
	public void syncEvent(SyncService.SyncTask syncTask) {
		if (syncTask.type == SyncService.SyncTask.TYPE_COMMENTS && syncTask.status == SyncService.SyncTask.FINISHED) {
			if (mItem != null && syncTask.item != null && syncTask.item.getDatabaseId() == mItem.getDatabaseId()) {
				mListCommentsAdapter = new StoryItemCommentsAdapter(this, App.getInstance().socialReader.getItemComments(mItem));
				mListComments.setAdapter(mListCommentsAdapter);
			}
		}
	}
}
