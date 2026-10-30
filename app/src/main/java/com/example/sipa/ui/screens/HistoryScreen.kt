package com.example.sipa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sipa.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Modelo de datos actualizado
data class HistorialItem(
    val id: String = "",
    val accion: String = "",
    val lightId: String = "",
    val lightName: String = "", // 🔥 NUEVO
    val oldValue: String = "",
    val newValue: String = "",
    val timestamp: Date? = null,
    val performedBy: String = ""
)

@Composable
fun HistoryScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var houseId by remember { mutableStateOf<String?>(null) }
    var historial by remember { mutableStateOf<List<HistorialItem>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    houseId = document.getString("houseId")
                }
        }
    }

    LaunchedEffect(houseId) {
        if (houseId != null) {
            db.collection("houses").document(houseId!!)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        cargando = false
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        historial = snapshot.documents.map { doc ->
                            HistorialItem(
                                id = doc.id,
                                accion = doc.getString("action") ?: "Acción desconocida",
                                lightId = doc.getString("lightId") ?: "",
                                lightName = doc.getString("lightName") ?: "Luz", // Fallback por si es antiguo
                                // Convertimos cualquier valor a String de forma segura
                                oldValue = doc.get("oldValue")?.toString() ?: "",
                                newValue = doc.get("newValue")?.toString() ?: "",
                                timestamp = doc.getTimestamp("timestamp")?.toDate(),
                                performedBy = doc.getString("performedBy") ?: ""
                            )
                        }
                        cargando = false
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SipaBackground)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = SipaTextDark)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("HISTORIAL", fontSize = 12.sp, color = SipaTextLight)
                Text("Actividad de tu casa", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SipaGold)
            }
        } else if (historial.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = SipaTextLight, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Aún no hay actividad registrada.", color = SipaTextLight)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(historial) { item ->
                    HistoryCard(item)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: HistorialItem) {
    val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val fechaFormateada = item.timestamp?.let { formatter.format(it) } ?: "Fecha desconocida"

    // Traducimos la acción a texto amigable
    val textoAccion = when (item.accion) {
        "encendido" -> "${item.lightName} encendida"
        "apagado" -> "${item.lightName} apagada"
        "horario_modificado" -> "Horario de ${item.lightName} modificado"
        "luz_vinculada" -> "Nueva luz vinculada: ${item.newValue}"
        else -> item.accion.replace("_", " ").capitalize()
    }

    val icono = when (item.accion) {
        "encendido" -> "💡"
        "apagado" -> "🌙"
        "horario_modificado" -> "⏰"
        "luz_vinculada" -> "🔗"
        else -> "📝"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SipaWhite)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icono, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(textoAccion, fontWeight = FontWeight.Bold, color = SipaTextDark)
                Text(fechaFormateada, color = SipaTextLight, fontSize = 12.sp)

                // 🔥 Mostrar detalle solo si es útil y no es "true/false" o "Encendida/Apagada" redundante
                if (item.accion == "horario_modificado" && item.newValue.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.newValue, fontSize = 12.sp, color = SipaGold)
                }
            }
        }
    }
}