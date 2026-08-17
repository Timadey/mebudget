package com.mebudget.app.ui.subscription

import android.content.Context
import com.mebudget.app.billing.PaystackManager

internal fun Context.paystackManager(): PaystackManager {
    return PaystackManager(publicKey = PAYSTACK_PUBLIC_KEY)
}

private const val PAYSTACK_PUBLIC_KEY = "pk_test_placeholder"