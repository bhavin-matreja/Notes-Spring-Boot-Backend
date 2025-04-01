package com.example.spring_boot_notes.security

import com.example.spring_boot_notes.database.model.RefreshToken
import com.example.spring_boot_notes.database.model.User
import com.example.spring_boot_notes.database.repository.RefreshTokenRepository
import com.example.spring_boot_notes.database.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )
    fun register(email: String, password: String): User {
        val user = userRepository.findByEmail(email.trim())
        if (user != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists")
        }
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password)
            )
            // real world application - send confirmation email
        )
    }

    fun login(email: String, password: String): TokenPair {
        // check if user exists
        val user = userRepository.findByEmail(email)
            ?: throw BadCredentialsException("Invalid Credentials")
        // now here we have valid email but not checked password
        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException("Invalid Credentials")
        }

        val newAccessToken = jwtService.generateAccessToken(userId = user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        storeRefreshToken(user.id, newRefreshToken)
        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiresAt,
                hashedToken = hashed
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashByes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashByes)
    }

    /*
    User wants to refresh their access with their refresh token and get access token
    Transactional - Applied coz any of the write operations i.e refreshTokenRepository.deleteTokenByUserIdAndHashedToken
        or storeRefreshToken(user.id, newRefreshToken)
        any of that can fail. So this changes such that all will be applied or none of it.
    */
    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token")
            // this will send http 500 error with internal server error but its client who actually sent incorrect token
            // throw IllegalArgumentException("Invalid refresh token")
        }
        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            ResponseStatusException(HttpStatusCode.valueOf(404), "Invalid refresh token")
            // IllegalArgumentException("Invalid refresh token")
        }

        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(
            userId = user.id,
            hashedToken = hashed
        ) ?: throw ResponseStatusException(HttpStatusCode.valueOf(401), "Refresh token not recognized (maybe used or expired)")
        //throw IllegalArgumentException("Refresh token not recognized (maybe used or expired)")

        refreshTokenRepository.deleteTokenByUserIdAndHashedToken(
            userId = user.id,
            hashedToken = hashed
        )

        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = refreshToken
        )
    }
}