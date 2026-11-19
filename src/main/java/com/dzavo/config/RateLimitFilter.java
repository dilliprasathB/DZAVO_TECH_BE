package com.dzavo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding rate limiter filter.
 * Protects /api/contact from automated flooding.
 */
@Component
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_WINDOW = 10;
    private static final long WINDOW_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static class RequestCounter {
        private final long windowStart;
        private final AtomicInteger count;

        public RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Apply rate limit specifically to POST requests to /api/contact
        if ("POST".equalsIgnoreCase(httpRequest.getMethod()) && httpRequest.getRequestURI().contains("/api/contact")) {
            String clientIp = getClientIp(httpRequest);
            long now = System.currentTimeMillis();

            RequestCounter counter = requestCounts.compute(clientIp, (key, existing) -> {
                if (existing == null || (now - existing.windowStart) > WINDOW_DURATION_MS) {
                    return new RequestCounter(now);
                }
                existing.count.incrementAndGet();
                return existing;
            });

            if (counter.count.get() > MAX_REQUESTS_PER_WINDOW) {
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                Map<String, Object> body = Map.of(
                        "success", false,
                        "message", "Too many submission attempts from your IP. Please wait a few minutes before trying again."
                );
                httpResponse.getWriter().write(objectMapper.writeValueAsString(body));
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
