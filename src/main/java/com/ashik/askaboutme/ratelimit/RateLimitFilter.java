package com.ashik.askaboutme.ratelimit;

import com.ashik.askaboutme.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LIMITED_PATH = "/api/v1/ask";

    // Built locally rather than injected: this app's web starter doesn't publish an
    // ObjectMapper bean. findAndRegisterModules() picks up jackson-datatype-jsr310 from the
    // classpath, and disabling WRITE_DATES_AS_TIMESTAMPS matches Spring's own default so
    // Instant renders as the same ISO-8601 string used by every other ErrorResponse.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final IpRateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!LIMITED_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        if (rateLimiter.tryConsume(clientIp)) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded for {} on {}", clientIp, LIMITED_PATH);
        writeTooManyRequests(request, response);
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                "Too many requests - please slow down and try again shortly.",
                request.getRequestURI()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }

    /**
     * Trusts the first hop's X-Forwarded-For entry when present (e.g. behind a reverse proxy
     * or load balancer), falling back to the socket address otherwise. If the app is ever
     * deployed behind a proxy that doesn't overwrite client-supplied headers, this would need
     * to be tightened to only trust a configured, known proxy.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
