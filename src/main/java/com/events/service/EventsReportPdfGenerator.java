package com.events.service;

import com.events.model.Event;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class EventsReportPdfGenerator {
    private static final float MARGIN = 50;
    private static final PDType1Font HEADER_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font BODY_FONT = PDType1Font.HELVETICA;
    private static final float HEADER_FONT_SIZE = 18;
    private static final float BODY_FONT_SIZE = 10;
    private static final float LINE_HEIGHT = 18;
    private static final float ROW_OFFSET = 8;
    private static final Color HEADER_BG_COLOR = new Color(63, 81, 181);
    private static final Color SEPARATOR_COLOR = new Color(200, 200, 200);

    public byte[] generate(List<Event> events) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = createLandscapePage();
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float pageWidth = page.getMediaBox().getWidth();
            float yPosition = page.getMediaBox().getHeight() - MARGIN;

            drawHeader(contentStream, pageWidth, yPosition);
            yPosition -= LINE_HEIGHT * 3;

            drawTable(document, contentStream, events, page, pageWidth, yPosition);

            contentStream.close();
            return saveDocumentToByteArray(document);
        }
    }

    private PDPage createLandscapePage() {
        return new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
    }

    private void drawHeader(PDPageContentStream contentStream, float pageWidth, float yPosition) throws IOException {
        contentStream.setFont(HEADER_FONT, HEADER_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition - HEADER_FONT_SIZE);
        contentStream.showText("Events Report");
        contentStream.endText();

        String generatedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", Locale.ENGLISH));
        float textWidth = HEADER_FONT.getStringWidth(generatedDate) / 1000 * HEADER_FONT_SIZE;
        contentStream.beginText();
        contentStream.newLineAtOffset(pageWidth - MARGIN - textWidth, yPosition - HEADER_FONT_SIZE);
        contentStream.showText(generatedDate);
        contentStream.endText();
    }

    private void drawTable(PDDocument document, PDPageContentStream contentStream, List<Event> events,
                           PDPage page, float pageWidth, float startY) throws IOException {
        float[] columnWidths = calculateColumnWidths(pageWidth);
        int rowCounter = 0;
        float yPosition = startY;

        while (rowCounter < events.size()) {
            if (yPosition < MARGIN + LINE_HEIGHT) {
                contentStream.close();
                page = createLandscapePage();
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                yPosition = page.getMediaBox().getHeight() - MARGIN;
            }

            drawTableHeader(contentStream, yPosition, columnWidths);
            yPosition -= LINE_HEIGHT * 1.5f;

            for (int i = rowCounter; i < events.size(); i++) {
                if (yPosition < MARGIN + LINE_HEIGHT) break;

                Event event = events.get(i);
                drawEventRow(contentStream, yPosition, columnWidths, event);
                drawRowSeparator(contentStream, yPosition, columnWidths);

                yPosition -= LINE_HEIGHT + ROW_OFFSET;
                rowCounter++;
            }
        }
        contentStream.close();
    }

    private float[] calculateColumnWidths(float pageWidth) {
        float tableWidth = pageWidth - 2 * MARGIN;
        return new float[]{
                tableWidth * 0.10f,
                tableWidth * 0.25f,
                tableWidth * 0.15f,
                tableWidth * 0.30f,
                tableWidth * 0.20f
        };
    }

    private void drawTableHeader(PDPageContentStream contentStream, float yPosition,
                                 float[] columnWidths) throws IOException {
        contentStream.setNonStrokingColor(HEADER_BG_COLOR);
        contentStream.addRect(MARGIN, yPosition - LINE_HEIGHT, sum(columnWidths), LINE_HEIGHT);
        contentStream.fill();

        contentStream.setFont(HEADER_FONT, BODY_FONT_SIZE);
        contentStream.setNonStrokingColor(Color.WHITE);

        float xPosition = MARGIN + 5;
        String[] headers = {"ID", "Event Name", "Type", "Description", "Date & Time"};
        float verticalOffset = (LINE_HEIGHT - BODY_FONT_SIZE) / 2 + 2;

        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition, yPosition - LINE_HEIGHT + verticalOffset);
            contentStream.showText(headers[i]);
            contentStream.endText();
            xPosition += columnWidths[i];
        }
    }

    private void drawEventRow(PDPageContentStream contentStream, float yPosition,
                              float[] columnWidths, Event event) throws IOException {
        contentStream.setFont(BODY_FONT, BODY_FONT_SIZE);
        contentStream.setNonStrokingColor(Color.BLACK);

        String[] columns = {
                String.valueOf(event.getId()),
                event.getName(),
                event.getType().toString(),
                truncateText(event.getDescription(), 35),
                event.getDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH))
        };

        float xPosition = MARGIN + 5;
        for (int i = 0; i < columns.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition, yPosition);
            contentStream.showText(columns[i]);
            contentStream.endText();
            xPosition += columnWidths[i];
        }
    }

    private void drawRowSeparator(PDPageContentStream contentStream, float yPosition,
                                  float[] columnWidths) throws IOException {
        contentStream.setStrokingColor(SEPARATOR_COLOR);
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, yPosition - ROW_OFFSET + 2);
        contentStream.lineTo(MARGIN + sum(columnWidths), yPosition - ROW_OFFSET + 2);
        contentStream.stroke();
    }

    private String truncateText(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

    private float sum(float[] arr) {
        float total = 0;
        for (float num : arr) {
            total += num;
        }
        return total;
    }

    private byte[] saveDocumentToByteArray(PDDocument document) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        return outputStream.toByteArray();
    }
}