package com.example.covidinfoapp.graphic

import com.example.covidinfoapp.CountryData
import com.example.covidinfoapp.TrackerData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface CovidService {
    @GET("historical/all")
    fun getWorldHistorical(): Call<CountryData>

    @GET("historical/{country}")
    fun getCountryData(@Path("country") country: String): Call<List<CovidData>>

    @GET("all")
    fun getTrackerWorld(): Call<TrackerData>

    @GET("countries")
    fun getCountriesData(): Call<List<CountryData>>
}