package com.taxiapp.driver

data class Driver(
    val id: String = "",
    val name: String = "",
    val vehicle: String = "",
    val licensePlate: String = "",
    val phone: String = "",
    val suspended: Boolean = false,
    val totalEarnings: Double = 0.0,
    val totalCommissions: Double = 0.0
)
