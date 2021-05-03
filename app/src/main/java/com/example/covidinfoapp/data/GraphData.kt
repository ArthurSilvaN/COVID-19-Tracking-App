package com.example.covidinfoapp.data

@Suppress("ArrayInDataClass")
data class GraphData (
    val cases: List<CovidData>,
    val deaths: List<CovidData>,
    val recovered: List<CovidData>
)
