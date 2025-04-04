package com.example.spring_boot_notes.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Suppress("DEPRECATION")
@Service
class JwtService(
   @Value("\${jwt.secret}") private val jwtSecret: String
) {

    // Generate a secure 256-bit key
    final val secretKey256bit = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256)

    // Encode the key to base64, and store this base 64 string.
    val secretString = Base64.getEncoder().encodeToString(secretKey256bit.encoded)

    // Then in your springbootsecret variable, place the generated string.
    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretString))

    // sprintbootsecret
//    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

    // access token is the real token that grants the real access to our backend. It is typically
    // a very short lived token and would mention time that it would expire. An hour or 15mins
    private val accessTokenValidityMs = 15L * 60L * 1000L // 15mins

    /* refresh token has longer validity - one or two weeks. Only with refresh token client is
     able to refresh their access token. So when their short lived access token expires and client
    client will still have access to refresh token in order to refresh their access token automatically
    in the background so they keep their access token without having to relogin.

    Once the refresh token expires, the user will only get authentication token by relogin in.
    Only after login user able to get refresh token
     */

    val refreshTokenValidityMs = 30L * 24L * 60L * 60L * 1000L // 30 days

    private fun generateToken(
        userId: String,
        type: String,
        expiry: Long
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)
        return Jwts.builder()
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    fun generateAccessToken(userId: String): String {
        return generateToken(userId, "access", accessTokenValidityMs)
    }

    fun generateRefreshToken(userId: String): String {
        return generateToken(userId, "refresh", refreshTokenValidityMs)
    }

    fun validateAccessToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "access"
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "refresh"
    }

    // Authorization: Bearer <token>
    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token) ?:
        throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid Token")
        //throw IllegalArgumentException("Invalid Token")
        return claims.subject
    }

    private fun parseAllClaims(token: String): Claims? {
        val rawToken = if(token.startsWith("Bearer ")) {
            token.removePrefix("Bearer ")
        } else token
        return try {
            Jwts.parser() // first parse this token
                .verifyWith(secretKey) //parse with secret key
                .build()
                .parseSignedClaims(rawToken)
                .payload
        } catch(e: Exception) {
            null
        }
    }
}