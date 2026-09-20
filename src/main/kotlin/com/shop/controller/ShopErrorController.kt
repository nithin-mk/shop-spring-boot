package com.shop.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.webmvc.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class ShopErrorController : ErrorController {
    @RequestMapping("/error")
    fun handleError(
        request: HttpServletRequest,
        model: Model,
    ): String {
        val status = request.getAttribute("jakarta.servlet.error.status_code") as? Int
        model.addAttribute("pageTitle", "Error")
        model.addAttribute("path", "/error")
        return if (status == HttpStatus.NOT_FOUND.value()) "404" else "500"
    }
}
