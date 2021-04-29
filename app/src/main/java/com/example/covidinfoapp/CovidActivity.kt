package com.example.covidinfoapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.covidinfoapp.graphic.*
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

    private lateinit var adapter: CovidSparkAdapter
    private lateinit var currentlyShownData: CountryData

    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var perCountryDailyData: Map<String, List<CountryData>>
    private lateinit var countryDailyData: List<CountryData>
    private lateinit var wordlDailyData: CountryData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_covid)

        GraphicData()
        fetchData()
        stickySwitch()
    }

    private fun getDate(milliSecond: Long): String? {
        // Mon, 23 May 2021 02:01:04 PM
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss aaa")
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSecond
        return formatter.format(calendar.time)
    }

    private fun ActionPerformed(){
        var mDialog: SimpleArcDialog? = SimpleArcDialog(this)
        mDialog?.setConfiguration(ArcConfiguration(this@CovidActivity))
        mDialog?.setTitle("Loading...")
        mDialog?.show()
    }

    private fun fetchData() {
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
                Log.i(TAG, "onResponse $response")
                val worldData = response.body()
                if (worldData != null) {
                    updateTrackerWithData(worldData)
                }
            }
        })

        covidService.getCountriesData().enqueue(object : Callback<List<CountryData>>{
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
                    .filter { it.update != null }
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

    private fun GraphicData(){
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        covidService.getWorldHistorical().enqueue(object : Callback<CountryData> {
            override fun onFailure(call: Call<CountryData>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(call: Call<CountryData>, response: Response<CountryData>) {
                Log.i(TAG, "onResponse $response")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }

                setupEventListeners()
                wordlDailyData = nationalData
                Log.i(TAG, "Update graph with national data")
                updateDisplayWithData(wordlDailyData)
            }
        })

//        covidService.getCountryData().enqueue(object : retrofit2.Callback<List<CovidData>> {
//            override fun onFailure(call: retrofit2.Call<List<CovidData>>, t: Throwable) {
//                Log.e(TAG, "onFailure $t")
//            }
//
//            override fun onResponse(
//                call: retrofit2.Call<List<CovidData>>,
//                response: Response<List<CovidData>>
//            ) {
//                Log.i(TAG, "onResponse $response")
//                val statesData = response.body()
//                if (statesData == null) {
//                    Log.w(TAG, "Did not receive a valid response body")
//                    return
//                }
//
//                perStateDailyData = statesData
//                    .filter { it.dateChecked != null }
//                    .map {
//                        CovidData(
//                            it.dateChecked,
//                            it.positiveIncrease.coerceAtLeast(0),
//                            it.negativeIncrease.coerceAtLeast(0),
//                            it.deathIncrease.coerceAtLeast(0),
//                            it.state
//                        ) }
//                    .reversed()
//                    .groupBy { it.state }
//                Log.i(TAG, "Update spinner with state names")
//                updateSpinnerWithStateData(perStateDailyData.keys)
//            }
//        })
    }

//    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
//        val stateAbbreviationList = stateNames.toMutableList()
//        stateAbbreviationList.sort()
//        stateAbbreviationList.add(0, ALL)
//        spinnerSelect.attachDataSource(stateAbbreviationList)
//        spinnerSelect.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
//            val selectedState = parent.getItemAtPosition(position) as String
//            val selectedData = perStateDailyData[selectedState] ?: wordlDailyData
//            updateDisplayWithData(selectedData)
//        }
//    }

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
    private fun updateCountryWithData(selectedData: List<CountryData>) {
        val trackerData = selectedData.get(0)
        tvConfirmed?.text = NumberFormat.getInstance().format(trackerData.cases)
        tvRecovered?.text = NumberFormat.getInstance().format(trackerData.recovered)
        tvActive?.text = NumberFormat.getInstance().format(trackerData.active)
        tvDeaths?.text = NumberFormat.getInstance().format(trackerData.deaths)
        tvDate?.text = "Last Updated:" + "   ${getDate(trackerData.update)}"
    }

    private fun setupEventListeners() {
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData ->
            if (itemData is CountryData) {
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
            updateInfoForDate(currentlyShownData)
            adapter.notifyDataSetChanged()
        }
        radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        // Update color of the chart
        @ColorRes val colorRes = when (metric) {
            Metric.RECOVERED -> R.color.colorRecovered
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        sparkView.lineColor = colorInt
        tickerView.textColor = colorInt

        // Update metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()

        // Reset number/date shown for most recent date
        updateInfoForDate(currentlyShownData)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTrackerWithData(trackerData: TrackerData){
        tvConfirmed?.text = NumberFormat.getInstance().format(trackerData?.cases)
        tvRecovered?.text = NumberFormat.getInstance().format(trackerData?.recovered)
        tvActive?.text = NumberFormat.getInstance().format(trackerData?.active)
        tvDeaths?.text = NumberFormat.getInstance().format(trackerData?.deaths)
        tvDate?.text = "Last Updated:" + "   ${getDate(trackerData?.updated!!.toLong())}"
    }

    private fun updateDisplayWithData(dailyData: CountryData) {
        currentlyShownData = dailyData
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(countryData: CountryData) {
        val numCases = when (adapter.metric) {
            Metric.RECOVERED -> countryData.recovered
            Metric.POSITIVE -> countryData.cases
            Metric.DEATH -> countryData.deaths
        }
        tickerView.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(countryData.update)
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