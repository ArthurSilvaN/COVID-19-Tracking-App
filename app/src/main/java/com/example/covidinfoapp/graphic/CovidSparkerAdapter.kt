package com.example.covidinfoapp.graphic

import android.graphics.RectF
import com.example.covidinfoapp.CountryData
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: CountryData) : SparkAdapter() {

    var daysAgo = TimeScale.MAX
    var metric = Metric.POSITIVE

    override fun getCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getItem(index: Int): Any {
        TODO("Not yet implemented")
    }

    override fun getY(index: Int): Float {
        val chosenDayData = dailyData
        return when (metric) {
            Metric.RECOVERED -> chosenDayData.recovered.toFloat()
            Metric.POSITIVE -> chosenDayData.cases.toFloat()
            Metric.DEATH -> chosenDayData.deaths.toFloat()
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