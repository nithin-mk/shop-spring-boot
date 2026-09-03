package com.shop.controller

import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.shop.repository.UserRepository
import com.shop.service.CartService
import com.shop.service.ImageStorageService
import com.shop.service.OrderService
import com.shop.service.ProductService
import com.shop.service.StripeService
import com.shop.util.currentUser
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
class ShopController(
    private val productService: ProductService,
    private val cartService: CartService,
    private val orderService: OrderService,
    private val userRepository: UserRepository,
    private val imageStorageService: ImageStorageService,
    private val stripeService: StripeService
) {
    @GetMapping("/")
    fun index(@RequestParam(defaultValue = "1") page: Int, model: Model): String {
        val productPage = productService.findAll(page)
        model.addAttribute("prods", productPage.content)
        model.addAttribute("imageUrls", productPage.content.associate { it.id to imageStorageService.presignedUrl(it.imageUrl) })
        model.addAttribute("currentPage", page)
        model.addAttribute("hasNextPage", productPage.hasNext())
        model.addAttribute("hasPreviousPage", productPage.hasPrevious())
        model.addAttribute("nextPage", page + 1)
        model.addAttribute("previousPage", page - 1)
        model.addAttribute("lastPage", productPage.totalPages.coerceAtLeast(1))
        model.addAttribute("pageTitle", "Shop")
        model.addAttribute("path", "/")
        return "shop/index"
    }

    @GetMapping("/products")
    fun products(@RequestParam(defaultValue = "1") page: Int, model: Model): String {
        val productPage = productService.findAll(page)
        model.addAttribute("prods", productPage.content)
        model.addAttribute("imageUrls", productPage.content.associate { it.id to imageStorageService.presignedUrl(it.imageUrl) })
        model.addAttribute("currentPage", page)
        model.addAttribute("hasNextPage", productPage.hasNext())
        model.addAttribute("hasPreviousPage", productPage.hasPrevious())
        model.addAttribute("nextPage", page + 1)
        model.addAttribute("previousPage", page - 1)
        model.addAttribute("lastPage", productPage.totalPages.coerceAtLeast(1))
        model.addAttribute("pageTitle", "Products")
        model.addAttribute("path", "/products")
        return "shop/product-list"
    }

    @GetMapping("/products/{productId}")
    fun productDetail(@PathVariable productId: Long, model: Model): String {
        val product = productService.findById(productId) ?: return "redirect:/"
        model.addAttribute("product", product)
        model.addAttribute("imageUrl", imageStorageService.presignedUrl(product.imageUrl))
        model.addAttribute("pageTitle", product.title)
        model.addAttribute("path", "/products")
        return "shop/product-detail"
    }

    @GetMapping("/cart")
    fun cart(auth: Authentication, model: Model): String {
        val user = auth.currentUser(userRepository)
        val items = cartService.getCartItems(user)
        model.addAttribute("products", items)
        model.addAttribute("pageTitle", "Your Cart")
        model.addAttribute("path", "/cart")
        return "shop/cart"
    }

    @PostMapping("/cart")
    fun addToCart(@RequestParam productId: Long, auth: Authentication): String {
        cartService.addToCart(auth.currentUser(userRepository), productId)
        return "redirect:/cart"
    }

    @PostMapping("/cart-delete-item")
    fun removeFromCart(@RequestParam productId: Long, auth: Authentication): String {
        cartService.removeFromCart(auth.currentUser(userRepository), productId)
        return "redirect:/cart"
    }

    @GetMapping("/checkout")
    fun checkout(auth: Authentication, model: Model): String {
        val user = auth.currentUser(userRepository)
        val items = cartService.getCartItems(user)
        val total = cartService.getTotal(user)
        model.addAttribute("products", items)
        model.addAttribute("totalSum", total)
        model.addAttribute("stripePublishableKey", if (stripeService.enabled) stripeService.publishableKey else null)
        model.addAttribute("errorMessage", null)
        model.addAttribute("pageTitle", "Checkout")
        model.addAttribute("path", "/checkout")
        return "shop/checkout"
    }

    @PostMapping("/create-order")
    fun createOrder(
        @RequestParam(required = false) stripeToken: String?,
        auth: Authentication,
        model: Model
    ): String {
        val user = auth.currentUser(userRepository)
        if (stripeService.enabled) {
            if (stripeToken.isNullOrBlank()) {
                val items = cartService.getCartItems(user)
                val total = cartService.getTotal(user)
                model.addAttribute("products", items)
                model.addAttribute("totalSum", total)
                model.addAttribute("stripePublishableKey", stripeService.publishableKey)
                model.addAttribute("errorMessage", "Payments are not configured.")
                model.addAttribute("pageTitle", "Checkout")
                model.addAttribute("path", "/checkout")
                return "shop/checkout"
            }
            val total = cartService.getTotal(user)
            val amountCents = total.multiply(java.math.BigDecimal(100)).toLong()
            try {
                stripeService.charge(stripeToken, amountCents, "Demo Order")
            } catch (e: Exception) {
                val items = cartService.getCartItems(user)
                model.addAttribute("products", items)
                model.addAttribute("totalSum", total)
                model.addAttribute("stripePublishableKey", stripeService.publishableKey)
                model.addAttribute("errorMessage", "Payment failed: ${e.message}")
                model.addAttribute("pageTitle", "Checkout")
                model.addAttribute("path", "/checkout")
                return "shop/checkout"
            }
        }
        orderService.placeOrder(user)
        return "redirect:/orders"
    }

    @GetMapping("/orders")
    fun orders(auth: Authentication, model: Model): String {
        val user = auth.currentUser(userRepository)
        val orders = orderService.getOrdersForUser(user)
        model.addAttribute("orders", orders)
        model.addAttribute("pageTitle", "Your Orders")
        model.addAttribute("path", "/orders")
        return "shop/orders"
    }

    @GetMapping("/orders/{orderId}")
    fun invoice(@PathVariable orderId: Long, auth: Authentication, response: HttpServletResponse) {
        val user = auth.currentUser(userRepository)
        val order = orderService.findById(orderId)
            ?: run { response.sendError(404); return }
        if (order.user?.id != user.id) {
            response.sendError(403)
            return
        }
        response.contentType = MediaType.APPLICATION_PDF_VALUE
        response.setHeader("Content-Disposition", "inline; filename=\"invoice-$orderId.pdf\"")
        generateInvoicePdf(order, response.outputStream)
    }

    private fun generateInvoicePdf(order: com.shop.model.Order, out: java.io.OutputStream) {
        val pdfWriter = PdfWriter(out)
        val pdfDoc = PdfDocument(pdfWriter)
        val document = Document(pdfDoc)
        try {
            val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
            document.add(
                Paragraph("Invoice")
                    .setFont(boldFont)
                    .setFontSize(26f)
                    .setUnderline()
            )
            document.add(Paragraph("-----------------------"))
            var totalPrice = java.math.BigDecimal.ZERO
            order.items.forEach { item ->
                totalPrice = totalPrice.add(item.productPrice.multiply(java.math.BigDecimal(item.quantity)))
                document.add(
                    Paragraph("${item.productTitle} - ${item.quantity} x \$${item.productPrice}")
                        .setFontSize(14f)
                )
            }
            document.add(Paragraph("---"))
            document.add(Paragraph("Total Price: \$$totalPrice").setFontSize(20f))
        } finally {
            document.close()
        }
    }
}
