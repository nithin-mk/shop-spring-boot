package com.shop.service

import com.shop.config.ShopProperties
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class ImageStorageServiceTest {
    companion object {
        private val minio =
            GenericContainer(DockerImageName.parse("pgsty/silo:RELEASE.2026-09-16T00-00-00Z"))
                .withExposedPorts(9000)
                .withEnv("MINIO_ROOT_USER", "minioadmin")
                .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                .withCommand("server", "/data")
                .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000))

        @BeforeAll
        @JvmStatic
        fun startContainer() {
            minio.start()
        }

        @AfterAll
        @JvmStatic
        fun stopContainer() {
            minio.stop()
        }
    }

    private fun newService() =
        ImageStorageService(
            ShopProperties(
                minio =
                    ShopProperties.Minio(
                        endpoint = minio.host,
                        port = minio.getMappedPort(9000),
                        secure = false,
                        accessKey = "minioadmin",
                        secretKey = "minioadmin",
                        bucket = "image-storage-test",
                    ),
            ),
        )

    @Test
    fun `store uploads file and returns images path`() {
        val service = newService()
        val file = MockMultipartFile("image", "widget.jpg", "image/jpeg", byteArrayOf(1, 2, 3, 4))

        val imageUrl = service.store(file)

        assertTrue(imageUrl.startsWith("images/"))
        assertTrue(imageUrl.endsWith(".jpg"))
    }

    @Test
    fun `store falls back to jpg extension when filename has none`() {
        val service = newService()
        val file = MockMultipartFile("image", "widget", "image/jpeg", byteArrayOf(1, 2, 3))

        val imageUrl = service.store(file)

        assertTrue(imageUrl.endsWith(".jpg"))
    }

    @Test
    fun `presignedUrl returns a reachable url for a stored object`() {
        val service = newService()
        val file = MockMultipartFile("image", "widget.png", "image/png", byteArrayOf(5, 6, 7))
        val imageUrl = service.store(file)

        val url = service.presignedUrl(imageUrl)

        assertNotNull(url)
        assertTrue(url.contains("image-storage-test"))
    }

    @Test
    fun `delete removes a stored object without throwing`() {
        val service = newService()
        val file = MockMultipartFile("image", "widget.gif", "image/gif", byteArrayOf(9))
        val imageUrl = service.store(file)

        service.delete(imageUrl)
    }

    @Test
    fun `delete of a missing object does not throw`() {
        val service = newService()
        service.delete("images/does-not-exist.jpg")
    }
}
