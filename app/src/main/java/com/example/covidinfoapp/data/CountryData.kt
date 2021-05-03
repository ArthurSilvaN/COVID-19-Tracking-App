package com.example.covidinfoapp.data

import com.google.gson.annotations.SerializedName

data class CountryData(
    @SerializedName("updated") val update: Long,
    @SerializedName("recovered") val recovered: Int,
    @SerializedName("active") val active: Int,
    @SerializedName("deaths") val deaths: Int,
    @SerializedName("cases") val cases: Int,
    @SerializedName("country") val countries: String
)
