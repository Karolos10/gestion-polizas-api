package com.segurosbolivar.polizas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Seguridad minima basada en API key. Toda peticion debe incluir el header
 * {@code x-api-key} con el valor configurado. El endpoint del core-mock y la
 * consola H2 quedan exentos para facilitar las pruebas.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "x-api-key";

    private final String apiKeyEsperada;

    public ApiKeyFilter(@Value("${app.api-key:123456}") String apiKeyEsperada) {
        this.apiKeyEsperada = apiKeyEsperada;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // El mock del CORE y la consola H2 no exigen api-key.
        return path.startsWith("/core-mock") || path.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(HEADER);

        if (apiKey == null || !apiKey.equals(apiKeyEsperada)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"UNAUTHORIZED\","
                            + "\"mensaje\":\"Header 'x-api-key' ausente o invalido\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
