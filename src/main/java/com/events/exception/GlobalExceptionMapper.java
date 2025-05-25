package com.events.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", exception.getMessage());
        error.put("timestamp", System.currentTimeMillis());

        switch (exception) {
            case EventNotFoundException eventNotFoundException -> {
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }
            case InvalidEventException invalidEventException -> {
                return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
            }
            case ReportGenerationException reportGenerationException -> {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
            }
            default -> {
            }
        }

        error.put("message", "Internal server error");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
    }
}