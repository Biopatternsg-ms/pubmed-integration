package com.biopatternsg.infrastructure.config.interceptors;

import com.biopatternsg.infrastructure.session.SessionUtils;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@Provider
@RequiredArgsConstructor
public class ContextInterceptor implements ContainerRequestFilter {
    private final SessionUtils sessionUtil;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        var context = containerRequestContext.getHeaders();
        sessionUtil.setContext(context);
    }
}
