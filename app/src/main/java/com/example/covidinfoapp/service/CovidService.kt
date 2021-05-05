package com.example.covidinfoapp.service

import com.example.covidinfoapp.data.CountryData
import com.example.covidinfoapp.data.CovidData
import com.example.covidinfoapp.data.TrackerData
import retrofit2.Call
import retrofit2.http.GET

interface CovidService {
    /*@GET("historical/all")
    fun getWorldHistorical(): Call<GraphData>

    @GET("historical/{country}")
    fun getCountryData(@Path("country") country: String): Call<List<GraphData>>*/

    @GET("all")
    fun getTrackerWorld(): Call<TrackerData>

    @GET("countries")
    fun getCountriesData(): Call<List<CountryData>>

    @GET("us/daily.json")
    fun getNationalData(): Call<List<CovidData>>
}