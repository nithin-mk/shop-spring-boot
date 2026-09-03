package com.shop.service

import com.shop.model.CartItem
import com.shop.model.User
import com.shop.repository.CartItemRepository
import com.shop.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CartService(
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository
) {
    @Transactional(readOnly = true)
    fun getCartItems(user: User): List<CartItem> = cartItemRepository.findByUserWithProduct(user)

    fun addToCart(user: User, productId: Long) {
        val product = productRepository.findById(productId).orElse(null) ?: return
        val existing = cartItemRepository.findByUserAndProductId(user, productId)
        if (existing.isPresent) {
            val item = existing.get()
            item.quantity++
            cartItemRepository.save(item)
        } else {
            cartItemRepository.save(CartItem(user = user, product = product, quantity = 1))
        }
    }

    fun removeFromCart(user: User, productId: Long) {
        val item = cartItemRepository.findByUserAndProductId(user, productId).orElse(null) ?: return
        cartItemRepository.delete(item)
    }

    fun clearCart(user: User) {
        cartItemRepository.deleteByUser(user)
    }

    @Transactional(readOnly = true)
    fun getTotal(user: User): java.math.BigDecimal =
        getCartItems(user).sumOf { it.product!!.price.multiply(java.math.BigDecimal(it.quantity)) }
}
