package com.example.sipa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sipa.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(navController: NavController, lightId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    // Estados para los campos
    var nombreLuz by remember { mutableStateOf("Cargando...") }
    var habitacion by remember { mutableStateOf("") }
    var horaEncendido by remember { mutableStateOf("19:00") }
    var horaApagado by remember { mutableStateOf("23:30") }
    var intensidad by remember { mutableStateOf(70) }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }

    // 🔥 NUEVO: Estado para los días de la semana seleccionados
    // Por defecto, todos los días están seleccionados
    var diasSeleccionados by remember { mutableStateOf(listOf("L", "M", "X", "J", "V", "S", "D")) }
    val todosLosDias = listOf("L", "M", "X", "J", "V", "S", "D")

    // Estados para el TimePicker
    var showTimePicker by remember { mutableStateOf(false) }
    var editandoEncendido by remember { mutableStateOf(true) }
    val timePickerState = rememberTimePickerState(initialHour = 19, initialMinute = 0, is24Hour = true)

    var houseId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    houseId = document.getString("houseId")
                }
        }
    }

    // Cargar los datos de la luz desde Firestore
    LaunchedEffect(houseId, lightId) {
        if (houseId != null && lightId.isNotEmpty()) {
            db.collection("houses").document(houseId!!)
                .collection("lights").document(lightId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        nombreLuz = doc.getString("name") ?: "Luz"
                        habitacion = doc.getString("room") ?: ""
                        horaEncendido = doc.getString("scheduleOn") ?: "19:00"
                        horaApagado = doc.getString("scheduleOff") ?: "23:30"
                        intensidad = when (val valor = doc.get("brightness")) {
                            is Long -> valor.toInt()
                            is Int -> valor
                            is String -> valor.toIntOrNull() ?: 70
                            else -> 70
                        }
                        // 🔥 NUEVO: Leer los días guardados en Firestore
                        val diasGuardados = doc.get("repeatDays") as? List<*>
                        if (diasGuardados != null) {
                            diasSeleccionados = diasGuardados.mapNotNull { it as? String }
                        }
                    }
                    cargando = false
                }
                .addOnFailureListener { cargando = false }
        }
    }

    // Diálogo del TimePicker
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    val horaSeleccionada = "$hour:$minute"

                    if (editandoEncendido) horaEncendido = horaSeleccionada
                    else horaApagado = horaSeleccionada

                    showTimePicker = false
                }) {
                    Text("Aceptar", color = SipaGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar", color = SipaTextLight)
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (cargando) {
        Box(modifier = Modifier.fillMaxSize().background(SipaBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SipaGold)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SipaBackground)
            .padding(24.dp)
    ) {
        Text("EDITAR · PROGRAMACIÓN ACTIVA", fontSize = 12.sp, color = SipaTextLight)
        Text("Tu horario, a tu manera.", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
        Text("Toca una hora para cambiar el encendido o el apagado.", fontSize = 12.sp, color = SipaTextLight)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = SipaGold)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(nombreLuz, fontWeight = FontWeight.Bold, color = SipaTextDark)
                Text(habitacion, color = SipaTextLight, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjetas de Encendido / Apagado
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Card(
                modifier = Modifier.weight(1f).padding(end = 8.dp).clickable {
                    editandoEncendido = true
                    val partes = horaEncendido.split(":")
                    timePickerState.hour = partes.getOrNull(0)?.toIntOrNull() ?: 19
                    timePickerState.minute = partes.getOrNull(1)?.toIntOrNull() ?: 0
                    showTimePicker = true
                },
                colors = CardDefaults.cardColors(containerColor = SipaGoldLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("☀️ Encender", fontWeight = FontWeight.Bold, color = SipaTextDark)
                    Text(horaEncendido, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
                    Text("Toca para cambiar", fontSize = 12.sp, color = SipaTextLight)
                }
            }
            Card(
                modifier = Modifier.weight(1f).padding(start = 8.dp).clickable {
                    editandoEncendido = false
                    val partes = horaApagado.split(":")
                    timePickerState.hour = partes.getOrNull(0)?.toIntOrNull() ?: 23
                    timePickerState.minute = partes.getOrNull(1)?.toIntOrNull() ?: 30
                    showTimePicker = true
                },
                colors = CardDefaults.cardColors(containerColor = SipaWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🌙 Apagar", fontWeight = FontWeight.Bold, color = SipaTextDark)
                    Text(horaApagado, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
                    Text("Toca para cambiar", fontSize = 12.sp, color = SipaTextLight)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Repetir", fontWeight = FontWeight.Bold, color = SipaTextDark)
        Spacer(modifier = Modifier.height(8.dp))

        // 🔥 NUEVO: Días seleccionables
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            todosLosDias.forEach { dia ->
                val isSelected = diasSeleccionados.contains(dia)
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) SipaGold else SipaGoldLight,
                            shape = CircleShape
                        )
                        .clickable {
                            // Lógica para añadir o quitar el día de la lista
                            diasSeleccionados = if (isSelected) {
                                diasSeleccionados - dia
                            } else {
                                diasSeleccionados + dia
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        dia,
                        color = if (isSelected) SipaWhite else SipaGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Tu intensidad actual", fontWeight = FontWeight.Bold, color = SipaTextDark)
        Text("$intensidad % al encender", fontSize = 12.sp, color = SipaTextLight)

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text("Cancelar", color = SipaTextLight)
            }
            Button(
                onClick = {
                    val currentHouseId = houseId
                    if (currentHouseId == null) return@Button
                    guardando = true

                    val houseRef = db.collection("houses").document(currentHouseId)
                    val lightRef = houseRef.collection("lights").document(lightId)

                    db.runBatch { batch ->
                        batch.update(lightRef, mapOf(
                            "scheduleOn" to horaEncendido,
                            "scheduleOff" to horaApagado,
                            "repeatDays" to diasSeleccionados, // 🔥 NUEVO: Guardamos los días
                            "lastUpdated" to FieldValue.serverTimestamp(),
                            "updatedBy" to auth.currentUser?.uid
                        ))

                        val historyRef = houseRef.collection("history").document()
                        batch.set(historyRef, mapOf(
                            "lightId" to lightId,
                            "action" to "horario_modificado",
                            "newValue" to "Encendido: $horaEncendido, Apagado: $horaApagado, Días: ${diasSeleccionados.joinToString(",")}",
                            "timestamp" to FieldValue.serverTimestamp(),
                            "performedBy" to auth.currentUser?.uid
                        ))
                    }.addOnSuccessListener {
                        guardando = false
                        navController.popBackStack()
                    }.addOnFailureListener {
                        guardando = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SipaGold),
                enabled = !guardando && diasSeleccionados.isNotEmpty() // No permitir guardar si no hay días
            ) {
                if (guardando) {
                    CircularProgressIndicator(color = SipaWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text("Guardar cambios", color = SipaWhite)
                }
            }
        }
    }
}