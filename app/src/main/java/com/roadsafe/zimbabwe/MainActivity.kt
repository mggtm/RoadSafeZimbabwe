package com.roadsafe.zimbabwe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.roadsafe.zimbabwe.ui.theme.RoadSafeZimbabweTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private val Navy = Color(0xFF10243A)
private val Orange = Color(0xFFFF8A35)
private val LightBackground = Color(0xFFF3F6FA)
private val MutedText = Color(0xFF718096)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RoadSafeZimbabweTheme {
                var currentScreen by remember {
                    mutableStateOf("home")
                }

                var supabaseStatus by remember {
                    mutableStateOf("Connecting to cloud...")
                }

                LaunchedEffect(Unit) {
                    try {
                        RoadSafeSupabase.signInAnonymously()
                        supabaseStatus = "Connected to RoadSafe cloud"
                    } catch (e: Exception) {
                        supabaseStatus = "Offline - reports saved on this device"
                    }
                }

                when (currentScreen) {

                    "report" -> ReportScreen(
                        onBack = { currentScreen = "home" }
                    )

                    "saved" -> SavedIncidentsScreen(
                        onBack = { currentScreen = "home" }
                    )

                    "map" -> IncidentMapScreen(
                        onBack = { currentScreen = "home" }
                    )

                    else -> RoadSafeHomeScreen(
                        onReportClick = { currentScreen = "report" },
                        onSavedClick = { currentScreen = "saved" },
                        onMapClick = { currentScreen = "map" },
                        supabaseStatus = supabaseStatus
                    )
                }
            }
        }
    }
}

