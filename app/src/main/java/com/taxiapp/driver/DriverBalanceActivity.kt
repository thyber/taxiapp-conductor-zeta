package com.taxiapp.driver

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DriverBalanceActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvDayTotal: TextView
    private lateinit var tvDayCommission: TextView
    private lateinit var tvDayNet: TextView
    private lateinit var listViewRides: ListView
    
    private val database = FirebaseDatabase.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_balance)
        
        sessionManager = SessionManager(this)
        
        initViews()
        loadBalance()
    }
    
    private fun initViews() {
        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        tvDayTotal = findViewById(R.id.tvDayTotal)
        tvDayCommission = findViewById(R.id.tvDayCommission)
        tvDayNet = findViewById(R.id.tvDayNet)
        listViewRides = findViewById(R.id.listViewRides)
    }
    
    private fun loadBalance() {
        val driverId = sessionManager.getUserId() ?: return
        
        val ridesRef = database.getReference("rides")
        ridesRef.orderByChild("driverId").equalTo(driverId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentTime = System.currentTimeMillis()
                    val oneDay = 24 * 60 * 60 * 1000
                    
                    var dayTotal = 0.0
                    var dayCommission = 0.0
                    var totalBalance = 0.0
                    val rideDetails = mutableListOf<String>()
                    
                    for (rideSnapshot in snapshot.children) {
                        val rideData = rideSnapshot.value as? Map<String, Any> ?: continue
                        val status = rideData["status"] as? String ?: ""
                        val completedAt = rideData["completedAt"] as? Long ?: 0
                        val fare = rideData["fare"] as? Double ?: 0.0
                        val commission = fare * 0.10
                        val net = fare - commission
                        
                        if (status == "completed") {
                            totalBalance += net
                            
                            if (currentTime - completedAt <= oneDay) {
                                dayTotal += fare
                                dayCommission += commission
                                
                                val pickup = rideData["pickup"] as? String ?: "Sin dirección"
                                rideDetails.add("📍 $pickup\n💰 Total: Bs ${fare.toInt()} | 🔴 Comisión: Bs ${String.format("%.2f", commission)} | 🔵 Neto: Bs ${String.format("%.2f", net)}")
                            }
                        }
                    }
                    
                    tvTotalBalance.text = "Bs ${String.format("%.2f", totalBalance)}"
                    tvDayTotal.text = "Bs ${String.format("%.2f", dayTotal)}"
                    tvDayCommission.text = "Bs ${String.format("%.2f", dayCommission)}"
                    tvDayNet.text = "Bs ${String.format("%.2f", dayTotal - dayCommission)}"
                    
                    val adapter = ArrayAdapter(this@DriverBalanceActivity, android.R.layout.simple_list_item_1, rideDetails)
                    listViewRides.adapter = adapter
                    
                    if (rideDetails.isEmpty()) {
                        Toast.makeText(this@DriverBalanceActivity, "No hay carreras hoy", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DriverBalanceActivity, "Error al cargar saldo", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
