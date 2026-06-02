package com.taxiapp.driver

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TabHost
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DriverHistoryActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var tabHost: TabHost
    private lateinit var listViewDay: ListView
    private lateinit var listViewWeek: ListView
    private lateinit var listViewMonth: ListView
    
    private val database = FirebaseDatabase.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_history)
        
        sessionManager = SessionManager(this)
        
        initViews()
        setupTabs()
        loadHistory()
    }
    
    private fun initViews() {
        tabHost = findViewById(android.R.id.tabhost)
        listViewDay = findViewById(R.id.listViewDay)
        listViewWeek = findViewById(R.id.listViewWeek)
        listViewMonth = findViewById(R.id.listViewMonth)
    }
    
    private fun setupTabs() {
        tabHost.setup()
        
        val spec1 = tabHost.newTabSpec("dia")
        spec1.setContent(R.id.tabDay)
        spec1.setIndicator("📅 Hoy")
        tabHost.addTab(spec1)
        
        val spec2 = tabHost.newTabSpec("semana")
        spec2.setContent(R.id.tabWeek)
        spec2.setIndicator("📆 Semana")
        tabHost.addTab(spec2)
        
        val spec3 = tabHost.newTabSpec("mes")
        spec3.setContent(R.id.tabMonth)
        spec3.setIndicator("📅 Mes")
        tabHost.addTab(spec3)
    }
    
    private fun loadHistory() {
        val driverId = sessionManager.getUserId() ?: return
        
        val ridesRef = database.getReference("drivers").child(driverId).child("rides")
        ridesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentTime = System.currentTimeMillis()
                    val oneDay = 24 * 60 * 60 * 1000
                    val oneWeek = 7 * oneDay
                    val oneMonth = 30 * oneDay
                    
                    val dayRides = mutableListOf<String>()
                    val weekRides = mutableListOf<String>()
                    val monthRides = mutableListOf<String>()
                    
                    for (rideSnapshot in snapshot.children) {
                        val rideData = rideSnapshot.value as? Map<String, Any> ?: continue
                        val status = rideData["status"] as? String ?: ""
                        val completedAt = rideData["completedAt"] as? Long ?: 0
                        
                        if (status == "completed") {
                            val pickup = rideData["pickup"] as? String ?: "Sin dirección"
                            val destination = rideData["destination"] as? String ?: "Sin destino"
                            var fareValue = 0.0
                            
                            // Manejar fare como String o Double
                            when (val fareObj = rideData["fare"]) {
                                is String -> fareValue = fareObj.toDoubleOrNull() ?: 0.0
                                is Double -> fareValue = fareObj
                                is Int -> fareValue = fareObj.toDouble()
                            }
                            
                            val distance = rideData["distanceKm"] as? Double ?: 0.0
                            
                            val rideText = "📍 Recogida: $pickup\n🏁 Destino: $destination\n💰 Tarifa: Bs ${fareValue.toInt()}\n📏 Distancia: ${String.format("%.1f", distance)} km"
                            
                            if (currentTime - completedAt <= oneDay) {
                                dayRides.add(rideText)
                            }
                            if (currentTime - completedAt <= oneWeek) {
                                weekRides.add(rideText)
                            }
                            if (currentTime - completedAt <= oneMonth) {
                                monthRides.add(rideText)
                            }
                        }
                    }
                    
                    val adapterDay = ArrayAdapter(this@DriverHistoryActivity, android.R.layout.simple_list_item_1, dayRides)
                    listViewDay.adapter = adapterDay
                    
                    val adapterWeek = ArrayAdapter(this@DriverHistoryActivity, android.R.layout.simple_list_item_1, weekRides)
                    listViewWeek.adapter = adapterWeek
                    
                    val adapterMonth = ArrayAdapter(this@DriverHistoryActivity, android.R.layout.simple_list_item_1, monthRides)
                    listViewMonth.adapter = adapterMonth
                    
                    if (dayRides.isEmpty()) {
                        Toast.makeText(this@DriverHistoryActivity, "No hay carreras hoy", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DriverHistoryActivity, "Error al cargar historial", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
