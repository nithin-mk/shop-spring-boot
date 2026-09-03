package com.shop.service

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.Http
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class ImageStorageService(
    @Value("\${shop.minio.endpoint:localhost}") endpoint: String,
    @Value("\${shop.minio.port:9000}") port: Int,
    @Value("\${shop.minio.secure:false}") secure: Boolean,
    @Value("\${shop.minio.access-key:minioadmin}") accessKey: String,
    @Value("\${shop.minio.secret-key:minioadmin}") secretKey: String,
    @Value("\${shop.minio.bucket:shop-spring-boot}") private val bucket: String
) {
    private val client: MinioClient = MinioClient.builder()
        .endpoint(endpoint, port, secure)
        .credentials(accessKey, secretKey)
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
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .stream(stream, file.size, -1L)
                    .contentType(file.contentType ?: "image/jpeg")
                    .build()
            )
        }
        return "images/$objectName"
    }

    fun delete(imageUrl: String) {
        try {
            val objectName = imageUrl.substringAfterLast('/')
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(objectName).build())
        } catch (_: Exception) {}
    }

    fun presignedUrl(imageUrl: String): String {
        val objectName = imageUrl.substringAfterLast('/')
        return client.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Http.Method.GET)
                .bucket(bucket)
                .`object`(objectName)
                .expiry(1, TimeUnit.HOURS)
                .build()
        )
    }
}
