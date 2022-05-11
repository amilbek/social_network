package com.project.social_network.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class RequestAndResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAndResponseLoggingFilter.class);

    private static final List<MediaType> VISIBLE_TYPES = Arrays.asList(
            MediaType.valueOf("text/*"),
            MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            MediaType.valueOf("application/*+json"),
            MediaType.valueOf("application/*+xml"),
            MediaType.MULTIPART_FORM_DATA
    );

    private boolean enabled = true;

    @ManagedOperation(description = "Enable logging of HTTP requests and responses")
    public void enable() {
        this.enabled = true;
    }

    @ManagedOperation(description = "Disable logging of HTTP requests and responses")
    public void disable() {
        this.enabled = false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
        } else {
            doFilterWrapped(wrapRequest(request), wrapResponse(response), filterChain);
        }
    }

    protected void doFilterWrapped(ContentCachingRequestWrapper request,
                                   ContentCachingResponseWrapper response,
                                   FilterChain filterChain) throws ServletException, IOException {

        StringBuilder msg = new StringBuilder();

        try {
            beforeRequest(request, msg);
            filterChain.doFilter(request, response);
        }
        finally {
            afterRequest(request, response, msg);
            if(LOG.isInfoEnabled()) {
                LOG.info(msg.toString());
            }
            response.copyBodyToResponse();
        }
    }

    protected void beforeRequest(ContentCachingRequestWrapper request,
                                 StringBuilder msg) {
        if (enabled && LOG.isInfoEnabled()) {
            msg.append("\n --- REQUEST --\n");
            logRequestHeader(request, msg);
        }
    }

    protected void afterRequest(ContentCachingRequestWrapper request,
                                ContentCachingResponseWrapper response,
                                StringBuilder msg) {
        if (enabled && LOG.isInfoEnabled()) {
            logRequestBody(request, msg);
            msg.append("\n-- RESPONSE --\n");
            logResponse(response, msg);
        }
    }

    private static void logRequestHeader(ContentCachingRequestWrapper request,
                                         StringBuilder msg) {
        String queryString = request.getQueryString();
        if (queryString == null) {
            msg.append(String.format("%s %s", request.getMethod(), request.getRequestURI())).append("\n");
        } else {
            msg.append(String.format("%s %s?%s", request.getMethod(), request.getRequestURI(), queryString)).append("\n");
        }
        msg.append(String.format("%s", request.getHeader("Authorization"))).append("\n");
    }

    private static void logRequestBody(ContentCachingRequestWrapper request,
                                       StringBuilder msg) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            logContent(content, request.getContentType(), request.getCharacterEncoding(), msg);
        }
    }

    private static void logResponse(ContentCachingResponseWrapper response,
                                    StringBuilder msg) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            logContent(content, response.getContentType(), response.getCharacterEncoding(), msg);
        }
    }

    private static void logContent(byte[] content,
                                   String contentType,
                                   String contentEncoding,
                                   StringBuilder msg) {
        MediaType mediaType = MediaType.valueOf(contentType);
        boolean visible = VISIBLE_TYPES.stream().anyMatch(visibleType -> visibleType.includes(mediaType));
        if (visible) {
            try {
                String contentString = new String(content, contentEncoding);
                Stream.of(contentString.split("\r\n|\r|\n")).forEach(line -> msg.append(line).append("\n"));
            } catch (UnsupportedEncodingException e) {
                msg.append(String.format("[%d bytes content]", content.length)).append("\n");
            }
        } else {
            msg.append(String.format("[%d bytes content]", content.length)).append("\n");
        }
    }

    private static ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        } else {
            return new ContentCachingRequestWrapper(request);
        }
    }

    private static ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) response;
        } else {
            return new ContentCachingResponseWrapper(response);
        }
    }
}
