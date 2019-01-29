package com.arlong.stepcounter;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Long on 7/4/2016.
 */
public class ChartActivity extends Activity {

    private final static String TAG = ChartActivity.class.getSimpleName();

    private LinearLayout chartLyt;
    private LineChart mLineChart;
    Typeface mTf; // 自定义显示字体

    private Button getDataBtn;

    private List<Integer> lists = new ArrayList<Integer>();

    private void setLists() {
        lists.clear();
        for (int i = 1; i < 20; i++) {
            int value = ((int) (Math.random() * 100));
            lists.add(value);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chart);
        getDataBtn = (Button) findViewById(R.id.getData);
        getDataBtn.setOnClickListener(btnListener);
        chartLyt = (LinearLayout) findViewById(R.id.chart);

        drawTheChart();
        drawTheChartByMPAndroid();
    }


    private void drawTheChartByMPAndroid() {
        mLineChart = (LineChart) findViewById(R.id.spread_line_chart);
        LineData lineData = getLineData(36, 1000);
        showChart(mLineChart, lineData, Color.rgb(137, 230, 81));
    }

    private void showChart(LineChart lineChart, LineData lineData, int color) {
        lineChart.setDrawBorders(false); //在折线图上添加边框
        lineChart.setDescription(""); //数据描述
        lineChart.setNoDataTextDescription("You need to provide data for the chart.");

        lineChart.setDrawGridBackground(false); //表格颜色
        lineChart.setGridBackgroundColor(Color.WHITE & 0x70FFFFFF); //表格的颜色，设置一个透明度

        lineChart.setTouchEnabled(true); //可点击

        lineChart.setDragEnabled(true);  //可拖拽
        lineChart.setScaleEnabled(true);  //可缩放

        lineChart.setPinchZoom(false);

        lineChart.setBackgroundColor(color); //设置背景颜色

        lineChart.setData(lineData);  //填充数据

        Legend mLegend = lineChart.getLegend(); //设置标示，就是那个一组y的value的

        mLegend.setForm(Legend.LegendForm.CIRCLE); //样式
        mLegend.setFormSize(6f); //字体
        mLegend.setTextColor(Color.WHITE); //颜色

        lineChart.setVisibleXRange(0, 15);   //x轴可显示的坐标范围
        XAxis xAxis = lineChart.getXAxis();  //x轴的标示
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //x轴位置
        xAxis.setTextColor(Color.WHITE);    //字体的颜色
        xAxis.setTextSize(10f); //字体大小
        xAxis.setGridColor(Color.WHITE);//网格线颜色
        xAxis.setDrawGridLines(false); //不显示网格线
        xAxis.setTypeface(mTf);

        YAxis axisLeft = lineChart.getAxisLeft(); //y轴左边标示
        YAxis axisRight = lineChart.getAxisRight(); //y轴右边标示
        axisLeft.setTextColor(Color.WHITE); //字体颜色
        axisLeft.setTextSize(10f); //字体大小
        axisLeft.setAxisMaxValue(20f); //最大值
        axisLeft.setLabelCount(6, true); //显示格数
        axisLeft.setGridColor(Color.WHITE); //网格线颜色
        axisLeft.setTypeface(mTf);

        axisRight.setDrawAxisLine(false);
        axisRight.setDrawGridLines(false);
        axisRight.setDrawLabels(false);

        lineChart.animateX(1);  //立即执行动画
    }

    private LineData getLineData(int count, float range) {

        // y轴的数据
        ArrayList<Entry> yValues = new ArrayList<Entry>();
        for (int i = 0; i < 10; i++) {
            float value = i;
            yValues.add(new Entry(i,value));
        }
        for (int i = 0; i < 10; i++) {
            float value = i;
            yValues.add(new Entry(i,value+1));
        }
        // create a dataset and give it a type
        // y轴的数据集合
        LineDataSet lineDataSet = new LineDataSet(yValues, "访问量统计");
        // mLineDataSet.setFillAlpha(110);
        // mLineDataSet.setFillColor(Color.RED);

        //用y轴的集合来设置参数
        lineDataSet.setLineWidth(1.75f); // 线宽
        lineDataSet.setCircleRadius(3f);// 显示的圆形大小
        lineDataSet.setColor(Color.WHITE);// 显示颜色
        lineDataSet.setCircleColor(Color.WHITE);// 圆形的颜色
        lineDataSet.setHighLightColor(Color.WHITE); // 高亮的线的颜色
        lineDataSet.setHighlightEnabled(true);
        lineDataSet.setValueTextColor(Color.WHITE); //数值显示的颜色
        lineDataSet.setValueTextSize(8f);     //数值显示的大小
        lineDataSet.setValueTypeface(mTf);

        ArrayList<ILineDataSet> lineDataSets = new ArrayList<ILineDataSet>();
        lineDataSets.add(lineDataSet); // 添加数据集合

        //创建lineData
        LineData lineData = new LineData(lineDataSets);
        return lineData;
    }

