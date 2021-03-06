package com.example.covidinfoapp.data

data class TrackerData(
    val updated: Long,
    val recovered: Int,
    val active: Int,
    val deaths: Int,
    val cases: Int
)
