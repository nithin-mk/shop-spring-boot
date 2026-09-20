package com.shop.controller

import com.shop.service.UserService
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes

data class SignupForm(
    @field:Email @field:NotBlank val email: String = "",
    @field:NotBlank @field:Size(min = 5) val password: String = "",
    @field:NotBlank val confirmPassword: String = "",
)

@Controller
class AuthController(
    private val userService: UserService,
    private val mailSender: JavaMailSender,
) {
    @GetMapping("/login")
    fun loginPage(
        @RequestParam(required = false) error: String?,
        model: Model,
    ): String {
        model.addAttribute("errorMessage", if (error != null) "Invalid email or password." else null)
        model.addAttribute("oldInput", mapOf("email" to "", "password" to ""))
        model.addAttribute("validationErrors", emptyList<Any>())
        model.addAttribute("pageTitle", "Login")
        model.addAttribute("path", "/login")
        return "auth/login"
    }

    @GetMapping("/signup")
    fun signupPage(model: Model): String {
        model.addAttribute("errorMessage", null)
        model.addAttribute("oldInput", mapOf("email" to "", "password" to "", "confirmPassword" to ""))
        model.addAttribute("validationErrors", emptyList<Any>())
        model.addAttribute("pageTitle", "Signup")
        model.addAttribute("path", "/signup")
        return "auth/signup"
    }

    @PostMapping("/signup")
    fun signup(
        @Valid @ModelAttribute form: SignupForm,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        val oldInput = mapOf("email" to form.email, "password" to form.password, "confirmPassword" to form.confirmPassword)
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", bindingResult.allErrors.first().defaultMessage)
            model.addAttribute("oldInput", oldInput)
            model.addAttribute("validationErrors", bindingResult.fieldErrors)
            model.addAttribute("pageTitle", "Signup")
            model.addAttribute("path", "/signup")
            return "auth/signup"
        }
        if (form.password != form.confirmPassword) {
            model.addAttribute("errorMessage", "Passwords have to match!")
            model.addAttribute("oldInput", oldInput)
            model.addAttribute("validationErrors", emptyList<Any>())
            model.addAttribute("pageTitle", "Signup")
            model.addAttribute("path", "/signup")
            return "auth/signup"
        }
        if (userService.existsByEmail(form.email)) {
            model.addAttribute("errorMessage", "E-Mail exists already, please pick a different one.")
            model.addAttribute("oldInput", oldInput)
            model.addAttribute("validationErrors", emptyList<Any>())
            model.addAttribute("pageTitle", "Signup")
            model.addAttribute("path", "/signup")
            return "auth/signup"
        }
        userService.register(form.email, form.password)
        return "redirect:/login"
    }

    @GetMapping("/reset")
    fun resetPage(
        @RequestParam(required = false) error: String?,
        model: Model,
    ): String {
        model.addAttribute("errorMessage", if (error != null) "No account with that email found." else null)
        model.addAttribute("pageTitle", "Reset Password")
        model.addAttribute("path", "/reset")
        return "auth/reset"
    }

    @PostMapping("/reset")
    fun postReset(
        @RequestParam email: String,
        redirectAttributes: RedirectAttributes,
    ): String {
        val token = userService.createPasswordResetToken(email)
        if (token == null) {
            redirectAttributes.addAttribute("error", "")
            return "redirect:/reset"
        }
        try {
            val msg = SimpleMailMessage()
            msg.setTo(email)
            msg.subject = "Password reset"
            msg.text = "Click this link to set a new password: http://localhost:8080/reset/$token"
            mailSender.send(msg)
        } catch (_: Exception) {
        }
        return "redirect:/"
    }

    @GetMapping("/reset/{token}")
    fun newPasswordPage(
        @PathVariable token: String,
        model: Model,
    ): String {
        val resetToken =
            userService.findValidResetToken(token)
                ?: return "redirect:/reset?error"
        model.addAttribute("errorMessage", null)
        model.addAttribute("userId", resetToken.user?.id)
        model.addAttribute("passwordToken", token)
        model.addAttribute("pageTitle", "New Password")
        model.addAttribute("path", "/new-password")
        return "auth/new-password"
    }

    @PostMapping("/new-password")
    fun postNewPassword(
        @RequestParam password: String,
        @RequestParam passwordToken: String,
        model: Model,
    ): String {
        val success = userService.resetPassword(passwordToken, password)
        if (!success) {
            model.addAttribute("errorMessage", "Invalid or expired token.")
            model.addAttribute("pageTitle", "New Password")
            model.addAttribute("path", "/new-password")
            return "auth/new-password"
        }
        return "redirect:/login"
    }
}
