package com.fileshare.oneshot.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            OAuth2User user = token.getPrincipal();
            
            // Log thông tin xác thực để debug
            System.out.println("Authentication success with user: " + user.getAttribute("email"));
            
            // Sử dụng đường dẫn tuyệt đối
            String targetUrl = request.getContextPath() + "/upload";
            System.out.println("Redirecting to: " + targetUrl);
            
            // Đặt vài header để đảm bảo redirect hoạt động
            response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            
            // Redirect đến trang upload
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            // Log lỗi để debug
            System.err.println("Error in OAuth success handler: " + e.getMessage());
            e.printStackTrace();
            
            // Thử redirect về trang chủ nếu có lỗi
            getRedirectStrategy().sendRedirect(request, response, "/");
        }
    }
}
