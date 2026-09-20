package com.shop.config

import org.springframework.boot.context.properties.ConfigurationProperties

// spring-boot-configuration-processor (run here via kapt) only sees the Java
// stub kapt generates, and — for a Kotlin data class where every constructor
// parameter has a default — that stub exposes both the real constructor and a
// synthetic no-arg one. The processor can't tell which one to bind against,
// so it documents each nested type as a bare group (e.g. "shop.minio") with
// no properties underneath. The leaf properties (shop.minio.endpoint, etc.)
// are filled in by hand in META-INF/additional-spring-configuration-metadata.json
// instead, which IDE tooling reads alongside the generated metadata.
@ConfigurationProperties(prefix = "shop")
data class ShopProperties(
    val itemsPerPage: Int = 2,
    val minio: Minio = Minio(),
    val stripe: Stripe = Stripe(),
    val upload: Upload = Upload(),
) {
    data class Minio(
        val endpoint: String = "localhost",
        val port: Int = 9000,
        val secure: Boolean = false,
        val accessKey: String = "minioadmin",
        val secretKey: String = "minioadmin",
        val bucket: String = "shop-spring-boot",
    )

    data class Stripe(
        val secretKey: String = "",
        val publishableKey: String = "",
    )

    data class Upload(
        val dir: String = "uploads",
    )
}
