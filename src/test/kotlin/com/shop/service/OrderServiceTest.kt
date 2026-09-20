package com.shop.service

import com.shop.model.CartItem
import com.shop.model.Order
import com.shop.model.Product
import com.shop.model.User
import com.shop.repository.OrderRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {
    @Mock
    lateinit var orderRepository: OrderRepository

    @Mock
    lateinit var cartService: CartService

    @InjectMocks
    lateinit var orderService: OrderService

    private val user = User(id = 1L, email = "test@test.com", password = "hashed")
    private val product =
        Product(id = 10L, title = "Widget", price = BigDecimal("9.99"), description = "A widget", imageUrl = "img.jpg", user = user)

    @Test
    fun `placeOrder builds order from cart items, saves it and clears the cart`() {
        val cartItems = listOf(CartItem(id = 1L, user = user, product = product, quantity = 2))
        whenever(cartService.getCartItems(user)).thenReturn(cartItems)
        whenever(orderRepository.save(any())).thenAnswer { it.arguments[0] as Order }

        val order = orderService.placeOrder(user)

        assertEquals(1, order.items.size)
        assertEquals("Widget", order.items[0].productTitle)
        assertEquals(BigDecimal("9.99"), order.items[0].productPrice)
        assertEquals(2, order.items[0].quantity)
        verify(orderRepository).save(any())
        verify(cartService).clearCart(user)
    }

    @Test
    fun `getOrdersForUser delegates to repository`() {
        val orders = listOf(Order(id = 1L, user = user))
        whenever(orderRepository.findByUserWithItems(user)).thenReturn(orders)

        val result = orderService.getOrdersForUser(user)

        assertEquals(orders, result)
    }

    @Test
    fun `findById returns order when found`() {
        val order = Order(id = 1L, user = user)
        whenever(orderRepository.findById(1L)).thenReturn(Optional.of(order))

        assertEquals(order, orderService.findById(1L))
    }

    @Test
    fun `findById returns null when not found`() {
        whenever(orderRepository.findById(99L)).thenReturn(Optional.empty())

        assertNull(orderService.findById(99L))
    }
}
