package com.shop.controller

import com.shop.config.SecurityConfig
import com.shop.config.ShopProperties
import com.shop.model.Product
import com.shop.model.User
import com.shop.repository.UserRepository
import com.shop.security.ShopUserDetailsService
import com.shop.service.ImageStorageService
import com.shop.service.ProductService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.util.Optional

@WebMvcTest(AdminController::class)
@Import(SecurityConfig::class)
@EnableConfigurationProperties(ShopProperties::class)
class AdminControllerTest {
    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUpMockMvc() {
        // @WithMockUser only survives into the request if the MockMvc security
        // bridge is applied explicitly — without it, Spring Security's
        // SecurityContextHolderFilter overwrites the test-provided context with
        // an empty one loaded from the (nonexistent) session before the request
        // reaches the controller, and every request looks unauthenticated.
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @MockitoBean
    lateinit var productService: ProductService

    @MockitoBean
    lateinit var imageStorageService: ImageStorageService

    @MockitoBean
    lateinit var userRepository: UserRepository

    @MockitoBean
    lateinit var userDetailsService: ShopUserDetailsService

    private val user = User(id = 1L, email = "admin@test.com", password = "hashed")

    private fun stubCurrentUser() {
        whenever(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user))
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `showAddProduct returns 200`() {
        mockMvc
            .perform(get("/admin/add-product"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `postAddProduct with empty image re-renders form with error`() {
        stubCurrentUser()
        val emptyImage = MockMultipartFile("image", "", "image/jpeg", ByteArray(0))

        mockMvc
            .perform(
                multipart("/admin/add-product")
                    .file(emptyImage)
                    .param("title", "Widget")
                    .param("price", "9.99")
                    .param("description", "A nice widget")
                    .with(csrf()),
            ).andExpect(status().isOk)

        verify(productService, never()).create(any(), any(), any(), any(), any())
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `postAddProduct with invalid title re-renders form with validation error`() {
        stubCurrentUser()
        val image = MockMultipartFile("image", "widget.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

        mockMvc
            .perform(
                multipart("/admin/add-product")
                    .file(image)
                    .param("title", "AB")
                    .param("price", "9.99")
                    .param("description", "A valid description")
                    .with(csrf()),
            ).andExpect(status().isOk)

        verify(productService, never()).create(any(), any(), any(), any(), any())
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `postAddProduct with valid data creates product and redirects`() {
        stubCurrentUser()
        val image = MockMultipartFile("image", "widget.jpg", "image/jpeg", byteArrayOf(1, 2, 3))
        whenever(imageStorageService.store(any())).thenReturn("images/widget.jpg")

        mockMvc
            .perform(
                multipart("/admin/add-product")
                    .file(image)
                    .param("title", "Widget")
                    .param("price", "9.99")
                    .param("description", "A nice widget indeed")
                    .with(csrf()),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin/products"))

        verify(productService).create(eq("Widget"), eq(BigDecimal("9.99")), eq("A nice widget indeed"), eq("images/widget.jpg"), eq(user))
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `adminProducts returns 200`() {
        stubCurrentUser()
        whenever(productService.findAllByUser(user)).thenReturn(emptyList())

        mockMvc
            .perform(get("/admin/products"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `showEditProduct redirects home when edit param missing`() {
        mockMvc
            .perform(get("/admin/edit-product/1"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `showEditProduct redirects home when product not found`() {
        whenever(productService.findById(99L)).thenReturn(null)

        mockMvc
            .perform(get("/admin/edit-product/99").param("edit", "true"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `showEditProduct returns 200 when product found`() {
        stubCurrentUser()
        val product = Product(id = 1, title = "Widget", price = BigDecimal("9.99"), description = "desc", imageUrl = "img.jpg", user = user)
        whenever(productService.findById(1L)).thenReturn(product)

        mockMvc
            .perform(get("/admin/edit-product/1").param("edit", "true"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `showEditProduct redirects home when product belongs to another user`() {
        stubCurrentUser()
        val otherUser = User(id = 2L, email = "other@test.com", password = "hashed")
        val product =
            Product(id = 1, title = "Widget", price = BigDecimal("9.99"), description = "desc", imageUrl = "img.jpg", user = otherUser)
        whenever(productService.findById(1L)).thenReturn(product)

        mockMvc
            .perform(get("/admin/edit-product/1").param("edit", "true"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `postEditProduct with validation errors re-renders form`() {
        stubCurrentUser()
        val image = MockMultipartFile("image", "", "image/jpeg", ByteArray(0))

        mockMvc
            .perform(
                multipart("/admin/edit-product")
                    .file(image)
                    .param("productId", "1")
                    .param("title", "")
                    .param("price", "9.99")
                    .param("description", "A valid description")
                    .with(csrf()),
            ).andExpect(status().isOk)

        verify(productService, never()).update(any(), any(), any(), any(), any(), any())
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `postEditProduct without new image updates and redirects`() {
        stubCurrentUser()
        val image = MockMultipartFile("image", "", "image/jpeg", ByteArray(0))
        val existing =
            Product(id = 1, title = "Old", price = BigDecimal("9.99"), description = "old desc", imageUrl = "images/old.jpg", user = user)
        whenever(productService.findById(1L)).thenReturn(existing)

        mockMvc
            .perform(
                multipart("/admin/edit-product")
                    .file(image)
                    .param("productId", "1")
                    .param("title", "Updated Widget")
                    .param("price", "12.99")
                    .param("description", "An updated description")
                    .with(csrf()),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin/products"))

        verify(imageStorageService, never()).delete(any())
        verify(
            productService,
        ).update(eq(1L), eq("Updated Widget"), eq(BigDecimal("12.99")), eq("An updated description"), eq(null), eq(user))
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `postEditProduct with new image deletes old image and stores new one`() {
        stubCurrentUser()
        val image = MockMultipartFile("image", "new.jpg", "image/jpeg", byteArrayOf(1, 2, 3))
        val existing =
            Product(id = 1, title = "Old", price = BigDecimal("9.99"), description = "old desc", imageUrl = "images/old.jpg", user = user)
        whenever(productService.findById(1L)).thenReturn(existing)
        whenever(imageStorageService.store(any())).thenReturn("images/new.jpg")

        mockMvc
            .perform(
                multipart("/admin/edit-product")
                    .file(image)
                    .param("productId", "1")
                    .param("title", "Updated Widget")
                    .param("price", "12.99")
                    .param("description", "An updated description")
                    .with(csrf()),
            ).andExpect(status().is3xxRedirection)

        verify(imageStorageService).delete("images/old.jpg")
        verify(
            productService,
        ).update(eq(1L), eq("Updated Widget"), eq(BigDecimal("12.99")), eq("An updated description"), eq("images/new.jpg"), eq(user))
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `deleteProduct removes image and product`() {
        stubCurrentUser()
        val product =
            Product(id = 1, title = "Widget", price = BigDecimal("9.99"), description = "desc", imageUrl = "images/widget.jpg", user = user)
        whenever(productService.findById(1L)).thenReturn(product)

        mockMvc
            .perform(delete("/admin/product/1/delete").with(csrf()))
            .andExpect(status().isOk)

        verify(imageStorageService).delete("images/widget.jpg")
        verify(productService).delete(1L, user)
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    fun `deleteProduct when product already gone skips image deletion`() {
        stubCurrentUser()
        whenever(productService.findById(99L)).thenReturn(null)

        mockMvc
            .perform(delete("/admin/product/99/delete").with(csrf()))
            .andExpect(status().isOk)

        verify(imageStorageService, never()).delete(any())
        verify(productService).delete(99L, user)
    }
}
