package com.unibazaar.db;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class SupabaseClient {

    private static volatile SupabaseClient instance;

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    private SupabaseClient() {
        this.baseUrl = EnvConfig.get("SUPABASE_URL", "https://your-project.supabase.co");
        this.apiKey = EnvConfig.get("SUPABASE_KEY", "your-anon-key");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public static SupabaseClient getInstance() {
        if (instance == null) {
            synchronized (SupabaseClient.class) {
                if (instance == null) {
                    instance = new SupabaseClient();
                }
            }
        }
        return instance;
    }

    public String get(String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rest/v1/" + endpoint))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Supabase GET failed (" + response.statusCode() + "): " + response.body());
            }
            return response.body();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Supabase GET request failed", e);
        }
    }

    public String post(String table, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rest/v1/" + table))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Supabase POST failed (" + response.statusCode() + "): " + response.body());
            }
            return response.body();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Supabase POST request failed", e);
        }
    }

    public String postAuth(String endpoint, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/v1/" + endpoint))
                .header("apikey", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Supabase Auth failed (" + response.statusCode() + "): " + response.body());
            }
            return response.body();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Supabase Auth request failed", e);
        }
    }

    public void patch(String endpoint, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rest/v1/" + endpoint))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Supabase PATCH failed (" + response.statusCode() + "): " + response.body());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Supabase PATCH request failed", e);
        }
    }
    public void delete(String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/rest/v1/" + endpoint))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .DELETE()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Supabase DELETE failed (" + response.statusCode() + "): " + response.body());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Supabase DELETE request failed", e);
        }
    }
}
