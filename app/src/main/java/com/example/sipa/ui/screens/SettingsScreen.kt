package com.example.sipa.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sipa.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SettingsScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Estados para guardar la info del usuario y la casa
    var nombreUsuario by remember { mutableStateOf("Cargando...") }
    var correoUsuario by remember { mutableStateOf("") }
    var nombreCasa by remember { mutableStateOf("Cargando...") }
    var codigoCasa by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }

    // Obtener datos al entrar a la pantalla
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        correoUsuario = auth.currentUser?.email ?: "Sin correo"

        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc.exists()) {
                        nombreUsuario = userDoc.getString("nombre") ?: "Usuario SIPA"
                        val houseId = userDoc.getString("houseId")

                        if (houseId != null) {
                            codigoCasa = houseId
                            // Obtener el nombre de la casa
                            db.collection("houses").document(houseId).get()
                                .addOnSuccessListener { houseDoc ->
                                    if (houseDoc.exists()) {
                                        nombreCasa = houseDoc.getString("name") ?: "Mi casa"
                                        // 🔥 NUEVO: Leemos el código corto
                                        codigoCasa = houseDoc.getString("shortCode") ?: houseId
                                    }
                                    cargando = false
                                }
                                .addOnFailureListener { cargando = false }
                        } else {
                            nombreCasa = "Sin casa asignada"
                            codigoCasa = "N/A"
                            cargando = false
                        }
                    } else {
                        cargando = false
                    }
                }
                .addOnFailureListener { cargando = false }
        }
    }

    // Función para cerrar sesión
    fun cerrarSesion() {
        auth.signOut()
        navController.navigate("login") {
            popUpTo(0) { inclusive = true } // Limpia todo el backstack
        }
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
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = SipaTextDark)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("AJUSTES", fontSize = 12.sp, color = SipaTextLight)
                Text("Tu cuenta y tu casa", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SECCIÓN USUARIO ---
        Text("PERFIL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SipaGold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = SipaWhite),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = SipaGold, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(nombreUsuario, fontWeight = FontWeight.Bold, color = SipaTextDark, fontSize = 16.sp)
                    Text(correoUsuario, color = SipaTextLight, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN CASA ---
        Text("MI CASA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SipaGold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = SipaWhite),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = SipaGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(nombreCasa, fontWeight = FontWeight.Bold, color = SipaTextDark, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = SipaBackground)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Código para invitar a otros", fontSize = 12.sp, color = SipaTextLight)
                Spacer(modifier = Modifier.height(4.dp))

                // Caja para copiar el código
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SipaBackground, RoundedCornerShape(8.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(codigoCasa))
                            Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(codigoCasa, fontWeight = FontWeight.Bold, color = SipaTextDark, fontSize = 14.sp)
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = SipaGold, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // --- BOTÓN CERRAR SESIÓN ---
        Button(
            onClick = { cerrarSesion() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SipaWhite,
                contentColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar sesión", fontWeight = FontWeight.Bold)
        }
    }
}