package com.example.sipa.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sipa.ui.theme.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.items
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// Modelo de datos para una luz
data class Luz(
    val id: String = "",
    val nombre: String = "",
    val habitacion: String = "",
    val encendida: Boolean = false,
    val intensidad: Int = 0,
    val horaApagado: String = ""
)

@Composable
fun HomeScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    // ⚠️ Cambia esto por el ID real de la casa. Por ahora usamos "4826" como ejemplo.
    val houseId = "4826"

    // Estado que se actualizará con el listener de Firestore (Etapa 3)
    var luces by remember { mutableStateOf<List<Luz>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    // --- ETAPA 3: ESCUCHAR CAMBIOS EN TIEMPO REAL ---
    // Este bloque se ejecuta una vez al entrar a la pantalla y se mantiene escuchando
    LaunchedEffect(houseId) {
        db.collection("houses").document(houseId)
            .collection("lights")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    cargando = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    luces = snapshot.documents.map { doc ->
                        Luz(
                            id = doc.id,
                            nombre = doc.getString("name") ?: "",
                            habitacion = doc.getString("room") ?: "",
                            encendida = doc.getBoolean("isOn") ?: false,
                            intensidad = doc.getLong("brightness")?.toInt() ?: 0,
                            horaApagado = doc.getString("scheduleOff") ?: ""
                        )
                    }
                    cargando = false
                }
            }
    }

    // --- ETAPA 4: FUNCIÓN PARA ESCRIBIR CAMBIOS + HISTORIAL ---
    // Esta función se llamará cuando el usuario toque el switch de una luz
    fun toggleLuz(luz: Luz) {
        val nuevoEstado = !luz.encendida
        val houseRef = db.collection("houses").document(houseId)
        val lightRef = houseRef.collection("lights").document(luz.id)

        // Usamos un batch para actualizar la luz Y guardar el historial al mismo tiempo
        db.runBatch { batch ->
            // 1. Actualizar el estado de la luz
            batch.update(lightRef, mapOf(
                "isOn" to nuevoEstado,
                "lastUpdated" to FieldValue.serverTimestamp(),
                "updatedBy" to auth.currentUser?.uid
            ))

            // 2. Añadir un documento al historial
            val historyRef = houseRef.collection("history").document()
            batch.set(historyRef, mapOf(
                "lightId" to luz.id,
                "action" to if (nuevoEstado) "encendido" else "apagado",
                "oldValue" to luz.encendida,
                "newValue" to nuevoEstado,
                "timestamp" to FieldValue.serverTimestamp(),
                "performedBy" to auth.currentUser?.uid
            ))
        }.addOnFailureListener {
            // Aquí podrías mostrar un error al usuario
        }
    }

    // --- INTERFAZ DE USUARIO ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SipaBackground)
            .padding(16.dp)
    ) {
        // Header (sin cambios)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Mi casa", fontWeight = FontWeight.Bold, color = SipaTextDark)
                Text("Madrid · Europe/Madrid · UTC+02:00", color = SipaTextLight, fontSize = 12.sp)
            }
            Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = SipaTextDark)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val encendidas = luces.count { it.encendida }
        Text("Buenas noches", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
        Text("$encendidas luces encendidas", color = SipaGold, fontWeight = FontWeight.Bold)
        Text("${luces.size - encendidas} luces apagadas", color = SipaTextLight, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Grid de luces (ahora usa la lista de Firestore)
        if (cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SipaGold)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(luces) { luz ->
                    LightCard(
                        luz = luz,
                        onToggle = { toggleLuz(luz) },
                        onClick = { navController.navigate("edit_schedule") }
                    )
                }
            }
        }

        // Bottom Navigation (sin cambios)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Home, contentDescription = "Mi casa", tint = SipaGold)
                Text("Mi casa", fontSize = 12.sp, color = SipaGold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DateRange, contentDescription = "Horarios", tint = SipaTextLight)
                Text("Horarios", fontSize = 12.sp, color = SipaTextLight)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = SipaTextLight)
                Text("Ajustes", fontSize = 12.sp, color = SipaTextLight)
            }
        }
    }
}

@Composable
fun LightCard(
    luz: Luz,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (luz.encendida) SipaGoldLight else SipaWhite
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = if (luz.encendida) SipaGold else SipaTextLight
                )
                // Switch para encender/apagar (esto dispara la Etapa 4)
                Switch(
                    checked = luz.encendida,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SipaGold,
                        checkedTrackColor = SipaGoldLight
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(luz.nombre, fontWeight = FontWeight.Bold, color = SipaTextDark, fontSize = 14.sp)
            Text(luz.habitacion, color = SipaTextLight, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (luz.encendida) "${luz.intensidad} %" else "Apagada",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (luz.encendida) SipaTextDark else SipaTextLight
            )
            LinearProgressIndicator(
                progress = { if (luz.encendida) luz.intensidad / 100f else 0f },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = SipaGold,
                trackColor = SipaWhite
            )
            Text(luz.horaApagado, fontSize = 12.sp, color = SipaTextLight)
        }
    }
}