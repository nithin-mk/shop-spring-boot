package com.shop.repository

import com.shop.model.CartItem
import com.shop.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface CartItemRepository : JpaRepository<CartItem, Long> {
    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.user = :user")
    fun findByUserWithProduct(user: User): List<CartItem>

    fun findByUserAndProductId(
        user: User,
        productId: Long,
    ): Optional<CartItem>

    fun deleteByUser(user: User)
}
