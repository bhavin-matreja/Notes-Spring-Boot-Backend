package com.example.spring_boot_notes.database.repository

import com.example.spring_boot_notes.database.model.RefreshToken
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface RefreshTokenRepository : MongoRepository<RefreshToken, ObjectId> {
    fun findByUserIdAndHashedToken(userId: ObjectId, hashedToken:String): RefreshToken?
    // if user relogins before earlier refresh token expires, we delete old refresh token
    fun deleteTokenByUserIdAndHashedToken(userId: ObjectId, hashedToken: String)
}