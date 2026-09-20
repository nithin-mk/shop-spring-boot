package com.shop.controller

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.ui.ExtendedModelMap

class ErrorControllerTest {
    private val controller = ShopErrorController()

    @Test
    fun `handleError returns 404 view for not-found status`() {
        val request = mock<HttpServletRequest>()
        whenever(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(404)

        val view = controller.handleError(request, ExtendedModelMap())

        assertEquals("404", view)
    }

    @Test
    fun `handleError returns 500 view for other statuses`() {
        val request = mock<HttpServletRequest>()
        whenever(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(500)

        val view = controller.handleError(request, ExtendedModelMap())

        assertEquals("500", view)
    }

    @Test
    fun `handleError returns 500 view when status is missing`() {
        val request = mock<HttpServletRequest>()
        whenever(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(null)

        val view = controller.handleError(request, ExtendedModelMap())

        assertEquals("500", view)
    }
}
