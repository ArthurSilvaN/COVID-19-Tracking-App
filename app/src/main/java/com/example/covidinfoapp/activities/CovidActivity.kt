package com.example.covidinfoapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.covidinfoapp.R
import com.example.covidinfoapp.data.CountryData
import com.example.covidinfoapp.data.CovidData
import com.example.covidinfoapp.data.TrackerData
import com.example.covidinfoapp.graphic.*
import com.example.covidinfoapp.service.CovidService
import com.google.gson.GsonBuilder
import com.leo.simplearcloader.ArcConfiguration
import com.leo.simplearcloader.SimpleArcDialog
import com.robinhood.ticker.TickerUtils
import io.ghyeok.stickyswitch.widget.StickySwitch
import kotlinx.android.synthetic.main.activity_covid.*
import okhttp3.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class CovidActivity : AppCompatActivity() {

    companion object {
        const val TAG = "CovidActivity"
        const val BASE_URL = "https://disease.sh/v3/covid-19/"
        const val ALL = "World"
    }

    private lateinit var perCountryDailyData: Map<String, List<CountryData>>
    private lateinit var countryDailyData: List<CountryData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_covid)

        GraphicData()
        fetchData()
        imageButton.setOnClickListener {
            val url = "https://www.redcross.org/get-help/how-to-prepare-for-emergencies/types-of-emergencies/coronavirus-safety.html"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        stickySwitch()
    }

    fun fetchData() {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        covidService.getTrackerWorld().enqueue(object : Callback<TrackerData> {
            override fun onFailure(call: Call<TrackerData>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<TrackerData>, response: Response<TrackerData>) {
                val worldData = response.body()
                Log.i(TAG, "onResponse $response")
                if (worldData != null) {
                    updateTrackerWithData(worldData)
                }
            }
        })

        covidService.getCountriesData().enqueue(object : Callback<List<CountryData>> {
            override fun onResponse(
                call: Call<List<CountryData>>,
                response: Response<List<CountryData>>
            ) {
                val countriesData = response.body()
                if (countriesData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }

                perCountryDailyData = countriesData
                    .filter { true }
                    .map { // State data may have negative deltas, which don't make sense to graph
                        CountryData(
                            it.update,
                            it.recovered.coerceAtLeast(0),
                            it.active.coerceAtLeast(0),
                            it.deaths.coerceAtLeast(0),
                            it.cases.coerceAtLeast(0),
                            it.countries
                        ) }
                    .reversed()
                    .groupBy { it.countries }
                Log.i(TAG, "Update spinner with countries names")
                updateSpinnerWithCountryData(perCountryDailyData.keys)
            }

            override fun onFailure(call: Call<List<CountryData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })
    }

    @SuppressLint("SimpleDateFormat")
    private fun getDate(milliSecond: Long): String? {
        // Mon, 23 May 2021 02:01:04 PM
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss aaa")
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSecond
        return formatter.format(calendar.time)
    }

    private fun updateSpinnerWithCountryData(countryNames: Set<String>) {
        val countryAbbreviationList = countryNames.toMutableList()
        countryAbbreviationList.sort()
        countryAbbreviationList.add(0, ALL)
        spinnerSelectCountry.attachDataSource(countryAbbreviationList)
        spinnerSelectCountry.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedCountry = parent.getItemAtPosition(position) as String
            if(selectedCountry != ALL) {
                val selectedData = perCountryDailyData[selectedCountry] ?: countryDailyData
                updateCountryWithData(selectedData)
            }else{
                fetchData()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateTrackerWithData(trackerData: TrackerData){
        tvConfirmed?.text = NumberFormat.getInstance().format(trackerData.cases)
        tvRecovered?.text = NumberFormat.getInstance().format(trackerData.recovered)
        tvActive?.text = NumberFormat.getInstance().format(trackerData.active)
        tvDeaths?.text = NumberFormat.getInstance().format(trackerData.deaths)
        tvDate?.text = "Last Updated:" + "   ${getDate(trackerData.updated)}"
    }

    @SuppressLint("SetTextI18n")
    private fun updateCountryWithData(selectedData: List<CountryData>) {
        val trackerData = selectedData[0]
        tvConfirmed?.text = NumberFormat.getInstance().format(trackerData.cases)
        tvRecovered?.text = NumberFormat.getInstance().format(trackerData.recovered)
        tvActive?.text = NumberFormat.getInstance().format(trackerData.active)
        tvDeaths?.text = NumberFormat.getInstance().format(trackerData.deaths)
        tvDate?.text = "Last Updated:" + "   ${getDate(trackerData.update)}"
    }

    ////////////////////////////////////////// Graphic
    private fun GraphicData() {
        val mDialog = SimpleArcDialog(this)
        mDialog.setConfiguration(ArcConfiguration(this@CovidActivity))
        mDialog.setTitle("Loading...")
        mDialog.show()

        val urlGraph= "https://covidtracking.com/api/v1/"
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(urlGraph)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        covidService.getNationalData().enqueue(object : Callback<List<CovidData>> {
            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>) {
                mDialog.dismiss()
                Log.i(TAG, "onResponse $response")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }

                setupEventListeners()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "Update graph with national data")
                updateDisplayWithData(nationalDailyData)
            }
        })
    }
    private fun setupEventListeners() {
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData ->
            if (itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }
        tickerView.setCharacterLists(TickerUtils.provideNumberList())

        // Respond to radio button selected events
        radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            // Display the last day of the metric
            updateInfoForDate(currentlyShownData.last())
            adapter.notifyDataSetChanged()
        }
        radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        // Update color of the chart
        @ColorRes val colorRes = when (metric) {
            Metric.NEGATIVE -> R.color.colorRecovered
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        sparkView.lineColor = colorInt
        tickerView.textColor = colorInt
        adapter.metric = metric
        adapter.notifyDataSetChanged()
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        tickerView.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }

    private fun stickySwitch(){
        sticky_switch.onSelectedChangeListener = object : StickySwitch.OnSelectedChangeListener {
            override fun onSelectedChange(direction: StickySwitch.Direction, text: String) {
                if (sticky_switch.getDirection() == StickySwitch.Direction.LEFT) {
                    groupTracker.visibility = View.VISIBLE
                    groupGraphic.visibility = View.GONE
                } else {
                    groupTracker.visibility = View.GONE
                    groupGraphic.visibility = View.VISIBLE
                }
            }
        }
    }
}