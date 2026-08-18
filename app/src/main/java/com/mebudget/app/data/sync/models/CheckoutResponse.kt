package com.mebudget.app.data.sync.models

import com.google.gson.annotations.SerializedName

data class CheckoutResponse(
    @SerializedName("authorization_url") val authorizationUrl: String,
    @SerializedName("access_code") val accessCode: String? = null,
    @SerializedName("reference") val reference: String? = null
)