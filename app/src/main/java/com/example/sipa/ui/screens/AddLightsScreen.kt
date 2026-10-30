package com.example.sipa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sipa.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import android.widget.Toast

data class Dispositivo(
    val id: String,
    val nombre: String,
    val ubicacion: String,
    val intensidadDefecto: Int
)

@Composable
fun AddLightsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    // ⚠️ Recuerda cambiar esto por el ID real de la casa
    val houseId = "4826"
    val MAX_LUCES = 6

    var buscando by remember { mutableStateOf(true) }
    var dispositivosEncontrados by remember { mutableStateOf<List<Dispositivo>>(emptyList()) }
    var seleccionados by remember { mutableStateOf<Set<String>>(emptySet()) }
    var guardando by remember { mutableStateOf(false) }

    // ✨ Estado para el diálogo del PIN
    var mostrarDialogoPin by remember { mutableStateOf(false) }
    var pinIngresado by remember { mutableStateOf("") }

    // Simulamos la búsqueda de dispositivos WiFi
    LaunchedEffect(Unit) {
        delay(2000)
        dispositivosEncontrados = listOf(
            Dispositivo("lampara_aux", "Lámpara auxiliar", "Sala · señal excelente", 70),
            Dispositivo("luz_lectura", "Luz de lectura", "Dormitorio · señal buena", 50),
            Dispositivo("tira_led", "Tira LED", "Sala · señal excelente", 100),
            Dispositivo("bombillo_cocina", "Bombillo cocina", "Cocina · señal regular", 80),
            Dispositivo("lampara_pie", "Lámpara de pie", "Sala · señal buena", 65),
            Dispositivo("luz_nocturna", "Luz nocturna", "Dormitorio · señal excelente", 30)
        )
        buscando = false
    }

    // ✨ Función que se ejecuta cuando el usuario confirma el PIN
    fun guardarLucesConPin(pin: String) {
        guardando = true
        val uid = auth.currentUser?.uid
        val houseRef = db.collection("houses").document(houseId)

        // Primero consultamos el PIN de la casa
        houseRef.get().addOnSuccessListener { document ->
            val pinCorrecto = document.getString("accessPin")

            if (pin == pinCorrecto) {
                // PIN correcto: guardamos las luces
                val lucesAGuardar = dispositivosEncontrados.filter { seleccionados.contains(it.id) }

                db.runBatch { batch ->
                    lucesAGuardar.forEach { luz ->
                        val lightRef = houseRef.collection("lights").document(luz.id)
                        batch.set(lightRef, mapOf(
                            "name" to luz.nombre,
                            "room" to luz.ubicacion.substringBefore(" ·"),
                            "isOn" to false,
                            "brightness" to luz.intensidadDefecto,
                            "scheduleOff" to "23:30",
                            "addedAt" to FieldValue.serverTimestamp()
                        ))

                        val historyRef = houseRef.collection("history").document()
                        batch.set(historyRef, mapOf(
                            "action" to "luz_vinculada",
                            "lightName" to luz.nombre,
                            "oldValue" to "",
                            "newValue" to luz.nombre,
                            "valueType" to "string",
                            "timestamp" to FieldValue.serverTimestamp(),
                            "performedBy" to uid
                        ))
                    }
                }.addOnSuccessListener {
                    guardando = false
                    mostrarDialogoPin = false
                    pinIngresado = ""
                    Toast.makeText(context, "Luces conectadas con éxito", Toast.LENGTH_SHORT).show()
                    navController.navigate("home") {
                        popUpTo("add_lights") { inclusive = true }
                    }
                }.addOnFailureListener { e ->
                    guardando = false
                    Toast.makeText(context, "Error al conectar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                // PIN incorrecto
                guardando = false
                Toast.makeText(context, "PIN incorrecto. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener { e ->
            guardando = false
            Toast.makeText(context, "Error al verificar PIN: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // --- DIÁLOGO DEL PIN ---
    if (mostrarDialogoPin) {
        AlertDialog(
            onDismissRequest = {
                if (!guardando) {
                    mostrarDialogoPin = false
                    pinIngresado = ""
                }
            },
            title = {
                Text("Verificación de seguridad", fontWeight = FontWeight.Bold, color = SipaTextDark)
            },
            text = {
                Column {
                    Text(
                        "Ingresa el PIN de 4 dígitos de la casa para agregar las luces.",
                        fontSize = 14.sp,
                        color = SipaTextLight
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinIngresado,
                        onValueChange = {
                            // Solo permitimos hasta 4 dígitos
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinIngresado = it
                            }
                        },
                        label = { Text("PIN de la casa") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { guardarLucesConPin(pinIngresado) },
                    enabled = pinIngresado.length == 4 && !guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = SipaGold)
                ) {
                    if (guardando) {
                        CircularProgressIndicator(
                            color = SipaWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Confirmar", color = SipaWhite)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoPin = false
                        pinIngresado = ""
                    },
                    enabled = !guardando
                ) {
                    Text("Cancelar", color = SipaTextLight)
                }
            }
        )
    }

    // --- INTERFAZ PRINCIPAL ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SipaBackground)
            .padding(24.dp)
    ) {
        Text("MI CASA · NUEVAS LUCES", fontSize = 12.sp, color = SipaTextLight)
        Text("Más luz en casa.", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
        Text("Busca luces cercanas y elige cuáles quieres añadir.", fontSize = 12.sp, color = SipaTextLight)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Buscar por nombre o modelo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (buscando) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = SipaGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Buscando dispositivos en la red WiFi...", color = SipaTextLight, fontSize = 14.sp)
            }
        } else {
            Text(
                "${dispositivosEncontrados.size} luces encontradas (Máx. $MAX_LUCES)",
                fontWeight = FontWeight.Bold,
                color = SipaTextDark
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(dispositivosEncontrados) { disp ->
                val isSelected = seleccionados.contains(disp.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            if (isSelected) {
                                seleccionados = seleccionados - disp.id
                            } else {
                                if (seleccionados.size < MAX_LUCES) {
                                    seleccionados = seleccionados + disp.id
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Solo puedes seleccionar hasta $MAX_LUCES luces",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) SipaGoldLight else SipaWhite
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = if (isSelected) SipaGold else SipaTextLight
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(disp.nombre, fontWeight = FontWeight.Bold, color = SipaTextDark)
                            Text(disp.ubicacion, color = SipaTextLight, fontSize = 12.sp)
                        }
                        if (isSelected) {
                            Checkbox(
                                checked = true,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = SipaGold)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (seleccionados.isEmpty()) {
                    Toast.makeText(context, "Selecciona al menos una luz", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                // ✨ En lugar de guardar directamente, mostramos el diálogo del PIN
                mostrarDialogoPin = true
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SipaGold),
            enabled = !guardando && !buscando
        ) {
            val textoBoton = if (seleccionados.isEmpty()) {
                "+ Conectar luces a Mi casa"
            } else {
                "+ Conectar ${seleccionados.size} luces a Mi casa"
            }
            Text(textoBoton, color = SipaWhite)
        }
    }
}