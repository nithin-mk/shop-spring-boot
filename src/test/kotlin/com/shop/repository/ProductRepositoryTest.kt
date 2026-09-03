package com.shop.repository

import com.shop.PostgresTestContainerConfig
import com.shop.model.Product
import com.shop.model.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import java.math.BigDecimal

@DataJpaTest
@Import(PostgresTestContainerConfig::class)
class ProductRepositoryTest {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var productRepository: ProductRepository

    private fun savedUser(): User {
        val user = User(email = "prod_repo@test.com", password = "hashedpassword")
        return entityManager.persist(user)
    }

    @Test
    fun `findAllByUser returns only products owned by that user`() {
        val user = savedUser()
        val other = entityManager.persist(User(email = "other@test.com", password = "hashedpassword"))
        entityManager.persist(Product(title = "Mine", price = BigDecimal("5.00"), description = "desc_mine", imageUrl = "img.jpg", user = user))
        entityManager.persist(Product(title = "Theirs", price = BigDecimal("10.00"), description = "desc_theirs", imageUrl = "img2.jpg", user = other))
        entityManager.flush()

        val result = productRepository.findAllByUser(user)

        assertEquals(1, result.size)
        assertEquals("Mine", result[0].title)
    }

    @Test
    fun `save and findById round-trip`() {
        val user = savedUser()
        val product = Product(title = "Round Trip", price = BigDecimal("3.50"), description = "test desc here", imageUrl = "img.jpg", user = user)
        entityManager.persistAndFlush(product)

        val found = productRepository.findById(product.id)

        assertTrue(found.isPresent)
        assertEquals("Round Trip", found.get().title)
    }
}
