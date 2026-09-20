package com.shop.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FormsTest {
    @Test
    fun `ProductForm has sensible defaults, equality and copy semantics`() {
        val defaultForm = ProductForm()
        assertEquals("", defaultForm.title)
        assertEquals(BigDecimal.ZERO, defaultForm.price)
        assertEquals("", defaultForm.description)

        val form = ProductForm(title = "Widget", price = BigDecimal("9.99"), description = "A widget")
        val same = ProductForm(title = "Widget", price = BigDecimal("9.99"), description = "A widget")
        val different = form.copy(title = "Gadget")

        assertEquals(form, same)
        assertEquals(form.hashCode(), same.hashCode())
        assertNotEquals(form, different)
        assertEquals("Gadget", different.title)
        assertEquals(form.price, different.price)
        assert(form.toString().contains("Widget"))
    }

    @Test
    fun `SignupForm has sensible defaults, equality and copy semantics`() {
        val defaultForm = SignupForm()
        assertEquals("", defaultForm.email)
        assertEquals("", defaultForm.password)
        assertEquals("", defaultForm.confirmPassword)

        val form = SignupForm(email = "a@test.com", password = "secret1", confirmPassword = "secret1")
        val same = SignupForm(email = "a@test.com", password = "secret1", confirmPassword = "secret1")
        val different = form.copy(email = "b@test.com")

        assertEquals(form, same)
        assertEquals(form.hashCode(), same.hashCode())
        assertNotEquals(form, different)
        assertEquals("b@test.com", different.email)
        assert(form.toString().contains("a@test.com"))
    }
}
