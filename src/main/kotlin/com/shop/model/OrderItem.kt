package com.shop.model

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null,

    @Column(nullable = false)
    var productTitle: String = "",

    @Column(nullable = false, precision = 10, scale = 2)
    var productPrice: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var quantity: Int = 1
)
