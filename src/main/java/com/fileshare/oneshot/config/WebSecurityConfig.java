package com.fileshare.oneshot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private OAuthSuccessHandler oAuthSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests(authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/", "/css/**", "/js/**", "/favicon.ico").permitAll() // Only allow homepage and static resources
                    .requestMatchers("/login/oauth2/code/google").permitAll() // Allow callback URL without authentication
                    .requestMatchers("/login/oauth2/code/**").permitAll() // Allow all OAuth callback URLs
                    .requestMatchers("/oauth2/**").permitAll() // Allow all OAuth URLs
                    .requestMatchers("/download").permitAll() // Allow download page view
                    .requestMatchers("/api/download/**").authenticated() // Require authentication for download APIs
                    .anyRequest().authenticated() // Require authentication for all other URLs (including /upload)
            )
            .oauth2Login(oauth2Login ->
                oauth2Login
                    .loginPage("/")
                    .defaultSuccessUrl("/", true) // Always redirect to homepage after successful login
                    .successHandler(oAuthSuccessHandler)
                    .failureUrl("/?error=oauth_failure") // URL when login fails
                    .permitAll()
            )
            .logout(logout ->
                logout
                    .logoutSuccessUrl("/")
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "https://accounts.google.com", 
            "http://localhost:5000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}