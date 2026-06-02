package com.taxiapp.driver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.firebase.database.FirebaseDatabase

class DriverLocationService : Service() {
    
    companion object {
        const val CHANNEL_ID = "driver_location_channel"
        const val NOTIFICATION_ID = 1001
    }
    
    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var sessionManager: SessionManager
    private var isTracking = false
    private val database = FirebaseDatabase.getInstance()
    private val activeDriversRef = database.getReference("activeDrivers")
    
    inner class LocalBinder : Binder() {
        fun getService(): DriverLocationService = this@DriverLocationService
    }
    
    override fun onCreate() {
        super.onCreate()
        firebaseHelper = FirebaseHelper()
        sessionManager = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationUpdate(location)
                }
            }
        }
        
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ubicación del Conductor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mantiene la conexión activa y actualiza la ubicación"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun getNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚕 App de Conductores")
            .setContentText("Conectado y listo para recibir viajes")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    private fun sendLocationUpdate(location: Location) {
        sessionManager.getUserId()?.let { driverId ->
            firebaseHelper.updateDriverLocation(driverId, location.latitude, location.longitude)
        }
    }
    
    fun startTracking() {
        if (isTracking) return
        
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        startForeground(NOTIFICATION_ID, getNotification())
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        
        isTracking = true
        
        sessionManager.getUserId()?.let { driverId ->
            val driverData = mutableMapOf<String, Any?>(
                "id" to driverId,
                "name" to (sessionManager.getUserName() ?: ""),
                "vehicle" to (sessionManager.getVehicle() ?: ""),
                "licensePlate" to (sessionManager.getLicensePlate() ?: ""),
                "status" to "available"
            )
            
            val driverRef = database.getReference("drivers").child(driverId)
            driverRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val isApproved = snapshot.child("approved").getValue(Boolean::class.java) ?: false
                    val isSuspended = snapshot.child("suspended").getValue(Boolean::class.java) ?: false
                    
                    if (!isApproved || isSuspended) {
                        stopTracking()
                        return
                    }
                    
                    val driverPhotoUrl = snapshot.child("driverPhotoUrl").getValue(String::class.java)
                    val vehiclePhotoUrl = snapshot.child("vehiclePhotoUrl").getValue(String::class.java)
                    val phone = snapshot.child("phone").getValue(String::class.java)
                    val vehicleType = snapshot.child("vehicleType").getValue(String::class.java)
                    
                    driverData["driverPhotoUrl"] = driverPhotoUrl
                    driverData["vehiclePhotoUrl"] = vehiclePhotoUrl
                    driverData["phone"] = phone
                    driverData["vehicleType"] = vehicleType
                    
                    sessionManager.getUserId()?.let { id ->
                        firebaseHelper.goOnline(id, driverData)
                    }
                }
                
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        }
    }
    
    fun stopTracking() {
        if (!isTracking) return
        
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        
        sessionManager.getUserId()?.let { driverId ->
            firebaseHelper.goOffline(driverId)
        }
        
        isTracking = false
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }
}
