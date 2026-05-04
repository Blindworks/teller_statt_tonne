package de.tellerstatttonne.backend.push;

public record PushPayload(
    String title,
    String body,
    String url,
    String tag
) {
    public static PushPayload of(String title, String body) {
        return new PushPayload(title, body, null, null);
    }

    public static PushPayload of(String title, String body, String url) {
        return new PushPayload(title, body, url, null);
    }
}
