package com.shop.service

import com.shop.config.ShopProperties
import com.shop.model.Product
import com.shop.model.User
import com.shop.repository.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class ProductService(
    private val productRepository: ProductRepository,
    private val shopProperties: ShopProperties,
) {
    @Transactional(readOnly = true)
    fun findAll(page: Int): Page<Product> = productRepository.findAll(PageRequest.of(page - 1, shopProperties.itemsPerPage))

    @Transactional(readOnly = true)
    fun findById(id: Long): Product? = productRepository.findById(id).orElse(null)

    @Transactional(readOnly = true)
    fun findAllByUser(user: User): List<Product> = productRepository.findAllByUser(user)

    fun create(
        title: String,
        price: BigDecimal,
        description: String,
        imageUrl: String,
        user: User,
    ): Product {
        val product =
            Product(
                title = title,
                price = price,
                description = description,
                imageUrl = imageUrl,
                user = user,
            )
        return productRepository.save(product)
    }

    fun update(
        id: Long,
        title: String,
        price: BigDecimal,
        description: String,
        imageUrl: String?,
        user: User,
    ): Product? {
        val product = productRepository.findById(id).orElse(null) ?: return null
        if (product.user?.id != user.id) return null
        product.title = title
        product.price = price
        product.description = description
        if (imageUrl != null) product.imageUrl = imageUrl
        return productRepository.save(product)
    }

    fun delete(
        id: Long,
        user: User,
    ): Boolean {
        val product = productRepository.findById(id).orElse(null) ?: return false
        if (product.user?.id != user.id) return false
        productRepository.delete(product)
        return true
    }
}
