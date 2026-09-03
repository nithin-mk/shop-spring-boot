package com.shop.model

import jakarta.persistence.*

@Entity
@Table(name = "cart_items", uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "product_id"])])
class CartItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product? = null,

    @Column(nullable = false)
    var quantity: Int = 1
)
