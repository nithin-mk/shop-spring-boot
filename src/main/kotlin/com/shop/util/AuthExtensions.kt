package com.shop.util

import com.shop.model.User
import com.shop.repository.UserRepository
import org.springframework.security.core.Authentication

fun Authentication.currentUser(userRepository: UserRepository): User =
    userRepository.findByEmail(this.name).orElseThrow {
        IllegalStateException("Authenticated user not found in DB: ${this.name}")
    }
