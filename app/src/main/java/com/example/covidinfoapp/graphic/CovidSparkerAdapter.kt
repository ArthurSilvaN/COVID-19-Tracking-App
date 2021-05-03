package com.example.covidinfoapp.graphic

import android.graphics.RectF
import android.util.Half.toFloat
import com.example.covidinfoapp.data.GraphData
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter : SparkAdapter() {
    var daysAgo = TimeScale.MAX
    var metric = Metric.POSITIVE

    override fun getItem(index: Int) = dailyData.copy()

    override fun getCount(): Int = dailyData.cases.dateChecked.size

    override fun getY(index: Int): Float {
        val chosenDayData = dailyData
        return when (metric) {
            Metric.RECOVERED -> chosenDayData.recovered.dateChecked[]
            Metric.POSITIVE -> chosenDayData.cases.dateChecked[]
            Metric.DEATH -> chosenDayData.deaths.dateChecked.toFloat()
        }
    }

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (daysAgo != TimeScale.MAX) {
            bounds.left = count - daysAgo.numDays.toFloat()
        }
        return bounds
    }
}