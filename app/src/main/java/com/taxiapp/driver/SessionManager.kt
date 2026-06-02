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
        private const val KEY_USER_FIRST_NAME = "user_first_name"
        private const val KEY_USER_LAST_NAME = "user_last_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_VEHICLE = "vehicle"
        private const val KEY_VEHICLE_TYPE = "vehicle_type"
        private const val KEY_VEHICLE_BRAND = "vehicle_brand"
        private const val KEY_VEHICLE_MODEL = "vehicle_model"
        private const val KEY_VEHICLE_COLOR = "vehicle_color"
        private const val KEY_LICENSE_PLATE = "license_plate"
    }
    
    fun saveSession(userId: String, userName: String, userEmail: String, userPhone: String, vehicle: String, licensePlate: String, vehicleType: String, token: String = "") {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_TOKEN, token)
        editor.putString(KEY_USER_ID, userId)
        editor.putString(KEY_USER_NAME, userName)
        editor.putString(KEY_USER_EMAIL, userEmail)
        editor.putString(KEY_USER_PHONE, userPhone)
        editor.putString(KEY_VEHICLE, vehicle)
        editor.putString(KEY_VEHICLE_TYPE, vehicleType)
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
    
    fun getVehicleType(): String? {
        return prefs.getString(KEY_VEHICLE_TYPE, null)
    }
    
    fun saveVehicleType(vehicleType: String) {
        val editor = prefs.edit()
        editor.putString(KEY_VEHICLE_TYPE, vehicleType)
        editor.apply()
    }
    
    fun getUserFirstName(): String? {
        return prefs.getString(KEY_USER_FIRST_NAME, null)
    }
    
    fun saveUserFirstName(firstName: String) {
        val editor = prefs.edit()
        editor.putString(KEY_USER_FIRST_NAME, firstName)
        editor.apply()
    }
    
    fun getUserLastName(): String? {
        return prefs.getString(KEY_USER_LAST_NAME, null)
    }
    
    fun saveUserLastName(lastName: String) {
        val editor = prefs.edit()
        editor.putString(KEY_USER_LAST_NAME, lastName)
        editor.apply()
    }
    
    fun getVehicleBrand(): String? {
        return prefs.getString(KEY_VEHICLE_BRAND, null)
    }
    
    fun saveVehicleBrand(brand: String) {
        val editor = prefs.edit()
        editor.putString(KEY_VEHICLE_BRAND, brand)
        editor.apply()
    }
    
    fun getVehicleModel(): String? {
        return prefs.getString(KEY_VEHICLE_MODEL, null)
    }
    
    fun saveVehicleModel(model: String) {
        val editor = prefs.edit()
        editor.putString(KEY_VEHICLE_MODEL, model)
        editor.apply()
    }
    
    fun getVehicleColor(): String? {
        return prefs.getString(KEY_VEHICLE_COLOR, null)
    }
    
    fun saveVehicleColor(color: String) {
        val editor = prefs.edit()
        editor.putString(KEY_VEHICLE_COLOR, color)
        editor.apply()
    }
    
    fun saveUserName(name: String) {
        val editor = prefs.edit()
        editor.putString(KEY_USER_NAME, name)
        editor.apply()
    }
    
    fun saveUserPhone(phone: String) {
        val editor = prefs.edit()
        editor.putString(KEY_USER_PHONE, phone)
        editor.apply()
    }
    
    fun saveUserEmail(email: String) {
        val editor = prefs.edit()
        editor.putString(KEY_USER_EMAIL, email)
        editor.apply()
    }
    
    fun saveVehicle(vehicle: String) {
        val editor = prefs.edit()
        editor.putString(KEY_VEHICLE, vehicle)
        editor.apply()
    }
    
    fun saveLicensePlate(plate: String) {
        val editor = prefs.edit()
        editor.putString(KEY_LICENSE_PLATE, plate)
        editor.apply()
    }
    
    fun clearSession() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
