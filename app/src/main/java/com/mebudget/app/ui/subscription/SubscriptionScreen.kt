package com.mebudget.app.ui.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mebudget.app.billing.BillingPlan
import com.mebudget.app.ui.common.offsetShadow
import com.mebudget.app.ui.theme.AccentBlue

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onSubscribeSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onSubscribeSuccess()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Upgrade to Pro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ComparisonCard()
            ComparisonCard(isPro = true)

            uiState.plans.forEach { plan ->
                PlanSelectorRow(
                    plan = plan,
                    selected = uiState.selectedPlan == plan,
                    onSelect = { viewModel.selectPlan(plan) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text("Email for receipts") },
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.cardNumber,
                onValueChange = viewModel::onCardNumberChanged,
                label = { Text("Card number") },
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.expiryMonth,
                    onValueChange = viewModel::onExpiryMonthChanged,
                    label = { Text("MM") },
                    singleLine = true,
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.expiryYear,
                    onValueChange = viewModel::onExpiryYearChanged,
                    label = { Text("YY") },
                    singleLine = true,
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.cvv,
                    onValueChange = viewModel::onCvvChanged,
                    label = { Text("CVV") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = { viewModel.subscribe(context as android.app.Activity) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.selectedPlan != null && !uiState.isLoading,
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        "SUBSCRIBE NOW",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun ComparisonCard(isPro: Boolean = false) {
    val items = if (isPro) {
        listOf("Unlimited budgets", "Premium insights", "Cloud sync", "Priority support")
    } else {
        listOf("2 budgets", "Basic insights", "No sync")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) AccentBlue.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            2.dp,
            if (isPro) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isPro) "PRO" else "FREE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (isPro) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            items.forEach {
                Text(
                    text = "• $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlanSelectorRow(
    plan: BillingPlan,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plan.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "₦${plan.price / 100} / ${if (plan.interval == "monthly") "month" else "year"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
    }
}