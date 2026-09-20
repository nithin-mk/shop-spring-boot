package com.shop.service

import com.shop.model.PasswordResetToken
import com.shop.model.User
import com.shop.repository.PasswordResetTokenRepository
import com.shop.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Mock
    lateinit var passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder

    @InjectMocks
    lateinit var userService: UserService

    private val user = User(id = 1L, email = "test@test.com", password = "hashed")

    @Test
    fun `existsByEmail returns true when user exists`() {
        whenever(userRepository.existsByEmail("test@test.com")).thenReturn(true)
        assertTrue(userService.existsByEmail("test@test.com"))
    }

    @Test
    fun `existsByEmail returns false when user not found`() {
        whenever(userRepository.existsByEmail("new@test.com")).thenReturn(false)
        assertFalse(userService.existsByEmail("new@test.com"))
    }

    @Test
    fun `register creates and saves user with encoded password`() {
        whenever(passwordEncoder.encode("secret")).thenReturn("hashed_secret")
        whenever(userRepository.save(any())).thenReturn(user)

        val result = userService.register("test@test.com", "secret")

        verify(passwordEncoder).encode("secret")
        verify(userRepository).save(any())
    }

    @Test
    fun `createPasswordResetToken returns null when email not found`() {
        whenever(userRepository.findByEmail("nope@test.com")).thenReturn(Optional.empty())

        val token = userService.createPasswordResetToken("nope@test.com")

        assertNull(token)
    }

    @Test
    fun `createPasswordResetToken creates and returns token`() {
        whenever(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user))
        whenever(passwordResetTokenRepository.save(any())).thenAnswer { it.arguments[0] as PasswordResetToken }

        val token = userService.createPasswordResetToken("test@test.com")

        assertNotNull(token)
        verify(passwordResetTokenRepository).deleteByUser(user)
        verify(passwordResetTokenRepository).save(any())
    }

    @Test
    fun `findValidResetToken returns null when token not found`() {
        whenever(passwordResetTokenRepository.findByToken("bad")).thenReturn(Optional.empty())
        assertNull(userService.findValidResetToken("bad"))
    }

    @Test
    fun `findValidResetToken returns null when token is expired`() {
        val expired = PasswordResetToken(token = "tok", user = user, expiresAt = LocalDateTime.now().minusHours(2))
        whenever(passwordResetTokenRepository.findByToken("tok")).thenReturn(Optional.of(expired))
        assertNull(userService.findValidResetToken("tok"))
    }

    @Test
    fun `resetPassword returns false when token is invalid`() {
        whenever(passwordResetTokenRepository.findByToken("bad")).thenReturn(Optional.empty())
        assertFalse(userService.resetPassword("bad", "newpass"))
    }

    @Test
    fun `resetPassword updates password and deletes token`() {
        val resetToken = PasswordResetToken(token = "valid", user = user, expiresAt = LocalDateTime.now().plusHours(1))
        whenever(passwordResetTokenRepository.findByToken("valid")).thenReturn(Optional.of(resetToken))
        whenever(passwordEncoder.encode("newpass")).thenReturn("new_hashed")
        whenever(userRepository.save(any())).thenReturn(user)

        val result = userService.resetPassword("valid", "newpass")

        assertTrue(result)
        assertEquals("new_hashed", user.password)
        verify(passwordResetTokenRepository).delete(resetToken)
    }
}
