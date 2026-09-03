package com.shop.service

import com.stripe.StripeClient
import com.stripe.param.ChargeCreateParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StripeService(
    @Value("\${shop.stripe.secret-key:}") private val secretKey: String,
    @Value("\${shop.stripe.publishable-key:}") val publishableKey: String
) {
    val enabled: Boolean get() = secretKey.isNotBlank()

    private val client: StripeClient? by lazy {
        if (enabled) StripeClient(secretKey) else null
    }

    fun charge(token: String, amountCents: Long, description: String) {
        val c = client ?: throw IllegalStateException("Stripe is not configured.")
        val params = ChargeCreateParams.builder()
            .setAmount(amountCents)
            .setCurrency("usd")
            .setDescription(description)
            .setSource(token)
            .build()
        c.v1().charges().create(params)
    }
}
