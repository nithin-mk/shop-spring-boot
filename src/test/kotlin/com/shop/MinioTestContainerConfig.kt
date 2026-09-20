package com.shop

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * Full-context tests (e.g. ShopIntegrationTest) boot the real ImageStorageService
 * bean, which connects to MinIO on startup — without this, that connection goes
 * to whatever shop.minio.endpoint/port default to, which only happens to work
 * when a matching MinIO instance is already running locally (e.g. via
 * compose.yaml) and fails outright in CI.
 */
@TestConfiguration(proxyBeanMethods = false)
class MinioTestContainerConfig {
    companion object {
        private val minioContainer =
            GenericContainer(DockerImageName.parse("pgsty/silo:RELEASE.2026-09-16T00-00-00Z"))
                .withExposedPorts(9000)
                .withEnv("MINIO_ROOT_USER", "minioadmin")
                .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                .withCommand("server", "/data")
                .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000))
                .apply { start() }
    }

    @Bean
    fun minioDynamicProperties(): DynamicPropertyRegistrar =
        DynamicPropertyRegistrar { registry ->
            registry.add("shop.minio.endpoint") { minioContainer.host }
            registry.add("shop.minio.port") { minioContainer.getMappedPort(9000) }
        }
}
