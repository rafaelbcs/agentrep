package br.com.agentrep.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import br.com.agentrep.service.RateLimitService;

import java.io.IOException;

/**
 * Aplica rate limiting por wallet antes da autenticação.
 * Identidade extraída do header X-Wallet-Address (não autenticado ainda).
 * Rotas públicas sem wallet no header são ignoradas.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {

        String wallet = request.getHeader("X-Wallet-Address");
        if (wallet == null || wallet.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String endpoint = resolveEndpoint(request.getRequestURI());
        if (endpoint == null) {
            chain.doFilter(request, response);
            return;
        }

        if (!rateLimitService.isAllowed(wallet, endpoint)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                """
                {"error":"Too Many Requests","message":"Rate limit exceeded. Try again in 1 minute."}
                """
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveEndpoint(String uri) {
        if (uri.contains("/outcomes"))  return RateLimitService.ENDPOINT_OUTCOME;
        if (uri.contains("/disputes"))  return RateLimitService.ENDPOINT_DISPUTE;
        if (uri.contains("/register"))  return RateLimitService.ENDPOINT_REGISTER;
        return null;
    }
}
