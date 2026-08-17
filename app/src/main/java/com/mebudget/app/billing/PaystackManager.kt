package com.mebudget.app.billing

import android.app.Activity
import co.paystack.android.Paystack
import co.paystack.android.PaystackSdk
import co.paystack.android.Transaction
import co.paystack.android.model.Card
import co.paystack.android.model.Charge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Thin wrapper around the Paystack Android SDK (co.paystack.android:paystack).
 *
 * Billing decisions live at the UI layer; this manager only exposes the plans
 * and the card-charge entry point. Successful transactions are reconciled
 * server-side via the Paystack webhook writing into the `subscriptions`
 * PocketBase collection.
 */
class PaystackManager(
    private val publicKey: String
) {
    val plans: List<BillingPlan>
        get() = BillingPlan.DEFAULTS

    /** Must be called once, before any transaction. Safe to call multiple times. */
    fun initialize() {
        PaystackSdk.setPublicKey(publicKey)
    }

    fun getAvailablePlans(): List<BillingPlan> = plans

    /**
     * Charges `amount` via client-side card tokenization. Returns the completed
     * [Transaction] on success (drives a server webhook on settle).
     */
    suspend fun chargeCard(
        activity: Activity,
        amount: Long,
        email: String,
        card: Card
    ): Result<Transaction> {
        return try {
            initialize()
            val charge = Charge()
                .setAmount(amount.toInt())
                .setEmail(email)
                .setCard(card)

            suspendCancellableCoroutine { continuation ->
                PaystackSdk.chargeCard(
                    activity,
                    charge,
                    object : Paystack.TransactionCallback {
                        override fun onSuccess(transaction: Transaction) {
                            if (continuation.isActive) {
                                continuation.resume(Result.success(transaction))
                            }
                        }

                        override fun beforeValidate(transaction: Transaction) {
                            // No-op; validation is handled by the SDK UI.
                        }

                        override fun onError(error: Throwable, transaction: Transaction) {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(error))
                            }
                        }
                    }
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}