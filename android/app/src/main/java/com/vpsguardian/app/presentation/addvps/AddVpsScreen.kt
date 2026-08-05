package com.vpsguardian.app.presentation.addvps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vpsguardian.app.presentation.dashboard.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVpsScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    var name by rememberSaveable { mutableStateOf("") }
    var ip by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("8443") }
    var token by rememberSaveable { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val isValid = name.isNotBlank() && ip.isNotBlank() && token.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar VPS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Conecte-se ao agente instalado na VPS",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                placeholder = { Text("Ex: Produção") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("IP ou Hostname") },
                placeholder = { Text("Ex: 192.168.1.100") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() } },
                label = { Text("Porta") },
                placeholder = { Text("8443") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Token do Agente") },
                placeholder = { Text("Token gerado na instalação") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.addVps(name, ip, port.toIntOrNull() ?: 8443, token)
                    onSuccess()
                },
                enabled = isValid && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Conectar e Descobrir Serviços")
                }
            }
        }
    }
}
