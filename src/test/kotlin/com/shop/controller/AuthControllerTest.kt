package com.shop.controller

import com.shop.config.SecurityConfig
import com.shop.config.ShopProperties
import com.shop.model.PasswordResetToken
import com.shop.model.User
import com.shop.security.ShopUserDetailsService
import com.shop.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class)
@EnableConfigurationProperties(ShopProperties::class)
class AuthControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var userService: UserService

    @MockitoBean
    lateinit var mailSender: JavaMailSender

    @MockitoBean
    lateinit var userDetailsService: ShopUserDetailsService

    @Test
    fun `login page returns 200`() {
        mockMvc
            .perform(get("/login"))
            .andExpect(status().isOk)
    }

    @Test
    fun `signup page returns 200`() {
        mockMvc
            .perform(get("/signup"))
            .andExpect(status().isOk)
    }

    @Test
    fun `reset page returns 200`() {
        mockMvc
            .perform(get("/reset"))
            .andExpect(status().isOk)
    }

    @Test
    fun `signup with validation error re-renders form`() {
        mockMvc
            .perform(
                post("/signup")
                    .param("email", "not-an-email")
                    .param("password", "secret1")
                    .param("confirmPassword", "secret1")
                    .with(csrf()),
            ).andExpect(status().isOk)

        verify(userService, org.mockito.kotlin.never()).register(any(), any())
    }

    @Test
    fun `signup with mismatched passwords re-renders form`() {
        mockMvc
            .perform(
                post("/signup")
                    .param("email", "new@test.com")
                    .param("password", "secret1")
                    .param("confirmPassword", "different1")
                    .with(csrf()),
            ).andExpect(status().isOk)

        verify(userService, org.mockito.kotlin.never()).register(any(), any())
    }

    @Test
    fun `signup with existing email re-renders form`() {
        whenever(userService.existsByEmail("dup@test.com")).thenReturn(true)

        mockMvc
            .perform(
                post("/signup")
                    .param("email", "dup@test.com")
                    .param("password", "secret1")
                    .param("confirmPassword", "secret1")
                    .with(csrf()),
            ).andExpect(status().isOk)

        verify(userService, org.mockito.kotlin.never()).register(any(), any())
    }

    @Test
    fun `signup with valid data registers user and redirects to login`() {
        whenever(userService.existsByEmail("new@test.com")).thenReturn(false)

        mockMvc
            .perform(
                post("/signup")
                    .param("email", "new@test.com")
                    .param("password", "secret1")
                    .param("confirmPassword", "secret1")
                    .with(csrf()),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/login"))

        verify(userService).register("new@test.com", "secret1")
    }

    @Test
    fun `postReset with unknown email redirects back to reset`() {
        whenever(userService.createPasswordResetToken("nobody@test.com")).thenReturn(null)

        mockMvc
            .perform(post("/reset").param("email", "nobody@test.com").with(csrf()))
            .andExpect(status().is3xxRedirection)

        verify(mailSender, org.mockito.kotlin.never()).send(any<SimpleMailMessage>())
    }

    @Test
    fun `postReset with known email sends mail and redirects home`() {
        whenever(userService.createPasswordResetToken("user@test.com")).thenReturn("reset-token")

        mockMvc
            .perform(post("/reset").param("email", "user@test.com").with(csrf()))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        verify(mailSender).send(any<SimpleMailMessage>())
    }

    @Test
    fun `postReset swallows mail sending failures`() {
        whenever(userService.createPasswordResetToken("user@test.com")).thenReturn("reset-token")
        doThrow(RuntimeException("smtp down")).whenever(mailSender).send(any<SimpleMailMessage>())

        mockMvc
            .perform(post("/reset").param("email", "user@test.com").with(csrf()))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))
    }

    @Test
    fun `newPasswordPage redirects when token invalid`() {
        whenever(userService.findValidResetToken("bad-token")).thenReturn(null)

        mockMvc
            .perform(get("/reset/bad-token"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    fun `newPasswordPage returns 200 when token valid`() {
        val user = User(id = 1L, email = "user@test.com", password = "hashed")
        val resetToken = PasswordResetToken(id = 1L, token = "good-token", user = user)
        whenever(userService.findValidResetToken("good-token")).thenReturn(resetToken)

        mockMvc
            .perform(get("/reset/good-token"))
            .andExpect(status().isOk)
    }

    @Test
    fun `postNewPassword with invalid token re-renders form`() {
        whenever(userService.resetPassword("bad-token", "newpass1")).thenReturn(false)

        mockMvc
            .perform(
                post("/new-password")
                    .param("password", "newpass1")
                    .param("passwordToken", "bad-token")
                    .with(csrf()),
            ).andExpect(status().isOk)
    }

    @Test
    fun `postNewPassword with valid token redirects to login`() {
        whenever(userService.resetPassword("good-token", "newpass1")).thenReturn(true)

        mockMvc
            .perform(
                post("/new-password")
                    .param("password", "newpass1")
                    .param("passwordToken", "good-token")
                    .with(csrf()),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/login"))
    }
}
