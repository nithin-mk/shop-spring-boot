package com.shop.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class WebConfig(
    @Value("\${shop.upload.dir:uploads}") private val uploadDir: String
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString()
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(absolutePath)
    }
}
