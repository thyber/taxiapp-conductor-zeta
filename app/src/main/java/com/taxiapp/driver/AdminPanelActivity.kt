package com.taxiapp.driver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var recyclerViewDrivers: RecyclerView
    private lateinit var driverAdapter: DriverAdapter
    private val drivers = mutableListOf<Driver>()
    private val database = FirebaseDatabase.getInstance()
    private val driversRef = database.getReference("drivers")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        initViews()
        setupRecyclerView()
        loadDrivers()
    }

    private fun initViews() {
        recyclerViewDrivers = findViewById(R.id.recyclerViewDrivers)
    }

    private fun setupRecyclerView() {
        driverAdapter = DriverAdapter(
            drivers,
            onEditClick = { driver -> showEditDriverDialog(driver) },
            onToggleSuspendClick = { driver -> toggleDriverSuspension(driver) }
        )
        recyclerViewDrivers.layoutManager = LinearLayoutManager(this)
        recyclerViewDrivers.adapter = driverAdapter
    }

    private fun loadDrivers() {
        driversRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newDrivers = mutableListOf<Driver>()
                for (driverSnapshot in snapshot.children) {
                    val id = driverSnapshot.key ?: continue
                    val name = driverSnapshot.child("name").getValue(String::class.java) ?: ""
                    val vehicle = driverSnapshot.child("vehicle").getValue(String::class.java) ?: ""
                    val licensePlate = driverSnapshot.child("licensePlate").getValue(String::class.java) ?: ""
                    val phone = driverSnapshot.child("phone").getValue(String::class.java) ?: ""
                    val suspended = driverSnapshot.child("suspended").getValue(Boolean::class.java) ?: false
                    val totalEarnings = driverSnapshot.child("totalEarnings").getValue(Double::class.java) ?: 0.0
                    val totalCommissions = driverSnapshot.child("totalCommissions").getValue(Double::class.java) ?: 0.0

                    newDrivers.add(
                        Driver(
                            id = id,
                            name = name,
                            vehicle = vehicle,
                            licensePlate = licensePlate,
                            phone = phone,
                            suspended = suspended,
                            totalEarnings = totalEarnings,
                            totalCommissions = totalCommissions
                        )
                    )
                }
                driverAdapter.updateDrivers(newDrivers)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminPanelActivity, "Error al cargar conductores", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showEditDriverDialog(driver: Driver) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_driver, null)
        
        val editTextName = dialogView.findViewById<android.widget.EditText>(R.id.editTextName)
        val editTextVehicle = dialogView.findViewById<android.widget.EditText>(R.id.editTextVehicle)
        val editTextLicensePlate = dialogView.findViewById<android.widget.EditText>(R.id.editTextLicensePlate)
        val editTextPhone = dialogView.findViewById<android.widget.EditText>(R.id.editTextPhone)

        editTextName.setText(driver.name)
        editTextVehicle.setText(driver.vehicle)
        editTextLicensePlate.setText(driver.licensePlate)
        editTextPhone.setText(driver.phone)

        AlertDialog.Builder(this)
            .setTitle("Editar Conductor")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editTextName.text.toString()
                val newVehicle = editTextVehicle.text.toString()
                val newLicensePlate = editTextLicensePlate.text.toString()
                val newPhone = editTextPhone.text.toString()

                val driverRef = driversRef.child(driver.id)
                val updates = mapOf(
                    "name" to newName,
                    "vehicle" to newVehicle,
                    "licensePlate" to newLicensePlate,
                    "phone" to newPhone
                )
                driverRef.updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Conductor actualizado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toggleDriverSuspension(driver: Driver) {
        val newSuspendedState = !driver.suspended
        val driverRef = driversRef.child(driver.id)
        driverRef.child("suspended").setValue(newSuspendedState)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    if (newSuspendedState) "Conductor suspendido" else "Conductor activado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cambiar estado", Toast.LENGTH_SHORT).show()
            }
    }
}
