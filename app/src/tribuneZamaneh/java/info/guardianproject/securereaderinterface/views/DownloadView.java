package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.FragmentActivityWithMenu;
import info.guardianproject.securereaderinterface.FragmentActivityWithMenu.PullDownActionBarListener;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class DownloadView extends RelativeLayout implements PullDownActionBarListener {

	public DownloadView(Context context) {
		super(context);
	}

	public DownloadView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DownloadView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (getContext() instanceof FragmentActivityWithMenu)
		{
			FragmentActivityWithMenu activity = (FragmentActivityWithMenu) getContext();
			setTopOffset(activity.getPullDownActionBarHeight());
			activity.addPullDownActionBarListener(this);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		if (getContext() instanceof FragmentActivityWithMenu)
		{
			FragmentActivityWithMenu activity = (FragmentActivityWithMenu) getContext();
			activity.removePullDownActionBarListener(this);
		}
		super.onDetachedFromWindow();
	}

	private void setTopOffset(int offset)
	{
		setPadding(getPaddingLeft(), offset, getPaddingRight(), getPaddingBottom());
	}

	@Override
	public void onOffsetChanged(int height, int offset) {
		setTopOffset(height);
	}
}
