package app.getupcoming.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.getupcoming.BuildConfig
import app.getupcoming.core.designsystem.*
import app.getupcoming.ui.theme.*

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            MonogramBadge("U", size = 72.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Upcoming",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Scheduling that runs itself",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            UpcomingCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                // Mode toggle
                Row(modifier = Modifier.fillMaxWidth()) {
                    ModeTab("Log in", !state.isSignUp) { if (state.isSignUp) viewModel.toggleMode() }
                    ModeTab("Sign up", state.isSignUp) { if (!state.isSignUp) viewModel.toggleMode() }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.update { s -> s.copy(email = it.trim()) } },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { viewModel.update { s -> s.copy(password = it) } },
                    label = { Text("Password (8+ characters)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.isSignUp) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { viewModel.update { s -> s.copy(username = it.filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '_' || ch == '-' }.lowercase()) } },
                        label = { Text("Username (your booking link)") },
                        singleLine = true,
                        supportingText = { Text("getupcoming.app/${state.username.ifBlank { "you" }}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = { viewModel.update { s -> s.copy(displayName = it) } },
                        label = { Text("Display name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.agreedToTerms,
                            onCheckedChange = { viewModel.update { s -> s.copy(agreedToTerms = it) } }
                        )
                        Text(
                            text = "I agree to the ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onOpenTerms, contentPadding = PaddingValues(0.dp)) {
                            Text("Terms of Use", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = " & ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onOpenPrivacy, contentPadding = PaddingValues(0.dp)) {
                            Text("Privacy Policy", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                state.error?.let {
                    Text(it, color = SemanticError, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                val enabled = if (state.isSignUp) state.canSubmitSignUp else state.canSubmitLogin
                UpcomingPrimaryButton(
                    text = if (state.isSignUp) "Create account" else "Log in",
                    onClick = {
                        if (state.isSignUp) viewModel.signUp(onAuthenticated) else viewModel.login(onAuthenticated)
                    },
                    enabled = enabled,
                    isLoading = state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Phase 0.2: demo mode is a development aid — never offer it in a
            // release build.
            if (BuildConfig.DEBUG) {
                UpcomingSecondaryButton(
                    text = "Explore demo mode",
                    onClick = { viewModel.enterDemo(onAuthenticated) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onOpenTerms) {
                    Text("Terms of Use", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenPrivacy) {
                    Text("Privacy Policy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.weight(1f)) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) UpcomingTokens.BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(
            thickness = if (selected) 2.dp else 1.dp,
            color = if (selected) UpcomingTokens.BrandPrimary else MaterialTheme.colorScheme.outline
        )
    }
}

