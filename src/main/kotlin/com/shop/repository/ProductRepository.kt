package com.shop.repository

import com.shop.model.Product
import com.shop.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long> {
    fun findAllByUser(
        user: User,
        pageable: Pageable,
    ): Page<Product>

    fun findAllByUser(user: User): List<Product>

    fun deleteByIdAndUser(
        id: Long,
        user: User,
    ): Long
}
