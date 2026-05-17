package com.taxiapp.driver

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "taxi_driver_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_VEHICLE = "vehicle"
        private const val KEY_LICENSE_PLATE = "license_plate"
    }
    
    fun saveSession(userId: String, userName: String, userEmail: String, userPhone: String, vehicle: String, licensePlate: String, token: String = "") {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_TOKEN, token)
        editor.putString(KEY_USER_ID, userId)
        editor.putString(KEY_USER_NAME, userName)
        editor.putString(KEY_USER_EMAIL, userEmail)
        editor.putString(KEY_USER_PHONE, userPhone)
        editor.putString(KEY_VEHICLE, vehicle)
        editor.putString(KEY_LICENSE_PLATE, licensePlate)
        editor.apply()
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }
    
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    fun getUserPhone(): String? {
        return prefs.getString(KEY_USER_PHONE, null)
    }
    
    fun getVehicle(): String? {
        return prefs.getString(KEY_VEHICLE, null)
    }
    
    fun getLicensePlate(): String? {
        return prefs.getString(KEY_LICENSE_PLATE, null)
    }
    
    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
