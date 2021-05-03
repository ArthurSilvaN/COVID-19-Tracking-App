package com.example.covidinfoapp.graphic

import android.graphics.RectF
import com.example.covidinfoapp.data.GraphData
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: List<GraphData>) : SparkAdapter() {

    var daysAgo = TimeScale.MAX
    var metric = Metric.POSITIVE

    override fun getY(index: Int): Float {
        val chosenDayData = dailyData[index]
        return when (metric) {
            Metric.RECOVERED -> chosenDayData.recovered[index].dateChecked.cases.toFloat()
            Metric.POSITIVE -> chosenDayData.cases[index].dateChecked.cases.toFloat()
            Metric.DEATH -> chosenDayData.deaths[index].dateChecked.cases.toFloat()
        }
    }

    override fun getItem(index: Int) = dailyData[index]

    override fun getCount() = dailyData.size

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (daysAgo != TimeScale.MAX) {
            bounds.left = count - daysAgo.numDays.toFloat()
        }
        return bounds
    }
}