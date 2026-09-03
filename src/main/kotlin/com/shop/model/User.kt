package com.shop.model

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @field:Email
    @field:NotBlank
    @Column(nullable = false, unique = true)
    var email: String = "",

    @field:NotBlank
    @Column(nullable = false)
    var password: String = "",

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var cartItems: MutableList<CartItem> = mutableListOf()
)
