package com.shop.controller

import com.shop.config.SecurityConfig
import com.shop.config.ShopProperties
import com.shop.model.Order
import com.shop.model.OrderItem
import com.shop.model.Product
import com.shop.model.User
import com.shop.repository.UserRepository
import com.shop.security.ShopUserDetailsService
import com.shop.service.CartService
import com.shop.service.ImageStorageService
import com.shop.service.OrderService
import com.shop.service.ProductService
import com.shop.service.StripeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.util.Optional

@WebMvcTest(ShopController::class)
@Import(SecurityConfig::class)
@EnableConfigurationProperties(ShopProperties::class)
class ShopControllerTest {
    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUpMockMvc() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

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

    private val user = User(id = 1L, email = "shopper@test.com", password = "hashed")

    private fun stubCurrentUser() {
        whenever(userRepository.findByEmail("shopper@test.com")).thenReturn(Optional.of(user))
    }

    @Test
    fun `index page returns 200`() {
        val products =
            listOf(
                Product(id = 1, title = "Widget", price = BigDecimal("9.99"), description = "A widget", imageUrl = "images/img.jpg"),
            )
        val page = PageImpl(products, PageRequest.of(0, 2), 1)
        whenever(productService.findAll(1)).thenReturn(page)
        whenever(imageStorageService.presignedUrl(any())).thenReturn("http://minio/img.jpg")

        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk)
    }

    @Test
    fun `products page returns 200`() {
        val page = PageImpl(emptyList<Product>(), PageRequest.of(0, 2), 0)
        whenever(productService.findAll(1)).thenReturn(page)

        mockMvc
            .perform(get("/products"))
            .andExpect(status().isOk)
    }

    @Test
    fun `product detail page returns 200 when product exists`() {
        val product = Product(id = 1, title = "Widget", price = BigDecimal("9.99"), description = "desc", imageUrl = "images/img.jpg")
        whenever(productService.findById(1L)).thenReturn(product)
        whenever(imageStorageService.presignedUrl(any())).thenReturn("http://minio/img.jpg")

        mockMvc
            .perform(get("/products/1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `product detail redirects when product not found`() {
        whenever(productService.findById(99L)).thenReturn(null)

        mockMvc
            .perform(get("/products/99"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    fun `cart requires authentication`() {
        mockMvc
            .perform(get("/cart"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    fun `orders requires authentication`() {
        mockMvc
            .perform(get("/orders"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `cart page returns 200 for authenticated user`() {
        stubCurrentUser()
        whenever(cartService.getCartItems(user)).thenReturn(emptyList())

        mockMvc
            .perform(get("/cart"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `addToCart redirects to cart`() {
        stubCurrentUser()

        mockMvc
            .perform(post("/cart").param("productId", "10").with(csrf()))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/cart"))

        verify(cartService).addToCart(user, 10L)
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `removeFromCart redirects to cart`() {
        stubCurrentUser()

        mockMvc
            .perform(post("/cart-delete-item").param("productId", "10").with(csrf()))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/cart"))

        verify(cartService).removeFromCart(user, 10L)
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `checkout page returns 200`() {
        stubCurrentUser()
        whenever(cartService.getCartItems(user)).thenReturn(emptyList())
        whenever(cartService.getTotal(user)).thenReturn(BigDecimal.ZERO)
        whenever(stripeService.enabled).thenReturn(false)

        mockMvc
            .perform(get("/checkout"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `createOrder places order directly when stripe disabled`() {
        stubCurrentUser()
        whenever(stripeService.enabled).thenReturn(false)

        mockMvc
            .perform(post("/create-order").with(csrf()))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/orders"))

        verify(orderService).placeOrder(user)
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `createOrder re-renders checkout when stripe enabled but token missing`() {
        stubCurrentUser()
        whenever(stripeService.enabled).thenReturn(true)
        whenever(stripeService.publishableKey).thenReturn("pk_test")
        whenever(cartService.getCartItems(user)).thenReturn(emptyList())
        whenever(cartService.getTotal(user)).thenReturn(BigDecimal.ZERO)

        mockMvc
            .perform(post("/create-order").with(csrf()))
            .andExpect(status().isOk)

        verify(orderService, org.mockito.kotlin.never()).placeOrder(any())
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `createOrder charges via stripe and places order on success`() {
        stubCurrentUser()
        whenever(stripeService.enabled).thenReturn(true)
        whenever(cartService.getTotal(user)).thenReturn(BigDecimal("10.00"))

        mockMvc
            .perform(post("/create-order").param("stripeToken", "tok_test").with(csrf()))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/orders"))

        verify(stripeService).charge(eq("tok_test"), eq(1000L), any())
        verify(orderService).placeOrder(user)
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `createOrder re-renders checkout with error when stripe charge fails`() {
        stubCurrentUser()
        whenever(stripeService.enabled).thenReturn(true)
        whenever(stripeService.publishableKey).thenReturn("pk_test")
        whenever(cartService.getTotal(user)).thenReturn(BigDecimal("10.00"))
        whenever(cartService.getCartItems(user)).thenReturn(emptyList())
        whenever(stripeService.charge(any(), any(), any())).doThrow(RuntimeException("card declined"))

        mockMvc
            .perform(post("/create-order").param("stripeToken", "tok_bad").with(csrf()))
            .andExpect(status().isOk)

        verify(orderService, org.mockito.kotlin.never()).placeOrder(any())
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `orders page returns 200`() {
        stubCurrentUser()
        whenever(orderService.getOrdersForUser(user)).thenReturn(emptyList())

        mockMvc
            .perform(get("/orders"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `invoice returns 404 when order not found`() {
        stubCurrentUser()
        whenever(orderService.findById(1L)).thenReturn(null)

        mockMvc
            .perform(get("/orders/1"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `invoice returns 403 when order belongs to another user`() {
        stubCurrentUser()
        val otherUser = User(id = 2L, email = "other@test.com", password = "hashed")
        val order = Order(id = 1, user = otherUser)
        whenever(orderService.findById(1L)).thenReturn(order)

        mockMvc
            .perform(get("/orders/1"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "shopper@test.com")
    fun `invoice returns a PDF when order is owned by the user`() {
        stubCurrentUser()
        val order = Order(id = 1, user = user)
        order.addItem(OrderItem(productTitle = "Widget", productPrice = BigDecimal("9.99"), quantity = 2))
        whenever(orderService.findById(1L)).thenReturn(order)

        mockMvc
            .perform(get("/orders/1"))
            .andExpect(status().isOk)
    }
}
