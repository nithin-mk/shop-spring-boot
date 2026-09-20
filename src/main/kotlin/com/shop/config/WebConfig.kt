package com.shop.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class WebConfig(
    private val shopProperties: ShopProperties,
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val absolutePath =
            Paths
                .get(shopProperties.upload.dir)
                .toAbsolutePath()
                .toUri()
                .toString()
        registry
            .addResourceHandler("/uploads/**")
            .addResourceLocations(absolutePath)
    }
}
