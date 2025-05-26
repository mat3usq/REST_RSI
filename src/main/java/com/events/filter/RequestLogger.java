package com.events.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import jakarta.ws.rs.ext.Provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Provider
public class RequestLogger implements ContainerRequestFilter, ContainerResponseFilter, WriterInterceptor {
    private final Path logFilePath;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final boolean logHeaders = true;
    private final boolean logPayloads = true;

    public RequestLogger() {
        this.logFilePath = Paths.get("app_events.log");
        initializeLogFile();
    }

    private void initializeLogFile() {
        try {
            if (Files.notExists(logFilePath)) {
                String header = "Timestamp|Method|Path|Status|Auth|Duration(ms)";
                if (logHeaders) header += "|Headers";
                if (logPayloads) header += "|Request Body|Response Body";
                header += "\n";
                Files.write(logFilePath, header.getBytes(), StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        ctx.setProperty("startTime", System.currentTimeMillis());
        if (logPayloads && ctx.hasEntity()) {
            byte[] entity = ctx.getEntityStream().readAllBytes();
            String body = new String(entity, StandardCharsets.UTF_8);
            ctx.setProperty("requestBody", body);
            ctx.setEntityStream(new ByteArrayInputStream(entity));
        }
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
        if (!logPayloads) {
            context.proceed();
            return;
        }
        OutputStream original = context.getOutputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        context.setOutputStream(buffer);
        context.proceed();
        byte[] data = buffer.toByteArray();
        original.write(data);
        context.setOutputStream(original);
        String body = new String(data, StandardCharsets.UTF_8);
        context.getHeaders().putSingle("X-Logged-Response-Body", body);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        long start = (Long) requestContext.getProperty("startTime");
        long duration = System.currentTimeMillis() - start;
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        boolean auth = requestContext.getHeaderString("Authorization") != null;
        int status = responseContext.getStatus();
        StringBuilder entry = new StringBuilder();
        entry.append(String.format("%s|%s|%s|%d|%s|%d",
                LocalDateTime.now().format(dtf),
                method,
                path,
                status,
                auth ? "YES" : "NO",
                duration
        ));
        if (logHeaders) {
            String headers = requestContext.getHeaders().entrySet().stream()
                    .filter(e -> !e.getKey().equalsIgnoreCase("Authorization"))
                    .map(e -> e.getKey() + ": " + String.join(", ", e.getValue()))
                    .collect(Collectors.joining("; "));
            entry.append("|").append(headers);
        }
        if (logPayloads) {
            String req = (String) requestContext.getProperty("requestBody");
            String resp = responseContext.getHeaderString("X-Logged-Response-Body");
            entry.append("|").append(escape(req)).append("|").append(escape(resp));
        }
        entry.append("\n");
        Files.write(logFilePath, entry.toString().getBytes(), StandardOpenOption.APPEND);
    }

    private String escape(String t) {
        if (t == null) return "";
        return t.replace("\n", "\\n").replace("\r", "\\r");
    }
}