package com.masprog.shopping_list_api.auth.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int LOGIN_LIMIT = 5;
    private static final int REGISTER_LIMIT = 5;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final int REFRESH_LIMIT = 10;
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000L;

    private final AtomicLong lastCleanup = new AtomicLong();

    private final ConcurrentHashMap<String, RequestWindow> requests =
            new ConcurrentHashMap<>();

    private final Clock clock;

    public AuthRateLimitFilter() {
        this(Clock.systemUTC());
    }

    public AuthRateLimitFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        return !("/api/v1/auth/login".equals(path)
                || "/api/v1/auth/register".equals(path)
                || "/api/v1/auth/refresh".equals(path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        // Cada endpoint possui o seu próprio contador por IP.
        String key = path + ":" + clientIp;

        int limit = switch (path) {
            case "/api/v1/auth/login" -> LOGIN_LIMIT;
            case "/api/v1/auth/register" -> REGISTER_LIMIT;
            case "/api/v1/auth/refresh" -> REFRESH_LIMIT;
            default -> throw new IllegalStateException(
                    "Unsupported rate limited endpoint: " + path
            );
        };
        long now = clock.millis();
        cleanupIfNecessary();

        RequestWindow window = requests.compute(key, (ignored, current) -> {

            if (current == null
                    || now - current.startedAt() >= WINDOW_MILLIS) {

                return new RequestWindow(now, 1);
            }

            return new RequestWindow(
                    current.startedAt(),
                    current.count() + 1
            );
        });

        if (window.count() > limit) {

            response.sendError(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too many login attempts"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupIfNecessary() {

        long now = clock.millis();

        while (true) {

            long previousCleanup = lastCleanup.get();

            if (previousCleanup != 0
                    && now - previousCleanup < CLEANUP_INTERVAL_MILLIS) {
                return;
            }

            if (lastCleanup.compareAndSet(previousCleanup, now)) {
                removeExpiredWindows();
                return;
            }
        }
    }

    public void clearRequests() {
        requests.clear();
        lastCleanup.set(0L);
    }

    int removeExpiredWindows() {

        long now = clock.millis();

        int sizeBefore = requests.size();

        requests.entrySet().removeIf(entry ->
                now - entry.getValue().startedAt() >= WINDOW_MILLIS
        );

        return sizeBefore - requests.size();
    }

    private record RequestWindow(
            long startedAt,
            int count
    ) {
    }
}