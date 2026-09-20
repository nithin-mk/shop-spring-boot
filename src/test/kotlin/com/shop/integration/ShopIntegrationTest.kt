package com.shop.integration

import com.shop.PostgresTestContainerConfig
import com.shop.model.User
import com.shop.repository.UserRepository
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestContainerConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShopIntegrationTest {
    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var userRepository: UserRepository

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
}
