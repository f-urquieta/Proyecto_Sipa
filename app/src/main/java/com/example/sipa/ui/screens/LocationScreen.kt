package com.example.sipa.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.sipa.ui.theme.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// 🔥 NUEVO: Imports de Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LocationScreen(
    navController: NavController,
    nombreCasa: String = "Mi casa" // 🔥 NUEVO: Recibimos el nombre de la casa
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()

    // 🔥 NUEVO: Instancias de Firebase
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Estados
    var ciudad by remember { mutableStateOf("Madrid") }
    var zonaHoraria by remember { mutableStateOf("Europe/Madrid") }
    var horaActual by remember { mutableStateOf("21:41") }
    var offsetUtc by remember { mutableStateOf("UTC+02:00") }
    var horaAmanecer by remember { mutableStateOf("--:--") }
    var horaAtardecer by remember { mutableStateOf("--:--") }
    var cargandoUbicacion by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // 🔥 NUEVO: Estado para saber si estamos guardando en Firestore
    var guardandoCasa by remember { mutableStateOf(false) }

    // Lanzador de permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            obtenerUbicacionYActualizar(
                fusedLocationClient = fusedLocationClient,
                context = context,
                onSuccess = { lat, lon ->
                    cargandoUbicacion = true
                    scope.launch {
                        try {
                            val tzData = obtenerZonaHorariaRtz(lat, lon)
                            zonaHoraria = tzData.first
                            horaActual = tzData.second

                            val tz = TimeZone.getTimeZone(zonaHoraria)
                            val offset = tz.getOffset(System.currentTimeMillis()) / (1000 * 60 * 60)
                            offsetUtc = String.format("UTC%+03d:00", offset)

                            val solData = obtenerHorarioSol(lat, lon, zonaHoraria)
                            horaAmanecer = solData.first
                            horaAtardecer = solData.second

                            error = null
                        } catch (e: Exception) {
                            Log.e("LOCATION_DEBUG", "Error: ${e.message}", e)
                            error = "Error al obtener datos: ${e.message}"
                        } finally {
                            cargandoUbicacion = false
                        }
                    }
                },
                onFailure = {
                    cargandoUbicacion = false
                    Toast.makeText(context, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SipaBackground)
            .padding(24.dp)
    ) {
        Text("La luz sigue tu ritmo.", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
        Text("Sincroniza tu hogar con la hora local.", fontSize = 12.sp, color = SipaTextLight)

        Spacer(modifier = Modifier.height(32.dp))
        Text("UBICACIÓN", color = SipaGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("¿Dónde está tu casa?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (hasFineLocation || hasCoarseLocation) {
                    obtenerUbicacionYActualizar(
                        fusedLocationClient = fusedLocationClient,
                        context = context,
                        onSuccess = { lat, lon ->
                            cargandoUbicacion = true
                            scope.launch {
                                try {
                                    val tzData = obtenerZonaHorariaRtz(lat, lon)
                                    zonaHoraria = tzData.first
                                    horaActual = tzData.second

                                    val tz = TimeZone.getTimeZone(zonaHoraria)
                                    val offset = tz.getOffset(System.currentTimeMillis()) / (1000 * 60 * 60)
                                    offsetUtc = String.format("UTC%+03d:00", offset)

                                    val solData = obtenerHorarioSol(lat, lon, zonaHoraria)
                                    horaAmanecer = solData.first
                                    horaAtardecer = solData.second

                                    error = null
                                } catch (e: Exception) {
                                    error = "Error: ${e.message}"
                                } finally {
                                    cargandoUbicacion = false
                                }
                            }
                        },
                        onFailure = {
                            cargandoUbicacion = false
                            Toast.makeText(context, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SipaGold),
            enabled = !cargandoUbicacion
        ) {
            if (cargandoUbicacion) {
                CircularProgressIndicator(color = SipaWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("📍 Usar mi ubicación actual", color = SipaWhite)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SipaGoldLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Zona horaria detectada", fontWeight = FontWeight.Bold, color = SipaTextDark, fontSize = 12.sp)
                Text(zonaHoraria, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
                Text("$offsetUtc · Hora local", color = SipaTextLight, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(horaActual, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = SipaGold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SipaWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Horario solar", fontWeight = FontWeight.Bold, color = SipaTextDark)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("🌅 Amanecer", fontSize = 12.sp, color = SipaTextLight)
                        Text(horaAmanecer, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
                    }
                    Column {
                        Text("🌇 Atardecer", fontSize = 12.sp, color = SipaTextLight)
                        Text(horaAtardecer, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
                    }
                }
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 🔥 NUEVO: Botón modificado para guardar en Firestore
        Button(
            onClick = {
                val user = auth.currentUser
                if (user == null) {
                    Toast.makeText(context, "Error: No hay usuario logueado", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                guardandoCasa = true
                val houseId = db.collection("houses").document().id

                val houseData = hashMapOf(
                    "name" to nombreCasa,
                    "ownerUid" to user.uid,
                    "location" to ciudad,
                    "timezone" to zonaHoraria,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "members" to listOf(user.uid)
                )

                db.collection("houses").document(houseId)
                    .set(houseData)
                    .addOnSuccessListener {
                        // 🔥 NUEVO: Ahora actualizamos el documento del usuario con su houseId
                        db.collection("users").document(user.uid)
                            .update("houseId", houseId)
                            .addOnSuccessListener {
                                guardandoCasa = false
                                navController.navigate("add_lights") {
                                    popUpTo("houses") { inclusive = true }
                                }
                            }
                            .addOnFailureListener { e ->
                                guardandoCasa = false
                                Toast.makeText(context, "Casa creada, pero error al vincular usuario: ${e.message}", Toast.LENGTH_LONG).show()
                                // Aún así navegamos, porque la casa se creó
                                navController.navigate("add_lights") {
                                    popUpTo("houses") { inclusive = true }
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        guardandoCasa = false
                        Toast.makeText(context, "Error al guardar la casa: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SipaGold),
            enabled = !guardandoCasa // Deshabilitar mientras guarda
        ) {
            if (guardandoCasa) {
                CircularProgressIndicator(color = SipaWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("Confirmar ubicación", color = SipaWhite)
            }
        }
    }
}

// --- FUNCIONES AUXILIARES (sin cambios) ---

@SuppressLint("MissingPermission")
fun obtenerUbicacionYActualizar(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    context: android.content.Context,
    onSuccess: (Double, Double) -> Unit,
    onFailure: () -> Unit
) {
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            if (location != null) {
                onSuccess(location.latitude, location.longitude)
            } else {
                onFailure()
            }
        }
        .addOnFailureListener {
            onFailure()
        }
}

suspend fun obtenerZonaHorariaRtz(lat: Double, lng: Double): Pair<String, String> = withContext(Dispatchers.IO) {
    val url = "http://tz.twitchax.com/api/v1/osm/tz/$lng/$lat"
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Error HTTP RTZ: ${response.code}")
        val body = response.body?.string() ?: throw Exception("Respuesta vacía")

        val jsonArray = JSONArray(body)
        if (jsonArray.length() == 0) throw Exception("RTZ no devolvió datos")

        val json = jsonArray.getJSONObject(0)
        val timeZoneId = json.getString("identifier")
        val currentTimeIso = json.getString("currentTime")

        val horaLocal = currentTimeIso.substringAfter("T").take(5)

        Pair(timeZoneId, horaLocal)
    }
}

suspend fun obtenerHorarioSol(lat: Double, lng: Double, zonaHorariaId: String): Pair<String, String> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val url = "https://api.sunrise-sunset.org/json?lat=$lat&lng=$lng&formatted=0"
    val request = Request.Builder().url(url).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Error HTTP Sol: ${response.code}")
        val body = response.body?.string() ?: throw Exception("Respuesta vacía")
        val json = JSONObject(body)

        val results = json.getJSONObject("results")
        val sunriseUtc = results.getString("sunrise")
        val sunsetUtc = results.getString("sunset")

        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")

        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone(zonaHorariaId)

        val fechaAmanecer = parser.parse(sunriseUtc)
        val fechaAtardecer = parser.parse(sunsetUtc)

        val horaAmanecerLocal = if (fechaAmanecer != null) formatter.format(fechaAmanecer) else "--:--"
        val horaAtardecerLocal = if (fechaAtardecer != null) formatter.format(fechaAtardecer) else "--:--"

        Pair(horaAmanecerLocal, horaAtardecerLocal)
    }
}