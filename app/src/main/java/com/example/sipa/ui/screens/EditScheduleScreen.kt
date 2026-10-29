package com.example.sipa.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sipa.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val auth = Firebase.auth

    // ⚠️ Datos simulados: en producción deberías recibir el ID de la luz y la casa
    val houseId = "4826"
    val lightId = "lampara_aux"
    val lightName = "Lámpara de pie"

    // Estados de la UI
    var horaEncendido by remember { mutableStateOf("19:00") }
    var horaApagado by remember { mutableStateOf("23:30") }
    var diasSeleccionados by remember { mutableStateOf(setOf("L", "M", "X", "J", "V", "S", "D")) }

    // Estado para la automatización (sensor de luz)
    var usarLuzNatural by remember { mutableStateOf(false) }
    var umbralLuz by remember { mutableStateOf("500") } // Lúmenes/Lux
    var accionLuz by remember { mutableStateOf("Apagar") } // Apagar o Encender
    var guardando by remember { mutableStateOf(false) }

    // Función para abrir el reloj y actualizar la hora
    fun mostrarTimePicker(horaActual: String, onHoraSeleccionada: (String) -> Unit) {
        val partes = horaActual.split(":")
        val hora = partes.getOrNull(0)?.toIntOrNull() ?: 12
        val minuto = partes.getOrNull(1)?.toIntOrNull() ?: 0

        val timePicker = TimePickerDialog(
            context,
            { _, h, m ->
                val horaFormateada = String.format("%02d:%02d", h, m)
                onHoraSeleccionada(horaFormateada)
            },
            hora,
            minuto,
            true // Formato 24h
        )
        timePicker.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SipaBackground)
            .padding(24.dp)
    ) {
        Text("EDITAR · PROGRAMACIÓN ACTIVA", fontSize = 12.sp, color = SipaTextLight)
        Text("Tu horario, a tu manera.", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
        Text("Configura los horarios y la automatización.", fontSize = 12.sp, color = SipaTextLight)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = SipaGold)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(lightName, fontWeight = FontWeight.Bold, color = SipaTextDark)
                Text("Sala · Mi casa", color = SipaTextLight, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjetas de Encendido / Apagado
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Card(
                modifier = Modifier.weight(1f).padding(end = 8.dp).clickable { mostrarTimePicker(horaEncendido) { horaEncendido = it } },
                colors = CardDefaults.cardColors(containerColor = SipaGoldLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("☀️ Encender", fontWeight = FontWeight.Bold, color = SipaTextDark)
                    Text(horaEncendido, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
                    Text("Toca para cambiar", fontSize = 12.sp, color = SipaTextLight)
                }
            }
            Card(
                modifier = Modifier.weight(1f).padding(start = 8.dp).clickable { mostrarTimePicker(horaApagado) { horaApagado = it } },
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

        // Selección de días
        Text("Repetir", fontWeight = FontWeight.Bold, color = SipaTextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { dia ->
                val isSelected = diasSeleccionados.contains(dia)
                Box(
                    modifier = Modifier
                        .background(if (isSelected) SipaGold else SipaGoldLight, shape = CircleShape)
                        .clickable {
                            diasSeleccionados = if (isSelected) diasSeleccionados - dia else diasSeleccionados + dia
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(dia, color = if (isSelected) SipaWhite else SipaGold, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- NUEVA SECCIÓN: AUTOMATIZACIÓN POR SENSOR DE LUZ ---
        Card(
            colors = CardDefaults.cardColors(containerColor = SipaWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Automatización (Luz Natural)", fontWeight = FontWeight.Bold, color = SipaTextDark)
                    Switch(
                        checked = usarLuzNatural,
                        onCheckedChange = { usarLuzNatural = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = SipaGold, checkedTrackColor = SipaGoldLight)
                    )
                }

                if (usarLuzNatural) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Si la luz natural es mayor a:", fontSize = 14.sp, color = SipaTextDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = umbralLuz,
                            onValueChange = { if (it.all { char -> char.isDigit() }) umbralLuz = it },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            label = { Text("Lux", fontSize = 10.sp) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("lx", fontSize = 14.sp, color = SipaTextDark)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Entonces:", fontSize = 14.sp, color = SipaTextDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { accionLuz = if (accionLuz == "Apagar") "Encender" else "Apagar" }) {
                            Text(accionLuz, color = SipaGold, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Se aplicará de $horaEncendido a $horaApagado, todos los días.", fontSize = 12.sp, color = SipaTextLight)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text("Cancelar", color = SipaTextLight)
            }
            Button(
                onClick = {
                    guardando = true
                    val uid = auth.currentUser?.uid
                    val houseRef = db.collection("houses").document(houseId)
                    val lightRef = houseRef.collection("lights").document(lightId)

                    // Guardamos en Firestore
                    db.runBatch { batch ->
                        // 1. Actualizar los datos de la luz
                        batch.update(lightRef, mapOf(
                            "scheduleOn" to horaEncendido,
                            "scheduleOff" to horaApagado,
                            "daysActive" to diasSeleccionados.toList(),
                            "useNaturalLight" to usarLuzNatural,
                            "lightThreshold" to (umbralLuz.toIntOrNull() ?: 500),
                            "thresholdAction" to accionLuz,
                            "lastUpdated" to FieldValue.serverTimestamp()
                        ))

                        // 2. Guardar en el historial
                        val historyRef = houseRef.collection("history").document()
                        batch.set(historyRef, mapOf(
                            "action" to "horario_modificado",
                            "lightId" to lightId,
                            "lightName" to lightName,
                            "timestamp" to FieldValue.serverTimestamp(),
                            "performedBy" to uid
                        ))
                    }.addOnSuccessListener {
                        guardando = false
                        Toast.makeText(context, "Configuración guardada", Toast.LENGTH_SHORT).show()
                        navController.navigate("home") {
                            popUpTo("edit_schedule") { inclusive = true }
                        }
                    }.addOnFailureListener { e ->
                        guardando = false
                        Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SipaGold),
                enabled = !guardando
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