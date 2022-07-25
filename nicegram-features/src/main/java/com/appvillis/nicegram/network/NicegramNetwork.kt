package com.appvillis.nicegram.network

import android.content.Context
import com.appvillis.nicegram.BuildConfig
import com.appvillis.nicegram.NicegramConstsAndKeys.API_KEY
import com.appvillis.nicegram.NicegramConstsAndKeys.BASE_URL
import com.appvillis.nicegram.NicegramConstsAndKeys.BASE_URL_NG_APP
import com.appvillis.nicegram.NicegramScopes.ioScope
import com.appvillis.nicegram.NicegramScopes.uiScope
import com.appvillis.nicegram.R
import com.appvillis.nicegram.network.request.RegDateRequest
import com.appvillis.nicegram.network.response.RegDateResponse
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NicegramNetwork {
    private const val TIMEOUT = 30L

    private val nicegramApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(NicegramApi::class.java)
    }

    private val nicegramAppApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL_NG_APP)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(NicegramAppApi::class.java)
    }

    private val okHttpClient by lazy {
         OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().setLevel(
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
                )
            )
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    fun getRegDate(context: Context?, userId: Long, callback: (regDate: String?) -> Unit) {
        if (context == null) {
            return callback("Error code: 1002")
        }

        ioScope.launch {
            try {
                val result = nicegramApi.getRegDate(RegDateRequest((userId)), API_KEY)
                if (result.data == null) {
                    uiScope.launch { callback(null) }
                    return@launch
                }

                uiScope.launch {
                    try {
                        val prefix = when (result.data.type) {
                            RegDateResponse.RegDateType.Approximately -> {
                                context.getString(R.string.NicegramApproximately)
                            }
                            RegDateResponse.RegDateType.OlderThan -> {
                                context.getString(R.string.NicegramOlderThan)
                            }
                            RegDateResponse.RegDateType.NewerThan -> {
                                context.getString(R.string.NicegramNewerThan)
                            }
                            else -> ""
                        }
                        callback("$prefix ${result.data.date}")
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) e.printStackTrace()

                        uiScope.launch { callback(null) }
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) e.printStackTrace()

                uiScope.launch { callback(null) }
            }
        }
    }

    var chatsUnblocked = false

    fun getSettings(userId: Long) {
        ioScope.launch {
            try {
                val result = nicegramAppApi.getSettings(userId)
                chatsUnblocked = result.settings.syncChats
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) e.printStackTrace()
            }
        }
    }
}