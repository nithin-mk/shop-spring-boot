package com.shop.controller

import com.shop.config.SecurityConfig
import com.shop.security.ShopUserDetailsService
import com.shop.service.UserService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class)
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
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk)
    }

    @Test
    fun `signup page returns 200`() {
        mockMvc.perform(get("/signup"))
            .andExpect(status().isOk)
    }

    @Test
    fun `reset page returns 200`() {
        mockMvc.perform(get("/reset"))
            .andExpect(status().isOk)
    }
}
