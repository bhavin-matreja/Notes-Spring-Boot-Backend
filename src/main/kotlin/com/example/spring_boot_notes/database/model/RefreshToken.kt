package com.example.spring_boot_notes.database.model

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("refresh_tokens")
data class RefreshToken(
    val userId: ObjectId,
    @Indexed(expireAfter = "0s") // faster access using binary search and mongoDb will periodically check and delete when expired
    val expiresAt: Instant,
    val hashedToken: String,
    val createdAt: Instant = Instant.now()
)
