package com.shop.security

import com.shop.model.User
import com.shop.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ShopUserDetailsServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var userDetailsService: ShopUserDetailsService

    @Test
    fun `loadUserByUsername returns user details when found`() {
        val user = User(id = 1L, email = "test@test.com", password = "hashed")
        whenever(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user))

        val details = userDetailsService.loadUserByUsername("test@test.com")

        assertEquals("test@test.com", details.username)
        assertEquals("hashed", details.password)
        assertEquals(1, details.authorities.size)
    }

    @Test
    fun `loadUserByUsername throws when user not found`() {
        whenever(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty())

        assertThrows(UsernameNotFoundException::class.java) {
            userDetailsService.loadUserByUsername("nobody@test.com")
        }
    }
}
