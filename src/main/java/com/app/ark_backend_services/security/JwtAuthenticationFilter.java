package com.app.ark_backend_services.security;

import com.app.ark_backend_services.exception.ErrorCodes;
import com.app.ark_backend_services.model.Branch;
import com.app.ark_backend_services.model.Organization;
import com.app.ark_backend_services.model.User;
import com.app.ark_backend_services.repository.BranchRepository;
import com.app.ark_backend_services.repository.OrganizationRepository;
import com.app.ark_backend_services.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
import java.time.Instant;
import java.util.List;

/**
 * Authenticates Bearer access tokens. Beyond signature/expiry it checks, once per request, that the user
 * is ACTIVE and not locked, the token version still matches, and the user's organization/branch are active.
 * On failure the request continues unauthenticated with the reason in a request attribute, which
 * {@link AuthEntryPoint} turns into the stable error body.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(authHeader.substring(7));
        } catch (ExpiredJwtException e) {
            reject(request, ErrorCodes.TOKEN_EXPIRED, "Access token has expired");
            filterChain.doFilter(request, response);
            return;
        } catch (JwtException | IllegalArgumentException e) {
            reject(request, ErrorCodes.TOKEN_REVOKED, "Access token is invalid");
            filterChain.doFilter(request, response);
            return;
        }

        if (!JwtUtil.TYPE_ACCESS.equals(claims.get("typ", String.class))) {
            reject(request, ErrorCodes.TOKEN_REVOKED, "Token is not an access token");
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findById(claims.getSubject()).orElse(null);
        Integer ver = claims.get("ver", Integer.class);

        if (user == null || ver == null || ver != user.getTokenVersion()) {
            reject(request, ErrorCodes.TOKEN_REVOKED, "Session is no longer valid, please log in again");
        } else if (user.getStatus() != User.UserStatus.ACTIVE) {
            if (user.getStatus() == User.UserStatus.LOCKED) {
                reject(request, ErrorCodes.ACCOUNT_LOCKED, "Account is locked");
            } else {
                reject(request, ErrorCodes.TOKEN_REVOKED, "Account is inactive");
            }
        } else if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            reject(request, ErrorCodes.ACCOUNT_LOCKED, "Account is temporarily locked");
        } else {
            String blocked = checkOrgAndBranch(user);
            if (blocked != null) {
                reject(request, ErrorCodes.ORG_SUSPENDED, blocked);
            } else {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Returns a message when the user's organization or branch is not active, otherwise null. */
    private String checkOrgAndBranch(User user) {
        if (user.getOrganizationId() == null) {
            return null; // SUPER_ADMIN
        }
        Organization org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
        if (org == null || org.getStatus() != Organization.OrganizationStatus.ACTIVE) {
            return "Organization is not active";
        }
        if (user.getRole().isBranchScoped() && user.getBranchId() != null) {
            Branch branch = branchRepository.findByIdAndOrganizationId(user.getBranchId(), user.getOrganizationId())
                    .orElse(null);
            if (branch == null || branch.getStatus() != Branch.BranchStatus.ACTIVE) {
                return "Branch is not active";
            }
        }
        return null;
    }

    private static void reject(HttpServletRequest request, String code, String message) {
        request.setAttribute(AuthEntryPoint.ERROR_CODE_ATTRIBUTE, code);
        request.setAttribute(AuthEntryPoint.ERROR_MESSAGE_ATTRIBUTE, message);
    }
}