    public void drawTheChart() {
        XYMultipleSeriesRenderer mRenderer = getXYMulSeriesRenderer();

        XYSeriesRenderer renderer = getXYSeriesRenderer();

        mRenderer.addSeriesRenderer(renderer);

        setLists();

        XYMultipleSeriesDataset dataset = getDataSet();

        GraphicalView chartView = ChartFactory.getLineChartView(this, dataset, mRenderer);

        chartLyt.addView(chartView, 0);
        //chartLyt.invalidate();
    }

    public XYSeriesRenderer getXYSeriesRenderer() {
        XYSeriesRenderer renderer = new XYSeriesRenderer();
        //设置折线宽度
        renderer.setLineWidth(2);
        //设置折线颜色
        renderer.setColor(Color.GRAY);
        renderer.setDisplayBoundingPoints(true);
        //点的样式
        renderer.setPointStyle(PointStyle.CIRCLE);
        //设置点的大小
        renderer.setPointStrokeWidth(3);
        //设置数值显示的字体大小
        renderer.setChartValuesTextSize(30);
        //显示数值
        renderer.setDisplayChartValues(true);
        return renderer;
    }

    public XYMultipleSeriesDataset getDataSet() {
        XYMultipleSeriesDataset barDataset = new XYMultipleSeriesDataset();
        CategorySeries barSeries = new CategorySeries("2016年");

        for (int i = 0; i < lists.size(); i++) {
            barSeries.add(lists.get(i));
        }

        barDataset.addSeries(barSeries.toXYSeries());
        return barDataset;
    }

    public XYMultipleSeriesRenderer getXYMulSeriesRenderer() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setMarginsColor(Color.argb(0x00, 0xF3, 0xF3, 0xF3));

        // 设置背景颜色
        renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(Color.WHITE);

        //设置Title的内容和大小
        renderer.setChartTitle("访问量统计");
        renderer.setChartTitleTextSize(50);

        //图表与四周的边距
        renderer.setMargins(new int[]{80, 80, 50, 50});

        //设置X,Y轴title的内容和大小
        renderer.setXTitle("日期");
        renderer.setYTitle("访问数");
        renderer.setAxisTitleTextSize(30);
        //renderer.setAxesColor(Color.WHITE);
        renderer.setLabelsColor(Color.BLACK);

        //图例文字的大小
        renderer.setLegendTextSize(20);

        // x、y轴上刻度颜色和大小
        renderer.setXLabelsColor(Color.BLACK);
        renderer.setYLabelsColor(0, Color.BLACK);
        renderer.setLabelsTextSize(20);
        renderer.setYLabelsPadding(30);

        // 设置X轴的最小数字和最大数字，由于我们的数据是从1开始，所以设置为0.5就可以在1之前让出一部分
        // 有兴趣的童鞋可以删除下面两行代码看一下效果
        renderer.setPanEnabled(false, false);

        //显示网格
        renderer.setShowGrid(true);

        //X,Y轴上的数字数量
        renderer.setXLabels(10);
        renderer.setYLabels(10);

        // 设置X轴的最小数字和最大数字
        renderer.setXAxisMin(1);
        renderer.setXAxisMax(20);
        // 设置Y轴的最小数字和最大数字
        renderer.setYAxisMin(0);
        renderer.setYAxisMax(100);

        // 设置渲染器显示缩放按钮
        renderer.setZoomButtonsVisible(true);
        // 设置渲染器允许放大缩小
        renderer.setZoomEnabled(true);
        // 消除锯齿
        renderer.setAntialiasing(true);

        // 刻度线与X轴坐标文字左侧对齐
        renderer.setXLabelsAlign(Paint.Align.LEFT);
        // Y轴与Y轴坐标文字左对齐
        renderer.setYLabelsAlign(Paint.Align.LEFT);

        // 允许左右拖动,但不允许上下拖动.
        renderer.setPanEnabled(true, false);

        return renderer;
    }

    private Button.OnClickListener btnListener = new Button.OnClickListener(){
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.getData:
                    drawTheChart();
                    drawTheChartByMPAndroid();
                    break;
                default:
                    break;
            }
        }
    };

}
