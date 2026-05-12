package com.habitat.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Global page-size cap. Wraps every request and rewrites a too-large
 * {@code size} query parameter down to a safe cap. Default size is left to
 * controller signatures; this enforces the upper bound only.
 */
@Configuration
public class WebConfig {

    @Bean
    public PageSizeFilter pageSizeFilter() {
        return new PageSizeFilter();
    }

    @Component
    @Order(1)
    public static class PageSizeFilter extends OncePerRequestFilter {
        private static final int MAX_SIZE = 100;

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String size = req.getParameter("size");
            if (size != null) {
                try {
                    int parsed = Integer.parseInt(size);
                    if (parsed > MAX_SIZE) {
                        chain.doFilter(new CappedSizeRequest(req, MAX_SIZE), res);
                        return;
                    }
                } catch (NumberFormatException ignored) {
                    // Spring will reject invalid integers downstream — let it.
                }
            }
            chain.doFilter(req, res);
        }
    }

    /** Tiny wrapper that overrides the "size" parameter only. */
    private static class CappedSizeRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final String capped;

        CappedSizeRequest(HttpServletRequest req, int max) {
            super(req);
            this.capped = String.valueOf(max);
        }

        @Override
        public String getParameter(String name) {
            return "size".equals(name) ? capped : super.getParameter(name);
        }

        @Override
        public String[] getParameterValues(String name) {
            return "size".equals(name) ? new String[]{capped} : super.getParameterValues(name);
        }
    }
}
