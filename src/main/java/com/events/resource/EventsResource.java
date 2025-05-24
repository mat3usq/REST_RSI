package com.events.resource;

import com.events.model.EventRequest;
import com.events.model.Event;
import com.events.model.EventType;
import com.events.service.EventLinksFactory;
import com.events.service.EventsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventsResource {

    @Inject
    private EventsService eventsService;
    @Context
    private UriInfo uriInfo;

    private EventLinksFactory createLinksFactory() {
        return new EventLinksFactory(uriInfo);
    }

    private void addLinksToEvent(Event event) {
        event.addLinks(createLinksFactory().createForEvent(event));
    }

    @GET
    public Response findAllEvents() {
        List<Event> events = eventsService.findAllEvents();
        events.forEach(this::addLinksToEvent);
        return Response.ok(events).build();
    }

    @GET
    @Path("/{id}")
    public Response findEventById(@PathParam("id") long id) {
        Event event = eventsService.findEventById(id);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Event with ID " + id + " not found")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        addLinksToEvent(event);
        return Response.ok(event).build();
    }

    @GET
    @Path("/date/{date}")
    public Response findEventsByDate(@PathParam("date") String date) {
        try {
            List<Event> events = eventsService.findEventsByDate(LocalDate.parse(date));
            events.forEach(this::addLinksToEvent);
            return Response.ok(events).build();
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid date format. Please use YYYY-MM-DD format")
                    .build();
        }
    }

    @GET
    @Path("/week")
    public Response findEventsByWeek(
            @QueryParam("week") int weekNumber,
            @QueryParam("year") int year
    ) {
        List<Event> events = eventsService.findEventsByWeek(weekNumber, year);
        events.forEach(this::addLinksToEvent);
        return Response.ok(events).build();
    }

    @POST
    public Response addEvent(EventRequest event, @Context UriInfo uriInfo) {
        try {
            LocalDateTime.parse(event.getDateTime());
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid date format. Please use YYYY-MM-DDTHH:MM:SS format")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        if (event.getType() == null || eventsService.isValidEventType(event.getType())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid event type. Valid types are: " + Arrays.toString(EventType.values()))
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        Event newEvent = eventsService.addEvent(new Event(event));
        if (newEvent == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to create event")
                    .build();
        }

        addLinksToEvent(newEvent);
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path(Long.toString(newEvent.getId()));
        return Response.created(builder.build()).entity(newEvent).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateEvent(@PathParam("id") long id, EventRequest updatedEvent) {
        try {
            LocalDateTime.parse(updatedEvent.getDateTime());
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid date format. Please use YYYY-MM-DDTHH:MM:SS format")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        if (updatedEvent.getType() == null || eventsService.isValidEventType(updatedEvent.getType())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid event type. Valid types are: " + Arrays.toString(EventType.values()))
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        Event event = eventsService.updateEvent(id, new Event(updatedEvent));
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Failed to update Event with ID " + id)
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
        addLinksToEvent(event);
        return Response.ok(event).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteEvent(@PathParam("id") long id) {
        Event existingEvent = eventsService.findEventById(id);
        if (existingEvent == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Event with ID " + id + " not found")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        boolean deleted = eventsService.deleteEvent(id);

        if (!deleted) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to delete event with ID " + id)
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        return Response.noContent().build();
    }

    @GET
    @Path("/report")
    @Produces("application/pdf")
    public Response generateEventsReport(
            @QueryParam("date") String date,
            @QueryParam("byWeek") @DefaultValue("false") boolean byWeek) {
        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid date format. Please use YYYY-MM-DD format")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        byte[] report = eventsService.generateEventsReport(parsedDate, byWeek);
        if (report == null) {
            return Response.serverError().build();
        }

        return Response.ok(report)
                .header("Content-Disposition", "attachment; filename=events_report.pdf")
                .build();
    }

    @GET
    @Path("/types")
    public Response findAllEventsTypes() {
        List<String> types = eventsService.findAllEventsTypes();
        return Response.ok(types).build();
    }
}