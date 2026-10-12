package com.ballastlane.pokeproject.api.filter;

import com.ballastlane.pokeproject.util.LogSanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Captures the optional audit headers (X-Forwarded-For, x-user, x-latitude, x-longitude, x-channel)
 * once per request: sanitized, validated, stored as a request attribute and in the logging MDC, so
 * every log line of the request carries them. Runs first so even security rejections are audited.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuditHeadersFilter extends OncePerRequestFilter {

    private final LogSanitizer sanitizer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        AuditContext audit = new AuditContext(
                clientIp(request),
                sanitizer.sanitize(request.getHeader("x-user")),
                coordinate(request.getHeader("x-latitude"), 90),
                coordinate(request.getHeader("x-longitude"), 180),
                sanitizer.sanitize(request.getHeader("x-channel")));
        request.setAttribute(AuditContext.ATTRIBUTE, audit);
        try {
            putIfPresent("clientIp", audit.clientIp());
            putIfPresent("xUser", audit.user());
            putIfPresent("channel", audit.channel());
            putIfPresent("latitude", audit.latitude());
            putIfPresent("longitude", audit.longitude());
            chain.doFilter(request, response);
        } finally {
            MDC.clear(); // threads are reused: never leak audit data into the next request
        }
    }

    /** First address of X-Forwarded-For (the original client); otherwise the socket address. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return sanitizer.sanitize(forwarded.split(",")[0]);
        }
        return request.getRemoteAddr();
    }

    private Double coordinate(String raw, double limit) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw.strip());
            return Math.abs(value) <= limit ? value : null;
        } catch (NumberFormatException ex) {
            log.debug("[REQUEST] Ignoring invalid coordinate header");
            return null;
        }
    }

    private static void putIfPresent(String key, Object value) {
        if (value != null) {
            MDC.put(key, String.valueOf(value));
        }
    }
}
