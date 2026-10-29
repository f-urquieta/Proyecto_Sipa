package com.example.sipa.ui.screens

import android.widget.Toast
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
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    val auth = Firebase.auth
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Teléfono") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Validación básica
                if (nombre.isBlank() || correo.isBlank() || contrasena.isBlank()) {
                    Toast.makeText(context, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                Log.d("REGISTER_DEBUG", "Iniciando creación de usuario...")

                auth.createUserWithEmailAndPassword(correo, contrasena)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("REGISTER_DEBUG", "Usuario creado en Auth exitosamente")
                            val user = auth.currentUser
                            val uid = user?.uid

                            if (uid != null) {
                                val userData = hashMapOf(
                                    "nombre" to nombre,
                                    "correo" to correo,
                                    "telefono" to telefono,
                                    "uid" to uid
                                )
                                Log.d("REGISTER_DEBUG", "Intentando guardar en Firestore para UID: $uid")

                                db.collection("users").document(uid).set(userData)
                                    .addOnSuccessListener {
                                        Log.d("REGISTER_DEBUG", "Perfil guardado en Firestore exitosamente")
                                        isLoading = false
                                        try {
                                            navController.navigate("houses") {
                                                popUpTo("register") { inclusive = true }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("REGISTER_DEBUG", "Error al navegar: ${e.message}", e)
                                            Toast.makeText(context, "Error al navegar: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("REGISTER_DEBUG", "Error al guardar perfil en Firestore: ${e.message}", e)
                                        isLoading = false
                                        Toast.makeText(context, "Error al guardar perfil: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Log.e("REGISTER_DEBUG", "UID es null, no se puede guardar en Firestore")
                                isLoading = false
                                Toast.makeText(context, "Error: UID no disponible", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Log.e("REGISTER_DEBUG", "Error en Auth: ${task.exception?.message}", task.exception)
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