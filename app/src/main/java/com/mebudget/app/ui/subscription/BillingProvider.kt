package com.mebudget.app.ui.subscription

import android.content.Context
import com.mebudget.app.BuildConfig
import com.mebudget.app.billing.PaystackManager

internal fun Context.paystackManager(): PaystackManager {
    return PaystackManager(publicKey = BuildConfig.PAYSTACK_PUBLIC_KEY)
}