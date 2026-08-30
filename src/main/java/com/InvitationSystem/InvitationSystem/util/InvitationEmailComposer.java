package com.InvitationSystem.InvitationSystem.util;

import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;

/**
 * Short personal invitation copy. Plain text + HTML keep the same words so filters see a real letter, not an image.
 */
public final class InvitationEmailComposer {

    private InvitationEmailComposer() {}

    public static String subject(DeliveryRequest request) {
        return "Your invitation to " + eventName(request, false);
    }

    public static String plainText(DeliveryRequest request) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(guestName(request, false)).append(",\n\n");
        body.append("You are invited to ").append(eventName(request, false)).append(".\n");
        String when = whenLine(request, false);
        if (when != null) {
            body.append(when).append("\n");
        }
        String where = whereLine(request, false);
        if (where != null) {
            body.append(where).append("\n");
        }
        body.append("\nYour personal card is included with this message. Please bring it — the QR is for the door.\n\n");
        body.append("We look forward to seeing you.\n");
        return body.toString();
    }

    public static String html(DeliveryRequest request, boolean includeCardImage) {
        String cardBlock = includeCardImage
                ? "<p style=\"margin:28px 0;\"><img src=\"cid:invitation-card\" alt=\"Your invitation card\" width=\"520\" style=\"max-width:100%;height:auto;border:0;display:block;\" /></p>"
                : "<p style=\"margin:28px 0;color:#5c5a56;\">Your invitation card is attached to this email.</p>";

        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>"
                + "<title>" + escape(eventName(request, false)) + "</title></head>"
                + "<body style=\"margin:0;padding:0;background:#F8F5EF;color:#111318;\">"
                + "<div style=\"max-width:560px;margin:0 auto;padding:32px 24px;font-family:Georgia,'Times New Roman',serif;font-size:16px;line-height:1.55;\">"
                + "<p style=\"margin:0 0 16px;\">Hello " + guestName(request, true) + ",</p>"
                + "<p style=\"margin:0 0 16px;\">You are invited to <strong>" + eventName(request, true) + "</strong>.</p>"
                + detailHtml(whenLine(request, true))
                + detailHtml(whereLine(request, true))
                + cardBlock
                + "<p style=\"margin:24px 0 0;\">Please bring this card. The QR is for the door.</p>"
                + "<p style=\"margin:16px 0 0;\">We look forward to seeing you.</p>"
                + "</div></body></html>";
    }

    private static String detailHtml(String line) {
        if (line == null) {
            return "";
        }
        return "<p style=\"margin:0 0 8px;\">" + line + "</p>";
    }

    private static String guestName(DeliveryRequest request, boolean html) {
        String name = request.getGuestName() != null && !request.getGuestName().isBlank()
                ? request.getGuestName().trim()
                : "Guest";
        return html ? escape(name) : name;
    }

    private static String eventName(DeliveryRequest request, boolean html) {
        String name = request.getEventName() != null && !request.getEventName().isBlank()
                ? request.getEventName().trim()
                : "the event";
        return html ? escape(name) : name;
    }

    private static String whenLine(DeliveryRequest request, boolean html) {
        String raw = request.getEventDate();
        if (raw == null || raw.isBlank() || "TBD".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        String line = "When: " + raw.trim();
        return html ? escape(line) : line;
    }

    private static String whereLine(DeliveryRequest request, boolean html) {
        String raw = request.getVenue();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String line = "Where: " + raw.trim();
        return html ? escape(line) : line;
    }

    static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
