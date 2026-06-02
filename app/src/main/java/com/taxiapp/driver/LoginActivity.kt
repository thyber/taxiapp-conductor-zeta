package com.taxiapp.driver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {
    
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        sessionManager = SessionManager(this)
        
        if (sessionManager.isLoggedIn() && auth.currentUser != null) {
            goToMainActivity()
            return
        }
        
        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
    }
    
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            loginDriver()
        }
        
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        
        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }
    
    private fun loginDriver() {
        val input = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        if (input.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            signInWithEmail(input, password)
        } else {
            findEmailByPhone(input, password)
        }
    }
    
    private fun findEmailByPhone(phone: String, password: String) {
        val driversRef = database.getReference("drivers")
        driversRef.orderByChild("phone").equalTo(phone).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val email = userSnapshot.child("email").getValue(String::class.java)
                        if (email != null) {
                            signInWithEmail(email, password)
                            return
                        }
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "No se encontró un conductor con ese teléfono", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Error al buscar conductor", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    fetchUserData(userId)
                } else {
                    Toast.makeText(this, "Error al iniciar sesión: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val etEmailDialog = dialogView.findViewById<EditText>(R.id.etEmailDialog)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Recuperar Contraseña")
            .setView(dialogView)
            .setPositiveButton("Enviar") { _, _ ->
                val email = etEmailDialog.text.toString().trim()
                if (email.isNotEmpty()) {
                    sendPasswordResetEmail(email)
                } else {
                    Toast.makeText(this, "Por favor ingresa tu correo electrónico", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Se ha enviado un correo de recuperación a $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun fetchUserData(userId: String) {
        val userRef = database.getReference("drivers").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.child("name").getValue(String::class.java) ?: ""
                val userEmail = snapshot.child("email").getValue(String::class.java) ?: ""
                val userPhone = snapshot.child("phone").getValue(String::class.java) ?: ""
                val vehicle = snapshot.child("vehicle").getValue(String::class.java) ?: ""
                val licensePlate = snapshot.child("licensePlate").getValue(String::class.java) ?: ""
                val vehicleType = snapshot.child("vehicleType").getValue(String::class.java) ?: "auto"
                val vehicleBrand = snapshot.child("vehicleBrand").getValue(String::class.java) ?: ""
                val vehicleModel = snapshot.child("vehicleModel").getValue(String::class.java) ?: ""
                val vehicleColor = snapshot.child("vehicleColor").getValue(String::class.java) ?: ""
                
                sessionManager.saveSession(userId, userName, userEmail, userPhone, vehicle, licensePlate, vehicleType)
                sessionManager.saveVehicleType(vehicleType)
                sessionManager.saveVehicleBrand(vehicleBrand)
                sessionManager.saveVehicleModel(vehicleModel)
                sessionManager.saveVehicleColor(vehicleColor)
                
                Toast.makeText(this@LoginActivity, "¡Bienvenido $userName!", Toast.LENGTH_SHORT).show()
                goToMainActivity()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
