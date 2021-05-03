package com.example.covidinfoapp.util

import android.annotation.SuppressLint
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.covidinfoapp.activities.CovidActivity
import com.example.covidinfoapp.data.CountryData
import com.example.covidinfoapp.data.TrackerData
import com.example.covidinfoapp.service.CovidService
import kotlinx.android.synthetic.main.activity_covid.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TrackerClass : AppCompatActivity(){

    private lateinit var perCountryDailyData: Map<String, List<CountryData>>
    private lateinit var countryDailyData: List<CountryData>

    fun fetchData() {
        val retrofit = Retrofit.Builder()
            .baseUrl(CovidActivity.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        covidService.getTrackerWorld().enqueue(object : Callback<TrackerData> {
            override fun onFailure(call: Call<TrackerData>, t: Throwable) {
                Log.e(CovidActivity.TAG, "onFailure $t")
            }

            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<TrackerData>, response: Response<TrackerData>) {
                Log.i(CovidActivity.TAG, "onResponse $response")
                val worldData = response.body()
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
                    Log.w(CovidActivity.TAG, "Did not receive a valid response body")
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
                Log.i(CovidActivity.TAG, "Update spinner with countries names")
                updateSpinnerWithCountryData(perCountryDailyData.keys)
            }

            override fun onFailure(call: Call<List<CountryData>>, t: Throwable) {
                Log.e(CovidActivity.TAG, "onFailure $t")
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
        countryAbbreviationList.add(0, CovidActivity.ALL)
        spinnerSelectCountry.attachDataSource(countryAbbreviationList)
        spinnerSelectCountry.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedCountry = parent.getItemAtPosition(position) as String
            if(selectedCountry != CovidActivity.ALL) {
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
        val trackerData = selectedData.get(0)
        tvConfirmed?.text = NumberFormat.getInstance().format(trackerData.cases)
        tvRecovered?.text = NumberFormat.getInstance().format(trackerData.recovered)
        tvActive?.text = NumberFormat.getInstance().format(trackerData.active)
        tvDeaths?.text = NumberFormat.getInstance().format(trackerData.deaths)
        tvDate?.text = "Last Updated:" + "   ${getDate(trackerData.update)}"
    }
}