package com.example.sipa.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HousesScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var nombreCasa by remember { mutableStateOf("Mi nueva casa") }
    var codigoInvitacion by remember { mutableStateOf("") }

    // 🔥 Nuevos estados para manejar la carga
    var uniendoCasa by remember { mutableStateOf(false) }
    var creandoCasa by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SipaBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("TUS CASAS", fontSize = 12.sp, color = SipaTextLight)
        Text("Un lugar para tu luz.", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
        Text("Crea tu propia casa o pide acceso a una que ya exista.", fontSize = 12.sp, color = SipaTextLight)

        Spacer(modifier = Modifier.height(24.dp))

        // --- TARJETA CREAR CASA ---
        Card(
            colors = CardDefaults.cardColors(containerColor = SipaWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Home, contentDescription = null, tint = SipaGold)
                Text("Crear una casa", fontWeight = FontWeight.Bold, color = SipaTextDark)
                Text("Serás la dueña", color = SipaTextLight, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nombreCasa,
                    onValueChange = { nombreCasa = it },
                    label = { Text("Nombre de la casa") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { navController.navigate("location/$nombreCasa") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SipaGold),
                    enabled = !creandoCasa && !uniendoCasa
                ) {
                    Text("Crear casa", color = SipaWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- TARJETA UNIRME A CASA ---
        Card(
            colors = CardDefaults.cardColors(containerColor = SipaWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Key, contentDescription = null, tint = SipaGold)
                Text("Unirme a una casa", fontWeight = FontWeight.Bold, color = SipaTextDark)
                Text("Ingresa el código compartido por el dueño", color = SipaTextLight, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = codigoInvitacion,
                    onValueChange = { codigoInvitacion = it },
                    label = { Text("Código de la casa") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (codigoInvitacion.isBlank()) {
                            Toast.makeText(context, "Ingresa un código válido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val uid = auth.currentUser?.uid
                        if (uid == null) return@Button

                        uniendoCasa = true

                        // 🔥 NUEVO: Buscamos la casa por el shortCode
                        db.collection("houses")
                            .whereEqualTo("shortCode", codigoInvitacion.trim())
                            .limit(1)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    val document = querySnapshot.documents[0]
                                    val realHouseId = document.id // Este es el ID real del documento

                                    // 1. Añadir el UID al array "members" de la casa
                                    db.collection("houses").document(realHouseId)
                                        .update("members", FieldValue.arrayUnion(uid))
                                        .addOnSuccessListener {
                                            // 2. Actualizar el documento del usuario con el nuevo houseId
                                            db.collection("users").document(uid)
                                                .update("houseId", realHouseId)
                                                .addOnSuccessListener {
                                                    // 3. Registrar en el historial
                                                    val historyRef = db.collection("houses").document(realHouseId)
                                                        .collection("history").document()
                                                    historyRef.set(mapOf(
                                                        "action" to "usuario_unido",
                                                        "newValue" to (auth.currentUser?.email ?: "Nuevo miembro"),
                                                        "timestamp" to FieldValue.serverTimestamp(),
                                                        "performedBy" to uid
                                                    ))

                                                    uniendoCasa = false
                                                    Toast.makeText(context, "¡Te has unido a la casa!", Toast.LENGTH_SHORT).show()

                                                    navController.navigate("home") {
                                                        popUpTo("houses") { inclusive = true }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    uniendoCasa = false
                                                    Toast.makeText(context, "Error al actualizar perfil: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            uniendoCasa = false
                                            Toast.makeText(context, "Error al unirse: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                } else {
                                    uniendoCasa = false
                                    Toast.makeText(context, "Código incorrecto. Esa casa no existe.", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                uniendoCasa = false
                                Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    },
                    // ... resto del botón
                ) {
                    if (uniendoCasa) {
                        CircularProgressIndicator(color = SipaWhite, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Enviar solicitud de acceso", color = SipaWhite)
                    }
                }
            }
        }
    }
}