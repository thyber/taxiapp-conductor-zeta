package com.taxiapp.driver

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class DriverProfileActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnSaveProfile: Button
    
    private val database = FirebaseDatabase.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_profile)
        
        sessionManager = SessionManager(this)
        
        initViews()
        loadDriverData()
        setupClickListeners()
    }
    
    private fun initViews() {
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
    }
    
    private fun loadDriverData() {
        val driverId = sessionManager.getUserId() ?: return
        
        etFirstName.setText(sessionManager.getUserFirstName() ?: "")
        etLastName.setText(sessionManager.getUserLastName() ?: "")
        etPhone.setText(sessionManager.getUserPhone() ?: "")
        etEmail.setText(sessionManager.getUserEmail() ?: "")
    }
    
    private fun setupClickListeners() {
        btnSaveProfile.setOnClickListener {
            saveDriverData()
        }
    }
    
    private fun saveDriverData() {
        val driverId = sessionManager.getUserId() ?: return
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        
        if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }
        
        val driverData = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "phone" to phone,
            "email" to email,
            "name" to "$firstName $lastName"
        )
        
        val driverRef = database.getReference("drivers").child(driverId)
        driverRef.updateChildren(driverData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                sessionManager.saveUserFirstName(firstName)
                sessionManager.saveUserLastName(lastName)
                sessionManager.saveUserPhone(phone)
                sessionManager.saveUserEmail(email)
                sessionManager.saveUserName("$firstName $lastName")
                
                Toast.makeText(this, "Datos guardados exitosamente!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar los datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