@Composable
fun RoadSafeHomeScreen(
    onReportClick: () -> Unit,
    onSavedClick: () -> Unit,
    onMapClick: () -> Unit,
    supabaseStatus: String
) {
    val context = LocalContext.current

    val database = remember {
        RoadSafeDatabase.getDatabase(context)
    }

    val incidents by database.incidentDao()
        .getAllIncidents()
        .collectAsState(initial = emptyList())

    val reportedCount = incidents.size.toString()

    val verifiedCount = incidents.count {
        it.verificationStatus.equals("VERIFIED", ignoreCase = true)
    }.toString()

    val pendingCount = incidents.count {
        it.syncStatus.equals("PENDING", ignoreCase = true)
    }.toString()

    Scaffold(
        containerColor = LightBackground,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {

                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Text("⌂", fontSize = 22.sp) },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onMapClick,
                    icon = { Text("◎", fontSize = 22.sp) },
                    label = { Text("Map") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onSavedClick,
                    icon = { Text("▤", fontSize = 22.sp) },
                    label = { Text("My Reports") }
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            Text(
                text = "ROADSAFE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Orange,
                letterSpacing = 3.sp
            )

            Text(
                text = supabaseStatus,
                fontSize = 11.sp,
                color = MutedText
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Zimbabwe",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )

                    Text(
                        text = "Safer roads, safer communities.",
                        fontSize = 14.sp,
                        color = MutedText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Navy
                ) {
                    Text(
                        text = "RS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 12.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Navy)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {

                    Text(
                        text = "ROAD SAFETY STARTS WITH YOU",
                        color = Orange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "See an incident?\nReport it. Help save lives.",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Help authorities and communities stay informed about road incidents across Zimbabwe.",
                        color = Color(0xFFD4DFEA),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onReportClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange,
                            contentColor = Navy
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "+ REPORT AN INCIDENT",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            Text(
                text = "Incident Overview",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatisticCard(
                    title = "Reported",
                    value = reportedCount,
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = "Verified",
                    value = verifiedCount,
                    modifier = Modifier.weight(1f)
                )

                StatisticCard(
                    title = "Pending",
                    value = pendingCount,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(25.dp))

            Text(
                text = "How RoadSafe Works",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Spacer(modifier = Modifier.height(12.dp))

            InformationCard(
                number = "01",
                title = "Capture",
                description = "Photograph a road incident and record its location."
            )

            InformationCard(
                number = "02",
                title = "Analyse",
                description = "AI assists in analysing captured road images."
            )

            InformationCard(
                number = "03",
                title = "Verify",
                description = "Reports undergo verification before being marked as confirmed."
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "ROADSAFE ZIMBABWE • BUILDING SAFER ROADS",
                fontSize = 10.sp,
                color = MutedText,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun IncidentMapScreen(onBack: () -> Unit) {

    val context = LocalContext.current

    val database = remember {
        RoadSafeDatabase.getDatabase(context)
    }

    val incidents by database.incidentDao()
        .getAllIncidents()
        .collectAsState(initial = emptyList())

    val zimbabwe = LatLng(-19.0154, 29.1549)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(zimbabwe, 5.5f)
    }

    val locatedIncidents = incidents.filter {
        it.latitude != null && it.longitude != null &&
                it.latitude in -90.0..90.0 &&
                it.longitude in -180.0..180.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            TextButton(onClick = onBack) {
                Text("← Back", color = Navy)
            }

            Text(
                text = "Road Incident Map",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Text(
                text = "${locatedIncidents.size} incidents with GPS locations",
                color = MutedText,
                fontSize = 13.sp
            )
        }

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            cameraPositionState = cameraPositionState
        ) {

            locatedIncidents.forEach { incident ->

                Marker(
                    state = MarkerState(
                        position = LatLng(
                            incident.latitude!!,
                            incident.longitude!!
                        )
                    ),
                    title = "${incident.category} - Report #${incident.id}",
                    snippet = "${incident.description} | ${incident.verificationStatus}"
                )
            }
        }

        if (locatedIncidents.isEmpty()) {
            Text(
                text = "No incidents with GPS coordinates yet. Submit a report with location enabled to see it here.",
                color = MutedText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun SavedIncidentsScreen(onBack: () -> Unit) {

    val context = LocalContext.current

    val database = remember {
        RoadSafeDatabase.getDatabase(context)
    }

    val scope = rememberCoroutineScope()

    val incidents by database.incidentDao()
        .getAllIncidents()
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp)
    ) {

        TextButton(onClick = onBack) {
            Text("← Back", color = Navy)
        }

        Text(
            text = "Saved Incidents",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "${incidents.size} reports saved on this device",
            color = MutedText
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (incidents.isEmpty()) {

            Text(
                text = "No incidents saved yet.",
                color = MutedText
            )

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(incidents, key = { it.id }) { incident ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {

                        Column(modifier = Modifier.padding(16.dp)) {

                            Text(
                                text = incident.category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Report ID: ${incident.id}")
                            Text("Description: ${incident.description}")
                            Text("Latitude: ${incident.latitude ?: "Unavailable"}")
                            Text("Longitude: ${incident.longitude ?: "Unavailable"}")
                            Text("Time: ${incident.timestamp}")
                            Text("Sync status: ${incident.syncStatus}")

                            Spacer(modifier = Modifier.height(10.dp))

                            if (incident.photoPath != null) {
                                Text(
                                    text = "Photo: Attached",
                                    color = Color(0xFF218838)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "ROADSAFE AI ANALYSIS",
                                color = Orange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = incident.aiResult
                                    ?: "No AI analysis available for this report.",
                                color = Navy,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = "AI classification is informational and does not confirm a road incident.",
                                color = MutedText,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            if (
                                incident.verificationStatus.equals(
                                    "VERIFIED",
                                    ignoreCase = true
                                )
                            ) {

                                Surface(
                                    color = Color(0xFFE5F5E9),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "✓ VERIFIED (DEMO)",
                                        color = Color(0xFF218838),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }

                            } else {

                                Button(
                                    onClick = {
                                        scope.launch {
                                            database.incidentDao()
                                                .verifyIncident(incident.id)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Orange,
                                        contentColor = Navy
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "MARK AS VERIFIED",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = "Demo reviewer status only; this is not official authority verification.",
                                color = MutedText,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Text(
                text = value,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                color = MutedText
            )
        }
    }
}

@Composable
fun InformationCard(
    number: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFE9D8)
            ) {
                Text(
                    text = number,
                    color = Orange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {

                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MutedText,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
