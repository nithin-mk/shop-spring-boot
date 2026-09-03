package com.shop.service

import com.shop.model.Product
import com.shop.model.User
import com.shop.repository.ProductRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {

    @Mock
    lateinit var productRepository: ProductRepository

    @InjectMocks
    lateinit var productService: ProductService

    private val user = User(id = 1L, email = "test@test.com", password = "hashed")

    @Test
    fun `findAll returns paginated products`() {
        val products = listOf(
            Product(id = 1, title = "A", price = BigDecimal("9.99"), description = "Desc A", imageUrl = "img/a.jpg", user = user),
            Product(id = 2, title = "B", price = BigDecimal("19.99"), description = "Desc B", imageUrl = "img/b.jpg", user = user)
        )
        val page = PageImpl(products, PageRequest.of(0, 2), 2)
        whenever(productRepository.findAll(any<PageRequest>())).thenReturn(page)

        val result = productService.findAll(1)

        assertEquals(2, result.content.size)
        assertEquals("A", result.content[0].title)
    }

    @Test
    fun `findById returns product when found`() {
        val product = Product(id = 1, title = "A", price = BigDecimal("9.99"), description = "Desc A", imageUrl = "img/a.jpg", user = user)
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val result = productService.findById(1L)

        assertNotNull(result)
        assertEquals("A", result!!.title)
    }

    @Test
    fun `findById returns null when not found`() {
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        val result = productService.findById(99L)

        assertNull(result)
    }

    @Test
    fun `create saves and returns product`() {
        val product = Product(id = 1, title = "New", price = BigDecimal("5.00"), description = "New desc", imageUrl = "img/n.jpg", user = user)
        whenever(productRepository.save(any())).thenReturn(product)

        val result = productService.create("New", BigDecimal("5.00"), "New desc", "img/n.jpg", user)

        assertEquals("New", result.title)
        assertEquals(BigDecimal("5.00"), result.price)
    }

    @Test
    fun `update returns null when product not found`() {
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        val result = productService.update(99L, "X", BigDecimal.ONE, "desc", null, user)

        assertNull(result)
    }

    @Test
    fun `update returns null when user does not own product`() {
        val otherUser = User(id = 2L, email = "other@test.com", password = "hashed")
        val product = Product(id = 1, title = "A", price = BigDecimal("9.99"), description = "Desc", imageUrl = "img.jpg", user = otherUser)
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val result = productService.update(1L, "X", BigDecimal.ONE, "desc", null, user)

        assertNull(result)
    }

    @Test
    fun `delete returns false when product not owned by user`() {
        val otherUser = User(id = 2L, email = "other@test.com", password = "hashed")
        val product = Product(id = 1, title = "A", price = BigDecimal("9.99"), description = "Desc", imageUrl = "img.jpg", user = otherUser)
        whenever(productRepository.findById(1L)).thenReturn(Optional.of(product))

        val result = productService.delete(1L, user)

        assertFalse(result)
    }
}
