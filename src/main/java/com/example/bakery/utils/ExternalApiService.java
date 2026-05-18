package com.example.bakery.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Сервис для работы с внешними API через raw HttpClient (без сторонних библиотек).
 */
@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);
    
    private final HttpClient httpClient;

    public ExternalApiService() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Получает курс USD относительно базовой валюты (RUB) через публичное API.
     * URL: https://api.exchangerate-api.com/v4/latest/RUB
     * 
     * @return курс обмена (сколько рублей стоит 1 USD, или наоборот, зависит от структуры ответа API)
     */
    public double getUsdToRubRate() {
        String url = "https://api.exchangerate-api.com/v4/latest/RUB";
        
        log.info("Запрос курса валют USD/RUB из внешнего API: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Внешнее API вернуло ошибку: Status Code {}", response.statusCode());
                return 0.0; // Или выбросить исключение
            }

            String body = response.body();
            log.debug("Ответ от API: {}", body);

            // Простой парсинг JSON вручную (или можно использовать Jackson, если он есть в классе пути, 
            // но по заданию "raw HTTP", поэтому сделаем простой поиск подстроки для демонстрации,
            // либо используем встроенный JsonParser из Java 17+ если нужно строго без библиотек.
            // Для простоты и надежности в реальном проекте лучше Jackson, но здесь сделаем через indexOf для примера "raw")
            
            // Структура ответа: {"base":"RUB", "rates":{"USD": 0.0106, ...}}
            // Нам нужно найти значение после "USD":
            double rate = parseRateFromJson(body, "USD");
            
            log.info("Получен курс USD: {}", rate);
            return rate;

        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при вызове внешнего API", e);
            Thread.currentThread().interrupt();
            return 0.0;
        }
    }

    /**
     * Простой парсер для извлечения курса из JSON ответа.
     * В реальном проекте лучше использовать ObjectMapper (Jackson) или JsonParser.
     */
    private double parseRateFromJson(String json, String currency) {
        try {
            String key = "\"" + currency + "\":";
            int index = json.indexOf(key);
            if (index == -1) {
                log.warn("Валюта {} не найдена в ответе API", currency);
                return 0.0;
            }
            
            // Находим начало числа после ключа
            int start = index + key.length();
            // Пропускаем пробелы
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
                start++;
            }
            
            // Находим конец числа (запятая, скобка или конец строки)
            int end = start;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                    break;
                }
                end++;
            }
            
            String valueStr = json.substring(start, end);
            return Double.parseDouble(valueStr);
        } catch (Exception e) {
            log.error("Ошибка парсинга курса из JSON", e);
            return 0.0;
        }
    }
}