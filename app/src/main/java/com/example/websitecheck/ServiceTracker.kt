package com.example.websitecheck

import android.content.Context
import android.content.SharedPreferences

enum class ServiceState {
    STARTED,
    STOPPED,
}

private const val NAME = "SERVICE_KEY"
private const val KEY = "SERVICE_STATE"

fun setServiceState(context: Context, state: ServiceState) {
    val sharedPrefs = getPreferences(context)
    sharedPrefs.edit().let {
        it.putString(KEY, state.name)
        it.apply()
    }
}

fun getServiceState(context: Context): ServiceState {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs.getString(KEY, ServiceState.STOPPED.name)
    return ServiceState.valueOf(value.toString())
}

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(NAME, 0)
}