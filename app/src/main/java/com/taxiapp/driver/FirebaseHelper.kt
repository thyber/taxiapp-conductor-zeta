package com.taxiapp.driver

import com.google.firebase.database.*
import org.json.JSONObject

class FirebaseHelper {
    
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val activeDriversRef: DatabaseReference = database.getReference("activeDrivers")
    private val activeRidesRef: DatabaseReference = database.getReference("activeRides")
    
    interface DriverLocationListener {
        fun onDriverLocationUpdate(driverId: String, lat: Double, lng: Double)
    }
    
    interface RideStatusListener {
        fun onRideStatusChanged(ride: JSONObject)
    }
    
    interface NewRideRequestListener {
        fun onNewRideRequest(ride: JSONObject)
    }
    
    interface ActiveRideUpdateListener {
        fun onActiveRideUpdate(ride: JSONObject)
    }
    
    fun updateDriverLocation(driverId: String, lat: Double, lng: Double) {
        val driverRef = activeDriversRef.child(driverId)
        driverRef.child("location").setValue(mapOf(
            "lat" to lat,
            "lng" to lng
        ))
    }
    
    fun goOnline(driverId: String, driverData: Map<String, Any?>) {
        val driverRef = activeDriversRef.child(driverId)
        driverRef.setValue(driverData)
    }
    
    fun goOffline(driverId: String) {
        activeDriversRef.child(driverId).removeValue()
    }
    
    fun requestRide(rideId: String, rideData: Map<String, Any?>, onComplete: (Boolean) -> Unit) {
        val rideRef = activeRidesRef.child(rideId)
        rideRef.setValue(rideData)
            .addOnCompleteListener { task: com.google.android.gms.tasks.Task<Void> ->
                onComplete(task.isSuccessful)
            }
    }
    
    fun updateRideStatus(rideId: String, status: String, extraData: Map<String, Any?>? = null) {
        val rideRef = activeRidesRef.child(rideId)
        val updates = mutableMapOf<String, Any?>("status" to status)
        extraData?.let { updates.putAll(it) }
        rideRef.updateChildren(updates)
    }
    
    fun acceptRide(rideId: String, driverId: String, driverData: Map<String, Any?>) {
        val rideRef = activeRidesRef.child(rideId)
        rideRef.updateChildren(mapOf(
            "status" to "accepted",
            "driverId" to driverId,
            "driverData" to driverData,
            "acceptedAt" to ServerValue.TIMESTAMP
        ))
    }
    
    fun driverArrived(rideId: String) {
        val rideRef = activeRidesRef.child(rideId)
        rideRef.updateChildren(mapOf(
            "status" to "arrived",
            "arrivedAt" to ServerValue.TIMESTAMP
        ))
    }
    
    fun startRide(rideId: String) {
        val rideRef = activeRidesRef.child(rideId)
        rideRef.updateChildren(mapOf(
            "status" to "in_progress",
            "startedAt" to ServerValue.TIMESTAMP
        ))
    }
    
    fun completeRide(rideId: String, completedRideData: Map<String, Any?>) {
        val rideRef = activeRidesRef.child(rideId)
        rideRef.removeValue()
        
        val completedRidesRef = database.getReference("completedRides")
        completedRidesRef.push().setValue(completedRideData)
        
        val driverId = completedRideData["driverId"] as? String ?: return
        val fare = completedRideData["fare"] as? String ?: return
        val fareValue = fare.toDoubleOrNull() ?: return
        val commission = fareValue * 0.05
        
        val commissionData = mapOf(
            "rideId" to rideId,
            "driverId" to driverId,
            "fare" to fareValue,
            "commission" to commission,
            "timestamp" to System.currentTimeMillis()
        )
        
        val driverCommissionsRef = database.getReference("driverCommissions").child(driverId)
        driverCommissionsRef.push().setValue(commissionData)
        
        val driverEarningsRef = database.getReference("drivers").child(driverId).child("totalEarnings")
        driverEarningsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentEarnings = snapshot.getValue(Double::class.java) ?: 0.0
                driverEarningsRef.setValue(currentEarnings + fareValue)
            }
            
            override fun onCancelled(error: DatabaseError) {}
        })
        
        val driverCommissionsTotalRef = database.getReference("drivers").child(driverId).child("totalCommissions")
        driverCommissionsTotalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentCommissions = snapshot.getValue(Double::class.java) ?: 0.0
                driverCommissionsTotalRef.setValue(currentCommissions + commission)
            }
            
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    
    fun cancelRide(rideId: String) {
        val rideRef = activeRidesRef.child(rideId)
        rideRef.updateChildren(mapOf(
            "status" to "cancelled",
            "cancelledBy" to "driver",
            "cancelledAt" to ServerValue.TIMESTAMP
        ))
    }
    
    fun listenToDriverLocation(driverId: String, listener: DriverLocationListener) {
        activeDriversRef.child(driverId).child("location")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lat = snapshot.child("lat").getValue(Double::class.java) ?: return
                    val lng = snapshot.child("lng").getValue(Double::class.java) ?: return
                    listener.onDriverLocationUpdate(driverId, lat, lng)
                }
                
                override fun onCancelled(error: DatabaseError) {}
            })
    }
    
    fun listenToRideStatus(rideId: String, listener: RideStatusListener) {
        activeRidesRef.child(rideId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rideMap = snapshot.value as? Map<String, Any> ?: return
                val rideJson = JSONObject(rideMap)
                listener.onRideStatusChanged(rideJson)
            }
            
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    
    fun listenToNewRideRequests(listener: NewRideRequestListener) {
        activeRidesRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val rideMap = snapshot.value as? Map<String, Any> ?: return
                val rideJson = JSONObject(rideMap)
                if (rideJson.optString("status") == "pending") {
                    listener.onNewRideRequest(rideJson)
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val rideMap = snapshot.value as? Map<String, Any> ?: return
                val rideJson = JSONObject(rideMap)
                if (rideJson.optString("status") == "pending") {
                    listener.onNewRideRequest(rideJson)
                }
            }
            
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    
    fun listenToActiveRideUpdates(listener: ActiveRideUpdateListener) {
        activeRidesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (rideSnapshot in snapshot.children) {
                    val rideMap = rideSnapshot.value as? Map<String, Any> ?: continue
                    val rideJson = JSONObject(rideMap)
                    listener.onActiveRideUpdate(rideJson)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    
    fun getAppVersion(callback: (Map<String, Any>?) -> Unit) {
        database.getReference("appVersions").child("driver").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.value as? Map<String, Any>)
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }
}
