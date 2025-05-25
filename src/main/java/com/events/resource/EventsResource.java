package com.events.resource;

import com.events.model.EventRequest;
import com.events.model.Event;
import com.events.service.EventsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.List;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventsResource {

    @Inject
    private EventsService eventsService;

    @GET
    public Response findAllEvents() {
        return Response.ok(eventsService.findAllEvents()).build();
    }

    @GET
    @Path("/{id}")
    public Response findEventById(@PathParam("id") long id) {
        return Response.ok(eventsService.findEventById(id)).build();
    }

    @GET
    @Path("/date/{date}")
    public Response findEventsByDate(@PathParam("date") String date) {
        return Response.ok(eventsService.findEventsByDate(date)).build();
    }

    @GET
    @Path("/week")
    public Response findEventsByWeek(@QueryParam("week") int weekNumber, @QueryParam("year") int year) {
        return Response.ok(eventsService.findEventsByWeek(weekNumber, year)).build();
    }

    @POST
    public Response addEvent(EventRequest event, @Context UriInfo uriInfo) {
        Event newEvent = eventsService.addEvent(event);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(newEvent.getId())).build()).entity(newEvent).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateEvent(@PathParam("id") long id, EventRequest updatedEvent) {
        return Response.ok(eventsService.updateEvent(id, updatedEvent)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteEvent(@PathParam("id") long id) {
        eventsService.deleteEvent(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/report")
    @Produces({MediaType.APPLICATION_JSON, "application/pdf"})
    public Response generateEventsReport(@QueryParam("date") String date, @QueryParam("byWeek") @DefaultValue("false") boolean byWeek) {
        return Response.ok(eventsService.generateEventsReport(date, byWeek))
                .header("Content-Disposition", "attachment; filename=report.pdf")
                .build();
    }

    @GET
    @Path("/types")
    public Response findAllEventsTypes() {
        List<String> types = eventsService.findAllEventsTypes();
        return Response.ok(types).build();
    }
}