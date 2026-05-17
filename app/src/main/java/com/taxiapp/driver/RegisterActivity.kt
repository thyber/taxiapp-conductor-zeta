package com.taxiapp.driver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etCountryCode: EditText
    private lateinit var etPhone: EditText
    private lateinit var spinnerVehicleType: Spinner
    private lateinit var etVehicle: EditText
    private lateinit var etLicensePlate: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    private lateinit var btnUploadProfileGallery: Button
    private lateinit var btnUploadProfileCamera: Button
    private lateinit var tvProfileStatus: TextView

    private lateinit var btnUploadRUATGallery: Button
    private lateinit var btnUploadRUATCamera: Button
    private lateinit var tvRUATStatus: TextView

    private lateinit var btnUploadVehicleGallery: Button
    private lateinit var btnUploadVehicleCamera: Button
    private lateinit var tvVehicleStatus: TextView

    private lateinit var btnUploadLicenseGallery: Button
    private lateinit var btnUploadLicenseCamera: Button
    private lateinit var tvLicenseStatus: TextView

    private lateinit var btnUploadCIGallery: Button
    private lateinit var btnUploadCICamera: Button
    private lateinit var tvCIStatus: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    private var profilePhotoUri: Uri? = null
    private var ruatPhotoUri: Uri? = null
    private var vehiclePhotoUri: Uri? = null
    private var licensePhotoUri: Uri? = null
    private var ciPhotoUri: Uri? = null

    private var currentPhotoUri: Uri? = null
    private var currentPhotoType = ""

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            when (currentPhotoType) {
                "profile" -> {
                    profilePhotoUri = it
                    tvProfileStatus.text = "✅ Seleccionada"
                    tvProfileStatus.setTextColor(0xFF10b981.toInt())
                }
                "ruat" -> {
                    ruatPhotoUri = it
                    tvRUATStatus.text = "✅ Seleccionada"
                    tvRUATStatus.setTextColor(0xFF10b981.toInt())
                }
                "vehicle" -> {
                    vehiclePhotoUri = it
                    tvVehicleStatus.text = "✅ Seleccionada"
                    tvVehicleStatus.setTextColor(0xFF10b981.toInt())
                }
                "license" -> {
                    licensePhotoUri = it
                    tvLicenseStatus.text = "✅ Seleccionada"
                    tvLicenseStatus.setTextColor(0xFF10b981.toInt())
                }
                "ci" -> {
                    ciPhotoUri = it
                    tvCIStatus.text = "✅ Seleccionada"
                    tvCIStatus.setTextColor(0xFF10b981.toInt())
                }
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && currentPhotoUri != null) {
            when (currentPhotoType) {
                "profile" -> {
                    profilePhotoUri = currentPhotoUri
                    tvProfileStatus.text = "✅ Tomada"
                    tvProfileStatus.setTextColor(0xFF10b981.toInt())
                }
                "ruat" -> {
                    ruatPhotoUri = currentPhotoUri
                    tvRUATStatus.text = "✅ Tomada"
                    tvRUATStatus.setTextColor(0xFF10b981.toInt())
                }
                "vehicle" -> {
                    vehiclePhotoUri = currentPhotoUri
                    tvVehicleStatus.text = "✅ Tomada"
                    tvVehicleStatus.setTextColor(0xFF10b981.toInt())
                }
                "license" -> {
                    licensePhotoUri = currentPhotoUri
                    tvLicenseStatus.text = "✅ Tomada"
                    tvLicenseStatus.setTextColor(0xFF10b981.toInt())
                }
                "ci" -> {
                    ciPhotoUri = currentPhotoUri
                    tvCIStatus.text = "✅ Tomada"
                    tvCIStatus.setTextColor(0xFF10b981.toInt())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etCountryCode = findViewById(R.id.etCountryCode)
        etPhone = findViewById(R.id.etPhone)
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType)
        etVehicle = findViewById(R.id.etVehicle)
        etLicensePlate = findViewById(R.id.etLicensePlate)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        
        val vehicleTypes = arrayOf("Auto", "Vagoneta", "Moto")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehicleTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVehicleType.adapter = adapter

        btnUploadProfileGallery = findViewById(R.id.btnUploadProfileGallery)
        btnUploadProfileCamera = findViewById(R.id.btnUploadProfileCamera)
        tvProfileStatus = findViewById(R.id.tvProfileStatus)

        btnUploadRUATGallery = findViewById(R.id.btnUploadRUATGallery)
        btnUploadRUATCamera = findViewById(R.id.btnUploadRUATCamera)
        tvRUATStatus = findViewById(R.id.tvRUATStatus)

        btnUploadVehicleGallery = findViewById(R.id.btnUploadVehicleGallery)
        btnUploadVehicleCamera = findViewById(R.id.btnUploadVehicleCamera)
        tvVehicleStatus = findViewById(R.id.tvVehicleStatus)

        btnUploadLicenseGallery = findViewById(R.id.btnUploadLicenseGallery)
        btnUploadLicenseCamera = findViewById(R.id.btnUploadLicenseCamera)
        tvLicenseStatus = findViewById(R.id.tvLicenseStatus)

        btnUploadCIGallery = findViewById(R.id.btnUploadCIGallery)
        btnUploadCICamera = findViewById(R.id.btnUploadCICamera)
        tvCIStatus = findViewById(R.id.tvCIStatus)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener { registerUser() }
        tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        btnUploadProfileGallery.setOnClickListener {
            currentPhotoType = "profile"
            openGallery()
        }
        btnUploadProfileCamera.setOnClickListener {
            currentPhotoType = "profile"
            checkCameraPermission()
        }

        btnUploadRUATGallery.setOnClickListener {
            currentPhotoType = "ruat"
            openGallery()
        }
        btnUploadRUATCamera.setOnClickListener {
            currentPhotoType = "ruat"
            checkCameraPermission()
        }

        btnUploadVehicleGallery.setOnClickListener {
            currentPhotoType = "vehicle"
            openGallery()
        }
        btnUploadVehicleCamera.setOnClickListener {
            currentPhotoType = "vehicle"
            checkCameraPermission()
        }

        btnUploadLicenseGallery.setOnClickListener {
            currentPhotoType = "license"
            openGallery()
        }
        btnUploadLicenseCamera.setOnClickListener {
            currentPhotoType = "license"
            checkCameraPermission()
        }

        btnUploadCIGallery.setOnClickListener {
            currentPhotoType = "ci"
            openGallery()
        }
        btnUploadCICamera.setOnClickListener {
            currentPhotoType = "ci"
            checkCameraPermission()
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoFile?.let {
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                it
            )
            cameraLauncher.launch(currentPhotoUri)
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun registerUser() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val vehicleType = spinnerVehicleType.selectedItem.toString()
        val vehicle = etVehicle.text.toString().trim()
        val licensePlate = etLicensePlate.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() ||
            vehicle.isEmpty() || licensePlate.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        if (!emailPattern.matcher(email).matches()) {
            Toast.makeText(this, "Por favor ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        btnRegister.isEnabled = false
        btnRegister.text = "Registrando..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    uploadPhotosAndSaveData(userId, name, email, phone, vehicleType, vehicle, licensePlate)
                } else {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Registrarse"
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun uploadPhotosAndSaveData(
        userId: String,
        name: String,
        email: String,
        phone: String,
        vehicleType: String,
        vehicle: String,
        licensePlate: String
    ) {
        val photoUris = mutableListOf<Pair<Uri?, String>>()
        photoUris.add(profilePhotoUri to "driverPhotos/$userId/profile.jpg")
        photoUris.add(ruatPhotoUri to "driverPhotos/$userId/ruat.jpg")
        photoUris.add(vehiclePhotoUri to "driverPhotos/$userId/vehicle.jpg")
        photoUris.add(licensePhotoUri to "driverPhotos/$userId/license.jpg")
        photoUris.add(ciPhotoUri to "driverPhotos/$userId/ci.jpg")

        val downloadUrls = mutableMapOf<String, String>()
        var uploadedCount = 0

        for ((uri, path) in photoUris) {
            if (uri != null) {
                val storageRef = storage.reference.child(path)
                storageRef.putFile(uri)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { url ->
                            when (path) {
                                "driverPhotos/$userId/profile.jpg" -> downloadUrls["driverPhotoUrl"] = url.toString()
                                "driverPhotos/$userId/ruat.jpg" -> downloadUrls["ruatPhotoUrl"] = url.toString()
                                "driverPhotos/$userId/vehicle.jpg" -> downloadUrls["vehiclePhotoUrl"] = url.toString()
                                "driverPhotos/$userId/license.jpg" -> downloadUrls["licensePhotoUrl"] = url.toString()
                                "driverPhotos/$userId/ci.jpg" -> downloadUrls["ciPhotoUrl"] = url.toString()
                            }
                            uploadedCount++
                            if (uploadedCount == photoUris.count { it.first != null }) {
                                saveUserData(userId, name, email, phone, vehicleType, vehicle, licensePlate, downloadUrls)
                            }
                        }
                    }
                    .addOnFailureListener {
                        uploadedCount++
                        if (uploadedCount == photoUris.count { it.first != null }) {
                            saveUserData(userId, name, email, phone, vehicleType, vehicle, licensePlate, downloadUrls)
                        }
                    }
            } else {
                uploadedCount++
                if (uploadedCount == photoUris.count { it.first != null }) {
                    saveUserData(userId, name, email, phone, vehicleType, vehicle, licensePlate, downloadUrls)
                }
            }
        }

        if (photoUris.all { it.first == null }) {
            saveUserData(userId, name, email, phone, vehicleType, vehicle, licensePlate, downloadUrls)
        }
    }

    private fun saveUserData(
        userId: String,
        name: String,
        email: String,
        phone: String,
        vehicleType: String,
        vehicle: String,
        licensePlate: String,
        photoUrls: Map<String, String>
    ) {
        val user = mapOf(
            "id" to userId,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "vehicleType" to vehicleType,
            "vehicle" to vehicle,
            "licensePlate" to licensePlate,
            "suspended" to false,
            "approved" to false,
            "totalEarnings" to 0,
            "totalCommissions" to 0,
            "driverPhotoUrl" to (photoUrls["driverPhotoUrl"] ?: ""),
            "vehiclePhotoUrl" to (photoUrls["vehiclePhotoUrl"] ?: ""),
            "ruatPhotoUrl" to (photoUrls["ruatPhotoUrl"] ?: ""),
            "licensePhotoUrl" to (photoUrls["licensePhotoUrl"] ?: ""),
            "ciPhotoUrl" to (photoUrls["ciPhotoUrl"] ?: "")
        )

        database.getReference("drivers").child(userId).setValue(user)
            .addOnCompleteListener { task ->
                btnRegister.isEnabled = true
                btnRegister.text = "Registrarse"
                if (task.isSuccessful) {
                    val sessionManager = SessionManager(this)
                    sessionManager.saveSession(userId, name, email, phone, vehicle, licensePlate)
                    Toast.makeText(this, "Registro exitoso!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
