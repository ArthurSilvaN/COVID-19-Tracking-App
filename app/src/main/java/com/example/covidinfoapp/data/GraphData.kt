package com.example.covidinfoapp.data

@Suppress("ArrayInDataClass")
data class GraphData (
    val cases: CovidData,
    val deaths: CovidData,
    val recovered: CovidData
)
