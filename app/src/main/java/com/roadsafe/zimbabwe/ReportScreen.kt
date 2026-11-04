
package com.roadsafe.zimbabwe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ReportNavy = Color(0xFF172B3D)
private val ReportDark = Color(0xFF263746)
private val ReportOrange = Color(0xFFE98220)
private val ReportBackground = Color(0xFFFFFFFF)
private val ReportMuted = Color(0xFF526273)
private val ReportBorder = Color(0xFF8795A3)
private val ReportSuccess = Color(0xFF187344)

@Composable
fun ReportScreen(onBack: () -> Unit) {

    val context = LocalContext.current

    val database = remember {
        RoadSafeDatabase.getDatabase(context)
    }

    val scope = rememberCoroutineScope()

    val roadAI = remember(context) {
        RoadAI(context)
    }

    var aiResult by remember {
        mutableStateOf("AI analysis waiting for photo...")
    }

    DisposableEffect(roadAI) {
        onDispose {
            roadAI.close()
        }
    }

    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    var locationMessage by remember {
        mutableStateOf("Location not captured yet")
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var activeLocationListener by remember {
        mutableStateOf<LocationListener?>(null)
    }

    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var capturedPhoto by remember { mutableStateOf<File?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ReportNavy,
        unfocusedTextColor = ReportNavy,
        focusedBorderColor = ReportNavy,
        unfocusedBorderColor = ReportBorder,
        focusedLabelColor = ReportNavy,
        unfocusedLabelColor = ReportDark,
        focusedPlaceholderColor = ReportMuted,
        unfocusedPlaceholderColor = ReportMuted,
        cursorColor = ReportOrange,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedTrailingIconColor = ReportNavy,
        unfocusedTrailingIconColor = ReportNavy
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted

        if (!granted) {
            Toast.makeText(
                context,
                "Camera permission is required to capture evidence.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        locationMessage = if (hasLocationPermission) {
            "Permission granted. Tap GET GPS LOCATION."
        } else {
            "Location permission denied."
        }
    }

    fun captureLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        hasLocationPermission = true

        try {
            val provider = when {
                fineGranted && locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                ) -> LocationManager.GPS_PROVIDER

                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                ) -> LocationManager.NETWORK_PROVIDER

                else -> null
            }

            if (provider == null) {
                locationMessage =
                    "Location is turned off. Enable it in phone settings."
                return
            }

            locationMessage = "Getting your location..."

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    latitude = location.latitude
                    longitude = location.longitude
                    locationMessage = "GPS location captured successfully!"

                    locationManager.removeUpdates(this)
                    activeLocationListener = null
                }

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {
                    locationMessage = "Location provider disabled."
                }

                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(
                    provider: String?,
                    status: Int,
                    extras: Bundle?
                ) {}
            }

            activeLocationListener?.let {
                locationManager.removeUpdates(it)
            }

            activeLocationListener = listener

            val lastLocation = locationManager.getLastKnownLocation(provider)

            if (lastLocation != null) {
                latitude = lastLocation.latitude
                longitude = lastLocation.longitude
                locationMessage = "Location retrieved successfully!"
            }

            locationManager.requestLocationUpdates(
                provider,
                1000L,
                0f,
                listener,
                Looper.getMainLooper()
            )

        } catch (e: SecurityException) {
            locationMessage = "Location permission is required."
        } catch (e: Exception) {
            locationMessage = "Unable to retrieve location: ${e.message}"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeLocationListener?.let {
                locationManager.removeUpdates(it)
            }
        }
    }

    val reportTime = remember {
        SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        ).format(Date())
    }

    val categories = listOf(
        "Road accident",
        "Vehicle breakdown",
        "Road obstruction",
        "Fire",
        "Flooding",
        "Damaged road",
        "Other"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReportBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        TextButton(onClick = onBack) {
            Text(
                text = "← Back to Home",
                color = ReportNavy,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ROADSAFE ZIMBABWE",
            color = ReportOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Report an Incident",
            color = ReportNavy,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Provide accurate details about the road incident to help keep Zimbabwe's roads safer.",
            color = ReportMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "01  INCIDENT DETAILS",
            color = ReportNavy,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = ReportBorder
        )

        Text(
            text = "Incident category *",
            color = ReportDark,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text("Select incident type")
                },
                trailingIcon = {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(
                            if (expanded) "▲" else "▼",
                            color = ReportNavy
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                categories.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                item,
                                color = ReportNavy
                            )
                        },
                        onClick = {
                            category = item
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Incident description *",
            color = ReportDark,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = {
                Text("Describe what happened and any important details...")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 5,
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "02  INCIDENT LOCATION",
            color = ReportNavy,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = ReportBorder
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F7F9)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                ReportBorder
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "📍 GPS LOCATION",
                    color = ReportNavy,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = locationMessage,
                    color = ReportDark,
                    fontSize = 13.sp
                )

                if (latitude != null && longitude != null) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Latitude: $latitude",
                        color = ReportNavy,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Longitude: $longitude",
                        color = ReportNavy,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { captureLocation() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReportOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "GET GPS LOCATION",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "03  PHOTOGRAPHIC EVIDENCE",
            color = ReportNavy,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = ReportBorder
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F7F9)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                ReportBorder
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text(
                    text = "📷 CAPTURE ROAD SCENE",
                    fontWeight = FontWeight.ExtraBold,
                    color = ReportNavy
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (hasCameraPermission) {

                    AndroidView(
                        factory = { viewContext ->

                            val previewView = PreviewView(viewContext)

                            val cameraProviderFuture =
                                ProcessCameraProvider.getInstance(viewContext)

                            cameraProviderFuture.addListener({

                                try {
                                    val cameraProvider =
                                        cameraProviderFuture.get()

                                    val preview = Preview.Builder()
                                        .build()
                                        .also {
                                            it.setSurfaceProvider(
                                                previewView.surfaceProvider
                                            )
                                        }

                                    val capture = ImageCapture.Builder()
                                        .build()

                                    imageCapture = capture

                                    val lifecycleOwner =
                                        context as LifecycleOwner

                                    cameraProvider.unbindAll()

                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        capture
                                    )

                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Camera error: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            }, ContextCompat.getMainExecutor(viewContext))

                            previewView
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(ReportNavy)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {

                            val capture = imageCapture

                            if (capture == null) {
                                Toast.makeText(
                                    context,
                                    "Camera is still starting. Please wait.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {

                                val photoFile = File(
                                    context.cacheDir,
                                    "RoadSafe_${System.currentTimeMillis()}.jpg"
                                )

                                val outputOptions =
                                    ImageCapture.OutputFileOptions.Builder(
                                        photoFile
                                    ).build()

                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {

                                        override fun onImageSaved(
                                            outputFileResults:
                                            ImageCapture.OutputFileResults
                                        ) {
                                            capturedPhoto = photoFile
                                            aiResult =
                                                "AI is analysing the road image..."

                                            scope.launch {
                                                try {
                                                    val result = withContext(
                                                        Dispatchers.IO
                                                    ) {
                                                        val bitmap =
                                                            BitmapFactory.decodeFile(
                                                                photoFile.absolutePath
                                                            )

                                                        if (bitmap != null) {
                                                            try {
                                                                roadAI.classify(bitmap)
                                                            } finally {
                                                                bitmap.recycle()
                                                            }
                                                        } else {
                                                            "Unable to read captured image."
                                                        }
                                                    }

                                                    aiResult = result

                                                    Toast.makeText(
                                                        context,
                                                        "Photo captured and AI analysis completed!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                } catch (e: Exception) {
                                                    aiResult =
                                                        "AI analysis failed: ${e.message}"
                                                }
                                            }
                                        }

                                        override fun onError(
                                            exception: ImageCaptureException
                                        ) {
                                            Toast.makeText(
                                                context,
                                                "Capture failed: ${exception.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReportOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "CAPTURE PHOTO",
                            fontWeight = FontWeight.Bold
                        )
                    }

                } else {

                    Text(
                        text = "Camera permission is needed to capture road evidence.",
                        color = ReportDark,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                Manifest.permission.CAMERA
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReportNavy,
                            contentColor = Color.White
                        )
                    ) {
                        Text("OPEN CAMERA")
                    }
                }

                capturedPhoto?.let { photo ->
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "✓ Photo saved: ${photo.name}",
                        color = ReportSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (capturedPhoto != null) {
                    Spacer(modifier = Modifier.height(15.dp))

                    Text(
                        text = "ROADSAFE AI ANALYSIS",
                        color = ReportOrange,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = aiResult,
                        color = ReportNavy,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Note: This is general image classification, not confirmed road-incident detection.",
                        color = ReportMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "04  REPORT TIMESTAMP",
            color = ReportNavy,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = ReportBorder
        )

        OutlinedTextField(
            value = reportTime,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
        )

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {
                if (isSaving) return@Button

                isSaving = true

                scope.launch {
                    try {
                        val savedId = database.incidentDao().insertIncident(
                            Incident(
                                category = category,
                                description = description,
                                latitude = latitude,
                                longitude = longitude,
                                photoPath = capturedPhoto?.absolutePath,
                                timestamp = reportTime,
                                aiResult = if (capturedPhoto != null) {
                                    aiResult
                                } else {
                                    null
                                }
                            )
                        )

                        IncidentSyncWorker.schedule(context)

                        Toast.makeText(
                            context,
                            "Incident saved offline! ID: $savedId",
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Could not save incident: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        isSaving = false
                    }
                }
            },
            enabled = category.isNotBlank() &&
                    description.isNotBlank() &&
                    !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ReportOrange,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFB8BEC5),
                disabledContentColor = Color(0xFF263746)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isSaving) {
                    "SAVING REPORT..."
                } else {
                    "SUBMIT INCIDENT REPORT"
                },
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "* Required fields",
            color = ReportMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
