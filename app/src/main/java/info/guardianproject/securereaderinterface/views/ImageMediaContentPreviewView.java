package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.tinymission.rss.MediaContent;

import info.guardianproject.iocipher.File;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;

public class ImageMediaContentPreviewView extends ImageView implements MediaContentPreviewView
{
	public static final String LOGTAG = "ImageMediaPreviewView";
	public static final boolean LOGGING = false;
	
	private MediaContent mMediaContent;
	private File mMediaFile;
	private boolean mUseThisThread;
	private boolean mIsUpdate; // true if this view has already shown this content previously
	private boolean mHasSetDrawable;

	public ImageMediaContentPreviewView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public ImageMediaContentPreviewView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public ImageMediaContentPreviewView(Context context)
	{
		super(context);
		initView(context);
	}

	private void initView(Context context)
	{
		this.setScaleType(ScaleType.CENTER_CROP);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);

		if (!mHasSetDrawable)
			setBitmapIfDownloaded();
	}

	@Override
	public void setImageDrawable(Drawable d)
	{
		// If we are setting the image from a different thread, make sure to
		// fade it in.
		// If we, however, set it from this thread (as we do when closing the
		// full screen mode
		// view) we want it to show immediately!
		//if (d != null && !mUseThisThread && !mIsUpdate)
		//	AnimationHelpers.fadeOut(this, 0, 0, false);
		super.setImageDrawable(d);
		//if (d != null && !mUseThisThread && !mIsUpdate)
		//	AnimationHelpers.fadeIn(this, 500, 0, false);
		//else if (d != null)
		//	AnimationHelpers.fadeIn(this, 0, 0, false);
	}

	public void recycle()
	{
		setImageDrawable(null);
	}

	private synchronized void setBitmapIfDownloaded() {
		int w = getWidth();
		int h = getHeight();
		if (mMediaFile != null && w > 0 && h > 0 && !mHasSetDrawable) {
			mHasSetDrawable = true;
			Picasso.with(getContext())
					.load(Uri.parse(mMediaFile.getAbsolutePath()))
					.centerCrop()
					.resize(w, h)
					.into(this);
		}
	}

	@Override
	public void setMediaContent(MediaContent mediaContent, File mediaFile, java.io.File mediaFileNonVFS, boolean useThisThread)
	{
		mIsUpdate = (mediaContent == mMediaContent && mMediaFile != null && mMediaFile.equals(mediaFile));
		mMediaContent = mediaContent;
		mMediaFile = mediaFile;
		mHasSetDrawable = false;
		mUseThisThread = useThisThread;
		if (mMediaFile == null)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to download media, no file.");
			return;
		}
		setBitmapIfDownloaded();
	}

	@Override
	public MediaContent getMediaContent()
	{
		return mMediaContent;
	}

}
