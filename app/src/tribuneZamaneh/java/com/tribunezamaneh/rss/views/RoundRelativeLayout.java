package com.tribunezamaneh.rss.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import info.guardianproject.securereaderinterface.R;

/**
 * Created by N-Pex on 16-02-26.
 */
public class RoundRelativeLayout extends RelativeLayout {

    private Paint mBorderPaint;
    private Paint mFillPaint;
    private Path mClipPath;

    public RoundRelativeLayout(Context context) {
        super(context);
        init(context, null);
    }

    @SuppressLint("NewApi")
    public RoundRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public RoundRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RoundRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        Drawable d = getBackground();
        if (d != null && d instanceof ColorDrawable) {
            mFillPaint = new Paint();
            mFillPaint.setColor(((ColorDrawable)d).getColor());
            mFillPaint.setStyle(Paint.Style.FILL);
            mFillPaint.setAntiAlias(true);
            setBackgroundColor(Color.TRANSPARENT);
        }

        int borderColor = Color.TRANSPARENT;
        int borderSize = 0;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RoundRelativeLayout);
        if (a != null) {
            borderColor = a.getColor(R.styleable.RoundRelativeLayout_borderColor, Color.TRANSPARENT);
            borderSize = a.getDimensionPixelSize(R.styleable.RoundRelativeLayout_borderSize, 0);
            a.recycle();
        }

        // If we have a border, create a paint to use later
        if (borderSize > 0 && borderColor != Color.TRANSPARENT) {
            mBorderPaint = new Paint();
            mBorderPaint.setColor(borderColor);
            mBorderPaint.setStrokeWidth(borderSize);
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mBorderPaint.setAntiAlias(true);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mClipPath = new Path();
        mClipPath.addCircle(w / 2, h / 2, Math.min(w, h) / 2, Path.Direction.CW);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mClipPath != null)
            canvas.clipPath(mClipPath);
        if (mFillPaint != null && mClipPath != null)
            canvas.drawPath(mClipPath, mFillPaint);
        super.draw(canvas);
        if (mBorderPaint != null)
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, Math.min(getWidth(), getHeight()) / 2 - (mBorderPaint.getStrokeWidth() / 2), mBorderPaint);
    }
}
