package com.example.covidinfoapp.graphic

enum class Metric {
     POSITIVE, DEATH, RECOVERED
}

enum class TimeScale(val numDays: Int) {
    WEEK(7),
    MONTH(30),
    MAX(-1)
}