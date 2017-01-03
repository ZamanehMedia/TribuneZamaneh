package com.tribunezamaneh.rss.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

/**
 * Created by N-Pex on 16-03-01.
 *
 * A specialized view for creating the toolbar shadow
 */
public class ToolbarView extends Toolbar {

    private Path mClip;

    public ToolbarView(Context context) {
        super(context);
        init(context);
    }

    public ToolbarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ToolbarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        createClipPath(context, getWidth(), getHeight());
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                createClipPath(v.getContext(), right - left, bottom - top);
            }
        });
    }

    @Override
    public void draw(Canvas canvas) {
        if (mClip != null)
            canvas.clipPath(mClip);
        super.draw(canvas);
    }

    private void createClipPath(Context context, int width, int height) {
        mClip = new Path();
        int t = UIHelpers.dpToPx(20, context);
        mClip.moveTo(t + UIHelpers.dpToPx(4, context), 0);
        mClip.lineTo(t + UIHelpers.dpToPx(5, context), height / 3);
        mClip.lineTo(t + UIHelpers.dpToPx(5, context), height / 2);
        mClip.lineTo(t, height);
        mClip.lineTo(width - t, height);
        mClip.lineTo(width - t - UIHelpers.dpToPx(4, context), height / 2);
        mClip.lineTo(width - t - UIHelpers.dpToPx(2, context), 0);
        mClip.close();
    }
}
