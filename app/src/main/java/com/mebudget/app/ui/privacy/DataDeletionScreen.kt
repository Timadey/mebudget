package com.mebudget.app.ui.privacy

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mebudget.app.data.UserDataManager
import com.mebudget.app.data.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DataDeletionScreen(
    userDataManager: UserDataManager,
    authManager: AuthManager,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "DELETE YOUR DATA",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Delete all your budget, wallet, and transaction data from this device and from our servers. This action is irreversible and fulfills your right to erasure under the Nigeria Data Protection Act 2023.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isDeleting) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("DELETE ALL DATA", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text("BACK", fontWeight = FontWeight.Bold)
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently delete all your budgets, wallets, and transactions from this device and our servers. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    scope.launch {
                        isDeleting = true
                        try {
                            withContext(Dispatchers.IO) {
                                userDataManager.deleteAllLocalData()
                            }
                            authManager.deleteAccount()
                            Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                            onAccountDeleted()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Deletion failed: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isDeleting = false
                        }
                    }
                }) {
                    Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
