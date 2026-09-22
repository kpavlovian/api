package ru.ket.validator;

/**
 * Типы данных клиента, доступные через API эмулятора TransferSimulator.
 * Название метода API (регистр важен!) хранится в {@link #methodName}.
 */
public enum DataType {
    FULL_NAME("fullName", "ФИО клиента"),
    SNILS("snils", "СНИЛС"),
    INN("inn", "ИНН"),
    EMAIL("email", "Адрес электронной почты"),
    ID_ENTITY_CARD("idEntityCard", "Номер карты-пропуска");

    private final String methodName;
    private final String label;

    DataType(String methodName, String label) {
        this.methodName = methodName;
        this.label = label;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        // Отображается в выпадающем списке: "ФИО клиента (fullName)"
        return label + " (" + methodName + ")";
    }
}
