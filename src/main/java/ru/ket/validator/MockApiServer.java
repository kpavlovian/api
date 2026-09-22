package ru.ket.validator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * Встроенный локальный сервер-заглушка, эмулирующий TransferSimulator API.
 * <p>
 * НЕ является частью боевой логики приложения и НЕ заменяет реальный
 * эмулятор из задания — используется исключительно для автономной
 * отладки клиента, когда лабораторный/интернет-адрес API недоступен
 * (например, дома или вне сети техникума).
 * <p>
 * Часть ответов намеренно содержит "плохие" данные (ФИО с цифрами/
 * спецсимволами), чтобы можно было проверить обе ветки валидации —
 * как в макете задания ("Ива&нов 1ван 1ванович!").
 */
public class MockApiServer {

    private final HttpServer server;
    private final int port;
    private final Random random = new Random();

    // Примеры валидных и невалидных значений ФИО (как в макете задания)
    private static final String[] SAMPLE_FULL_NAMES = {
            "Павлов Кирилл Евгеньевич",
            "Иванова Мария Петровна",
            "Ива&нов 1ван 1ванович!",
            "Петров123 Иван Сергеевич",
            "Кузнецова Анна"
    };

    private static final String[] SAMPLE_SNILS = {
            "789-012-345 67",
            "123-456-789 00",
            "ABC-012-345 67"
    };

    private static final String[] SAMPLE_INN = {
            "7707083893",
            "770708389312",
            "77070838"
    };

    private static final String[] SAMPLE_EMAIL = {
            "sokolov.sokol@aol.com",
            "ivanova.maria@mail.ru",
            "не-email-строка"
    };

    private static final String[] SAMPLE_CARD = {
            "10 19 012345",
            "20 05 998877",
            "карта-XX-99"
    };

    public MockApiServer(int port) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        registerHandler("fullName", SAMPLE_FULL_NAMES);
        registerHandler("snils", SAMPLE_SNILS);
        registerHandler("inn", SAMPLE_INN);
        registerHandler("email", SAMPLE_EMAIL);
        registerHandler("idEntityCard", SAMPLE_CARD);
    }

    private void registerHandler(String path, String[] samples) {
        server.createContext("/TransferSimulator/" + path, exchange -> handle(exchange, samples));
    }

    private void handle(HttpExchange exchange, String[] samples) throws IOException {
        String value = samples[random.nextInt(samples.length)];
        String json = "{\n  \"value\": \"" + escapeJson(value) + "\"\n}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public String getBaseUrl() {
        return "http://localhost:" + port + "/TransferSimulator";
    }

    /** Позволяет запустить сервер отдельно, вне GUI, для проверки через Bruno/curl. */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 4444;
        MockApiServer mock = new MockApiServer(port);
        mock.start();
        System.out.println("Mock TransferSimulator API запущен: " + mock.getBaseUrl());
        System.out.println("Доступные методы: " + Map.of(
                "fullName", "ФИО",
                "snils", "СНИЛС",
                "inn", "ИНН",
                "email", "email",
                "idEntityCard", "номер карты"
        ));
    }
}
