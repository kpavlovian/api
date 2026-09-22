package ru.ket.validator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Клиент для обращения к API эмулятора TransferSimulator.
 * <p>
 * По условию задания (Модуль № 4) разрабатывать сам API не требуется —
 * используется предоставленный эмулятор. Класс отвечает только за
 * формирование GET-запроса вида {@code {baseUrl}/{method}} и разбор
 * ответа формата {@code {"value": "..."}}.
 */
public class ApiClient {

    /** Базовый адрес API в лаборатории (используется по умолчанию). */
    public static final String LAB_URL = "http://192.168.1.200:4444/TransferSimulator";

    /** Резервный (интернет) адрес API, указанный в задании. */
    public static final String BACKUP_URL = "http://prb.sylas.ru/TransferSimulator";

    // Простой парсер для {"value": "..."} — без внешних JSON-библиотек,
    // чтобы проект собирался только средствами стандартного JDK.
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("\"value\"\\s*:\\s*\"(.*?)\"\\s*}\\s*$", Pattern.DOTALL);

    private final HttpClient httpClient;
    private String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = normalize(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    private static String normalize(String url) {
        if (url == null || url.isBlank()) {
            return LAB_URL;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = normalize(baseUrl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Выполняет GET-запрос к методу API и возвращает значение поля {@code value}.
     *
     * @param methodName название метода (регистр важен): fullName, snils, inn, email, idEntityCard
     * @return значение из ответа API
     * @throws ApiException при ошибке сети, HTTP-ошибке или некорректном формате ответа
     */
    public String fetchValue(String methodName) throws ApiException {
        String url = baseUrl + "/" + methodName;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ApiException("Не удалось подключиться к API (" + url + "): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Запрос к API был прерван", e);
        }

        int status = response.statusCode();
        if (status == 500) {
            throw new ApiException("HTTP 500 Internal Server Error — обратитесь к главному эксперту," +
                    " скорее всего это не ваша ошибка.");
        }
        if (status < 200 || status >= 300) {
            throw new ApiException("API вернул HTTP " + status + " для метода \"" + methodName + "\"");
        }

        String body = response.body();
        Matcher matcher = VALUE_PATTERN.matcher(body == null ? "" : body.trim());
        if (!matcher.find()) {
            throw new ApiException("Не удалось разобрать ответ API: " + body);
        }
        // Разэкранируем базовые JSON-последовательности (\" \\ \n \t)
        return unescapeJson(matcher.group(1));
    }

    private static String unescapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    default: sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Исключение, оборачивающее ошибки обращения к API. */
    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }

        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
