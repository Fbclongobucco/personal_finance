package br.com.longobucco.personal_finance_app.infra.rest.security;

import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.security.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenService.extractEmailIfValidAccessToken(header.substring(7))
                    .flatMap(userRepository::findByEmail)
                    .ifPresent(this::setAuthentication);
        }
        filterChain.doFilter(request, response);
    }

    private void setAuthentication(User user) {
        var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
