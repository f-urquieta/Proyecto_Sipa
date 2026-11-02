package com.example.sipa.ui.screens

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sipa.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    val auth = Firebase.auth
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Estados de los campos
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // 🔥 NUEVO: Estados para los errores visuales en línea
    var nombreError by remember { mutableStateOf<String?>(null) }
    var correoError by remember { mutableStateOf<String?>(null) }
    var contrasenaError by remember { mutableStateOf<String?>(null) }
    var confirmarError by remember { mutableStateOf<String?>(null) }
    var mensajeErrorGeneral by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SipaBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text("TU HOGAR EMPIEZA AQUÍ", fontSize = 12.sp, color = SipaTextLight)
        Text("Crear cuenta", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SipaTextDark)
        Text(
            "Un primer paso para disfrutar de la luz a tu manera.",
            fontSize = 12.sp,
            color = SipaTextLight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 🔥 Campo Nombre
        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                nombreError = null // Limpia el error al escribir
            },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            isError = nombreError != null,
            supportingText = { if (nombreError != null) Text(nombreError!!) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 🔥 Campo Correo
        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                correoError = null
                mensajeErrorGeneral = null
            },
            label = { Text("Correo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            isError = correoError != null,
            supportingText = { if (correoError != null) Text(correoError!!) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 🔥 Campo Contraseña
        OutlinedTextField(
            value = contrasena,
            onValueChange = {
                contrasena = it
                contrasenaError = null
            },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = contrasenaError != null,
            supportingText = { if (contrasenaError != null) Text(contrasenaError!!) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 🔥 Campo Confirmar Contraseña (Con validación en línea automática)
        OutlinedTextField(
            value = confirmarContrasena,
            onValueChange = {
                confirmarContrasena = it
                confirmarError = null
            },
            label = { Text("Confirmar contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = confirmarError != null,
            supportingText = { if (confirmarError != null) Text(confirmarError!!) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 🔥 Campo Teléfono
        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Teléfono") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        // 🔥 Mensaje de error general (Ej. Correo ya registrado, error de red)
        if (mensajeErrorGeneral != null) {
            Text(
                text = mensajeErrorGeneral!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Validaciones locales (asignando a los estados de error)
                nombreError = if (nombre.isBlank()) "El nombre es obligatorio" else null
                correoError = if (correo.isBlank()) {
                    "El correo es obligatorio"
                } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                    "Ingresa un correo válido (ej: usuario@dominio.com)"
                } else null

                contrasenaError = if (contrasena.isBlank()) {
                    "La contraseña es obligatoria"
                } else if (contrasena.length < 6) {
                    "Debe tener al menos 6 caracteres"
                } else null

                confirmarError = if (confirmarContrasena.isBlank()) {
                    "Confirma tu contraseña"
                } else if (contrasena != confirmarContrasena) {
                    "Las contraseñas no coinciden"
                } else null

                // Si hay algún error en los campos, detenemos la ejecución
                if (nombreError != null || correoError != null || contrasenaError != null || confirmarError != null) {
                    return@Button
                }

                isLoading = true
                mensajeErrorGeneral = null
                Log.d("REGISTER_DEBUG", "Iniciando creación de usuario...")

                auth.createUserWithEmailAndPassword(correo.trim(), contrasena)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid
                            if (uid != null) {
                                val userData = hashMapOf(
                                    "nombre" to nombre,
                                    "correo" to correo.trim(),
                                    "telefono" to telefono,
                                    "uid" to uid
                                )

                                db.collection("users").document(uid).set(userData)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        navController.navigate("houses") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        mensajeErrorGeneral = "Error al guardar perfil: ${e.message}"
                                    }
                            } else {
                                isLoading = false
                                mensajeErrorGeneral = "Error: UID no disponible"
                            }
                        } else {
                            isLoading = false
                            // 🔥 Traducir los errores de Firebase a mensajes amigables
                            val exception = task.exception
                            mensajeErrorGeneral = when (exception) {
                                is FirebaseAuthUserCollisionException -> "Este correo ya está registrado. Intenta iniciar sesión."
                                is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil."
                                is FirebaseNetworkException -> "Error de conexión. Revisa tu internet."
                                else -> "Error: ${exception?.message}"
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
                Text("Crear cuenta", color = SipaWhite)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            Text("¿Ya tienes cuenta? ", color = SipaTextDark)
            TextButton(onClick = { navController.navigate("login") }) {
                Text("Iniciar sesión", color = SipaGold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Después, tu casa.\nPodrás crear una casa o solicitar acceso a una que ya exista.",
            textAlign = TextAlign.Center,
            color = SipaTextLight,
            fontSize = 12.sp
        )
    }
}
