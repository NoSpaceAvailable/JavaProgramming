package com.micord.client.gif;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GiphyService {
    private static final String GIF_BASE_URL = "https://api.giphy.com/v1/gifs";
    private static final String RANDOM_ID_URL = "https://api.giphy.com/v1/randomid";
    private static final int MAX_BETA_LIMIT = 50;

    private final String apiKey;
    private final HttpClient httpClient;
    private final CompletableFuture<String> randomIdFuture;

    public GiphyService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.randomIdFuture = fetchRandomId()
                .exceptionally(error -> UUID.randomUUID().toString());
    }

    public CompletableFuture<List<GifResult>> searchGifs(String query, int limit) {
        String cleanedQuery = query == null ? "" : query.trim();
        if (cleanedQuery.isBlank()) return trendingGifs(limit);
        if (cleanedQuery.length() > 50) cleanedQuery = cleanedQuery.substring(0, 50);
        int safeLimit = clampLimit(limit);
        String finalQuery = cleanedQuery;

        return randomIdFuture.thenCompose(randomId -> {
            String url = GIF_BASE_URL + "/search"
                    + "?api_key=" + encode(apiKey)
                    + "&q=" + encode(finalQuery)
                    + "&limit=" + safeLimit
                    + "&rating=pg-13&lang=en"
                    + "&random_id=" + encode(randomId)
                    + "&bundle=messaging_non_clips";
            return getBody(url).thenApply(this::parseGifList);
        });
    }

    public CompletableFuture<List<GifResult>> trendingGifs(int limit) {
        int safeLimit = clampLimit(limit);
        return randomIdFuture.thenCompose(randomId -> {
            String url = GIF_BASE_URL + "/trending"
                    + "?api_key=" + encode(apiKey)
                    + "&limit=" + safeLimit
                    + "&rating=pg-13"
                    + "&random_id=" + encode(randomId)
                    + "&bundle=messaging_non_clips";
            return getBody(url).thenApply(this::parseGifList);
        });
    }

    public CompletableFuture<Void> registerOnSent(GifResult gif) {
        return pingAnalytics(gif.onSentUrl());
    }

    private CompletableFuture<String> fetchRandomId() {
        String url = RANDOM_ID_URL + "?api_key=" + encode(apiKey);
        return getBody(url).thenApply(body -> {
            JSONObject root = new JSONObject(body);
            return root.getJSONObject("data").getString("random_id");
        });
    }

    private CompletableFuture<String> getBody(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .header("Accept", "application/json")
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new RuntimeException("GIPHY HTTP " + response.statusCode());
                    }
                    return response.body();
                });
    }

    private CompletableFuture<Void> pingAnalytics(String analyticsUrl) {
        if (analyticsUrl == null || analyticsUrl.isBlank()) return CompletableFuture.completedFuture(null);
        return randomIdFuture.thenCompose(randomId -> {
            String sep = analyticsUrl.contains("?") ? "&" : "?";
            String url = analyticsUrl + sep + "ts=" + System.currentTimeMillis() + "&random_id=" + encode(randomId);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
            return httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(r -> {})
                    .exceptionally(e -> null);
        });
    }

    private List<GifResult> parseGifList(String body) {
        JSONObject root = new JSONObject(body);
        JSONArray data = root.optJSONArray("data");
        List<GifResult> gifs = new ArrayList<>();
        if (data == null) return gifs;

        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            String id = item.optString("id", "");
            String title = item.optString("title", "GIF");
            JSONObject images = item.optJSONObject("images");
            if (images == null) continue;

            String previewUrl = pickImageUrl(images, "fixed_width_small", "fixed_width_downsampled", "fixed_width");
            String gifUrl = pickImageUrl(images, "downsized_medium", "downsized", "fixed_width", "original");
            if (id.isBlank() || previewUrl == null || gifUrl == null) continue;

            JSONObject analytics = item.optJSONObject("analytics");
            gifs.add(new GifResult("GIPHY", id, title, previewUrl, gifUrl,
                    pickAnalyticsUrl(analytics, "onload"),
                    pickAnalyticsUrl(analytics, "onclick"),
                    pickAnalyticsUrl(analytics, "onsent")));
        }
        return gifs;
    }

    private static String pickImageUrl(JSONObject images, String... keys) {
        for (String key : keys) {
            JSONObject r = images.optJSONObject(key);
            if (r != null) {
                String url = r.optString("url", "");
                if (!url.isBlank()) return url;
            }
        }
        return null;
    }

    private static String pickAnalyticsUrl(JSONObject analytics, String key) {
        if (analytics == null) return "";
        JSONObject item = analytics.optJSONObject(key);
        return item != null ? item.optString("url", "") : "";
    }

    private static int clampLimit(int limit) {
        return limit <= 0 ? 24 : Math.min(limit, MAX_BETA_LIMIT);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
