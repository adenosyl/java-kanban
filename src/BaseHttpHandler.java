import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BaseHttpHandler {
    protected final Gson gson;

    public BaseHttpHandler(Gson gson) {
        this.gson = gson;
    }

    protected void sendJson(HttpExchange h, Object body, int code) throws IOException {
        byte[] resp = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(code, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    protected void sendText(HttpExchange h, String text, int code) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(code, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    protected void sendOk(HttpExchange h, Object body) throws IOException {
        sendJson(h, body, 200);
    }

    protected void sendCreated(HttpExchange h) throws IOException {
        sendText(h, "{\"status\":\"created\"}", 201);
    }

    protected void sendNotFound(HttpExchange h, String message) throws IOException {
        sendText(h, "{\"error\":\"" + escape(message) + "\"}", 404);
    }

    protected void sendHasOverlaps(HttpExchange h, String message) throws IOException {
        sendText(h, "{\"error\":\"" + escape(message) + "\"}", 406);
    }

    protected void sendError(HttpExchange h, String message) throws IOException {
        sendText(h, "{\"error\":\"" + escape(message) + "\"}", 500);
    }

    protected String readBody(HttpExchange h) throws IOException {
        try (InputStream is = h.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected Integer parseId(String query) {
        if (query == null || query.isEmpty()) return null;
        Map<String, String> map = parseQuery(query);
        String idStr = map.get("id");
        if (idStr == null) return null;
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected Map<String, String> parseQuery(String query) {
        Map<String, String> res = new HashMap<>();
        if (query == null || query.isEmpty()) return res;
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                String k = part.substring(0, eq);
                String v = part.substring(eq + 1);
                res.put(k, v);
            } else if (!part.isEmpty()) {
                res.put(part, "");
            }
        }
        return res;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}