package com.shop

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ShopApplication

fun main(args: Array<String>) {
    runApplication<ShopApplication>(*args)
}
