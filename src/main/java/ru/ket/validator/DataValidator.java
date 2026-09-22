package ru.ket.validator;

import java.util.regex.Pattern;

/**
 * Валидация данных, полученных от API-эмулятора.
 * <p>
 * Основное требование задания: валидация ФИО клиента на вхождение
 * запрещённых символов (проверены минимум два критерия — цифры и
 * спецсимволы). Дополнительно реализована базовая валидация остальных
 * типов данных (СНИЛС, ИНН, email, номер карты-пропуска) — это не
 * обязательно по заданию, но соответствует общей логике приложения
 * (проверка корректности данных клиента).
 */
public final class DataValidator {

    private DataValidator() {
    }

    /** Разрешённые символы в ФИО: русские буквы, пробел, дефис. */
    private static final Pattern ALLOWED_FULL_NAME_CHARS =
            Pattern.compile("^[А-Яа-яЁё\\s-]+$");

    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL_CHAR = Pattern.compile("[^А-Яа-яЁё\\s-]");

    private static final Pattern SNILS_PATTERN = Pattern.compile("^\\d{3}-\\d{3}-\\d{3}[\\s-]?\\d{2}$");
    private static final Pattern INN_PATTERN = Pattern.compile("^\\d{10}(\\d{2})?$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /**
     * Результат проверки: признак корректности и текст сообщения.
     */
    public record Result(boolean valid, String message) {
        public static Result ok(String message) {
            return new Result(true, message);
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }
    }

    /**
     * Проверка ФИО на вхождение запрещённых символов.
     * Критерий 1: наличие цифр.
     * Критерий 2: наличие иных спецсимволов (не буква, не пробел, не дефис).
     */
    public static Result validateFullName(String value) {
        if (value == null || value.isBlank()) {
            return Result.fail("ФИО не должно быть пустым");
        }

        boolean hasDigit = HAS_DIGIT.matcher(value).find();
        boolean hasSpecial = HAS_SPECIAL_CHAR.matcher(value).find();

        if (hasDigit || hasSpecial) {
            return Result.fail("ФИО содержит запрещенные символы");
        }

        if (!ALLOWED_FULL_NAME_CHARS.matcher(value).matches()) {
            return Result.fail("ФИО содержит запрещенные символы");
        }

        String[] parts = value.trim().split("\\s+");
        if (parts.length < 2) {
            return Result.fail("ФИО должно содержать не менее двух слов (фамилия и имя)");
        }

        return Result.ok("ФИО корректно");
    }

    public static Result validateSnils(String value) {
        if (value == null || value.isBlank()) {
            return Result.fail("СНИЛС не должен быть пустым");
        }
        if (!SNILS_PATTERN.matcher(value.trim()).matches()) {
            return Result.fail("СНИЛС содержит запрещенные символы или неверный формат");
        }
        return Result.ok("СНИЛС корректен");
    }

    public static Result validateInn(String value) {
        if (value == null || value.isBlank()) {
            return Result.fail("ИНН не должен быть пустым");
        }
        if (!INN_PATTERN.matcher(value.trim()).matches()) {
            return Result.fail("ИНН содержит запрещенные символы или неверную длину");
        }
        return Result.ok("ИНН корректен");
    }

    public static Result validateEmail(String value) {
        if (value == null || value.isBlank()) {
            return Result.fail("Email не должен быть пустым");
        }
        if (!EMAIL_PATTERN.matcher(value.trim()).matches()) {
            return Result.fail("Email имеет некорректный формат");
        }
        return Result.ok("Email корректен");
    }

    public static Result validateIdEntityCard(String value) {
        if (value == null || value.isBlank()) {
            return Result.fail("Номер карты-пропуска не должен быть пустым");
        }
        if (!value.trim().matches("^[\\d\\s]+$")) {
            return Result.fail("Номер карты-пропуска содержит запрещенные символы");
        }
        return Result.ok("Номер карты-пропуска корректен");
    }

    /**
     * Диспетчер проверки в зависимости от выбранного типа данных.
     */
    public static Result validate(DataType type, String value) {
        return switch (type) {
            case FULL_NAME -> validateFullName(value);
            case SNILS -> validateSnils(value);
            case INN -> validateInn(value);
            case EMAIL -> validateEmail(value);
            case ID_ENTITY_CARD -> validateIdEntityCard(value);
        };
    }
}
