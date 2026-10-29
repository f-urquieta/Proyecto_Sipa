package com.example.sipa.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    // Inicializamos Firebase
    val auth = Firebase.auth
    val db = Firebase.firestore
    val context = LocalContext.current

    // Estados para los campos
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SipaBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SIPA", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = SipaGold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("QUÉ BUENO TENERTE AQUÍ", fontSize = 12.sp, color = SipaTextLight)

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            "Iniciar sesión",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SipaTextDark,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            "Vuelve a tus luces y horarios.",
            color = SipaTextLight,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "¿Olvidaste tu contraseña?",
            color = SipaGold,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (correo.isBlank() || contrasena.isBlank()) {
                    Toast.makeText(context, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isLoading = true
                auth.signInWithEmailAndPassword(correo, contrasena)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid
                            if (uid != null) {
                                // ✅ Consultamos el documento del usuario en Firestore
                                db.collection("users").document(uid).get()
                                    .addOnSuccessListener { document ->
                                        isLoading = false
                                        if (document.exists() && document.contains("houseId")) {
                                            // El usuario ya tiene casa → ir al Home
                                            navController.navigate("home") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } else {
                                            // El usuario no tiene casa → ir a crear/unirse a casa
                                            navController.navigate("houses") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Error al verificar casa: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Error: UID no disponible", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            isLoading = false
                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SipaGold),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = SipaWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("Iniciar sesión", color = SipaWhite)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.navigate("register") }) {
            Text("¿Aún no tienes cuenta? Crear cuenta", color = SipaTextDark)
        }
    }
}