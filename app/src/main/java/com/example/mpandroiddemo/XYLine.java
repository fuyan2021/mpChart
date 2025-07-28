package com.example.mpandroiddemo;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

/**
 * fy
 * 2025/7/23
 **/
public class XYLine extends View implements IMarker {

    public XYLine(Context context){
        super(context);
    }

    public XYLine(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public MPPointF getOffset() {
        return null;
    }

    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
        return null;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {

    }

    @Override
    public void draw(Canvas canvas, float posX, float posY) {

    }
}
