package com.example.mpandroiddemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LineChartView extends View {
    private Paint linePaint;
    private Paint pointPaint;
    private Paint haloPaint;
    private Paint axisPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint guideLinePaint;
    private Path path;
    private List<DataPoint> dataPoints = new ArrayList<>();
    private DataPoint selectedPoint = null;
    private float pointRadius = 16f;

    // 坐标轴边距
    private int paddingLeft = 80;
    private int paddingRight = 50;
    private int paddingTop = 40;
    private int paddingBottom = 60;

    // 网格线数量
    private int gridLinesX = 10;
    private int gridLinesY = 5;

    // 坐标轴范围
    private float xMin = 0f;
    private float xMax = 20000f;
    private float yMin = -50f;
    private float yMax = 50f;
    //节点颜色
    private int pointColor;
    //连接线颜色
    private int lineColor;
    //网格颜色
    private int gridColor;
    //坐标轴数字颜色
    private int axisTextColor;
    //坐标轴颜色
    private int axisColor;
    //显示坐标轴
    private boolean showAxis;
    // 定位线和数据信息相关变量
    private boolean showGuideLines = false;
    private PointF guideLinePoint = new PointF();
    // 最大节点数量
    private static final int MAX_POINTS = 20;
    // 扩大点击判断范围，比实际绘制的节点大50%
    private float touchRange = pointRadius * 5f;

    // 格式化数字显示
    private DecimalFormat xFormat = new DecimalFormat("####0");
    private DecimalFormat yFormat = new DecimalFormat("##0");

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttribute(context, attrs);
        init();
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttribute(context, attrs);
        init();
    }

    //初始化属性
    private void initAttribute(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LineChartView, 0, 0);
        pointColor = ta.getColor(R.styleable.LineChartView_pointColor, Color.YELLOW);
        lineColor = ta.getColor(R.styleable.LineChartView_lineColor, Color.YELLOW);
        axisTextColor = ta.getColor(R.styleable.LineChartView_axisTextColor, Color.GRAY);
        axisColor = ta.getColor(R.styleable.LineChartView_axisColor, Color.BLACK);
        gridColor = ta.getColor(R.styleable.LineChartView_gridColor, Color.BLUE);
        showAxis = ta.getBoolean(R.styleable.LineChartView_showAxis, false);
        ta.recycle();
    }

    private void init() {
        // 初始化折线画笔
        linePaint = new Paint();
        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(5);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        //初始化定位线画笔
        guideLinePaint = new Paint();
        guideLinePaint.setColor(Color.WHITE);
        guideLinePaint.setStrokeWidth(2);
        guideLinePaint.setStyle(Paint.Style.STROKE);
        guideLinePaint.setAntiAlias(true);
        PathEffect guideDashEffect = new DashPathEffect(new float[]{5, 5}, 0);
//        guideLinePaint.setPathEffect(guideDashEffect);
        // 初始化点画笔
        pointPaint = new Paint();
        pointPaint.setColor(pointColor);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);
        // 初始化光晕画笔
        haloPaint = new Paint();
        haloPaint.setStyle(Paint.Style.FILL);
        haloPaint.setAntiAlias(true);
        // 初始化坐标轴画笔
        axisPaint = new Paint();
        axisPaint.setColor(axisColor);
        axisPaint.setStrokeWidth(3);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setAntiAlias(true);

        // 初始化网格线画笔(虚线)
        gridPaint = new Paint();
        gridPaint.setColor(gridColor);
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);
        // 设置虚线效果
        PathEffect dashEffect = new DashPathEffect(new float[]{10, 10}, 0);
