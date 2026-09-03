package com.shop.model

import jakarta.persistence.*
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Entity
@Table(name = "products")
class Product(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @field:NotBlank
    @field:Size(min = 3)
    @Column(nullable = false)
    var title: String = "",

    @field:DecimalMin("0.01")
    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO,

    @field:NotBlank
    @field:Size(min = 5, max = 400)
    @Column(nullable = false, length = 400)
    var description: String = "",

    @Column(nullable = false)
    var imageUrl: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null
)
