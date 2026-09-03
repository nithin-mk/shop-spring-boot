package com.shop.controller

import com.shop.config.SecurityConfig
import com.shop.model.Product
import com.shop.repository.UserRepository
import com.shop.security.ShopUserDetailsService
import com.shop.service.CartService
import com.shop.service.OrderService
import com.shop.service.ImageStorageService
import com.shop.service.ProductService
import com.shop.service.StripeService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@WebMvcTest(ShopController::class)
@Import(SecurityConfig::class)
class ShopControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var productService: ProductService

    @MockitoBean
    lateinit var cartService: CartService

    @MockitoBean
    lateinit var orderService: OrderService

    @MockitoBean
    lateinit var userRepository: UserRepository

    @MockitoBean
    lateinit var imageStorageService: ImageStorageService

    @MockitoBean
    lateinit var stripeService: StripeService

    @MockitoBean
    lateinit var userDetailsService: ShopUserDetailsService

    @Test
    fun `index page returns 200`() {
        val products = listOf(
            Product(id = 1, title = "Widget", price = BigDecimal("9.99"), description = "A widget", imageUrl = "images/img.jpg")
        )
        val page = PageImpl(products, PageRequest.of(0, 2), 1)
        whenever(productService.findAll(1)).thenReturn(page)
        whenever(imageStorageService.presignedUrl(any())).thenReturn("http://minio/img.jpg")

        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
    }

    @Test
    fun `products page returns 200`() {
        val page = PageImpl(emptyList<Product>(), PageRequest.of(0, 2), 0)
        whenever(productService.findAll(1)).thenReturn(page)

        mockMvc.perform(get("/products"))
            .andExpect(status().isOk)
    }

    @Test
    fun `product detail page returns 200 when product exists`() {
        val product = Product(id = 1, title = "Widget", price = BigDecimal("9.99"), description = "desc", imageUrl = "images/img.jpg")
        whenever(productService.findById(1L)).thenReturn(product)
        whenever(imageStorageService.presignedUrl(any())).thenReturn("http://minio/img.jpg")

        mockMvc.perform(get("/products/1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `product detail redirects when product not found`() {
        whenever(productService.findById(99L)).thenReturn(null)

        mockMvc.perform(get("/products/99"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    fun `cart requires authentication`() {
        mockMvc.perform(get("/cart"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    fun `orders requires authentication`() {
        mockMvc.perform(get("/orders"))
            .andExpect(status().is3xxRedirection)
    }
}
