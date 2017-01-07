package com.tribunezamaneh.rss;

import info.guardianproject.securereader.DatabaseHelper;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SocialReporter;
import info.guardianproject.securereader.XMLRPCPublisher;
import info.guardianproject.securereader.XMLRPCPublisher.XMLRPCPublisherCallback;
import info.guardianproject.securereaderinterface.*;
import info.guardianproject.securereaderinterface.ui.MediaViewCollection;
import info.guardianproject.securereaderinterface.ui.UICallbacks;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import com.tribunezamaneh.rss.views.CreateAccountView;
import com.tribunezamaneh.rss.views.PostSignInView;
import info.guardianproject.securereaderinterface.views.StoryMediaContentView;
import com.tribunezamaneh.rss.views.CreateAccountView.OnActionListener;
import com.tribunezamaneh.rss.views.PostSignInView.OnAgreeListener;
import info.guardianproject.iocipher.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;
import com.tribunezamaneh.rss.views.WPSignInView;

public class AddPostActivity extends FragmentActivityWithMenu implements OnFocusChangeListener
{
	public static final String LOGTAG = "AddPostActivity";
	public static final boolean LOGGING = true;

	private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1;

	ProgressDialog loadingDialog;

	private static final int REQ_CODE_PICK_IMAGE = 1;
	private StoryMediaContentView mMediaView;
	private EditText mEditTitle;
	private EditText mEditContent;
	private EditText mEditTags;
	private Item mStory;
	private View mBtnMediaAdd;
	private View mBtnMediaAddMore;
	private View mBtnMediaReplace;
	private View mBtnMediaView;
	private View mBtnMediaDelete;
	private View mOperationButtons;
	private View mProgressIcon;
	private java.io.File mCurrentPhotoFile;
	private int mReplaceThisIndex;
	private boolean mIsAddingMedia;
	private AlertDialog mMediaChooserDialog;

