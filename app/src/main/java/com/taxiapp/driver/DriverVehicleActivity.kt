package com.taxiapp.driver

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class DriverVehicleActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    
    // Modo Vista
    private lateinit var tvVehicleBrand: TextView
    private lateinit var tvVehicleModel: TextView
    private lateinit var tvLicensePlate: TextView
    private lateinit var tvVehicleColor: TextView
    private lateinit var btnEdit: Button
    private lateinit var viewMode: LinearLayout
    
    // Modo Edición
    private lateinit var etVehicleBrand: EditText
    private lateinit var etVehicleModel: EditText
    private lateinit var etLicensePlate: EditText
    private lateinit var etVehicleColor: EditText
    private lateinit var btnSaveVehicle: Button
    private lateinit var btnCancel: Button
    private lateinit var editMode: LinearLayout
    
    private val database = FirebaseDatabase.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_vehicle)
        
        sessionManager = SessionManager(this)
        
        initViews()
        loadVehicleData()
        setupClickListeners()
        
        // Iniciar en modo vista
        showViewMode()
    }
    
    private fun initViews() {
        // Modo Vista
        viewMode = findViewById(R.id.viewMode)
        tvVehicleBrand = findViewById(R.id.tvVehicleBrand)
        tvVehicleModel = findViewById(R.id.tvVehicleModel)
        tvLicensePlate = findViewById(R.id.tvLicensePlate)
        tvVehicleColor = findViewById(R.id.tvVehicleColor)
        btnEdit = findViewById(R.id.btnEdit)
        
        // Modo Edición
        editMode = findViewById(R.id.editMode)
        etVehicleBrand = findViewById(R.id.etVehicleBrand)
        etVehicleModel = findViewById(R.id.etVehicleModel)
        etLicensePlate = findViewById(R.id.etLicensePlate)
        etVehicleColor = findViewById(R.id.etVehicleColor)
        btnSaveVehicle = findViewById(R.id.btnSaveVehicle)
        btnCancel = findViewById(R.id.btnCancel)
    }
    
    private fun loadVehicleData() {
        tvVehicleBrand.text = sessionManager.getVehicleBrand() ?: "-"
        tvVehicleModel.text = sessionManager.getVehicleModel() ?: "-"
        tvLicensePlate.text = sessionManager.getLicensePlate() ?: "-"
        tvVehicleColor.text = sessionManager.getVehicleColor() ?: "-"
    }
    
    private fun loadVehicleDataToEdit() {
        etVehicleBrand.setText(sessionManager.getVehicleBrand() ?: "")
        etVehicleModel.setText(sessionManager.getVehicleModel() ?: "")
        etLicensePlate.setText(sessionManager.getLicensePlate() ?: "")
        etVehicleColor.setText(sessionManager.getVehicleColor() ?: "")
    }
    
    private fun setupClickListeners() {
        btnEdit.setOnClickListener {
            showEditMode()
        }
        
        btnCancel.setOnClickListener {
            showViewMode()
        }
        
        btnSaveVehicle.setOnClickListener {
            saveVehicleData()
        }
    }
    
    private fun showViewMode() {
        viewMode.visibility = View.VISIBLE
        editMode.visibility = View.GONE
        btnEdit.visibility = View.VISIBLE
        
        // Recargar datos por si hubo cambios
        loadVehicleData()
    }
    
    private fun showEditMode() {
        viewMode.visibility = View.GONE
        editMode.visibility = View.VISIBLE
        btnEdit.visibility = View.GONE
        
        // Cargar datos en los campos de edición
        loadVehicleDataToEdit()
    }
    
    private fun saveVehicleData() {
        val driverId = sessionManager.getUserId() ?: return
        val brand = etVehicleBrand.text.toString().trim()
        val model = etVehicleModel.text.toString().trim()
        val plate = etLicensePlate.text.toString().trim()
        val color = etVehicleColor.text.toString().trim()
        
        if (brand.isEmpty() || model.isEmpty() || plate.isEmpty() || color.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        
        val vehicleData = mapOf(
            "vehicleBrand" to brand,
            "vehicleModel" to model,
            "licensePlate" to plate,
            "vehicleColor" to color,
            "vehicle" to "$brand $model"
        )
        
        val driverRef = database.getReference("drivers").child(driverId)
        driverRef.updateChildren(vehicleData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                sessionManager.saveVehicleBrand(brand)
                sessionManager.saveVehicleModel(model)
                sessionManager.saveLicensePlate(plate)
                sessionManager.saveVehicleColor(color)
                sessionManager.saveVehicle("$brand $model")
                
                Toast.makeText(this, "Datos del vehículo guardados exitosamente!", Toast.LENGTH_SHORT).show()
                showViewMode()
            } else {
                Toast.makeText(this, "Error al guardar los datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
