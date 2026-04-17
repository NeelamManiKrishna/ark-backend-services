package com.app.ark_backend_services.security;

import com.app.ark_backend_services.model.User;
import com.app.ark_backend_services.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenType = jwtUtil.getTokenType(token);
        if (!"access".equals(tokenType)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = jwtUtil.getUserId(token);
        User user = userRepository.findById(userId).orElse(null);

        if (user == null || user.getStatus() != User.UserStatus.ACTIVE) {
            filterChain.doFilter(request, response);
            return;
        }

        String role = user.getRole().name();
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
