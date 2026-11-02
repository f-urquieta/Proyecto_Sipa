package com.example.sipa.ui.screens

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
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    // 🔥 NUEVO: Estados para manejar los errores visuales
    var correoError by remember { mutableStateOf<String?>(null) }
    var contrasenaError by remember { mutableStateOf<String?>(null) }
    var mensajeErrorGeneral by remember { mutableStateOf<String?>(null) }

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

        Text("Iniciar sesión", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SipaTextDark, modifier = Modifier.align(Alignment.Start))
        Text("Vuelve a tus luces y horarios.", color = SipaTextLight, modifier = Modifier.align(Alignment.Start))

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                correoError = null // Limpiamos el error al escribir
                mensajeErrorGeneral = null
            },
            label = { Text("Correo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            isError = correoError != null, // 🔥 Pinta el borde de rojo
            supportingText = { if (correoError != null) Text(correoError!!) } // 🔥 Muestra el texto debajo
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = {
                contrasena = it
                contrasenaError = null
                mensajeErrorGeneral = null
            },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = contrasenaError != null,
            supportingText = { if (contrasenaError != null) Text(contrasenaError!!) }
        )

        // 🔥 NUEVO: Mensaje de error general (ej. contraseña incorrecta)
        if (mensajeErrorGeneral != null) {
            Text(
                text = mensajeErrorGeneral!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("¿Olvidaste tu contraseña?", color = SipaGold, fontSize = 12.sp, modifier = Modifier.align(Alignment.End))

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Validaciones locales
                correoError = if (correo.isBlank()) "El correo es obligatorio" else null
                contrasenaError = if (contrasena.isBlank()) "La contraseña es obligatoria" else null

                if (correoError != null || contrasenaError != null) return@Button

                isLoading = true
                mensajeErrorGeneral = null

                auth.signInWithEmailAndPassword(correo.trim(), contrasena)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid
                            if (uid != null) {
                                db.collection("users").document(uid).get()
                                    .addOnSuccessListener { document ->
                                        isLoading = false
                                        if (document.exists() && document.contains("houseId")) {
                                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                        } else {
                                            navController.navigate("houses") { popUpTo("login") { inclusive = true } }
                                        }
                                    }
                            }
                        } else {
                            isLoading = false
                            // 🔥 NUEVO: Traducir los errores de Firebase a mensajes amigables
                            val exception = task.exception
                            mensajeErrorGeneral = when (exception) {
                                is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
                                is FirebaseAuthInvalidUserException -> "No existe una cuenta con este correo."
                                is FirebaseNetworkException -> "Error de conexión. Revisa tu internet."
                                else -> "Error al iniciar sesión: ${exception?.message}"
                            }
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