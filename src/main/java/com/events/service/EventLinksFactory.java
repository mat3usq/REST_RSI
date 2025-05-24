package com.events.service;

import com.events.model.Event;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;

public class EventLinksFactory {
    private final UriInfo uriInfo;

    public EventLinksFactory(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public List<Link> createForEvent(Event event) {
        UriBuilder baseUri = uriInfo.getBaseUriBuilder()
                .path("events")
                .path(String.valueOf(event.getId()));

        return Arrays.asList(
                Link.fromUriBuilder(baseUri).rel("self").type("GET").build(),
                Link.fromUriBuilder(baseUri).rel("update").type("PUT").build(),
                Link.fromUriBuilder(baseUri).rel("delete").type("DELETE").build(),
                Link.fromUri(uriInfo.getBaseUriBuilder().path("events").build())
                        .rel("collection").type("GET").build()
        );
    }
}