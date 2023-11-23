package com.yxh.sensor.mvvm.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.yxh.sensor.R;

/**
 * @author donkor
 * 自定义水平\垂直电池控件
 */
public class BatteryView extends View {
    private int mPower = 100;
    private int orientation;
    private int width;
    private int height;
    private int mColor;

    private final Paint paint = new Paint();
    private final Paint outerStrokePaint = new Paint();

    private final Paint batteryHeadPaint = new Paint();

    private float strokeWidth = 0f;
    private float strokeWidth2 = 0f;

    private float step = 0f;

    private RectF outerRecf = null;

    public BatteryView(Context context) {
        super(context);
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Battery);
        mColor = typedArray.getColor(R.styleable.Battery_batteryColor, Color.TRANSPARENT);
        orientation = typedArray.getInt(R.styleable.Battery_batteryOrientation, 0);
        mPower = typedArray.getInt(R.styleable.Battery_batteryPower, 50);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
        /**
         * recycle() :官方的解释是：回收TypedArray，以便后面重用。在调用这个函数后，你就不能再使用这个TypedArray。
         * 在TypedArray后调用recycle主要是为了缓存。当recycle被调用后，这就说明这个对象从现在可以被重用了。
         *TypedArray 内部持有部分数组，它们缓存在Resources类中的静态字段中，这样就不用每次使用前都需要分配内存。
         */
        typedArray.recycle();
        //设置外边框颜色为黑色
        outerStrokePaint.setStyle(Paint.Style.STROKE);
        outerStrokePaint.setColor(Color.WHITE);
        //设置电池内部
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.FILL);

        batteryHeadPaint.setStyle(Paint.Style.FILL);
        batteryHeadPaint.setColor(Color.WHITE);
    }

    public BatteryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //对View上的內容进行测量后得到的View內容占据的宽度
        width = getMeasuredWidth();
        //对View上的內容进行测量后得到的View內容占据的高度
        height = getMeasuredHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //判断电池方向    horizontal: 0   vertical: 1
        if (orientation == 0) {
            drawHorizontalBattery(canvas);
        } else {
            drawVerticalBattery(canvas);
        }
    }

    /**
     * 绘制水平电池
     * @param canvas
     */
    private void drawHorizontalBattery(Canvas canvas) {
        // 电池外边框
        canvas.drawRect(outerRecf, outerStrokePaint);

        //画电池内矩形电量
        float offset = step * mPower + strokeWidth2;
//        System.out.println(mPower + " => " + offset);
        RectF r2 = new RectF(strokeWidth2 + 2, strokeWidth2 + 2, offset - 2, height - strokeWidth2 - 2);
        //根据电池电量决定电池内矩形电量颜色
        if (mPower < 30) {
            paint.setColor(Color.RED);
        }
        if (mPower >= 30) {
            paint.setColor(Color.WHITE);
        }
        canvas.drawRect(r2, paint);

        //画电池头
        RectF r3 = new RectF(width - strokeWidth/2, height * 0.25f, width, height * 0.75f);
        canvas.drawRect(r3, batteryHeadPaint);
    }

    /**
     * 绘制垂直电池
     *
     * @param canvas
     */
    private void drawVerticalBattery(Canvas canvas) {
        paint.setColor(mColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        int headHeight = (int) (strokeWidth + 0.5f);
        RectF rect = new RectF(strokeWidth2, headHeight + strokeWidth2, width - strokeWidth2, height - strokeWidth2);
        canvas.drawRect(rect, paint);
        paint.setStrokeWidth(0);
        float topOffset = (height - headHeight - strokeWidth) * (100 - mPower) / 100.0f;
        RectF rect2 = new RectF(strokeWidth, headHeight + strokeWidth + topOffset, width - strokeWidth, height - strokeWidth);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rect2, paint);
        RectF headRect = new RectF(width / 4.0f, 0, width * 0.75f, headHeight);
        canvas.drawRect(headRect, paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        strokeWidth = w/20.f;
        strokeWidth2 = strokeWidth/2;
        outerRecf = new RectF(strokeWidth2, strokeWidth2 , width - strokeWidth2, height - strokeWidth2);
        step = (w - strokeWidth)/100f;
        outerStrokePaint.setStrokeWidth(strokeWidth/2);
    }

    /**
     * 设置电池电量
     *
     * @param power
     */
    public void setPower(int power) {
        this.mPower = power;
        if (mPower < 0) {
            mPower = 100;
        }
        invalidate();//刷新VIEW
    }

    /**
     * 设置电池颜色
     *
     * @param color
     */
    public void setColor(int color) {
        this.mColor = color;
        invalidate();
    }

    /**
     * 获取电池电量
     *
     * @return
     */
    public int getPower() {
        return mPower;
    }
}