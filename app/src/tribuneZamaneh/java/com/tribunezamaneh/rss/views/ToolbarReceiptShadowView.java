package com.tribunezamaneh.rss.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.View;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

/**
 * Created by N-Pex on 16-03-01.
 *
 * A specialized view for creating the toolbar shadow
 */
public class ToolbarReceiptShadowView extends View {

    private ReceiptDrawable mReceiptDrawable;

    public ToolbarReceiptShadowView(Context context) {
        super(context);
        init(context);
    }

    @SuppressLint("NewApi")
    public ToolbarReceiptShadowView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public ToolbarReceiptShadowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ToolbarReceiptShadowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mReceiptDrawable = new ReceiptDrawable(context, getWidth(), getHeight(), UIHelpers.dpToPx(5, context));
        setBackground(mReceiptDrawable);
        addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ((right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)) {
                    mReceiptDrawable = new ReceiptDrawable(v.getContext(), right - left, bottom - top, UIHelpers.dpToPx(5, v.getContext()));
                    setBackground(mReceiptDrawable);
                }
            }
        });
    }

    private class ReceiptDrawable extends ShapeDrawable {
        private Path mReceiptPath;
        private Path mShadowPath;
        private Paint mShadowPaint;
        private LayerDrawable mLayerDrawable;

        public ReceiptDrawable(Context context, int w, int h, float dentRadius) {
            super();

            float paddingX = UIHelpers.dpToPx(20, context);
            float wMinusPadding = w - 2 * paddingX;

            mShadowPath = new Path();

            double cos = Math.cos(Math.toRadians(20));
            float overhang = (float)(((float)wMinusPadding / 2.0f) / cos);
            overhang -= ((float)wMinusPadding / 2.0f);
            overhang /= 2;

            RectF arcShadow = new RectF(paddingX - overhang, h - UIHelpers.dpToPx(50, context), w + overhang - paddingX, h);
            mShadowPath.arcTo(arcShadow, 200, 140);

            h = (int)dentRadius + 1;

            float dentDia = 2 * dentRadius;
            if (wMinusPadding > (2 * dentRadius + 3 * dentDia) && h > dentRadius) {

                float numDents = (int)Math.floor((wMinusPadding - dentRadius) / (3 * dentRadius));
                float numStraights = numDents + 1;
                float straightLength = (wMinusPadding - numDents * dentDia) / numStraights;

                float currentX = paddingX;

                mReceiptPath = new Path();
                mReceiptPath.moveTo(paddingX, h);

                for (int i = 0; i < numDents; i++) {
                    currentX += straightLength;
                    mReceiptPath.lineTo(currentX, h);
                    RectF arc = new RectF(currentX, h - dentRadius, currentX + dentDia, h + dentRadius);
                    mReceiptPath.arcTo(arc, 180, 180);
                    currentX += dentDia;
                }

                mReceiptPath.lineTo(w - paddingX, h);
                mReceiptPath.lineTo(w - paddingX, 0);
                mReceiptPath.lineTo(paddingX, 0);
                mReceiptPath.close();

                mLayerDrawable = (LayerDrawable)context.getResources().getDrawable(R.drawable.background_actionbar);
                mLayerDrawable.setBounds(0,0,w,h);

                mShadowPaint = new Paint();
                mShadowPaint.setAntiAlias(true);
                mShadowPaint.setDither(true);
                mShadowPaint.setColor(Color.parseColor("#80666666"));
                mShadowPaint.setStrokeWidth(40f);
                mShadowPaint.setStyle(Paint.Style.STROKE);
                mShadowPaint.setStrokeJoin(Paint.Join.ROUND);
                mShadowPaint.setStrokeCap(Paint.Cap.BUTT);
                mShadowPaint.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));

                Shape s = new Shape() {
                    @Override
                    public void draw(Canvas canvas, Paint paint) {
                        if (mShadowPath != null) {
                            canvas.drawPath(mShadowPath, mShadowPaint);
                        }
                        if (mReceiptPath != null) {
                            canvas.clipPath(mReceiptPath);
                            mLayerDrawable.draw(canvas);
                        }
                    }
                };
                super.setShape(s);
            }
        }
    }

}
