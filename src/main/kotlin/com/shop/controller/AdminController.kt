package com.shop.controller

import com.shop.repository.UserRepository
import com.shop.service.ImageStorageService
import com.shop.service.ProductService
import com.shop.util.currentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

data class ProductForm(
    @field:NotBlank @field:Size(min = 3) val title: String = "",
    @field:DecimalMin("0.01") val price: BigDecimal = BigDecimal.ZERO,
    @field:NotBlank @field:Size(min = 5, max = 400) val description: String = ""
)

@Controller
@RequestMapping("/admin")
class AdminController(
    private val productService: ProductService,
    private val imageStorageService: ImageStorageService,
    private val userRepository: UserRepository
) {
    private fun imageUrlsFor(products: List<com.shop.model.Product>) =
        products.associate { it.id to imageStorageService.presignedUrl(it.imageUrl) }
    @GetMapping("/add-product")
    fun showAddProduct(model: Model): String {
        model.addAttribute("editing", false)
        model.addAttribute("hasError", false)
        model.addAttribute("errorMessage", null)
        model.addAttribute("validationErrors", emptyList<Any>())
        model.addAttribute("product", ProductForm())
        model.addAttribute("pageTitle", "Add Product")
        model.addAttribute("path", "/admin/add-product")
        return "admin/edit-product"
    }

    @PostMapping("/add-product")
    fun postAddProduct(
        @Valid @ModelAttribute("product") form: ProductForm,
        bindingResult: BindingResult,
        @RequestParam("image") image: MultipartFile,
        auth: Authentication,
        model: Model
    ): String {
        if (image.isEmpty) {
            model.addAttribute("editing", false)
            model.addAttribute("hasError", true)
            model.addAttribute("errorMessage", "Attached file is not an image.")
            model.addAttribute("validationErrors", emptyList<Any>())
            model.addAttribute("pageTitle", "Add Product")
            model.addAttribute("path", "/admin/add-product")
            return "admin/edit-product"
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false)
            model.addAttribute("hasError", true)
            model.addAttribute("errorMessage", bindingResult.allErrors.first().defaultMessage)
            model.addAttribute("validationErrors", bindingResult.fieldErrors)
            model.addAttribute("pageTitle", "Add Product")
            model.addAttribute("path", "/admin/add-product")
            return "admin/edit-product"
        }
        val user = auth.currentUser(userRepository)
        val imageUrl = imageStorageService.store(image)
        productService.create(form.title, form.price, form.description, imageUrl, user)
        return "redirect:/admin/products"
    }

    @GetMapping("/products")
    fun adminProducts(auth: Authentication, model: Model): String {
        val user = auth.currentUser(userRepository)
        val products = productService.findAllByUser(user)
        model.addAttribute("prods", products)
        model.addAttribute("imageUrls", imageUrlsFor(products))
        model.addAttribute("pageTitle", "Admin Products")
        model.addAttribute("path", "/admin/products")
        return "admin/products"
    }

    @GetMapping("/edit-product/{productId}")
    fun showEditProduct(
        @PathVariable productId: Long,
        @RequestParam(required = false) edit: String?,
        auth: Authentication,
        model: Model
    ): String {
        if (edit == null) return "redirect:/"
        val product = productService.findById(productId) ?: return "redirect:/"
        model.addAttribute("editing", true)
        model.addAttribute("hasError", false)
        model.addAttribute("errorMessage", null)
        model.addAttribute("validationErrors", emptyList<Any>())
        model.addAttribute("product", product)
        model.addAttribute("pageTitle", "Edit Product")
        model.addAttribute("path", "/admin/edit-product")
        return "admin/edit-product"
    }

    @PostMapping("/edit-product")
    fun postEditProduct(
        @RequestParam productId: Long,
        @Valid @ModelAttribute("product") form: ProductForm,
        bindingResult: BindingResult,
        @RequestParam("image") image: MultipartFile,
        auth: Authentication,
        model: Model
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", true)
            model.addAttribute("hasError", true)
            model.addAttribute("errorMessage", bindingResult.allErrors.first().defaultMessage)
            model.addAttribute("validationErrors", bindingResult.fieldErrors)
            model.addAttribute("pageTitle", "Edit Product")
            model.addAttribute("path", "/admin/edit-product")
            return "admin/edit-product"
        }
        val user = auth.currentUser(userRepository)
        val oldProduct = productService.findById(productId)
        var newImageUrl: String? = null
        if (!image.isEmpty) {
            oldProduct?.imageUrl?.let { imageStorageService.delete(it) }
            newImageUrl = imageStorageService.store(image)
        }
        productService.update(productId, form.title, form.price, form.description, newImageUrl, user)
        return "redirect:/admin/products"
    }

    @DeleteMapping("/product/{productId}/delete")
    @ResponseBody
    fun deleteProduct(@PathVariable productId: Long, auth: Authentication): Map<String, String> {
        val user = auth.currentUser(userRepository)
        val product = productService.findById(productId)
        product?.imageUrl?.let { imageStorageService.delete(it) }
        productService.delete(productId, user)
        return mapOf("message" to "Success!")
    }
}
