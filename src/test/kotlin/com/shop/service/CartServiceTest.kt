package com.shop.service

import com.shop.model.CartItem
import com.shop.model.Product
import com.shop.model.User
import com.shop.repository.CartItemRepository
import com.shop.repository.ProductRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CartServiceTest {

    @Mock
    lateinit var cartItemRepository: CartItemRepository

    @Mock
    lateinit var productRepository: ProductRepository

    @InjectMocks
    lateinit var cartService: CartService

    private val user = User(id = 1L, email = "test@test.com", password = "hashed")
    private val product = Product(id = 10L, title = "Widget", price = BigDecimal("9.99"), description = "A widget", imageUrl = "img.jpg", user = user)

    @Test
    fun `addToCart creates new item when not in cart`() {
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(cartItemRepository.findByUserAndProductId(user, 10L)).thenReturn(Optional.empty())

        cartService.addToCart(user, 10L)

        verify(cartItemRepository).save(any<CartItem>())
    }

    @Test
    fun `addToCart increments quantity when already in cart`() {
        val existingItem = CartItem(id = 1L, user = user, product = product, quantity = 2)
        whenever(productRepository.findById(10L)).thenReturn(Optional.of(product))
        whenever(cartItemRepository.findByUserAndProductId(user, 10L)).thenReturn(Optional.of(existingItem))

        cartService.addToCart(user, 10L)

        assertEquals(3, existingItem.quantity)
        verify(cartItemRepository).save(existingItem)
    }

    @Test
    fun `addToCart does nothing when product not found`() {
        whenever(productRepository.findById(99L)).thenReturn(Optional.empty())

        cartService.addToCart(user, 99L)

        verify(cartItemRepository, never()).save(any())
    }

    @Test
    fun `removeFromCart deletes the item`() {
        val item = CartItem(id = 1L, user = user, product = product, quantity = 1)
        whenever(cartItemRepository.findByUserAndProductId(user, 10L)).thenReturn(Optional.of(item))

        cartService.removeFromCart(user, 10L)

        verify(cartItemRepository).delete(item)
    }

    @Test
    fun `getTotal returns sum of item prices`() {
        val p2 = Product(id = 11L, title = "Gadget", price = BigDecimal("5.00"), description = "desc", imageUrl = "img.jpg", user = user)
        val items = listOf(
            CartItem(id = 1L, user = user, product = product, quantity = 2),
            CartItem(id = 2L, user = user, product = p2, quantity = 1)
        )
        whenever(cartItemRepository.findByUserWithProduct(user)).thenReturn(items)

        val total = cartService.getTotal(user)

        assertEquals(BigDecimal("24.98"), total)
    }

    @Test
    fun `clearCart deletes all items for user`() {
        cartService.clearCart(user)
        verify(cartItemRepository).deleteByUser(user)
    }
}