	private Intent mStartedIntent;
	private MenuItem mMenuPost;
	private HandlerIntent mPendingIntent; // Start this when/if we get write external permission
	private WPSignInView mSignIn;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_post);

		// Display home as up
		setDisplayHomeAsUp(true);

		setMenuIdentifier(R.menu.activity_add_post);
		mReplaceThisIndex = -1;
		mIsAddingMedia = false;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		this.setContentView(R.layout.activity_add_post);
	}

	@Override
	public void onSupportContentChanged()
	{
		super.onSupportContentChanged();
		mEditTitle = (EditText) findViewById(R.id.editTitle);
		mEditContent = (EditText) findViewById(R.id.editContent);
		mEditTags = (EditText) findViewById(R.id.editTags);

		mMediaView = (StoryMediaContentView) findViewById(R.id.mediaContentView);

		mMediaView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (!mIsAddingMedia && mOperationButtons.getVisibility() == View.GONE)
				{
					mOperationButtons.setVisibility(View.VISIBLE);
					AnimationHelpers.fadeIn(mOperationButtons, 500, 5000, false);
				}
			}
		});
		mMediaView = (StoryMediaContentView) findViewById(R.id.mediaContentView);
		mMediaView.setShowPlaceholder(true);
		mMediaView.setShowPlaceholderWhileLoading(true);

		mOperationButtons = findViewById(R.id.llOperationButtons);
		mOperationButtons.setVisibility(View.GONE);
		AnimationHelpers.fadeOut(mOperationButtons, 0, 0, false);

		hookupMediaOperationButtons();

		// If this is "edit" and not "add" the edited story is sent in the
		// intent. Get it!
		if (getIntent().hasExtra("story"))
		{
			long storyId = getIntent().getLongExtra("story", 0);

			for (Item story : info.guardianproject.securereaderinterface.App.getInstance().socialReporter.getDrafts())
			{
				if (story.getDatabaseId() == storyId)
				{
					mStory = story;
					break;
				}
			}
		}

		// Hacking this in...
		// http://developer.android.com/training/sharing/receive.html
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (Intent.ACTION_SEND.equals(action) && type != null)
		{
			if ("text/plain".equals(type))
			{
				String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
				mEditContent.setText(sharedText);
			}
			else if (type.startsWith("image/"))
			{
				Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

				// addMediaItem(imageUri, type, -1);
			}
			else if (type.startsWith("video/"))
			{
				Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
				// addMediaItem(imageUri, type, -1);
			}
		}

		// We need to react on the soft keyboard being shown, to make sure that
		// the input fields are
		// shown correctly. This is taken from:
		// http://stackoverflow.com/questions/2150078/how-to-check-visibility-of-software-keyboard-in-android/4737265#4737265
		//
		final View activityRootView = findViewById(R.id.add_post_root);
		activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener()
		{
			@Override
			public void onGlobalLayout()
			{
				Rect r = new Rect();
				// r will be populated with the coordinates of your view
				// that
				// area still visible.
				activityRootView.getWindowVisibleDisplayFrame(r);

				int heightDiff = activityRootView.getRootView().getHeight() - r.height();
				if (heightDiff > 100)
				{ // if more than 100 pixels, its
					// probably a keyboard...
					final ScrollView sv = (ScrollView) findViewById(R.id.sv0);
					if (sv != null)
					{
						sv.post(new Runnable()
						{
							@Override
							public void run()
							{
								sv.scrollTo(0, sv.getBottom());
							}
						});
					}
				}
			}
		});

		mEditTags.addTextChangedListener(new TagTextWatcher());

		showHideCreateAccount(false);

		// Sat focus change listener so we can auto save draft on lost focus
		// event
		mEditTitle.setOnFocusChangeListener(this);
		mEditContent.setOnFocusChangeListener(this);
		mEditTags.setOnFocusChangeListener(this);

		populateFromStory();
		updateMediaControls();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if (LOGGING)
						Log.d(LOGTAG, "Received permission!");
					if (mPendingIntent != null)
						startResolvedIntent(mPendingIntent);
				}
			}
		}
		mPendingIntent = null;
	}

	private void hookupMediaOperationButtons()
	{
		mProgressIcon = findViewById(R.id.ivProgressIcon);
		mProgressIcon.setVisibility(View.GONE);
		
		mBtnMediaAdd = findViewById(R.id.btnMediaAdd);
		mBtnMediaAdd.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				createMediaChooser(-1);
			}
		});

		mBtnMediaAddMore = findViewById(R.id.btnMediaAddMore);
		mBtnMediaAddMore.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				createMediaChooser(-1);
			}
		});

		mBtnMediaReplace = findViewById(R.id.btnMediaReplace);
		mBtnMediaReplace.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				int currentIndex = mMediaView.getCurrentItemIndex();
				createMediaChooser(currentIndex);
			}
		});

		mBtnMediaView = findViewById(R.id.btnMediaView);
		if (mBtnMediaView != null) {
			mBtnMediaView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int currentIndex = mMediaView.getCurrentItemIndex();
					if (mStory != null && currentIndex < mStory.getNumberOfMediaContent()) {
						MediaContent mediaContent = mStory.getMediaContent().get(currentIndex);
						Bundle mediaData = new Bundle();
						mediaData.putSerializable("media", mediaContent);
						UICallbacks.handleCommand(v.getContext(), R.integer.command_view_media, mediaData);
					}
				}
			});
		}

		mBtnMediaDelete = findViewById(R.id.btnMediaDelete);
		mBtnMediaDelete.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				int currentIndex = mMediaView.getCurrentItemIndex();
				if (mStory != null && currentIndex < mStory.getNumberOfMediaContent())
				{
					mStory.getMediaContent().remove(currentIndex);
					onMediaChanged();
				}
			}
		});
	}

	private java.io.File getAlbumDir()
	{
		return new java.io.File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), info.guardianproject.securereaderinterface.App.getInstance().getString(R.string.app_name));
	}

	private void createImageFile(boolean isVideo) throws IOException
	{
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = timeStamp + "_";
		java.io.File dir = getAlbumDir();
		dir.mkdir();
		java.io.File image = File.createTempFile(imageFileName, isVideo ? ".mp4" : ".jpg", dir);
		mCurrentPhotoFile = image;
	}

	private void deleteImageFile()
	{
		if (mCurrentPhotoFile != null)
		{
			mCurrentPhotoFile.delete();
			mCurrentPhotoFile = null;
		}
	}

	private void copyFileFromFStoAppFS(java.io.InputStream in, java.io.File src, info.guardianproject.iocipher.File dst) throws IOException
	{
		if (in == null)
			in = new java.io.FileInputStream(src);
		OutputStream out = new info.guardianproject.iocipher.FileOutputStream(dst);

		if (LOGGING) {
			long cb = in.available();
			Log.d(LOGTAG, "File length is " + cb);
		}

		// Transfer bytes from in to out
		byte[] buf = new byte[8196];
		int len;
		while ((len = in.read(buf)) > 0)
		{
			out.write(buf, 0, len);
			if (LOGGING)
				Log.v(LOGTAG, "Writing:"+len);
		}
		in.close();
		out.close();

		if (LOGGING)
			Log.v(LOGTAG, "copyFileFromFStoAppFS Copied from " + ((src == null) ? "stream" : src.toString()) + " to " + dst.toString());
	}

	private void updateMediaControls()
	{
		// Show or hide the add media button
		if (mStory == null || mStory.getNumberOfMediaContent() == 0)
		{
			mMediaView.setVisibility(View.INVISIBLE);
			mBtnMediaAdd.setVisibility(View.VISIBLE);
			mOperationButtons.setVisibility(View.GONE);
		}
		else
		{
			mMediaView.setVisibility(View.VISIBLE);
			mBtnMediaAdd.setVisibility(View.GONE);
			mOperationButtons.setVisibility(View.GONE);
		}
	}

	private void populateFromStory()
	{
		if (mStory == null)
			return;
		mEditTitle.setText(mStory.getTitle());
		mEditContent.setText(mStory.getCleanMainContent());

		ArrayList<String> tags = mStory.getTags();
		if (tags != null)
		{
			StringBuilder sb = new StringBuilder();
			for (String tag : tags)
			{
				if (sb.length() > 0)
					sb.append(" ");
				sb.append("#");
				sb.append(tag);
			}
			mEditTags.setText(sb.toString());
		}

		MediaViewCollection collection = new MediaViewCollection(mMediaView.getContext(), mStory);
		collection.load(true, false);
		mMediaView.setMediaCollection(collection, false, false);
	}

	private ArrayList<String> getTagsFromInput()
	{
		ArrayList<String> ret = null;

		String tagsString = mEditTags.getText().toString();
		if (tagsString != null)
		{
			tagsString = tagsString.replace("#", ""); // remove # from the
														// string
			if (tagsString.length() > 0)
				ret = new ArrayList<String>(Arrays.asList(tagsString.split("\\s+", 0)));
		}
		return ret;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		if (mEditTitle.getText().length() > 0)
			outState.putString("title", mEditTitle.getText().toString());
		if (mEditContent.getText().length() > 0)
			outState.putString("content", mEditContent.getText().toString());
		if (mEditTags.getText().length() > 0)
			outState.putString("tags", mEditTags.getText().toString());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);

		if (savedInstanceState.containsKey("title"))
			mEditTitle.setText(savedInstanceState.getString("title"));
		if (savedInstanceState.containsKey("content"))
			mEditContent.setText(savedInstanceState.getString("content"));
		if (savedInstanceState.containsKey("tags"))
			mEditTags.setText(savedInstanceState.getString("tags"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		boolean ret = super.onCreateOptionsMenu(menu);

		mMenuPost = menu.findItem(R.id.menu_post);
		if (mMenuPost != null)
		{
			MenuItemCompat.setActionProvider(mMenuPost, new ActionProvider(this)
			{
				@Override
				public View onCreateActionView()
				{
					LayoutInflater inflater = LayoutInflater.from(AddPostActivity.this);
					View view = inflater.inflate(R.layout.actionbar_green_button, null);
					view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
					TextView tv = (TextView) view.findViewById(R.id.tvItem);
					tv.setText(R.string.add_post_menu_post);
					view.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							if (!checkValidPost())
							{
								showNoValidDataWarning();
								return;
							}

							saveDraft(false);
							loadingDialog = ProgressDialog.show(AddPostActivity.this, "", "Posting. Please wait...", true, true);

							XMLRPCPublisherCallback publisherCallback = new XMLRPCPublisherCallback()
							{

								@Override
								public void itemPublished(int itemId) {
									if (loadingDialog.isShowing())
									{
										loadingDialog.dismiss();
										quitBackToList(0); // "published"
									}
								}

								@Override
								public void publishingFailed(int reason) {
									loadingDialog.dismiss();

									// Reset to draft
									mStory.setFeedId(DatabaseHelper.DRAFTS_FEED_ID);
									info.guardianproject.securereaderinterface.App.getInstance().socialReporter.saveDraft(mStory);

									// Should add in reasons
									if (reason == XMLRPCPublisher.FAILURE_REASON_NO_PRIVACY_PROXY) {
										Toast.makeText(AddPostActivity.this, "Posting Failed, Tor or Psiphon Required for Posting", Toast.LENGTH_SHORT).show();
									} else if (reason == XMLRPCPublisher.FAILURE_REASON_NO_CONNECTION) {
										Toast.makeText(AddPostActivity.this, "Posting Failed, No Network Connection", Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText(AddPostActivity.this, "Posting Failed, Failure Unknown", Toast.LENGTH_SHORT).show();
									}
								}

							};
							info.guardianproject.securereaderinterface.App.getInstance().socialReporter.publish(mStory, publisherCallback);
						}
					});
					return view;
				}

				@Override
				public boolean hasSubMenu()
				{
					return false;
				}
			});
		}

		return ret;
	}

	private boolean saveDraftOrAskForDeletion()
	{
		// Does it contain data at the moment?
		if (isEmpty() && (mStory == null || mStory.getNumberOfMediaContent() == 0))
		{
			// No. If we have saved a draft already, ask if user wants to
			// delete it
			if (mStory != null)
			{
				Builder alert = new AlertDialog.Builder(this).setPositiveButton(R.string.add_post_draft_delete, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
						info.guardianproject.securereaderinterface.App.getInstance().socialReporter.deleteDraft(mStory);
						quitBackToList(-1); // wherever we were
											// before
					}
				}).setNegativeButton(R.string.add_post_draft_save, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if (saveDraft(false))
							Toast.makeText(AddPostActivity.this, R.string.add_post_draft_saved, Toast.LENGTH_SHORT).show();
						dialog.dismiss();
						quitBackToList(2); // drafts
					}
				}).setMessage(R.string.add_post_draft_delete_empty);
				alert.show();
				return false;
			}
		}

		if (saveDraft(false))
			Toast.makeText(this, R.string.add_post_draft_saved, Toast.LENGTH_SHORT).show();

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			if (this.saveDraftOrAskForDeletion())
				quitBackToList(2); // drafts
			return true;
		}
		boolean ret = super.onOptionsItemSelected(item);
		if (item.getItemId() == R.id.menu_logout) {
			// If we logged out, update views
			showHideCreateAccount(true);
		}
		return ret;
	}

	private void cleanupAfterFilePicking() {
		deleteImageFile();
	}

	@Override
	protected void onUnlockedActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent)
	{
		super.onUnlockedActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch (requestCode)
		{
		case REQ_CODE_PICK_IMAGE:
			if (resultCode == RESULT_OK)
			{
				try
				{
					String defaultType = "image/jpeg";
					if (mStartedIntent.getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE))
					{
						this.addMediaItem(null, mCurrentPhotoFile, null, defaultType, mReplaceThisIndex, null);
					}
					else if (mStartedIntent.getAction().equals(MediaStore.ACTION_VIDEO_CAPTURE))
					{
						defaultType = "video/mp4";
						this.addMediaItem(null, mCurrentPhotoFile, null, defaultType, mReplaceThisIndex, null);
					}
					else  if (imageReturnedIntent != null) {
						Uri mediaUri = imageReturnedIntent.getData();
						if (mediaUri == null && imageReturnedIntent.getDataString() != null)
							mediaUri = Uri.parse(imageReturnedIntent.getDataString());
						if (mediaUri == null) {
							if (LOGGING)
								Log.d(LOGTAG, "Got no valid uri, bailing");
							cleanupAfterFilePicking();
							return;
						}
						if (LOGGING)
							Log.d(LOGTAG, "Adding file " + mediaUri);

						java.io.InputStream mediaItemStream = null;
						String mediaDisplayName = getMediaDisplayName(mediaUri);

						String type = getContentResolver().getType(mediaUri);
						if (type != null)
							defaultType = type;

						mediaItemStream = getContentResolver().openInputStream(mediaUri);
						this.addMediaItem(mediaItemStream, null, imageReturnedIntent.getData().toString(), defaultType, mReplaceThisIndex, mediaDisplayName);
					} else {
						cleanupAfterFilePicking();
					}
				}
				catch (Exception ex)
				{
					if (LOGGING)
						Log.e(LOGTAG, "Failed to add image/video: " + ex.toString());
				}
			}
			else
			{
				// Delete temp file, if we created one
				if (LOGGING)
					Log.e(LOGTAG, "Canceled - remove file");
				cleanupAfterFilePicking();
			}
		}
	}

	private String getMediaDisplayName(final Uri uri) {
		String result = null;
		String[] projections = new String[]{
				OpenableColumns.DISPLAY_NAME
		};
		Cursor cursor = null;
		try {
			cursor = getContentResolver().query(uri, projections, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		if (result == null) {
			result = uri.getPath();
			int cut = result.lastIndexOf('/');
			if (cut != -1) {
				result = result.substring(cut + 1);
			}
		}
		return result;
	}

	/**
	 * Add or replace a piece of media in the current draft.
	 * 
	 * @param mediaItemUrl
	 *            Uri to the new media to add
	 * @param mediaType
	 *            If type can't be decided automatically, use this as a default
	 *            type
	 * @param replaceIndex
	 *            Optional index to replace (-1 for normal add)
	 */
	// private void addMediaItem(Uri mediaItem, String defaultType, int
	// replaceIndex)
	// We want files here so we can deal with them all the same
	private void addMediaItem(java.io.InputStream mediaItemStream, java.io.File mediaItemFile, String mediaItemUrl, String mediaType, int replaceIndex, String displayName)
	{
		if (LOGGING)
			Log.d(LOGTAG, "addMediaItem called");
		if (mStory == null) {
			if (LOGGING)
				Log.d(LOGTAG, "addMediaItem - saveDraft");
			saveDraft(true);
		}

		MediaContent newMediaContent = new MediaContent(mStory.getDatabaseId(), "", mediaType);

		if (displayName != null) {
			// Use the expression field for display name...
			newMediaContent.setExpression(displayName);
		}

		// Let's get a record with an id
		if (replaceIndex == -1)
		{
			if (LOGGING)
				Log.d(LOGTAG, "addMediaItem - new content");
			mStory.addMediaContent(newMediaContent);
		}
		else
		{
			if (LOGGING)
				Log.d(LOGTAG, "addMediaItem - replace content");
			ArrayList<MediaContent> rgMedia = mStory.getMediaContent();
			rgMedia.remove(replaceIndex);
			rgMedia.add(replaceIndex, newMediaContent);
		}
		// This should give us a database id if one doesn't exist already
		if (LOGGING)
			Log.d(LOGTAG, "addMediaItem - saveDraft(false)");
		saveDraft(false);

		mProgressIcon.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate));
		mProgressIcon.setVisibility(View.VISIBLE);
		mBtnMediaAdd.setVisibility(View.GONE);
		mIsAddingMedia = true;
	
		ThreadedTask<Void, Void, Void> addMediaTask = new ThreadedTask<Void, Void, Void>()
		{	
			private InputStream mediaItemStream;
			private java.io.File mediaItemFile;
			private String mediaItemUrl;
			private MediaContent mediaContent;

			public ThreadedTask<Void, Void, Void> init(java.io.InputStream mediaItemStream, java.io.File mediaItemFile, String mediaItemUrl, MediaContent mediaContent)
			{
				this.mediaItemStream = mediaItemStream;
				this.mediaItemFile = mediaItemFile;
				this.mediaItemUrl = mediaItemUrl;
				this.mediaContent = mediaContent;
				return this;
			}
			
			@Override
			protected Void doInBackground(Void... values) 
			{
				if (AddPostActivity.this.LOGGING)
					Log.d(AddPostActivity.this.LOGTAG, "addMediaItem - doInBackground");
				if (mediaItemStream != null || mediaItemFile != null)
				{
					if (AddPostActivity.this.LOGGING)
						Log.d(AddPostActivity.this.LOGTAG, "addMediaItem - copy file");
					// Now we can copy the file
					File outputFile;
					outputFile = new File(info.guardianproject.securereaderinterface.App.getInstance().socialReader.getFileSystemDir(), SocialReader.MEDIA_CONTENT_FILE_PREFIX + mediaContent.getDatabaseId());
					
					if (AddPostActivity.this.LOGGING)
						Log.v(AddPostActivity.this.LOGTAG, "Local App Storage File: " + outputFile);

					// First copy file to encrypted storage
					try
					{
						copyFileFromFStoAppFS(mediaItemStream, mediaItemFile, outputFile);
						mediaContent.setDownloaded(true);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}

					// Update record in media content
					mediaContent.setUrl("file://" + outputFile.getAbsolutePath());
				}
				else
				{
					mediaContent.setUrl(mediaItemUrl);
				}
				if (AddPostActivity.this.LOGGING)
					Log.v(AddPostActivity.this.LOGTAG, "Set Url to: " + mediaContent.getUrl());
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				mIsAddingMedia = false;

				mProgressIcon.clearAnimation();
				mProgressIcon.setVisibility(View.GONE);

				if (LOGGING)
					Log.d(LOGTAG, "addMediaItem - cleanup");

				// If this was a temp capture file, delete it
				deleteImageFile();
				onMediaChanged();
			}
		}.init(mediaItemStream, mediaItemFile, mediaItemUrl, newMediaContent);
		if (LOGGING)
			Log.d(LOGTAG, "addMediaItem - start async job");
		addMediaTask.execute((Void)null);
	}

	private void onMediaChanged()
	{
		// Save our changes
		saveDraft(false);

		// Update the media view to show new media as well
		MediaViewCollection collection = new MediaViewCollection(mMediaView.getContext(), mStory);
		collection.load(true, false);
		mMediaView.setMediaCollection(collection, false, false);
		updateMediaControls();
	}

	private boolean isEmpty()
	{
		if (TextUtils.isEmpty(mEditTitle.getText()) && TextUtils.isEmpty(mEditContent.getText()))
		{
			ArrayList<String> tags = getTagsFromInput();
			if (tags == null || tags.size() == 0)
				return true;
		}
		return false;
	}

	private boolean saveDraft(boolean forceCreate)
	{
		if (LOGGING) 
			Log.v(LOGTAG, "Saving draft");
		
		if (mStory == null)
		{
			// If nothing had been changed yet, no need to save!
			if (!forceCreate && TextUtils.isEmpty(mEditTitle.getText()) && TextUtils.isEmpty(mEditContent.getText()) && TextUtils.isEmpty(mEditTags.getText()))
				return false;

			mStory = info.guardianproject.securereaderinterface.App.getInstance().socialReporter.createDraft(mEditTitle.getText().toString(), mEditContent.getText().toString(), getTagsFromInput(), null);
			getIntent().putExtra("story", mStory.getDatabaseId());
		}
		else
		{
			mStory.setTitle(mEditTitle.getText().toString());
			mStory.setDescription(mEditContent.getText().toString());
			mStory.getTags().clear();
			ArrayList<String> tags = getTagsFromInput();
			if (tags != null && tags.size() > 0)
				mStory.getTags().addAll(tags);
		}
		info.guardianproject.securereaderinterface.App.getInstance().socialReporter.saveDraft(mStory);

		if (LOGGING)
			Log.v(LOGTAG, "SaveDraft: Story Database Id: " + mStory.getDatabaseId());

		return true;
	}

	private boolean checkValidPost()
	{
		if (TextUtils.isEmpty(mEditTitle.getText()))
			return false;
		return true;
	}

	private void showNoValidDataWarning()
	{
		Builder alert = new AlertDialog.Builder(this).setTitle(R.string.add_post_not_valid_data_warning_title)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				}).setMessage(R.string.add_post_not_valid_data_warning_content);
		alert.show();
	}

	private class TagTextWatcher implements TextWatcher
	{
		private boolean mRemoved;
		private boolean mChanging;
		private final int mHashColor;

		public TagTextWatcher()
		{
			mHashColor = getResources().getColor(R.color.grey_light_medium);
		}

		@Override
		public void afterTextChanged(Editable s)
		{
			if (!mChanging)
			{
				if (mRemoved)
				{
					if (s.length() > 0)
					{
						int remove = 0;
						int idx = s.length() - 1;
						while (Character.isWhitespace(s.charAt(idx)))
						{
							remove++;
							idx--;
						}
						if (remove > 0)
						{
							mChanging = true;
							s.delete(s.length() - remove, s.length());
							mChanging = false;
						}
					}
				}
				else
				{
					if (s.length() == 0 || Character.isWhitespace(s.charAt(s.length() - 1)))
					{
						mChanging = true;
						s.append("#");
						mChanging = false;
					}
					else if (s.length() > 0 && s.charAt(0) != '#')
					{
						mChanging = true;
						s.insert(0, "#");
						mChanging = false;
					}
				}

				mChanging = true;

				// Simply doing s.clearSpans does not work, see
				// http://stackoverflow.com/questions/9403057/android-edittext-clearing-spans
				ForegroundColorSpan[] toRemoveSpans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
				for (int i = 0; i < toRemoveSpans.length; i++)
					s.removeSpan(toRemoveSpans[i]);

				// Style the hashes in a more discrete grey color
				for (int i = s.length() - 1; i >= 0; i--)
				{
					if (s.charAt(i) == '#')
					{
						s.setSpan(new ForegroundColorSpan(mHashColor), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

						// Make sure a space exists before hashes
						if (i > 0 && !Character.isWhitespace(s.charAt(i - 1)))
						{
							s.insert(i, " ");
						}
					}
				}
				mChanging = false;

			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{
			if (count < before)
				mRemoved = true;
			else
				mRemoved = false;
		}
	}

	private void quitBackToList(int go_to_tab)
	{
		Bundle commandParameters = null;
		if (go_to_tab != -1)
		{
			commandParameters = new Bundle();
			commandParameters.putInt("go_to_tab", go_to_tab);
		}
		UICallbacks.handleCommand(this, R.integer.command_posts_list, commandParameters);
		overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
		finish();
	}

	@Override
	public void onBackPressed()
	{
		if (saveDraftOrAskForDeletion())
			quitBackToList(2); // drafts
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus)
	{
		saveDraft(false);
	}

	private class HandlerIntent
	{
		public final Intent intent;
		public final ResolveInfo resolveInfo;

		public HandlerIntent(Intent intent, ResolveInfo resolveInfo)
		{
			this.intent = intent;
			this.resolveInfo = resolveInfo;
		}
	}

	private class HandlerIntentListAdapter extends ArrayAdapter<HandlerIntent>
	{
		public HandlerIntentListAdapter(Context context, HandlerIntent[] intents)
		{
			super(context, android.R.layout.select_dialog_item, android.R.id.text1, intents);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			// User super class to create the View
			View v = super.getView(position, convertView, parent);
			TextView tv = (TextView) v.findViewById(android.R.id.text1);

			HandlerIntent handlerIntent = getItem(position);
			ResolveInfo info = handlerIntent.resolveInfo;
			PackageManager pm = getContext().getPackageManager();
			tv.setText(info.loadLabel(pm));

			Drawable icon = info.loadIcon(pm);
			int iconSize = UIHelpers.dpToPx(32, getContext());
			icon.setBounds(0, 0, iconSize, iconSize);

			// Put the image on the TextView
			tv.setCompoundDrawables(icon, null, null, null);
			tv.setCompoundDrawablePadding(UIHelpers.dpToPx(10, getContext()));

			return v;
		}
	};

	private void getHandlersForIntent(Intent intent, ArrayList<HandlerIntent> rgIntents)
	{
		PackageManager pm = getPackageManager();
		List<ResolveInfo> resInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

		for (ResolveInfo resInfo : resInfos)
		{
			rgIntents.add(new HandlerIntent(intent, resInfo));
		}
	}

	private void createMediaChooser(int replaceThisIndex)
	{
		mReplaceThisIndex = replaceThisIndex; // Remember which index to
												// replace, if any (set to -1
												// for "add")

		Builder alert = new AlertDialog.Builder(this);

		LayoutInflater inflater = LayoutInflater.from(this);
		View contentView = inflater.inflate(R.layout.add_post_media_chooser, null);

		TabHost tabHost = (TabHost) contentView.findViewById(android.R.id.tabhost);
		tabHost.setup();

		TabSpec spec = tabHost.newTabSpec("photo");
		View tabView = inflater.inflate(R.layout.light_tab_item, null);
		((TextView) tabView.findViewById(R.id.tvItem)).setText(getString(R.string.add_post_media_chooser_photo));
		spec.setIndicator(tabView);
		spec.setContent(R.id.lvPhoto);
		tabHost.addTab(spec);

		ListView lv = (ListView) contentView.findViewById(R.id.lvPhoto);

		// Populate photo intents
		final ArrayList<HandlerIntent> rgIntentsPhoto = new ArrayList<HandlerIntent>();
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		getHandlersForIntent(cameraIntent, rgIntentsPhoto);
		Intent intent = new Intent(Build.VERSION.SDK_INT >= 19 ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("image/*");
		getHandlersForIntent(intent, rgIntentsPhoto);
		ListAdapter adapterPhoto = new HandlerIntentListAdapter(this, rgIntentsPhoto.toArray(new HandlerIntent[rgIntentsPhoto.size()]));
		lv.setAdapter(adapterPhoto);
		lv.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
					requirePermissionThenStart((HandlerIntent) parent.getAdapter().getItem(position));
			}
		});

		spec = tabHost.newTabSpec("video");
		tabView = inflater.inflate(R.layout.light_tab_item, null);
		((TextView) tabView.findViewById(R.id.tvItem)).setText(getString(R.string.add_post_media_chooser_video));
		spec.setIndicator(tabView);
		spec.setContent(R.id.lvVideo);
		tabHost.addTab(spec);

		// Populate video intents
		lv = (ListView) contentView.findViewById(R.id.lvVideo);
		lv.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				requirePermissionThenStart((HandlerIntent) parent.getAdapter().getItem(position));
			}
		});

		final ArrayList<HandlerIntent> rgIntentsVideo = new ArrayList<HandlerIntent>();
		Intent cameraIntentVideo = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		getHandlersForIntent(cameraIntentVideo, rgIntentsVideo);
		intent = new Intent(Build.VERSION.SDK_INT >= 19 ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("video/*");
		getHandlersForIntent(intent, rgIntentsVideo);
		ListAdapter adapterVideo = new HandlerIntentListAdapter(this, rgIntentsVideo.toArray(new HandlerIntent[rgIntentsVideo.size()]));
		lv.setAdapter(adapterVideo);

		// Audio
		spec = tabHost.newTabSpec("audio");
		tabView = inflater.inflate(R.layout.light_tab_item, null);
		((TextView) tabView.findViewById(R.id.tvItem)).setText(getString(R.string.add_post_media_chooser_audio));
		spec.setIndicator(tabView);
		spec.setContent(R.id.lvAudio);
		tabHost.addTab(spec);

		// Populate audio intents
		lv = (ListView) contentView.findViewById(R.id.lvAudio);
		lv.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				requirePermissionThenStart((HandlerIntent) parent.getAdapter().getItem(position));
			}
		});

		final ArrayList<HandlerIntent> rgIntentsAudio = new ArrayList<HandlerIntent>();
		intent = new Intent(Build.VERSION.SDK_INT >= 19 ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("audio/*");
		getHandlersForIntent(intent, rgIntentsAudio);
		ListAdapter adapterAudio = new HandlerIntentListAdapter(this, rgIntentsAudio.toArray(new HandlerIntent[rgIntentsAudio.size()]));
		lv.setAdapter(adapterAudio);

		alert.setView(contentView);

		mMediaChooserDialog = alert.show();
	}

	private void requirePermissionThenStart(HandlerIntent info) {
		if (LOGGING)
			Log.d(LOGTAG, "requirePermissionThenStart");
		int permissionCheck = ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE);
		if (Build.VERSION.SDK_INT <= 18) {
			permissionCheck = PackageManager.PERMISSION_GRANTED; // For old devices we ask in the manifest!
			if (LOGGING)
				Log.d(LOGTAG, "Old device - granted");
		}
		if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
			if (LOGGING)
				Log.d(LOGTAG, "Permission not granted - ask");
			mPendingIntent = info; // Start this if we get permission
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
		} else {
			if (LOGGING)
				Log.d(LOGTAG, "Permission granted - start resolved intent");
			startResolvedIntent(info);
		}
	}

	protected void startResolvedIntent(HandlerIntent info)
	{
		try
		{
			if (LOGGING) {
				String packageName = "<null>";
				if (info.resolveInfo != null && info.resolveInfo.activityInfo != null && info.resolveInfo.activityInfo.packageName != null)
					packageName = info.resolveInfo.activityInfo.packageName;
				String name = "<null>";
				if (info.resolveInfo != null && info.resolveInfo.activityInfo != null && info.resolveInfo.activityInfo.name != null)
					packageName = info.resolveInfo.activityInfo.name;
				Log.e(LOGTAG, "startResolvedIntent: " + info.intent.toString() + " package " + packageName + " name " + name);
			}
			if (MediaStore.ACTION_IMAGE_CAPTURE.equals(info.intent.getAction()) || MediaStore.ACTION_VIDEO_CAPTURE.equals(info.intent.getAction()))
			{
				boolean bIsVideo = MediaStore.ACTION_VIDEO_CAPTURE.equals(info.intent.getAction());
				createImageFile(bIsVideo);
				Uri uriSavedImage = Uri.fromFile(mCurrentPhotoFile.getAbsoluteFile());
				info.intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
			}
			info.intent.setClassName(info.resolveInfo.activityInfo.packageName, info.resolveInfo.activityInfo.name);
			mStartedIntent = info.intent;
			startActivityForResult(info.intent, REQ_CODE_PICK_IMAGE);
			mMediaChooserDialog.dismiss();
		}
		catch (IOException e)
		{
			if (LOGGING)
				Log.d(LOGTAG, "Exception: " + e.toString());
			e.printStackTrace();
			deleteImageFile();
		}
	}

	private void showHideCreateAccount(boolean animate)
	{
		if (!App.isSignedIn()) {
			if (mMenuPost != null)
				mMenuPost.setVisible(false);
			if (mSignIn == null) {
				mSignIn = new WPSignInView(this);
				mSignIn.setListener(new WPSignInView.OnLoginListener() {
					@Override
					public void onLoggedIn(String username, String password) {
						UIHelpers.hideSoftKeyboard(AddPostActivity.this);
						((ViewGroup) mSignIn.getParent()).removeView(mSignIn);
						mSignIn = null;
						App.getInstance().socialReader.ssettings.setXMLRPCUsername(username);
						App.getInstance().socialReader.ssettings.setXMLRPCPassword(password);
						if (mMenuPost != null)
							mMenuPost.setVisible(true);
						showHideCreateAccount(true);
					}
				});
				((ViewGroup) findViewById(R.id.add_post_root)).addView(mSignIn, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			}
		}
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();
		quitBackToList(0); // published
	}
}
