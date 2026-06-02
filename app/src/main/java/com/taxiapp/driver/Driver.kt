package com.taxiapp.driver

data class Driver(
    val id: String = "",
    val name: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val vehicle: String = "",
    val vehicleBrand: String = "",
    val vehicleModel: String = "",
    val licensePlate: String = "",
    val vehicleType: String = "",
    val vanType: String = "",
    val vehicleColor: String = "",
    val suspended: Boolean = false,
    val approved: Boolean = false,
    val totalEarnings: Double = 0.0,
    val totalCommissions: Double = 0.0,
    val profilePhotoUrl: String = "",
    val licenseFrontUrl: String = "",
    val licenseBackUrl: String = "",
    val ciFrontUrl: String = "",
    val ciBackUrl: String = "",
    val ruatPhotoUrl: String = "",
    val vehiclePhotoUrl: String = ""
)
