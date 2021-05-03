package com.example.covidinfoapp.service

import com.example.covidinfoapp.data.CountryData
import com.example.covidinfoapp.data.CovidData
import com.example.covidinfoapp.data.GraphData
import com.example.covidinfoapp.data.TrackerData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface CovidService {
    @GET("historical/all")
    fun getWorldHistorical(): Call<GraphData>

    @GET("historical/{country}")
    fun getCountryData(@Path("country") country: String): Call<List<CovidData>>

    @GET("all")
    fun getTrackerWorld(): Call<TrackerData>

    @GET("countries")
    fun getCountriesData(): Call<List<CountryData>>
}