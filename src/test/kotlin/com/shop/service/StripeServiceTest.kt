package com.shop.service

import com.shop.config.ShopProperties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StripeServiceTest {
    private fun stripeService(
        secretKey: String,
        publishableKey: String = "",
    ) = StripeService(
        ShopProperties(stripe = ShopProperties.Stripe(secretKey = secretKey, publishableKey = publishableKey)),
    )

    @Test
    fun `enabled is false when secret key is blank`() {
        val service = stripeService(secretKey = "")
        assertFalse(service.enabled)
    }

    @Test
    fun `enabled is true when secret key is present`() {
        val service = stripeService(secretKey = "sk_test_fake", publishableKey = "pk_test_fake")
        assertTrue(service.enabled)
    }

    @Test
    fun `charge throws when stripe is not configured`() {
        val service = stripeService(secretKey = "")

        assertThrows(IllegalStateException::class.java) {
            service.charge("tok_visa", 1000L, "Demo Order")
        }
    }
}
