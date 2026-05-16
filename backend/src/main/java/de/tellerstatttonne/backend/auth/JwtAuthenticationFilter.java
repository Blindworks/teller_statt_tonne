package de.tellerstatttonne.backend.auth;

import de.tellerstatttonne.backend.user.UserEntity;
import de.tellerstatttonne.backend.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtService.parse(token);
                String purpose = claims.get("purpose", String.class);
                if (purpose != null) {
                    // Spezial-Tokens (z.B. Retter-Ausweis) duerfen nicht als Login-Session genutzt werden.
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }
                String userId = claims.getSubject();
                if (isLocked(userId)) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setHeader("X-Reason", "account_locked");
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write("Account locked");
                    return;
                }
                List<SimpleGrantedAuthority> authorities = extractAuthorities(claims);
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isLocked(String userId) {
        try {
            Long id = Long.parseLong(userId);
            return userRepository.findById(id)
                .map(u -> u.getStatus() == UserEntity.Status.LOCKED)
                .orElse(false);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + item.toString()));
                }
            }
        }
        if (authorities.isEmpty()) {
            // Backward compatibility: pre-0.4.0 tokens carried a single-string "role" claim.
            String legacyRole = claims.get("role", String.class);
            if (legacyRole != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + legacyRole));
            }
        }
        return authorities;
    }
}
