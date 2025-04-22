package com.fileshare.oneshot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

@Configuration
public class OAuthConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(this.googleClientRegistration());
    }

    private ClientRegistration googleClientRegistration() {
        // Thay thế các giá trị dưới đây với Client ID và Client Secret thực tế của bạn
        // Client ID thường có dạng như: 123456789012-xxxxxxxxxxxxxxxx.apps.googleusercontent.com
        // Client Secret thường bắt đầu bằng: GOCSPX-
        return ClientRegistration.withRegistrationId("google")
            .clientId("34101523698-vfpf19ii339i78ps9tlrfai95s5v991k.apps.googleusercontent.com") // ĐÂY LÀ CLIENT ID ĐÚNG TỪ LOG
            .clientSecret("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") // ĐANG SỬ DỤNG CLIENT SECRET TỪ LOG
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:5000/login/oauth2/code/google")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .clientName("Google")
            .build();
    }
}