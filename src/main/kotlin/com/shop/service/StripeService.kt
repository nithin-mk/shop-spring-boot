package com.shop.service

import com.shop.config.ShopProperties
import com.stripe.StripeClient
import com.stripe.param.ChargeCreateParams
import org.springframework.stereotype.Service

@Service
class StripeService(
    shopProperties: ShopProperties,
) {
    private val secretKey: String = shopProperties.stripe.secretKey
    val publishableKey: String = shopProperties.stripe.publishableKey

    val enabled: Boolean get() = secretKey.isNotBlank()

    private val client: StripeClient? by lazy {
        if (enabled) StripeClient(secretKey) else null
    }

    fun charge(
        token: String,
        amountCents: Long,
        description: String,
    ) {
        val c = client ?: error("Stripe is not configured.")
        val params =
            ChargeCreateParams
                .builder()
                .setAmount(amountCents)
                .setCurrency("usd")
                .setDescription(description)
                .setSource(token)
                .build()
        c.v1().charges().create(params)
    }
}
