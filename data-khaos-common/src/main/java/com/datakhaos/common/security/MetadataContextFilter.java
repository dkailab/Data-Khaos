package com.datakhaos.common.security;

import com.datakhaos.common.constant.CommonConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 请求上下文过滤器：解析网关透传的用户头，写入 ThreadLocal。
 * 仅在有 Servlet 容器的服务（非网关）中生效。
 */
public class MetadataContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String userId = request.getHeader(CommonConstants.HEADER_USER_ID);
            if (userId != null && !userId.isBlank()) {
                AuthUser user = AuthUser.builder()
                        .id(userId)
                        .username(request.getHeader(CommonConstants.HEADER_USERNAME))
                        .realName(request.getHeader(CommonConstants.HEADER_REAL_NAME))
                        .roles(split(request.getHeader(CommonConstants.HEADER_ROLES)))
                        .permissions(split(request.getHeader(CommonConstants.HEADER_PERMISSIONS)))
                        .build();
                MetadataHolder.set(user);
            }
        } finally {
            try {
                filterChain.doFilter(request, response);
            } finally {
                MetadataHolder.clear();
            }
        }
    }

    private List<String> split(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
