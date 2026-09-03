package com.shop.repository

import com.shop.model.Order
import com.shop.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OrderRepository : JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.user = :user ORDER BY o.createdAt DESC")
    fun findByUserWithItems(user: User): List<Order>
}
