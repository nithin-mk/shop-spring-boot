package com.shop.repository

import com.shop.PostgresTestContainerConfig
import com.shop.model.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(PostgresTestContainerConfig::class)
class UserRepositoryTest {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `findByEmail returns user when exists`() {
        val user = User(email = "repo@test.com", password = "hashedpassword")
        entityManager.persistAndFlush(user)

        val found = userRepository.findByEmail("repo@test.com")

        assertTrue(found.isPresent)
        assertEquals("repo@test.com", found.get().email)
    }

    @Test
    fun `findByEmail returns empty when not exists`() {
        val found = userRepository.findByEmail("nobody@test.com")
        assertFalse(found.isPresent)
    }

    @Test
    fun `existsByEmail returns true when user exists`() {
        val user = User(email = "exists@test.com", password = "hashedpassword")
        entityManager.persistAndFlush(user)

        assertTrue(userRepository.existsByEmail("exists@test.com"))
    }

    @Test
    fun `existsByEmail returns false when user not found`() {
        assertFalse(userRepository.existsByEmail("ghost@test.com"))
    }
}
