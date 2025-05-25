package com.events.service;

import com.events.exception.*;
import com.events.model.Event;
import com.events.model.EventRequest;
import com.events.model.EventType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class EventsService {
    @Context
    private UriInfo uriInfo;
    private final List<Event> events = new ArrayList<>();

    public EventsService() {
        events.add(new Event("Coldplay Concert", EventType.CONCERT, LocalDateTime.of(2024, 6, 20, 20, 0), "Concert at the National Stadium in Warsaw"));
        events.add(new Event("Music Festival", EventType.FESTIVAL, LocalDateTime.of(2024, 6, 20, 10, 0), "Annual music festival in the park"));
        events.add(new Event("Art Exhibition", EventType.EXHIBITION, LocalDateTime.of(2024, 6, 22, 12, 0), "Exhibition of contemporary Polish art"));
        events.add(new Event("Theatrical Performance", EventType.THEATER, LocalDateTime.of(2024, 6, 23, 19, 0), "Hamlet at the National Theater"));
        events.add(new Event("Soccer Match", EventType.SPORT, LocalDateTime.of(2024, 6, 24, 15, 0), "Friendly match between local teams"));
        events.add(new Event("Programming Workshop", EventType.WORKSHOP, LocalDateTime.of(2024, 6, 25, 9, 0), "Workshop on Java and Spring Boot"));
        events.add(new Event("Tech Conference", EventType.CONFERENCE, LocalDateTime.of(2024, 6, 26, 10, 0), "Annual tech conference with keynote speakers"));
        events.add(new Event("Book Fair", EventType.FAIR, LocalDateTime.of(2024, 6, 27, 11, 0), "Annual book fair with author signings"));
        events.add(new Event("Food Festival", EventType.GASTRONOMY, LocalDateTime.of(2024, 6, 28, 12, 0), "Festival celebrating local cuisine"));
        events.add(new Event("Historical Tour", EventType.HISTORY, LocalDateTime.of(2024, 6, 29, 10, 0), "Guided tour of historical sites"));
        events.add(new Event("Poetry Reading", EventType.LITERARY, LocalDateTime.of(2024, 6, 30, 18, 0), "Evening of poetry readings by local authors"));
        events.add(new Event("Children's Play", EventType.FOR_CHILDREN, LocalDateTime.of(2024, 7, 1, 11, 0), "Interactive play for children"));
        events.add(new Event("Movie Night", EventType.CINEMA, LocalDateTime.of(2024, 7, 2, 20, 0), "Outdoor movie screening in the park"));
        events.add(new Event("Community Cleanup", EventType.COMMUNITY, LocalDateTime.of(2024, 7, 3, 9, 0), "Community cleanup event at the local park"));
        events.add(new Event("Educational Seminar", EventType.EDUCATION, LocalDateTime.of(2024, 7, 4, 14, 0), "Seminar on educational trends and practices"));
    }

    public List<Event> findEventsByDate(String date) {
        try {
            LocalDate dat = LocalDate.parse(date);
            List<Event> evs = events.stream()
                    .filter(event -> event.getDateTime().toLocalDate().equals(dat))
                    .toList();
            evs.forEach(this::addLinksToEvent);
            return evs;
        } catch (DateTimeParseException e) {
            throw new InvalidEventException("Invalid date format. Please use YYYY-MM-DDTHH:MM:SS format");
        }
    }

    public List<Event> findEventsByWeek(int weekNumber, int year) {
        List<Event> evs = events.stream()
                .filter(event -> event.getWeekNumber() == weekNumber && event.getYearNumber() == year)
                .toList();
        evs.forEach(this::addLinksToEvent);
        return evs;
    }

    public Event findEventById(long eventId) {
        Event ev = events.stream()
                .filter(event -> event.getId() == eventId)
                .findFirst()
                .orElseThrow(() -> new EventNotFoundException(eventId));

        addLinksToEvent(ev);
        return ev;
    }

    public Event addEvent(EventRequest event) {
        Event ev = validateEvent(event);
        addLinksToEvent(ev);
        events.add(ev);
        return ev;
    }

    public Event updateEvent(long eventId, EventRequest updatedEvent) {
        Event ev = validateEvent(updatedEvent);
        Event existing = findEventById(eventId);

        existing.setName(ev.getName());
        existing.setType(ev.getType());
        existing.setDateTime(ev.getDateTime());
        existing.setDescription(ev.getDescription());
        addLinksToEvent(existing);

        return existing;
    }

    public void deleteEvent(long eventId) {
        if (!events.removeIf(event -> event.getId() == eventId)) {
            throw new EventNotFoundException(eventId);
        }
    }

    public List<Event> findAllEvents() {
        events.forEach(this::addLinksToEvent);
        return new ArrayList<>(events);
    }

    public byte[] generateEventsReport(String date, boolean byWeek) {
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            List<Event> events = resolveEvents(parsedDate, byWeek);
            return new EventsReportPdfGenerator().generate(events);
        } catch (DateTimeParseException e) {
            throw new ReportGenerationException("Invalid date format. Please use YYYY-MM-DD format");
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate PDF report");
        }
    }

    private List<Event> resolveEvents(LocalDate date, boolean byWeek) {
        if (date == null) return findAllEvents();
        if (byWeek) {
            int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            int year = date.getYear();
            return findEventsByWeek(week, year);
        }
        return findEventsByDate(date.toString());
    }

    private Event validateEvent(EventRequest event) {
        if (event.getName() == null || event.getName().isBlank()) {
            throw new InvalidEventException("Event name is required");
        }
        if (event.getDescription() == null || event.getDescription().isBlank()) {
            throw new InvalidEventException("Event description is required");
        }
        if (event.getType() == null) {
            throw new InvalidEventException("Event type is required");
        }
        if (!isValidEventType(event.getType())) {
            throw new InvalidEventException("Invalid event type. Valid types are: " + Arrays.toString(EventType.values()));
        }
        if (event.getDateTime() == null) {
            throw new InvalidEventException("Event date/time is required");
        }
        try {
            LocalDateTime.parse(event.getDateTime());
        } catch (DateTimeParseException e) {
            throw new InvalidEventException("Invalid date format. Please use YYYY-MM-DDTHH:MM:SS format");
        }

        return new Event(event);
    }

    public List<String> findAllEventsTypes() {
        return Arrays.stream(EventType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public boolean isValidEventType(String type) {
        try {
            EventType.valueOf(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private EventLinksFactory createLinksFactory() {
        return new EventLinksFactory(uriInfo);
    }

    private void addLinksToEvent(Event event) {
        event.addLinks(createLinksFactory().createForEvent(event));
    }

}