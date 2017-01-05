package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.tinymission.rss.MediaContent;

import info.guardianproject.iocipher.File;
import info.guardianproject.securereaderinterface.R;

public class AudioMediaContentPreviewView extends FrameLayout implements MediaContentPreviewView
{
	public static final String LOGTAG = "AudioMediaContentPreview";
	public static final boolean LOGGING = false;

	private File mMediaFile;
	private MediaContent mMediaContent;
	private TextView mTvDisplayName;

	public AudioMediaContentPreviewView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public AudioMediaContentPreviewView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public AudioMediaContentPreviewView(Context context)
	{
		super(context);
		initView(context);
	}

	public void setScaleType(ScaleType scaleType)
	{
	}

	private void initView(Context context)
	{
		View.inflate(context, R.layout.audio_preview_view, this);
		mTvDisplayName = (TextView)findViewById(R.id.mediaDisplayName);
	}

	public void setMediaContent(MediaContent mediaContent, File mediaFile, java.io.File mediaFileNonVFS, boolean useThisThread)
	{
		mMediaContent = mediaContent;
		mMediaFile = mediaFile;
		if (mTvDisplayName != null && mediaContent.getExpression() != null) {
			mTvDisplayName.setText(mediaContent.getExpression());
		}
		if (mMediaFile == null)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to download media, no file.");
			return;
		}
	}

	public MediaContent getMediaContent()
	{
		return mMediaContent;
	}
	
	public void recycle()
	{
	}
}