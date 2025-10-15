package module.platform.security.filter;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import module.platform.security.service.JwtTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenService tokenService;

    public JwtAuthFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        // permitAll 경로 빠르게 스킵 (SecurityConfig와 일치시켜라)
        return p.startsWith("/actuator")
                || p.startsWith("/swagger-ui")
                || p.startsWith("/v3/api-docs")
                || p.equals("/entity/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = auth.substring(7).trim();
            if (token.isEmpty()) {
                unauthorized(res, "invalid_token", "Empty token");
                return;
            }
            try {
                JWTClaimsSet claims = tokenService.verify(token);
                String sub = claims.getSubject();
                Collection<? extends GrantedAuthority> authorities = toAuthorities(claims.getClaim("roles"));

                AbstractAuthenticationToken authentication = new AbstractAuthenticationToken(authorities) {
                    @Override public Object getCredentials() { return token; }
                    @Override public Object getPrincipal() { return sub; }
                };
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                authentication.setAuthenticated(true);

                // 컨텍스트 설정
                org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (Exception e) {
                // 토큰이 있었는데 검증 실패 → 401로 명확히 응답
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
                unauthorized(res, "invalid_token", e.getMessage());
                return;
            }
        }
        chain.doFilter(req, res);
    }

    @SuppressWarnings("unchecked")
    private static Collection<? extends GrantedAuthority> toAuthorities(Object rolesClaim) {
        if (rolesClaim == null) return List.of();
        if (rolesClaim instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toUnmodifiableList());
        }
        if (rolesClaim instanceof String s) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toUnmodifiableList());
        }
        return List.of();
    }

    private static void unauthorized(HttpServletResponse res, String error, String desc) throws IOException {
        res.setStatus(HttpStatus.UNAUTHORIZED.value());
        res.setHeader("WWW-Authenticate", "Bearer error=\"" + error + "\", error_description=\"" + desc + "\"");
        res.setContentType("application/json");
        res.getWriter().write("""
            {"type":"about:blank","title":"Unauthorized","status":401,"detail":%s}
            """.formatted("\"" + (desc == null ? "unauthorized" : desc.replace("\"","\\\"")) + "\""));
    }
}
