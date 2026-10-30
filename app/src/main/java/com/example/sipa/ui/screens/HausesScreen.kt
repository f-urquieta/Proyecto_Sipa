package com.example.sipa.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sipa.ui.theme.*

@Composable
fun HousesScreen(navController: NavController) {
    var nombreCasa by remember { mutableStateOf("Mi nueva casa") }
    var codigoInvitacion by remember { mutableStateOf("") }

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

        // Tarjeta Crear Casa
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
                    colors = ButtonDefaults.buttonColors(containerColor = SipaGold)
                ) {
                    Text("Crear casa", color = SipaWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjeta Unirme a Casa
        Card(
            colors = CardDefaults.cardColors(containerColor = SipaWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Key, contentDescription = null, tint = SipaGold)
                Text("Unirme a una casa", fontWeight = FontWeight.Bold, color = SipaTextDark)
                Text("Código compartido por el dueño SIPA · 4826", color = SipaTextLight, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = codigoInvitacion,
                    onValueChange = { codigoInvitacion = it },
                    label = { Text("Código de invitación") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* Simulamos solicitud enviada */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SipaGold)
                ) {
                    Text("Enviar solicitud de acceso", color = SipaWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Tu solicitud pendiente", fontWeight = FontWeight.Bold, color = SipaTextDark)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = SipaGold)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Casa de la sierra", fontWeight = FontWeight.Bold, color = SipaTextDark)
                Text("Pendiente de aprobación · Enviada hoy", color = SipaTextLight, fontSize = 12.sp)
            }
        }
    }
}