//        gridPaint.setPathEffect(dashEffect);

        // 初始化文本画笔
        textPaint = new Paint();
        textPaint.setColor(axisTextColor);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        path = new Path();

        // 初始化默认数据点
        initDefaultPoints();
    }

    private void initDefaultPoints() {
        dataPoints.clear();
        // 添加用户指定的初始节点
        dataPoints.add(new DataPoint(0, 0));
        dataPoints.add(new DataPoint(20, 0));
        dataPoints.add(new DataPoint(30, 10));
        dataPoints.add(new DataPoint(20000, 0));
    }

    public void setDataPoints(List<DataPoint> points) {
        dataPoints.clear();
        dataPoints.addAll(points);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制顺序: 网格 -> 坐标轴 -> 折线 -> 点 -> 文本
        drawGrid(canvas);
        drawAxes(canvas);
        drawAxisLabels(canvas);
        drawLine(canvas);
        drawPoints(canvas);
        // 新增：绘制定位线和数据信息
        if (showGuideLines && selectedPoint != null) {
            guideLinePoint = toScreenCoordinates(selectedPoint);
            drawGuideLines(canvas);
            drawDataInfo(canvas);
        }
    }

    // 绘制坐标轴
    private void drawAxes(Canvas canvas) {
        if (!showAxis) return;
        // X轴 (底部)
        canvas.drawLine(paddingLeft, getHeight() - paddingBottom,
                getWidth() - paddingRight, getHeight() - paddingBottom, axisPaint);
        // Y轴 (左侧)
        canvas.drawLine(paddingLeft, paddingTop,
                paddingLeft, getHeight() - paddingBottom, axisPaint);
    }

    // 绘制坐标轴刻度和标签
    private void drawAxisLabels(Canvas canvas) {
        // 绘制X轴刻度和标签
        float xAxisLength = getWidth() - paddingLeft - paddingRight;
        float xStep = xAxisLength / gridLinesX;
        float xValueStep = (xMax - xMin) / gridLinesX;

        for (int i = 0; i <= gridLinesX; i++) {
            float screenX = paddingLeft + i * xStep;
            float valueX = xMin + i * xValueStep;

            // 绘制刻度线
//            canvas.drawLine(screenX, getHeight() - paddingBottom,
//                    screenX, getHeight() - paddingBottom + 10, axisPaint);
            // 绘制标签
            canvas.drawText(xFormat.format(valueX), screenX,
                    getHeight() - paddingBottom + 35, textPaint);
        }

        // 绘制Y轴刻度和标签
        float yAxisLength = getHeight() - paddingTop - paddingBottom;
        float yStep = yAxisLength / gridLinesY;
        float yValueStep = (yMax - yMin) / gridLinesY;

        for (int i = 0; i <= gridLinesY; i++) {
            float screenY = getHeight() - paddingBottom - i * yStep;
            float valueY = yMin + i * yValueStep;

            // 绘制刻度线
//            canvas.drawLine(paddingLeft - 10, screenY,
//                    paddingLeft, screenY, axisPaint);
            // 绘制标签
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(yFormat.format(valueY), paddingLeft - 15,
                    screenY + 10, textPaint);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    // 绘制网格线
    private void drawGrid(Canvas canvas) {
        // 绘制垂直线 (X方向网格)
        float xAxisLength = getWidth() - paddingLeft - paddingRight;
        float xStep = xAxisLength / gridLinesX;

        for (int i = 0; i <= gridLinesX; i++) {
            float screenX = paddingLeft + i * xStep;
            canvas.drawLine(screenX, paddingTop, screenX, getHeight() - paddingBottom, gridPaint);
        }

        // 绘制水平线 (Y方向网格)
        float yAxisLength = getHeight() - paddingTop - paddingBottom;
        float yStep = yAxisLength / gridLinesY;

        for (int i = 0; i <= gridLinesY; i++) {
            float screenY = getHeight() - paddingBottom - i * yStep;
            canvas.drawLine(paddingLeft, screenY, getWidth() - paddingRight, screenY, gridPaint);
        }
    }

    private void drawLine(Canvas canvas) {
        if (dataPoints.size() <= 1) return;

        path.reset();
        // 移动到第一个点
        PointF firstPoint = toScreenCoordinates(dataPoints.get(0));
        path.moveTo(firstPoint.x, firstPoint.y);

        // 连接其他点
        for (int i = 1; i < dataPoints.size(); i++) {
            PointF screenPoint = toScreenCoordinates(dataPoints.get(i));
            path.lineTo(screenPoint.x, screenPoint.y);
        }

        canvas.drawPath(path, linePaint);
    }

    private void drawPoints(Canvas canvas) {
        for (DataPoint dataPoint : dataPoints) {
            PointF screenPoint = toScreenCoordinates(dataPoint);
            // 创建径向渐变 (中心到边缘的渐变色)
            RadialGradient gradient = new RadialGradient(screenPoint.x, screenPoint.y,
                    // 渐变中心坐标
                    pointRadius * 3,
                    // 渐变半径
                    new int[]{Color.argb(180, 0, 150, 255), Color.argb(0, 0, 150, 255)},  // 颜色数组 (中心色到透明)
                    new float[]{0f, 1f},  // 颜色分布位置
                    Shader.TileMode.CLAMP
            );
            haloPaint.setShader(gradient);
            // 先绘制光晕 (比节点大2倍)
            canvas.drawCircle(screenPoint.x, screenPoint.y, pointRadius * 3, haloPaint);
            // 再绘制实际节点
            canvas.drawCircle(screenPoint.x, screenPoint.y, pointRadius, pointPaint);
        }
    }

    //绘制定位线
    private void drawGuideLines(Canvas canvas) {
        // 垂直定位线
        canvas.drawLine(guideLinePoint.x, paddingTop, guideLinePoint.x, getHeight() - paddingBottom, guideLinePaint);
        // 水平定位线
        canvas.drawLine(paddingLeft, guideLinePoint.y, getWidth() - paddingRight, guideLinePoint.y, guideLinePaint);
    }

    //绘制数据信息
    private void drawDataInfo(Canvas canvas) {
        String dataTextX = String.format("%.0f", selectedPoint.x);
        String dataTextY = String.format("%.0f", selectedPoint.y);
        float textX = guideLinePoint.x + 25;
        float textY = guideLinePoint.y - 25;

        // 确保文本在视图范围内
        if (textX + textPaint.measureText(dataTextX) > getWidth()) {
            textX = guideLinePoint.x - textPaint.measureText(dataTextX) - 10;
        }
        if (textY < paddingTop) {
            textY = guideLinePoint.y + 30;
        }

        canvas.drawText(dataTextX, textX, 25, textPaint);
        canvas.drawText(dataTextY, 25, textY, textPaint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selectedPoint = findSelectedPoint(x, y);
                // 新增：显示定位线
                showGuideLines = (selectedPoint != null);
                // 如果没有选中任何点，且点击在坐标轴范围内，且节点数量未达上限，则创建新节点
                if (dataPoints.size() == MAX_POINTS) {
//                    Toast.makeText(getContext(),"max 20",Toast.LENGTH_SHORT).show();
                }
                if (selectedPoint == null && isInAxisRange(x, y) && dataPoints.size() < MAX_POINTS) {
                    DataPoint newPoint = new DataPoint(0, 0);
                    convertScreenToData(x, y, newPoint);
                    limitPointWithinAxisRange(newPoint);
                    dataPoints.add(newPoint);
                    sortDataPointsByX(); // 按X轴排序节点
                    invalidate();
                    return true;
                }
                return selectedPoint != null;

            case MotionEvent.ACTION_MOVE:
                if (selectedPoint != null) {
                    convertScreenToData(x, y, selectedPoint);
                    limitPointWithinAxisRange(selectedPoint);
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                selectedPoint = null;
                // 新增：隐藏定位线
                showGuideLines = false;
                sortDataPointsByX();
                invalidate();
                break;
        }
        return super.onTouchEvent(event);
    }

    // 检查点击位置是否在坐标轴范围内
    private boolean isInAxisRange(float x, float y) {
        return x >= paddingLeft && x <= getWidth() - paddingRight &&
                y >= paddingTop && y <= getHeight() - paddingBottom;
    }

    // 按X轴值排序数据点
    private void sortDataPointsByX() {
        Collections.sort(dataPoints, new Comparator<DataPoint>() {
            @Override
            public int compare(DataPoint dp1, DataPoint dp2) {
                return Float.compare(dp1.x, dp2.x);
            }
        });
    }

    // ... existing code for findSelectedPoint, isPointTouched, limitPointWithinAxisRange ...

    // 将数据坐标转换为屏幕坐标
    private PointF toScreenCoordinates(DataPoint dataPoint) {
        float xAxisLength = getWidth() - paddingLeft - paddingRight;
        float yAxisLength = getHeight() - paddingTop - paddingBottom;

        float screenX = paddingLeft + (dataPoint.x - xMin) / (xMax - xMin) * xAxisLength;
        float screenY = getHeight() - paddingBottom - (dataPoint.y - yMin) / (yMax - yMin) * yAxisLength;

        return new PointF(screenX, screenY);
    }

    private DataPoint findSelectedPoint(float x, float y) {
        for (DataPoint dataPoint : dataPoints) {
            PointF screenPoint = toScreenCoordinates(dataPoint);
            if (isPointTouched(screenPoint, x, y)) {
                return dataPoint;
            }
        }
        return null;
    }

    private boolean isPointTouched(PointF screenPoint, float touchX, float touchY) {
        float dx = touchX - screenPoint.x;
        float dy = touchY - screenPoint.y;
        // 使用扩大后的判断范围
        return dx * dx + dy * dy <= touchRange * touchRange;
    }

    // 限制点在坐标轴范围内
    private void limitPointWithinAxisRange(DataPoint point) {
        point.x = Math.max(xMin, Math.min(point.x, xMax));
        point.y = Math.max(yMin, Math.min(point.y, yMax));
    }

    // 将屏幕坐标转换为数据坐标
    private void convertScreenToData(float screenX, float screenY, DataPoint dataPoint) {
        float xAxisLength = getWidth() - paddingLeft - paddingRight;
        float yAxisLength = getHeight() - paddingTop - paddingBottom;

        dataPoint.x = xMin + (screenX - paddingLeft) / xAxisLength * (xMax - xMin);
        dataPoint.y = yMin + (getHeight() - paddingBottom - screenY) / yAxisLength * (yMax - yMin);
    }

    // 获取当前数据源数组
    public List<DataPoint> getDataSource() {
        return new ArrayList<>(dataPoints);
    }

    // 数据点类，包含实际数据值
    public static class DataPoint {
        //x代表FC，y代表增益
        public float x;
        public float y;

        public DataPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            //保持数据格式 0 20000
            return x + " " + y;
        }
    }


}