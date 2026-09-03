package com.shop.service

import com.shop.model.Order
import com.shop.model.OrderItem
import com.shop.model.User
import com.shop.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderRepository,
    private val cartService: CartService
) {
    fun placeOrder(user: User): Order {
        val cartItems = cartService.getCartItems(user)
        val order = Order(user = user)
        cartItems.forEach { cartItem ->
            val product = cartItem.product!!
            order.addItem(
                OrderItem(
                    productTitle = product.title,
                    productPrice = product.price,
                    quantity = cartItem.quantity
                )
            )
        }
        val saved = orderRepository.save(order)
        cartService.clearCart(user)
        return saved
    }

    @Transactional(readOnly = true)
    fun getOrdersForUser(user: User): List<Order> = orderRepository.findByUserWithItems(user)

    @Transactional(readOnly = true)
    fun findById(id: Long): Order? = orderRepository.findById(id).orElse(null)
}
