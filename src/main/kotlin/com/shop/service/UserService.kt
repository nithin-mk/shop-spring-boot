package com.shop.service

import com.shop.model.PasswordResetToken
import com.shop.model.User
import com.shop.repository.PasswordResetTokenRepository
import com.shop.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun existsByEmail(email: String): Boolean = userRepository.existsByEmail(email)

    fun register(
        email: String,
        rawPassword: String,
    ): User {
        val user =
            User(
                email = email,
                password = passwordEncoder.encode(rawPassword)!!,
            )
        return userRepository.save(user)
    }

    fun createPasswordResetToken(email: String): String? {
        val user = userRepository.findByEmail(email).orElse(null) ?: return null
        passwordResetTokenRepository.deleteByUser(user)
        val token = UUID.randomUUID().toString()
        passwordResetTokenRepository.save(PasswordResetToken(token = token, user = user))
        return token
    }

    fun findValidResetToken(token: String): PasswordResetToken? {
        val resetToken = passwordResetTokenRepository.findByToken(token).orElse(null) ?: return null
        return if (resetToken.isExpired()) null else resetToken
    }

    fun resetPassword(
        token: String,
        newPassword: String,
    ): Boolean {
        val resetToken = findValidResetToken(token) ?: return false
        val user = resetToken.user ?: return false
        user.password = passwordEncoder.encode(newPassword)!!
        userRepository.save(user)
        passwordResetTokenRepository.delete(resetToken)
        return true
    }
}
