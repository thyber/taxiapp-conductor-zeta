package com.taxiapp.driver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
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

    // Datos del Conductor
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText

    // Datos del Vehículo
    private lateinit var etVehicleBrand: EditText
    private lateinit var etVehicleModel: EditText
    private lateinit var etLicensePlate: EditText
    private lateinit var spinnerVehicleType: Spinner
    private lateinit var spinnerVanType: Spinner
    private lateinit var tvSelectedColor: TextView
    private var selectedVehicleColor = ""

    // Botones y TextView
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    // Fotos - Conductor
    private lateinit var btnUploadProfileGallery: Button
    private lateinit var btnUploadProfileCamera: Button
    private lateinit var tvProfileStatus: TextView

    private lateinit var btnUploadLicenseFrontGallery: Button
    private lateinit var btnUploadLicenseFrontCamera: Button
    private lateinit var tvLicenseFrontStatus: TextView

    private lateinit var btnUploadLicenseBackGallery: Button
    private lateinit var btnUploadLicenseBackCamera: Button
    private lateinit var tvLicenseBackStatus: TextView

    private lateinit var btnUploadCIFrontGallery: Button
    private lateinit var btnUploadCIFrontCamera: Button
    private lateinit var tvCIFrontStatus: TextView

    private lateinit var btnUploadCIBackGallery: Button
    private lateinit var btnUploadCIBackCamera: Button
    private lateinit var tvCIBackStatus: TextView

    // Fotos - Vehículo
    private lateinit var btnUploadRUATGallery: Button
    private lateinit var btnUploadRUATCamera: Button
    private lateinit var tvRUATStatus: TextView

    private lateinit var btnUploadVehicleGallery: Button
    private lateinit var btnUploadVehicleCamera: Button
    private lateinit var tvVehicleStatus: TextView

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    // URIs de fotos
    private var profilePhotoUri: Uri? = null
    private var licenseFrontPhotoUri: Uri? = null
    private var licenseBackPhotoUri: Uri? = null
    private var ciFrontPhotoUri: Uri? = null
    private var ciBackPhotoUri: Uri? = null
    private var ruatPhotoUri: Uri? = null
    private var vehiclePhotoUri: Uri? = null

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
        uri?.let { handlePhotoSelection(it) }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && currentPhotoUri != null) {
            handlePhotoSelection(currentPhotoUri!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        initViews()
        setupSpinners()
        setupColorSelection()
        setupClickListeners()
    }

    private fun initViews() {
        // Datos conductor
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        // Datos vehículo
        etVehicleBrand = findViewById(R.id.etVehicleBrand)
        etVehicleModel = findViewById(R.id.etVehicleModel)
        etLicensePlate = findViewById(R.id.etLicensePlate)
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType)
        spinnerVanType = findViewById(R.id.spinnerVanType)
        tvSelectedColor = findViewById(R.id.tvSelectedColor)

        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        // Fotos conductor
        btnUploadProfileGallery = findViewById(R.id.btnUploadProfileGallery)
        btnUploadProfileCamera = findViewById(R.id.btnUploadProfileCamera)
        tvProfileStatus = findViewById(R.id.tvProfileStatus)

        btnUploadLicenseFrontGallery = findViewById(R.id.btnUploadLicenseFrontGallery)
        btnUploadLicenseFrontCamera = findViewById(R.id.btnUploadLicenseFrontCamera)
        tvLicenseFrontStatus = findViewById(R.id.tvLicenseFrontStatus)

        btnUploadLicenseBackGallery = findViewById(R.id.btnUploadLicenseBackGallery)
        btnUploadLicenseBackCamera = findViewById(R.id.btnUploadLicenseBackCamera)
        tvLicenseBackStatus = findViewById(R.id.tvLicenseBackStatus)

        btnUploadCIFrontGallery = findViewById(R.id.btnUploadCIFrontGallery)
        btnUploadCIFrontCamera = findViewById(R.id.btnUploadCIFrontCamera)
        tvCIFrontStatus = findViewById(R.id.tvCIFrontStatus)

        btnUploadCIBackGallery = findViewById(R.id.btnUploadCIBackGallery)
        btnUploadCIBackCamera = findViewById(R.id.btnUploadCIBackCamera)
        tvCIBackStatus = findViewById(R.id.tvCIBackStatus)

        // Fotos vehículo
        btnUploadRUATGallery = findViewById(R.id.btnUploadRUATGallery)
        btnUploadRUATCamera = findViewById(R.id.btnUploadRUATCamera)
        tvRUATStatus = findViewById(R.id.tvRUATStatus)

        btnUploadVehicleGallery = findViewById(R.id.btnUploadVehicleGallery)
        btnUploadVehicleCamera = findViewById(R.id.btnUploadVehicleCamera)
        tvVehicleStatus = findViewById(R.id.tvVehicleStatus)
    }

    private fun setupSpinners() {
        val vehicleTypes = arrayOf("Auto", "Vagoneta", "Moto")
        val vehicleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehicleTypes)
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVehicleType.adapter = vehicleAdapter

        spinnerVehicleType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (vehicleTypes[position] == "Vagoneta") {
                    spinnerVanType.visibility = View.VISIBLE
                } else {
                    spinnerVanType.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        val vanTypes = arrayOf("Maletero Libre", "Con Parrilla")
        val vanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vanTypes)
        vanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVanType.adapter = vanAdapter
    }

    private fun setupColorSelection() {
        val colorMap = mapOf(
            R.id.colorWhite to "Blanco",
            R.id.colorBlack to "Negro",
            R.id.colorSilver to "Plateado",
            R.id.colorGray to "Gris",
            R.id.colorRed to "Rojo",
            R.id.colorBlue to "Azul"
        )

        colorMap.keys.forEach { id ->
            findViewById<View>(id).setOnClickListener {
                selectedVehicleColor = colorMap[id] ?: ""
                tvSelectedColor.text = "Color seleccionado: $selectedVehicleColor"
                tvSelectedColor.setTextColor(0xFF10b981.toInt())
            }
        }
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener { registerUser() }
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Fotos Conductor
        btnUploadProfileGallery.setOnClickListener { openGalleryForPhoto("profile") }
        btnUploadProfileCamera.setOnClickListener { checkCameraPermissionAndOpen("profile") }

        btnUploadLicenseFrontGallery.setOnClickListener { openGalleryForPhoto("licenseFront") }
        btnUploadLicenseFrontCamera.setOnClickListener { checkCameraPermissionAndOpen("licenseFront") }

        btnUploadLicenseBackGallery.setOnClickListener { openGalleryForPhoto("licenseBack") }
        btnUploadLicenseBackCamera.setOnClickListener { checkCameraPermissionAndOpen("licenseBack") }

        btnUploadCIFrontGallery.setOnClickListener { openGalleryForPhoto("ciFront") }
        btnUploadCIFrontCamera.setOnClickListener { checkCameraPermissionAndOpen("ciFront") }

        btnUploadCIBackGallery.setOnClickListener { openGalleryForPhoto("ciBack") }
        btnUploadCIBackCamera.setOnClickListener { checkCameraPermissionAndOpen("ciBack") }

        // Fotos Vehículo
        btnUploadRUATGallery.setOnClickListener { openGalleryForPhoto("ruat") }
        btnUploadRUATCamera.setOnClickListener { checkCameraPermissionAndOpen("ruat") }

        btnUploadVehicleGallery.setOnClickListener { openGalleryForPhoto("vehicle") }
        btnUploadVehicleCamera.setOnClickListener { checkCameraPermissionAndOpen("vehicle") }
    }

    private fun openGalleryForPhoto(type: String) {
        currentPhotoType = type
        galleryLauncher.launch("image/*")
    }

    private fun checkCameraPermissionAndOpen(type: String) {
        currentPhotoType = type
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

    private fun handlePhotoSelection(uri: Uri) {
        when (currentPhotoType) {
            "profile" -> {
                profilePhotoUri = uri
                updatePhotoStatus(tvProfileStatus, true)
            }
            "licenseFront" -> {
                licenseFrontPhotoUri = uri
                updatePhotoStatus(tvLicenseFrontStatus, true)
            }
            "licenseBack" -> {
                licenseBackPhotoUri = uri
                updatePhotoStatus(tvLicenseBackStatus, true)
            }
            "ciFront" -> {
                ciFrontPhotoUri = uri
                updatePhotoStatus(tvCIFrontStatus, true)
            }
            "ciBack" -> {
                ciBackPhotoUri = uri
                updatePhotoStatus(tvCIBackStatus, true)
            }
            "ruat" -> {
                ruatPhotoUri = uri
                updatePhotoStatus(tvRUATStatus, true)
            }
            "vehicle" -> {
                vehiclePhotoUri = uri
                updatePhotoStatus(tvVehicleStatus, true)
            }
        }
    }

    private fun updatePhotoStatus(textView: TextView, selected: Boolean) {
        if (selected) {
            textView.text = "✅ Seleccionada"
            textView.setTextColor(0xFF10b981.toInt())
        } else {
            textView.text = "❌ No seleccionada"
            textView.setTextColor(0xFFef4444.toInt())
        }
    }

    private fun registerUser() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        val vehicleBrand = etVehicleBrand.text.toString().trim()
        val vehicleModel = etVehicleModel.text.toString().trim()
        val licensePlate = etLicensePlate.text.toString().trim()
        val vehicleType = spinnerVehicleType.selectedItem.toString()
        val vanType = if (spinnerVanType.visibility == View.VISIBLE) spinnerVanType.selectedItem.toString() else ""

        // Validaciones
        if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty() ||
            vehicleBrand.isEmpty() || vehicleModel.isEmpty() || licensePlate.isEmpty() ||
            password.isEmpty() || selectedVehicleColor.isEmpty()) {
            Toast.makeText(this, "Por favor llena todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        btnRegister.isEnabled = false
        btnRegister.text = "Registrando..."

        val authEmail = if (email.isNotEmpty()) email else "$phone@temp.com"

        auth.createUserWithEmailAndPassword(authEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    uploadPhotosAndSaveData(
                        userId, firstName, lastName, phone, email,
                        vehicleBrand, vehicleModel, licensePlate, vehicleType, vanType, selectedVehicleColor
                    )
                } else {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Registrarse"
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun uploadPhotosAndSaveData(
        userId: String,
        firstName: String,
        lastName: String,
        phone: String,
        email: String,
        vehicleBrand: String,
        vehicleModel: String,
        licensePlate: String,
        vehicleType: String,
        vanType: String,
        vehicleColor: String
    ) {
        val photoList = listOf(
            profilePhotoUri to "driverPhotos/$userId/profile.jpg",
            licenseFrontPhotoUri to "driverPhotos/$userId/license_front.jpg",
            licenseBackPhotoUri to "driverPhotos/$userId/license_back.jpg",
            ciFrontPhotoUri to "driverPhotos/$userId/ci_front.jpg",
            ciBackPhotoUri to "driverPhotos/$userId/ci_back.jpg",
            ruatPhotoUri to "driverPhotos/$userId/ruat.jpg",
            vehiclePhotoUri to "driverPhotos/$userId/vehicle.jpg"
        )

        val downloadUrls = mutableMapOf<String, String>()
        var uploadedCount = 0
        val totalToUpload = photoList.count { it.first != null }

        if (totalToUpload == 0) {
            saveUserData(
                userId, firstName, lastName, phone, email,
                vehicleBrand, vehicleModel, licensePlate, vehicleType, vanType, vehicleColor, downloadUrls
            )
            return
        }

        photoList.forEach { (uri, path) ->
            uri?.let {
                val storageRef = storage.reference.child(path)
                storageRef.putFile(it)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { url ->
                            when (path) {
                                "driverPhotos/$userId/profile.jpg" -> downloadUrls["profilePhotoUrl"] = url.toString()
                                "driverPhotos/$userId/license_front.jpg" -> downloadUrls["licenseFrontUrl"] = url.toString()
                                "driverPhotos/$userId/license_back.jpg" -> downloadUrls["licenseBackUrl"] = url.toString()
                                "driverPhotos/$userId/ci_front.jpg" -> downloadUrls["ciFrontUrl"] = url.toString()
                                "driverPhotos/$userId/ci_back.jpg" -> downloadUrls["ciBackUrl"] = url.toString()
                                "driverPhotos/$userId/ruat.jpg" -> downloadUrls["ruatPhotoUrl"] = url.toString()
                                "driverPhotos/$userId/vehicle.jpg" -> downloadUrls["vehiclePhotoUrl"] = url.toString()
                            }
                            uploadedCount++
                            if (uploadedCount == totalToUpload) {
                                saveUserData(
                                    userId, firstName, lastName, phone, email,
                                    vehicleBrand, vehicleModel, licensePlate, vehicleType, vanType, vehicleColor, downloadUrls
                                )
                            }
                        }
                    }
                    .addOnFailureListener {
                        uploadedCount++
                        if (uploadedCount == totalToUpload) {
                            saveUserData(
                                userId, firstName, lastName, phone, email,
                                vehicleBrand, vehicleModel, licensePlate, vehicleType, vanType, vehicleColor, downloadUrls
                            )
                        }
                    }
            } ?: run {
                uploadedCount++
                if (uploadedCount == totalToUpload) {
                    saveUserData(
                        userId, firstName, lastName, phone, email,
                        vehicleBrand, vehicleModel, licensePlate, vehicleType, vanType, vehicleColor, downloadUrls
                    )
                }
            }
        }
    }

    private fun saveUserData(
        userId: String,
        firstName: String,
        lastName: String,
        phone: String,
        email: String,
        vehicleBrand: String,
        vehicleModel: String,
        licensePlate: String,
        vehicleType: String,
        vanType: String,
        vehicleColor: String,
        photoUrls: Map<String, String>
    ) {
        val user = hashMapOf(
            "id" to userId,
            "firstName" to firstName,
            "lastName" to lastName,
            "name" to "$firstName $lastName",
            "phone" to phone,
            "email" to email,
            "vehicleBrand" to vehicleBrand,
            "vehicleModel" to vehicleModel,
            "licensePlate" to licensePlate,
            "vehicleType" to vehicleType,
            "vanType" to vanType,
            "vehicleColor" to vehicleColor,
            "suspended" to false,
            "approved" to false,
            "totalEarnings" to 0,
            "totalCommissions" to 0,
            "profilePhotoUrl" to (photoUrls["profilePhotoUrl"] ?: ""),
            "licenseFrontUrl" to (photoUrls["licenseFrontUrl"] ?: ""),
            "licenseBackUrl" to (photoUrls["licenseBackUrl"] ?: ""),
            "ciFrontUrl" to (photoUrls["ciFrontUrl"] ?: ""),
            "ciBackUrl" to (photoUrls["ciBackUrl"] ?: ""),
            "ruatPhotoUrl" to (photoUrls["ruatPhotoUrl"] ?: ""),
            "vehiclePhotoUrl" to (photoUrls["vehiclePhotoUrl"] ?: "")
        )

        database.getReference("drivers").child(userId).setValue(user)
            .addOnCompleteListener { task ->
                btnRegister.isEnabled = true
                btnRegister.text = "Registrarse"
                if (task.isSuccessful) {
                    val sessionManager = SessionManager(this)
                    sessionManager.saveSession(userId, "$firstName $lastName", email, phone, "$vehicleBrand $vehicleModel", licensePlate, vehicleType)
                    sessionManager.saveVehicleType(vehicleType)
                    sessionManager.saveVehicleBrand(vehicleBrand)
                    sessionManager.saveVehicleModel(vehicleModel)
                    sessionManager.saveVehicleColor(vehicleColor)
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
