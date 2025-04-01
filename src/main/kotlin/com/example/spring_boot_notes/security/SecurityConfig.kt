package com.example.spring_boot_notes.security

import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {

    @Bean
    fun filterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        return httpSecurity
            .csrf { csrf -> csrf.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/auth/**")
                    .permitAll() // permit auth/** without any authentication i.e. without user requiring to add any bearer token
                    .dispatcherTypeMatchers(
                        DispatcherType.ERROR,
                        DispatcherType.FORWARD
                    ) // (For Above 2 lines) if request actually results in an error then spring security wont intercept that request
                    // if we dont do this and client attach invalid data that can't be parsed from JSON to data classes
                    // then sprint boot will correctly map to BAD REQUEST error or 403 forbidden error before specific error can even reach client
                    // so with this we tell we want more fine grin error handling
                    .permitAll()
                    .anyRequest() // after above, any request requires authentication (auth header to be specified)
                    // these are the rules sprint security will respond to
                    .authenticated()
            }
            .exceptionHandling { configurer ->
                // if authentication fails then we by default want to respond with 401 instead of 403
                configurer
                    .authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))

            }
            // security config and auth filter both will filter request, above will validate on the security side
            // jwt filter will update our auth object
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}