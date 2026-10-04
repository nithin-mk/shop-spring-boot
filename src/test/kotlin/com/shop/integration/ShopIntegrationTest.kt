package com.shop.integration

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.shop.MinioTestContainerConfig
import com.shop.PostgresTestContainerConfig
import com.shop.model.Order
import com.shop.model.OrderItem
import com.shop.model.PasswordResetToken
import com.shop.model.User
import com.shop.repository.OrderRepository
import com.shop.repository.PasswordResetTokenRepository
import com.shop.repository.UserRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.annotation.Commit
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.io.ByteArrayInputStream
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfig::class, MinioTestContainerConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShopIntegrationTest {
    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    lateinit var mockMvc: MockMvc

    @BeforeAll
    fun setup() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .build()
    }

    @BeforeEach
    @Transactional
    @Commit
    fun seedUser() {
        if (!userRepository.existsByEmail("user@test.com")) {
            userRepository.save(User(email = "user@test.com", password = passwordEncoder.encode("password")!!))
        }
    }

    @Test
    fun `index is publicly accessible`() {
        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk)
    }

    @Test
    fun `products page is publicly accessible`() {
        mockMvc
            .perform(get("/products"))
            .andExpect(status().isOk)
    }

    @Test
    fun `login page is accessible`() {
        mockMvc
            .perform(get("/login"))
            .andExpect(status().isOk)
    }

    @Test
    fun `signup page is accessible`() {
        mockMvc
            .perform(get("/signup"))
            .andExpect(status().isOk)
    }

    @Test
    fun `cart redirects unauthenticated user`() {
        mockMvc
            .perform(get("/cart"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    fun `orders redirects unauthenticated user`() {
        mockMvc
            .perform(get("/orders"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    fun `admin add product redirects unauthenticated user`() {
        mockMvc
            .perform(get("/admin/add-product"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    fun `signup creates user and redirects`() {
        mockMvc
            .perform(
                post("/signup")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("email", "integration@test.com")
                    .param("password", "password123")
                    .param("confirmPassword", "password123")
                    .with(csrf()),
            ).andExpect(status().is3xxRedirection)
    }

    @Test
    @WithMockUser(username = "user@test.com")
    fun `authenticated user can access orders page`() {
        mockMvc
            .perform(get("/orders"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "user@test.com")
    fun `authenticated user can access cart page`() {
        mockMvc
            .perform(get("/cart"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "user@test.com")
    fun `authenticated user can access checkout page`() {
        mockMvc
            .perform(get("/checkout"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "user@test.com")
    fun `orders page lists the order items`() {
        val user = userRepository.findByEmail("user@test.com").orElseThrow()
        val order = Order(user = user)
        order.addItem(OrderItem(productTitle = "Orders Page Book", productPrice = BigDecimal("5.00"), quantity = 3))
        orderRepository.save(order)

        mockMvc
            .perform(get("/orders"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Orders Page Book")))
    }

    @Test
    fun `reset password page renders for a valid token`() {
        val user = userRepository.findByEmail("user@test.com").orElseThrow()
        passwordResetTokenRepository.save(PasswordResetToken(token = "integration-reset-token", user = user))

        mockMvc
            .perform(get("/reset/integration-reset-token"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("integration-reset-token")))
    }

    // open-in-view is off, so the order's lazy items must be fetched by the
    // query itself — mocked unit tests can't catch a LazyInitializationException.
    @Test
    @WithMockUser(username = "user@test.com")
    fun `invoice PDF lists the order items`() {
        val user = userRepository.findByEmail("user@test.com").orElseThrow()
        val order = Order(user = user)
        order.addItem(OrderItem(productTitle = "Invoice Test Book", productPrice = BigDecimal("12.50"), quantity = 2))
        val saved = orderRepository.save(order)

        val pdf =
            mockMvc
                .perform(get("/orders/${saved.id}"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .response.contentAsByteArray

        val text = PdfDocument(PdfReader(ByteArrayInputStream(pdf))).use { PdfTextExtractor.getTextFromPage(it.getPage(1)) }
        assertTrue(text.contains("Invoice Test Book - 2 x \$12.50"), text)
        assertTrue(text.contains("Total Price: \$25.00"), text)
    }
}
