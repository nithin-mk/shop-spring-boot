package com.shop.service

import com.shop.config.ShopProperties
import io.minio.BucketExistsArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.Http
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class ImageStorageService(
    shopProperties: ShopProperties,
) {
    private val bucket: String = shopProperties.minio.bucket

    private val client: MinioClient =
        MinioClient
            .builder()
            .endpoint(shopProperties.minio.endpoint, shopProperties.minio.port, shopProperties.minio.secure)
            .credentials(shopProperties.minio.accessKey, shopProperties.minio.secretKey)
            .build()

    init {
        val exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }
    }

    fun store(file: MultipartFile): String {
        val ext = file.originalFilename?.substringAfterLast('.', "jpg") ?: "jpg"
        val objectName = "${UUID.randomUUID()}.$ext"
        file.inputStream.use { stream ->
            client.putObject(
                PutObjectArgs
                    .builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .stream(stream, file.size, -1L)
                    .contentType(file.contentType ?: "image/jpeg")
                    .build(),
            )
        }
        return "images/$objectName"
    }

    fun delete(imageUrl: String) {
        try {
            val objectName = imageUrl.substringAfterLast('/')
            client.removeObject(
                RemoveObjectArgs
                    .builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .build(),
            )
        } catch (_: Exception) {
        }
    }

    fun presignedUrl(imageUrl: String): String {
        val objectName = imageUrl.substringAfterLast('/')
        return client.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs
                .builder()
                .method(Http.Method.GET)
                .bucket(bucket)
                .`object`(objectName)
                .expiry(1, TimeUnit.HOURS)
                .build(),
        )
    }
}
