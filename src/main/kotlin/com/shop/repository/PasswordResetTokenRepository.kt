package com.shop.repository

import com.shop.model.PasswordResetToken
import com.shop.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): Optional<PasswordResetToken>

    fun deleteByUser(user: User)
}
