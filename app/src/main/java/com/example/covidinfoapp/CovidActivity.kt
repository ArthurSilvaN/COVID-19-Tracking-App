package com.example.covidinfoapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.leo.simplearcloader.ArcConfiguration
import com.leo.simplearcloader.SimpleArcDialog
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


private var tvConfirmed: TextView? = null
private var tvActives: TextView? = null
private var tvRecovered: TextView? = null
private var tvDeaths: TextView? = null
private var tvDate: TextView? = null

@Suppress("DEPRECATION")
class CovidActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_covid)

        tvConfirmed = findViewById(R.id.tvConfirmed)
        tvActives = findViewById(R.id.tvActive)
        tvRecovered = findViewById(R.id.tvRecovered)
        tvDeaths = findViewById(R.id.tvDeaths)
        tvDate = findViewById(R.id.tvDate)

        fetchData()
    }

    private fun getDate(milliSecond: Long): String? {
        // Mon, 23 Mar 2020 02:01:04 PM
        val formatter = SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss aaa")
        val calendar: Calendar = Calendar.getInstance()
        calendar.setTimeInMillis(milliSecond)
        return formatter.format(calendar.getTime())
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
                                tvConfirmed?.text = jsonObject.getString("cases")
                                tvRecovered?.text = jsonObject.getString("recovered")
                                tvActives?.text = jsonObject.getString("active")
                                tvDeaths?.text = jsonObject.getString("deaths")
                                tvDate?.text =
                                    "Last Updated:" + "   ${getDate(jsonObject.getLong("updated"))}"
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                })
        }.start()
    }
}