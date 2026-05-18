package com.example.bakery.utils;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ExternalApiService {

    private final HttpClient httpClient;

    public ExternalApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Выполняет GET запрос к внешнему API.
     * Используется для получения данных (например, курсы валют, погода, проверка адреса).
     * 
     * @param url Полный URL внешнего сервиса
     * @return JSON ответ от сервера
     * @throws IOException если ошибка сети
     * @throws InterruptedException если поток прерван
     */
    public String fetchExternalData(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "BakeryShopApp/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Базовая проверка статуса (можно расширить логику обработки ошибок)
        if (response.statusCode() != 200) {
            throw new IOException("External API returned status: " + response.statusCode());
        }

        return response.body();
    }
}