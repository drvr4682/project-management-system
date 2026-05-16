package com.pms.apigateway.filter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void putHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {

        String headerValue = customHeaders.get(name);

        if (headerValue != null) {
            return headerValue;
        }

        return ((HttpServletRequest) getRequest()).getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {

        Map<String, String> combinedHeaders = new HashMap<>();

        Enumeration<String> headerNames =
                ((HttpServletRequest) getRequest()).getHeaderNames();

        while (headerNames.hasMoreElements()) {

            String headerName = headerNames.nextElement();

            combinedHeaders.put(
                    headerName,
                    ((HttpServletRequest) getRequest()).getHeader(headerName)
            );
        }

        combinedHeaders.putAll(customHeaders);

        return Collections.enumeration(combinedHeaders.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {

        String headerValue = customHeaders.get(name);

        if (headerValue != null) {
            return Collections.enumeration(Collections.singletonList(headerValue));
        }

        return ((HttpServletRequest) getRequest()).getHeaders(name);
    }
}