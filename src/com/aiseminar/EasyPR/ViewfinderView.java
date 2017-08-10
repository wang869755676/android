/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aiseminar.EasyPR;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * 自定义组件实现,扫描功能
 */
public final class ViewfinderView extends View {


    private final Paint paint;

    private final int maskColor = Color.parseColor("#80000000");


    private int center_x;
    private int center_y;
    private int xLength;
    private int yLength;


    private int recWidth = 500;
    private int recHeight = 316;

    // 扫描框边角长度
    private int innercornerlength = 20;
    // 扫描框边角宽度
    private int innercornerwidth = 5;

    private int height;
    private int width;
   private Rect frame;
    public ViewfinderView(Context context) {
        this(context, null);
    }

    public ViewfinderView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);

    }

    public ViewfinderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();

        recHeight = dip2px(context, 150);
        recWidth = dip2px(context, 250);
        innercornerlength = dip2px(context, 10);
        innercornerwidth = dip2px(context, 2);
        init();


    }

    private void init() {

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onDraw(Canvas canvas) {

        frame = new Rect();
        if (frame == null) {
            return;
        }
        width = canvas.getWidth();
        height = canvas.getHeight();
        center_x = (int) (width / 2.0);
        center_y = (int) (height / 2.0);
        xLength = (int) (recWidth / 2.0);
        yLength = (int) (recHeight / 2.0);


        frame.top = center_y - yLength;
        frame.left = center_x - xLength;

        frame.bottom = center_y + yLength;
        frame.right = center_x + xLength;


        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(Color.parseColor("#80000000"));
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        drawFrameBounds(canvas, frame);
    }


    /**
     * 绘制取景框边框
     *
     * @param canvas
     * @param frame
     */
    private void drawFrameBounds(Canvas canvas, Rect frame) {

        /*paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        canvas.drawRect(frame, paint);*/

        paint.setColor(Color.parseColor("#FD558D"));
        paint.setStyle(Paint.Style.FILL);

        int corWidth = innercornerwidth;
        int corLength = innercornerlength;

        // 左上角
        canvas.drawRect(frame.left, frame.top, frame.left + corWidth, frame.top
                + corLength, paint);
        canvas.drawRect(frame.left, frame.top, frame.left
                + corLength, frame.top + corWidth, paint);
        // 右上角
        canvas.drawRect(frame.right - corWidth, frame.top, frame.right,
                frame.top + corLength, paint);
        canvas.drawRect(frame.right - corLength, frame.top,
                frame.right, frame.top + corWidth, paint);
        // 左下角
        canvas.drawRect(frame.left, frame.bottom - corLength,
                frame.left + corWidth, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - corWidth, frame.left
                + corLength, frame.bottom, paint);
        // 右下角
        canvas.drawRect(frame.right - corWidth, frame.bottom - corLength,
                frame.right, frame.bottom, paint);
        canvas.drawRect(frame.right - corLength, frame.bottom - corWidth,
                frame.right, frame.bottom, paint);
    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public Rect getFrame() {
        return frame;
    }
}
