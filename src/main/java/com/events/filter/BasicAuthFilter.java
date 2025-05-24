package com.events.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Base64;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class BasicAuthFilter implements ContainerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";
    private static final String REALM = "EventsAPI";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authHeader = requestContext.getHeaderString(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BASIC_PREFIX)) {
            abortWithUnauthorized(requestContext);
            return;
        }

        String credentials = new String(Base64.getDecoder().decode(authHeader.substring(BASIC_PREFIX.length())));
        String[] parts = credentials.split(":");
        if (parts.length != 2 || !isValidUser(parts[0], parts[1])) {
            abortWithUnauthorized(requestContext);
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext context) {
        context.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header("WWW-Authenticate", "Basic realm=\"" + REALM + "\"")
                        .entity("No access - credentials required")
                        .build()
        );
    }

    private boolean isValidUser(String username, String password) {
        return "admin".equals(username) && "admin".equals(password);
    }
}