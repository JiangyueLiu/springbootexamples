package com.example.springboot.jwt.web.filter;

import com.example.springboot.jwt.configuration.JWTProperties;
import com.example.springboot.jwt.core.userdetails.JWTUserDetails;
import com.example.springboot.jwt.util.JWTTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <pre>
 *  JWTAuthenticationTokenFilter
 * </pre>
 *
 * <pre>
 * @author mazq
 * 修改记录
 *    修改后版本:     修改人：  修改日期: 2020/07/06 16:04  修改内容:
 * </pre>
 */
@Slf4j
public class JWTAuthenticationTokenFilter extends OncePerRequestFilter {

    private static final ConcurrentMap<String,Boolean> URI_CACHE_MAP = new ConcurrentHashMap<String,Boolean>();
    private final List<String> permitAllUris;
    private final List<String> authenticateUris;

    @Autowired
    JWTProperties jwtProperties;

    @Autowired
    JWTTokenUtil jwtTokenUtil;

    public JWTAuthenticationTokenFilter(JWTProperties jwtProperties) {
        this.permitAllUris = Arrays.asList(jwtProperties.getPermitAll().split(","));
        this.authenticateUris = Arrays.asList(jwtProperties.getAuthenticateUri().split(","));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isAllowUri(httpServletRequest)) {
            final String _authHeader = httpServletRequest.getHeader(jwtProperties.getHeaderKey());
            log.info("Authorization:[{}]",_authHeader);
            if (StringUtils.isEmpty(_authHeader) || ! _authHeader.startsWith(jwtProperties.getHeaderValuePrefix())) {
                return;
            }
            final String token = _authHeader.substring(7);
            log.info("acceptToken:[{}]",token);
            if (!jwtTokenUtil.validateToken(token)) {
                //return;
            }
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private Boolean isAllowUri(HttpServletRequest request) {
        String uri = request.getServletPath();
        if (URI_CACHE_MAP.containsKey(uri)) {
            // 缓存有数据，直接从缓存读取
            return URI_CACHE_MAP.get(uri);
        }
        boolean flag = checkRequestUri(uri);
        // 数据丢到缓存里
        URI_CACHE_MAP.putIfAbsent(uri, flag);
        return flag;
    }

    private Boolean checkRequestUri(String requestUri) {
        boolean filter = true;
        final PathMatcher pathMatcher = new AntPathMatcher();
        for (String permitUri : permitAllUris) {
            if (pathMatcher.match(permitUri, requestUri)) {
                // permit all的链接直接放过
                filter = false;
            }
        }
        for (String authUri : authenticateUris) {
            if (pathMatcher.match(authUri, requestUri)) {
                filter = true;
            }
        }
        return filter;
    }
}
