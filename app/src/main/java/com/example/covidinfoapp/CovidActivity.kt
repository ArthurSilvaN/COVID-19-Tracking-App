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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


@Suppress("DEPRECATION")
class CovidActivity : AppCompatActivity() {

    companion object {
        const val TAG = "CovidActivity"
        const val BASE_URL = "https://covidtracking.com/api/v1/"
        const val ALL_STATES = "All (Nationwide)"
    }

    private lateinit var adapter: CovidSparkAdapter
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

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

    private fun ActionPerformed(bool : Boolean){
        var mDialog: SimpleArcDialog? = SimpleArcDialog(this)
        if (bool){
            mDialog?.setConfiguration(ArcConfiguration(this@CovidActivity))
            mDialog?.setTitle("Loading...")
            mDialog?.show()
        }else{
            mDialog?.dismiss()
        }
    }

    private fun fetchData() {
        val url = "https://disease.sh/v3/covid-19/all"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).get().build()
        Thread {
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        println("Request Failure.")
                    }

                    @SuppressLint("SetTextI18n")
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        val responseData = response.body?.string()
                        runOnUiThread {
                            try {
                                var jsonObject = JSONObject(responseData)
                                println("Request Successful!!")
                                tvConfirmed?.text = NumberFormat.getInstance().format(jsonObject.getInt("cases"))
                                tvRecovered?.text = NumberFormat.getInstance().format(jsonObject.getInt("recovered"))
                                tvActive?.text = NumberFormat.getInstance().format(jsonObject.getInt("active"))
                                tvDeaths?.text = NumberFormat.getInstance().format(jsonObject.getInt("deaths"))
                                tvDate?.text = "Last Updated:" + "   ${getDate(jsonObject.getLong("updated"))}"
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                })
        }.start()
    }

    private fun GraphicData(){
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        covidService.getNationalData().enqueue(object : retrofit2.Callback<List<CovidData>> {
            override fun onFailure(call: retrofit2.Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(call: retrofit2.Call<List<CovidData>>, response: Response<List<CovidData>>) {
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

        covidService.getStatesData().enqueue(object : retrofit2.Callback<List<CovidData>> {
            override fun onFailure(call: retrofit2.Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

            override fun onResponse(
                call: retrofit2.Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG, "onResponse $response")
                val statesData = response.body()
                if (statesData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }

                perStateDailyData = statesData
                    .filter { it.dateChecked != null }
                    .map { // State data may have negative deltas, which don't make sense to graph
                        CovidData(
                            it.dateChecked,
                            it.positiveIncrease.coerceAtLeast(0),
                            it.negativeIncrease.coerceAtLeast(0),
                            it.deathIncrease.coerceAtLeast(0),
                            it.state
                        ) }
                    .reversed()
                    .groupBy { it.state }
                Log.i(TAG, "Update spinner with state names")
                updateSpinnerWithStateData(perStateDailyData.keys)
            }
        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0, ALL_STATES)
        spinnerSelect.attachDataSource(stateAbbreviationList)
        spinnerSelect.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perStateDailyData[selectedState] ?: nationalDailyData
            updateDisplayWithData(selectedData)
        }
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
            Metric.NEGATIVE -> R.color.colorNegative
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
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData
        // Create a new SparkAdapter with the data
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        // Update radio buttons to select positive cases and max time by default
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
                    fetchData()
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