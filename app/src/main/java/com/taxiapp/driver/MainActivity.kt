package com.taxiapp.driver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    
    companion object {
        private const val CHANNEL_ID = "driver_notifications"
        private const val NOTIFICATION_ID = 1001
    }
    
    private lateinit var firebaseHelper: FirebaseHelper
    private var locationService: DriverLocationService? = null
    private var isServiceBound = false
    private var isOnline = false
    private var googleMap: GoogleMap? = null
    private var driverMarker: Marker? = null
    private var pickupMarker: Marker? = null
    private var destMarker: Marker? = null
    private var passengerMarker: Marker? = null
    private var routeToPickup: Polyline? = null
    private var routeToDest: Polyline? = null
    private var currentRide: JSONObject? = null
    private var activeRide: JSONObject? = null
    private var lastKnownLocation: Location? = null
    private lateinit var sessionManager: SessionManager
    private var lastMarkerLocation: LatLng? = null
    private var todayRides = 0
    private var todayEarnings = 0.0
    private val okHttpClient = OkHttpClient()
    private val database = FirebaseDatabase.getInstance()
    private var notificationChildListener: ChildEventListener? = null
    
    private lateinit var statusTextView: TextView
    private lateinit var toggleButton: Button
    private lateinit var menuButton: ImageButton
    private lateinit var rideRequestLayout: LinearLayout
    private lateinit var txtPassengerName: TextView
    private lateinit var pickupTextView: TextView
    private lateinit var destinationTextView: TextView
    private lateinit var fareTextView: TextView
    private lateinit var acceptRideButton: Button
    private lateinit var rejectRideButton: Button
    private lateinit var activeRideLayout: LinearLayout
    private lateinit var txtRideDetails: TextView
    private lateinit var activeRidePassengerName: TextView
    private lateinit var activeRidePickup: TextView
    private lateinit var activeRideDestination: TextView
    private lateinit var activeRideFare: TextView
    private lateinit var activeRideDetails: TextView
    private lateinit var btnNavigate: ImageButton
    private lateinit var arrivedButton: Button
    private lateinit var startRideButton: Button
    private lateinit var completeRideButton: Button
    private lateinit var cancelRideButton: Button
    private lateinit var callPassengerButton: Button
    private lateinit var whatsappButton: Button
    private lateinit var todayRidesTextView: TextView
    private lateinit var todayEarningsTextView: TextView
    private var centerMapTimer: CountDownTimer? = null
    private var currentPassengerPhone: String? = null
    private var isNavigatingToPickup = true
    private var currentPickupLat: Double? = null
    private var currentPickupLng: Double? = null
    private var currentDestLat: Double? = null
    private var currentDestLng: Double? = null
    private var rideState = "idle"
    private var countDownTimer: CountDownTimer? = null
    private lateinit var txtCountdown: TextView
    private lateinit var progressCountdown: android.widget.ProgressBar
    
    private fun getCarBitmapDescriptor(): BitmapDescriptor? {
        val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_car)
        if (drawable == null) return null
        val bitmap: Bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas: Canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        val scaledBitmap: Bitmap = Bitmap.createScaledBitmap(bitmap, 80, 80, false)
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
    }
    
    private fun calculateRotation(from: LatLng, to: LatLng): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lng1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude)
        val lng2 = Math.toRadians(to.longitude)
        
        val dLng = lng2 - lng1
        val y = Math.sin(dLng) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)
        var bearing = Math.toDegrees(Math.atan2(y, x))
        bearing = (bearing + 360) % 360
        return bearing.toFloat()
    }
    
    private fun updatePassengerMarker(latLng: LatLng) {
        if (passengerMarker == null) {
            passengerMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Pasajero")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
            )
        } else {
            passengerMarker?.position = latLng
        }
    }

    private fun clearRoutes() {
        routeToPickup?.remove()
        routeToDest?.remove()
        routeToPickup = null
        routeToDest = null
    }

    private fun drawRouteToPickup(startLatLng: LatLng, endLatLng: LatLng) {
        val origin = "${startLatLng.latitude},${startLatLng.longitude}"
        val destination = "${endLatLng.latitude},${endLatLng.longitude}"
        val apiKey = "AIzaSyAO8drN6VO4LS1DtjA11hbiGVp-Sg5-PZI"
        
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$origin" +
                "&destination=$destination" +
                "&mode=driving" +
                "&key=$apiKey"
        
        val request = Request.Builder().url(url).build()
        
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                responseData?.let {
                    try {
                        val json = JSONObject(it)
                        val status = json.optString("status", "")
                        
                        if (status == "OK") {
                            val routes = json.getJSONArray("routes")
                            if (routes.length() > 0) {
                                val route = routes.getJSONObject(0)
                                val overviewPolyline = route.getJSONObject("overview_polyline")
                                val points = overviewPolyline.getString("points")
                                
                                val decodedPath = decodePoly(points)
                                
                                runOnUiThread {
                                    clearRoutes()
                                    val polylineOptions = PolylineOptions()
                                        .addAll(decodedPath)
                                        .width(12f)
                                        .color(0xFF2196F3.toInt()) // Color AZUL
                                        .geodesic(true)
                                    routeToPickup = googleMap?.addPolyline(polylineOptions)
                                    
                                    val boundsBuilder = LatLngBounds.Builder()
                                    boundsBuilder.include(startLatLng)
                                    boundsBuilder.include(endLatLng)
                                    val bounds = boundsBuilder.build()
                                    val padding = 100
                                    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                                    googleMap?.animateCamera(cameraUpdate)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun drawRouteToDest(startLatLng: LatLng, endLatLng: LatLng) {
        val origin = "${startLatLng.latitude},${startLatLng.longitude}"
        val destination = "${endLatLng.latitude},${endLatLng.longitude}"
        val apiKey = "AIzaSyAO8drN6VO4LS1DtjA11hbiGVp-Sg5-PZI"
        
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$origin" +
                "&destination=$destination" +
                "&mode=driving" +
                "&key=$apiKey"
        
        val request = Request.Builder().url(url).build()
        
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                responseData?.let {
                    try {
                        val json = JSONObject(it)
                        val status = json.optString("status", "")
                        
                        if (status == "OK") {
                            val routes = json.getJSONArray("routes")
                            if (routes.length() > 0) {
                                val route = routes.getJSONObject(0)
                                val overviewPolyline = route.getJSONObject("overview_polyline")
                                val points = overviewPolyline.getString("points")
                                
                                val decodedPath = decodePoly(points)
                                
                                runOnUiThread {
                                    clearRoutes()
                                    val polylineOptions = PolylineOptions()
                                        .addAll(decodedPath)
                                        .width(12f)
                                        .color(0xFFFF9800.toInt()) // Color NARANJA
                                        .geodesic(true)
                                    routeToDest = googleMap?.addPolyline(polylineOptions)
                                    
                                    val boundsBuilder = LatLngBounds.Builder()
                                    boundsBuilder.include(startLatLng)
                                    boundsBuilder.include(endLatLng)
                                    val bounds = boundsBuilder.build()
                                    val padding = 100
                                    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                                    googleMap?.animateCamera(cameraUpdate)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index].code - 63
                index++
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dLat
            
            shift = 0
            result = 0
            do {
                b = encoded[index].code - 63
                index++
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dLng
            
            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as DriverLocationService.LocalBinder
            locationService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            locationService = null
            isServiceBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        sessionManager = SessionManager(this)
        firebaseHelper = FirebaseHelper()
        
        if (!sessionManager.isLoggedIn()) {
            goToLoginActivity()
            return
        }
        
        try {
            statusTextView = findViewById<TextView>(R.id.statusTextView)
            toggleButton = findViewById<Button>(R.id.toggleButton)
            menuButton = findViewById<ImageButton>(R.id.menuButton)
            rideRequestLayout = findViewById<LinearLayout>(R.id.rideRequestLayout)
            txtPassengerName = findViewById<TextView>(R.id.txtPassengerName)
            pickupTextView = findViewById<TextView>(R.id.pickupTextView)
            destinationTextView = findViewById<TextView>(R.id.destinationTextView)
            fareTextView = findViewById<TextView>(R.id.fareTextView)
            txtRideDetails = findViewById<TextView>(R.id.txtRideDetails)
            txtCountdown = findViewById<TextView>(R.id.txtCountdown)
            progressCountdown = findViewById<ProgressBar>(R.id.progressCountdown)
            acceptRideButton = findViewById<Button>(R.id.acceptRideButton)
            rejectRideButton = findViewById<Button>(R.id.rejectRideButton)
            activeRideLayout = findViewById<LinearLayout>(R.id.activeRideLayout)
            activeRidePassengerName = findViewById<TextView>(R.id.activeRidePassengerName)
            activeRidePickup = findViewById<TextView>(R.id.activeRidePickup)
            activeRideDestination = findViewById<TextView>(R.id.activeRideDestination)
            activeRideFare = findViewById<TextView>(R.id.activeRideFare)
            activeRideDetails = findViewById<TextView>(R.id.activeRideDetails)
            btnNavigate = findViewById<ImageButton>(R.id.btnNavigate)
            arrivedButton = findViewById<Button>(R.id.arrivedButton)
            startRideButton = findViewById<Button>(R.id.startRideButton)
            completeRideButton = findViewById<Button>(R.id.completeRideButton)
            cancelRideButton = findViewById<Button>(R.id.cancelRideButton)
            todayRidesTextView = findViewById<TextView>(R.id.todayRidesTextView)
            todayEarningsTextView = findViewById<TextView>(R.id.todayEarningsTextView)
            callPassengerButton = findViewById<Button>(R.id.callPassengerButton)
            whatsappButton = findViewById<Button>(R.id.whatsappButton)
            
            setupClickListeners()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            return
        }
        
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        setupFirebaseListeners()
        createNotificationChannel()
        setupNotificationListener()
        
        val intent = Intent(this, DriverLocationService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        checkForAppUpdate()
    }
    
    private fun checkForAppUpdate() {
        firebaseHelper.getAppVersion { versionData ->
            versionData?.let { data ->
                val latestVersion = data["version"] as? String ?: ""
                val downloadUrl = data["downloadUrl"] as? String ?: ""
                val releaseNotes = data["releaseNotes"] as? String ?: ""
                val forceUpdate = data["forceUpdate"] as? Boolean ?: false
                
                if (latestVersion.isNotEmpty() && downloadUrl.isNotEmpty()) {
                    try {
                        val packageInfo = packageManager.getPackageInfo(packageName, 0)
                        val currentVersion = packageInfo.versionName
                        
                        if (currentVersion != latestVersion) {
                            runOnUiThread {
                                showUpdateDialog(latestVersion, downloadUrl, releaseNotes, forceUpdate)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    private fun showUpdateDialog(version: String, downloadUrl: String, releaseNotes: String, forceUpdate: Boolean) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Actualización Disponible! 📱")
            .setMessage("Versión: $version\n\nNotas:\n$releaseNotes")
            .setPositiveButton("Descargar") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                }
            }
        
        if (!forceUpdate) {
            dialog.setNegativeButton("Después", null)
        }
        
        dialog.setCancelable(!forceUpdate)
        dialog.show()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones de Conductores"
            val descriptionText = "Notificaciones importantes para conductores"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showSystemNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun setupNotificationListener() {
        val driverId = sessionManager.getUserId() ?: return
        val notificationsRef = database.getReference("drivers").child(driverId).child("notifications")
        
        notificationChildListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val notificationData = snapshot.value as? Map<String, Any> ?: return
                
                val read = notificationData["read"] as? Boolean ?: false
                if (!read) {
                    val title = notificationData["title"] as? String ?: "Notificación"
                    val message = notificationData["message"] as? String ?: ""
                    
                    // Mostrar notificación del sistema
                    showSystemNotification(title, message)
                    
                    // Marcar como leída
                    snapshot.ref.child("read").setValue(true)
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        
        notificationsRef.addChildEventListener(notificationChildListener!!)
    }
    
    private fun setupClickListeners() {
        toggleButton.setOnClickListener {
            if (isOnline) {
                goOffline()
            } else {
                goOnline()
            }
        }
        
        menuButton.setOnClickListener {
            showMenuDialog()
        }
        
        btnNavigate.setOnClickListener {
            openGoogleMapsNavigation()
        }
        
        acceptRideButton.setOnClickListener {
            countDownTimer?.cancel()
            currentRide?.let { ride ->
                currentShownRideId = null
                currentActiveRideId = ride.optString("id")
                val driverData = mapOf(
                    "id" to sessionManager.getUserId(),
                    "name" to sessionManager.getUserName(),
                    "vehicle" to sessionManager.getVehicle(),
                    "licensePlate" to sessionManager.getLicensePlate(),
                    "phone" to sessionManager.getUserPhone()
                )
                
                val pickupLocationMap = ride.optJSONObject("pickupLocation")
                val destLocationMap = ride.optJSONObject("destLocation")
                
                if (pickupLocationMap != null) {
                    currentPickupLat = pickupLocationMap.optDouble("lat")
                    currentPickupLng = pickupLocationMap.optDouble("lng")
                }
                
                if (destLocationMap != null) {
                    currentDestLat = destLocationMap.optDouble("lat")
                    currentDestLng = destLocationMap.optDouble("lng")
                }
                
                isNavigatingToPickup = true
                
                firebaseHelper.acceptRide(ride.optString("id"), sessionManager.getUserId() ?: "", driverData)
                
                rideRequestLayout.visibility = LinearLayout.GONE
                rideState = "accepted"
                
                currentPassengerPhone = ride.optString("passengerPhone", "")
                
                activeRide = ride
                val passengerName = ride.optString("passengerName", "Pasajero")
                activeRidePassengerName.text = "Pasajero: $passengerName"
                activeRidePickup.text = "Recogida: ${ride.optString("pickup")}"
                activeRideDestination.text = "Destino: ${ride.optString("destination")}"
                val fareValue = ride.optString("fare").toDoubleOrNull() ?: 0.0
                activeRideFare.text = "Tarifa: Bs ${fareValue.toInt()}"
                val details = ride.optString("rideDetails", "")
                if (details.isNotEmpty()) {
                    activeRideDetails.text = "Detalles: $details"
                    activeRideDetails.visibility = View.VISIBLE
                } else {
                    activeRideDetails.visibility = View.GONE
                }
                activeRideLayout.visibility = View.VISIBLE
                btnNavigate.visibility = View.VISIBLE
                arrivedButton.visibility = View.VISIBLE
                startRideButton.visibility = View.GONE
                completeRideButton.visibility = View.GONE
                callPassengerButton.visibility = View.VISIBLE
                whatsappButton.visibility = View.VISIBLE
                
                // Dibujar ruta AZUL desde ubicación actual hasta el pasajero
                val myLocation = lastKnownLocation
                if (myLocation != null && currentPickupLat != null && currentPickupLng != null) {
                    val startLatLng = LatLng(myLocation.latitude, myLocation.longitude)
                    val endLatLng = LatLng(currentPickupLat!!, currentPickupLng!!)
                    drawRouteToPickup(startLatLng, endLatLng)
                    
                    // También añadimos marcadores
                    pickupMarker = googleMap?.addMarker(
                        MarkerOptions()
                            .position(endLatLng)
                            .title("Recogida")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                }
                
                Toast.makeText(this, "Viaje aceptado!", Toast.LENGTH_SHORT).show()
                
                // Listen to passenger location updates
                val rideId = ride.optString("id")
                firebaseHelper.listenToPassengerLocation(rideId, object : FirebaseHelper.PassengerLocationListener {
                    override fun onPassengerLocationUpdate(lat: Double, lng: Double) {
                        runOnUiThread {
                            updatePassengerMarker(LatLng(lat, lng))
                        }
                    }
                    
                    override fun onPassengerSharingLocationChanged(isSharing: Boolean) {
                        runOnUiThread {
                            if (isSharing) {
                                Toast.makeText(this@MainActivity, "¡Pasajero está compartiendo ubicación en tiempo real! 📍", Toast.LENGTH_LONG).show()
                                showSystemNotification("Ubicación del pasajero", "Ahora puedes ver la ubicación en tiempo real del pasajero")
                            } else {
                                passengerMarker?.remove()
                                passengerMarker = null
                            }
                        }
                    }
                })
            }
        }
        
        rejectRideButton.setOnClickListener {
            countDownTimer?.cancel()
            currentRide?.let { ride ->
                currentShownRideId = null
                val rideId = ride.optString("id")
                val nearbyDrivers = ride.optJSONArray("nearbyDrivers")
                val currentIndex = ride.optInt("currentDriverIndex", 0)
                
                if (nearbyDrivers != null && nearbyDrivers.length() > currentIndex + 1) {
                    updateRideCurrentDriverIndex(rideId, currentIndex + 1)
                }
            }
            rideRequestLayout.visibility = View.GONE
            currentRide = null
        }
        
        arrivedButton.setOnClickListener {
            activeRide?.let { ride ->
                firebaseHelper.driverArrived(ride.optString("id"))
                
                isNavigatingToPickup = false
                rideState = "arrived"
                arrivedButton.visibility = View.GONE
                startRideButton.visibility = View.VISIBLE
                completeRideButton.visibility = View.GONE
                
                Toast.makeText(this, "Has llegado! Espera al pasajero.", Toast.LENGTH_SHORT).show()
            }
        }
        
        startRideButton.setOnClickListener {
            activeRide?.let { ride ->
                firebaseHelper.startRide(ride.optString("id"))
                
                rideState = "in_progress"
                startRideButton.visibility = View.GONE
                completeRideButton.visibility = View.VISIBLE
                completeRideButton.isEnabled = true
                
                // Dibujar ruta NARANJA desde la recogida hasta el destino
                val myLocation = lastKnownLocation
                if (currentPickupLat != null && currentPickupLng != null && 
                    currentDestLat != null && currentDestLng != null) {
                    val startLatLng = if (myLocation != null) {
                        LatLng(myLocation.latitude, myLocation.longitude)
                    } else {
                        LatLng(currentPickupLat!!, currentPickupLng!!)
                    }
                    val endLatLng = LatLng(currentDestLat!!, currentDestLng!!)
                    drawRouteToDest(startLatLng, endLatLng)
                    
                    // Actualizar marcadores
                    destMarker = googleMap?.addMarker(
                        MarkerOptions()
                            .position(endLatLng)
                            .title("Destino")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                }
                
                Toast.makeText(this, "Viaje iniciado!", Toast.LENGTH_SHORT).show()
            }
        }
        
        completeRideButton.setOnClickListener {
            activeRide?.let { ride ->
                currentShownRideId = null
                currentActiveRideId = null
                val rideId = ride.optString("id")
                val driverId = sessionManager.getUserId() ?: ""
                val passengerId = ride.optString("passengerId", "")
                val fareValue = ride.optString("fare").toDoubleOrNull() ?: 0.0
                val commission = fareValue * 0.10
                val netEarnings = fareValue - commission
                
                val completedRideData = mapOf(
                    "id" to ride.optString("id"),
                    "driverId" to driverId,
                    "passengerId" to passengerId,
                    "pickup" to ride.optString("pickup"),
                    "destination" to ride.optString("destination"),
                    "fare" to fareValue,
                    "commission" to commission,
                    "netEarnings" to netEarnings,
                    "distanceKm" to ride.optString("distanceKm"),
                    "status" to "completed",
                    "completedAt" to System.currentTimeMillis(),
                    "driverName" to sessionManager.getUserName(),
                    "passengerName" to ride.optString("passengerName")
                )
                
                firebaseHelper.completeRide(rideId, completedRideData)
                
                todayRides++
                todayEarnings += netEarnings
                updateStatsUI()
                
                rideState = "idle"
                activeRideLayout.visibility = View.GONE
                activeRide = null
                callPassengerButton.visibility = View.GONE
                whatsappButton.visibility = View.GONE
                
                // Clean up markers
                passengerMarker?.remove()
                passengerMarker = null
                pickupMarker?.remove()
                pickupMarker = null
                destMarker?.remove()
                destMarker = null
                clearRoutes()
                
                val intent = Intent(this, RatePassengerActivity::class.java)
                intent.putExtra("rideId", rideId)
                intent.putExtra("passengerId", passengerId)
                startActivity(intent)
                
                Toast.makeText(this, "Viaje completado!", Toast.LENGTH_LONG).show()
            }
        }
        
        callPassengerButton.setOnClickListener {
            currentPassengerPhone?.let { phone ->
                openPhoneCall(phone)
            }
        }
        
        whatsappButton.setOnClickListener {
            currentPassengerPhone?.let { phone ->
                openWhatsAppMessage(phone)
            }
        }

        cancelRideButton.setOnClickListener {
            activeRide?.let { ride ->
                AlertDialog.Builder(this)
                    .setTitle("Cancelar Viaje")
                    .setMessage("¿Estás seguro de que quieres cancelar el viaje?")
                    .setPositiveButton("Sí") { _, _ ->
                        firebaseHelper.cancelRide(ride.optString("id"))
                        currentShownRideId = null
                        currentActiveRideId = null
                        rideState = "idle"
                        activeRideLayout.visibility = View.GONE
                        activeRide = null
                        callPassengerButton.visibility = View.GONE
                        whatsappButton.visibility = View.GONE
                        
                        // Clean up markers
                        passengerMarker?.remove()
                        passengerMarker = null
                        pickupMarker?.remove()
                        pickupMarker = null
                        destMarker?.remove()
                        destMarker = null
                        clearRoutes()
                        
                        Toast.makeText(this, "Viaje cancelado!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
    }
    
    private fun openPhoneCall(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = android.net.Uri.parse("tel:591$phone")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir la llamada", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openWhatsAppCall(phone: String) {
        try {
            val driverName = sessionManager.getUserName() ?: "Conductor"
            val message = "Hola, soy $driverName, estoy en camino, espera verdad."
            val encodedMessage = android.net.Uri.encode(message)
            val uri = android.net.Uri.parse("https://wa.me/591$phone?text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openWhatsAppMessage(phone: String) {
        try {
            val driverName = sessionManager.getUserName() ?: "Conductor"
            val message = "Hola, soy $driverName, estoy en camino, espera verdad."
            val encodedMessage = android.net.Uri.encode(message)
            val uri = android.net.Uri.parse("https://wa.me/591$phone?text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openGoogleMapsNavigation() {
        val lat: Double
        val lng: Double
        
        if (isNavigatingToPickup) {
            lat = currentPickupLat ?: return
            lng = currentPickupLng ?: return
        } else {
            lat = currentDestLat ?: return
            lng = currentDestLng ?: return
        }
        
        try {
            val uri = android.net.Uri.parse("google.navigation:q=$lat,$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        } catch (e: Exception) {
            val uri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }
    
    private fun playNotificationSound() {
        try {
            val notification = Uri.parse("android.resource://" + packageName + "/" + R.raw.taxisms)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            
            r.play()
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    r.stop()
                    r.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 1500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun updateRideCurrentDriverIndex(rideId: String, currentIndex: Int) {
        val rideRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("activeRides").child(rideId)
        rideRef.updateChildren(mapOf("currentDriverIndex" to currentIndex))
    }
    
    private var currentShownRideId: String? = null
    private var currentActiveRideId: String? = null
    
    private fun setupFirebaseListeners() {
        val currentDriverId = sessionManager.getUserId() ?: return
        val driverVehicleType = sessionManager.getVehicleType() ?: "Auto"

        firebaseHelper.listenToNewRideRequests(object : FirebaseHelper.NewRideRequestListener {
            override fun onNewRideRequest(ride: JSONObject) {
                runOnUiThread {
                    val rideId = ride.optString("id")
                    val status = ride.optString("status", "")

                    if (status != "pending") return@runOnUiThread
                    if (activeRide != null || rideState != "idle") return@runOnUiThread
                    if (rideId == currentShownRideId) return@runOnUiThread

                    val rideVehicleType = ride.optString("vehicleType", "auto")

                    val driverVehicleTypeLower = driverVehicleType.lowercase()
                    val rideVehicleTypeLower = rideVehicleType.lowercase()

                    val matches = when (rideVehicleTypeLower) {
                        "auto" -> driverVehicleTypeLower == "auto" || driverVehicleTypeLower == "vagoneta"
                        "vagoneta" -> driverVehicleTypeLower == "vagoneta"
                        "moto" -> driverVehicleTypeLower == "moto"
                        else -> true
                    }

                    if (!matches) return@runOnUiThread

                    val createdAt = ride.optLong("createdAt", 0)
                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - createdAt

                    if (timeDiff > 5 * 60 * 1000) return@runOnUiThread

                    val nearbyDrivers = ride.optJSONArray("nearbyDrivers")
                    val currentIndex = ride.optInt("currentDriverIndex", 0)

                    if (nearbyDrivers != null && nearbyDrivers.length() > currentIndex) {
                        val assignedDriverId = nearbyDrivers.getJSONObject(currentIndex).optString("id")

                        if (assignedDriverId == currentDriverId) {
                            currentShownRideId = rideId
                            currentRide = ride
                            val passengerName = ride.optString("passengerName", "Pasajero")
                            txtPassengerName.text = "Pasajero: $passengerName"
                            currentPassengerPhone = ride.optString("passengerPhone", "")
                            pickupTextView.text = "Recogida: ${ride.optString("pickup")}"
                            destinationTextView.text = "Destino: ${ride.optString("destination")}"
                            val fareValue = ride.optString("fare").toDoubleOrNull() ?: 0.0
                            fareTextView.text = "Tarifa: Bs ${fareValue.toInt()}"
                            val details = ride.optString("rideDetails", "")
                            if (details.isNotEmpty()) {
                                txtRideDetails.text = "Detalles: $details"
                                txtRideDetails.visibility = View.VISIBLE
                            } else {
                                txtRideDetails.visibility = View.GONE
                            }
                            rideRequestLayout.visibility = View.VISIBLE
                            Toast.makeText(this@MainActivity, "Nuevo pedido de viaje!", Toast.LENGTH_LONG).show()
                            playNotificationSound()

                            progressCountdown.progress = 40
                            countDownTimer?.cancel()
                            countDownTimer = object : CountDownTimer(40000, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    val secondsRemaining = (millisUntilFinished / 1000).toInt()
                                    txtCountdown.text = "Tiempo restante: ${secondsRemaining}s"
                                    progressCountdown.progress = secondsRemaining
                                }

                                override fun onFinish() {
                                    txtCountdown.text = "Tiempo agotado!"
                                    progressCountdown.progress = 0
                                    currentRide?.let { r ->
                                        val nearbyDrivers = r.optJSONArray("nearbyDrivers")
                                        val currentIndex = r.optInt("currentDriverIndex", 0)
                                        if (nearbyDrivers != null && nearbyDrivers.length() > currentIndex + 1) {
                                            updateRideCurrentDriverIndex(r.optString("id"), currentIndex + 1)
                                        }
                                    }
                                    rideRequestLayout.visibility = View.GONE
                                    currentRide = null
                                    currentShownRideId = null
                                }
                            }.start()
                        }
                    }
                }
            }
        })

        listenToActiveRideUpdates()
    }
    
    private fun listenToActiveRideUpdates() {
        val database = FirebaseDatabase.getInstance()
        val ridesRef = database.getReference("activeRides")
        
        ridesRef.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                handleRideChange(snapshot)
            }
            
            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                handleRideChange(snapshot)
            }
            
            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            
            private fun handleRideChange(snapshot: com.google.firebase.database.DataSnapshot) {
                runOnUiThread {
                    val currentDriverId = sessionManager.getUserId() ?: return@runOnUiThread
                    val rideMap = snapshot.value as? Map<String, Any> ?: return@runOnUiThread
                    val ride = JSONObject(rideMap)
                    val rideId = ride.optString("id", snapshot.key ?: "")
                    val status = ride.optString("status", "")
                    val rideDriverId = ride.optString("driverId", "")
                    
                    if (currentActiveRideId != null && currentActiveRideId == rideId && rideDriverId == currentDriverId) {
                        if (status == "cancelled") {
                            val cancelledBy = ride.optString("cancelledBy", "passenger")
                            val message = if (cancelledBy == "passenger") {
                                "¡El pasajero canceló el pedido! 😔"
                            } else {
                                "¡Viaje cancelado por el conductor! 😔"
                            }
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                            playNotificationSound()
                            
                            rideState = "idle"
                            activeRideLayout.visibility = View.GONE
                            rideRequestLayout.visibility = View.GONE
                            activeRide = null
                            currentRide = null
                            currentShownRideId = null
                            currentActiveRideId = null
                            callPassengerButton.visibility = View.GONE
                            whatsappButton.visibility = View.GONE
                        }
                    }
                }
            }
        })
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
        }
    }
    
    private fun goOnline() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
            return
        }
        
        val driverId = sessionManager.getUserId() ?: ""
        val driverRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("drivers").child(driverId)
        driverRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val isSuspended = snapshot.child("suspended").getValue(Boolean::class.java) ?: false
                val isApproved = snapshot.child("approved").getValue(Boolean::class.java) ?: false
                if (isSuspended) {
                    Toast.makeText(this@MainActivity, "Tu cuenta está suspendida. Contacta al administrador.", Toast.LENGTH_LONG).show()
                    return
                }
                if (!isApproved) {
                    Toast.makeText(this@MainActivity, "Tu cuenta está pendiente de aprobación. Contacta al administrador.", Toast.LENGTH_LONG).show()
                    return
                }
                
                if (isServiceBound && locationService != null) {
                    locationService?.startTracking()
                    isOnline = true
                    updateUI()
                    
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            val finalLocation = location ?: run {
                                val defaultLocation = Location("default")
                                defaultLocation.latitude = -17.7833
                                defaultLocation.longitude = -63.1821
                                Toast.makeText(this@MainActivity, "Usando ubicación predeterminada (Santa Cruz)", Toast.LENGTH_SHORT).show()
                                defaultLocation
                            }
                            lastKnownLocation = finalLocation
                            updateMapLocation(finalLocation)
                        }
                    }
                    
                    Toast.makeText(this@MainActivity, "¡Conectado exitosamente!", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }
    
    private fun goOffline() {
        if (isServiceBound && locationService != null) {
            locationService?.stopTracking()
        }
        
        isOnline = false
        rideState = "idle"
        updateUI()
        driverMarker?.remove()
        rideRequestLayout.visibility = View.GONE
        activeRideLayout.visibility = View.GONE
        currentRide = null
        activeRide = null
    }
    
    private fun updateUI() {
        if (isOnline) {
            statusTextView.text = "Disponible"
            statusTextView.setBackgroundColor(0xFFd1fae5.toInt())
            statusTextView.setTextColor(0xFF059669.toInt())
            toggleButton.text = "Desconectar"
            toggleButton.setBackgroundColor(0xFFef4444.toInt())
        } else {
            statusTextView.text = "Desconectado"
            statusTextView.setBackgroundColor(0xFFf3f4f6.toInt())
            statusTextView.setTextColor(0xFF6b7280.toInt())
            toggleButton.text = "Conectar"
            toggleButton.setBackgroundColor(0xFF10b981.toInt())
        }
    }
    
    private fun updateStatsUI() {
        todayRidesTextView.text = todayRides.toString()
        todayEarningsTextView.text = "Bs ${String.format("%.2f", todayEarnings)}"
    }
    
    private fun updateMapLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        val carIcon = getCarBitmapDescriptor()
        
        if (driverMarker == null) {
            driverMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Mi ubicación")
                    .icon(carIcon)
                    .anchor(0.5f, 0.5f)
            )
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            startCenterMapTimer()
        } else {
            val lastLoc = lastMarkerLocation
            if (lastLoc != null) {
                val rotation = calculateRotation(lastLoc, latLng)
                driverMarker?.rotation = rotation
            }
            driverMarker?.position = latLng
        }
        
        lastMarkerLocation = latLng
    }
    
    private fun startCenterMapTimer() {
        centerMapTimer?.cancel()
        centerMapTimer = object : CountDownTimer(10000, 10000) {
            override fun onTick(millisUntilFinished: Long) {}
            
            override fun onFinish() {
                lastMarkerLocation?.let { latLng ->
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f), 500, null)
                }
                startCenterMapTimer()
            }
        }.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        centerMapTimer?.cancel()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        notificationChildListener?.let { listener ->
            val driverId = sessionManager.getUserId()
            if (driverId != null) {
                database.getReference("drivers").child(driverId).child("notifications")
                    .removeEventListener(listener)
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goOnline()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap?.isMyLocationEnabled = true
            }
        }
    }
    
    private fun showMenuDialog() {
        val userName = sessionManager.getUserName() ?: "Conductor"
        val vehicle = sessionManager.getVehicle() ?: "Vehículo"
        val menuItems = arrayOf(
            "📢 Notificaciones",
            "📊 Historial de Carreras",
            "💰 Saldo y Comisiones",
            "👤 Mis Datos",
            "🚗 Mi Vehículo",
            "📞 Contactar Central",
            "🚪 Cerrar sesión"
        )
        AlertDialog.Builder(this)
            .setTitle("Hola, $userName 👋\n$vehicle")
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, NotificationsActivity::class.java)
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(this, DriverHistoryActivity::class.java)
                        startActivity(intent)
                    }
                    2 -> {
                        val intent = Intent(this, DriverBalanceActivity::class.java)
                        startActivity(intent)
                    }
                    3 -> {
                        val intent = Intent(this, DriverProfileActivity::class.java)
                        startActivity(intent)
                    }
                    4 -> {
                        val intent = Intent(this, DriverVehicleActivity::class.java)
                        startActivity(intent)
                    }
                    5 -> {
                        openWhatsAppMessage("59177696600")
                    }
                    6 -> {
                        logout()
                    }
                }
            }
            .show()
    }
    
    private fun logout() {
        sessionManager.clearSession()
        goToLoginActivity()
    }
    
    private fun goToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
