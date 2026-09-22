package ru.ket.validator;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Главное окно приложения "Валидация данных" (Модуль № 4).
 * <p>
 * Реализует макет из задания: кнопки "Получить данные" и
 * "Отправить результат теста", поле с полученным значением и полем
 * результата проверки. Дополнительно к макету добавлены:
 * выпадающий список выбора типа данных (fullName/snils/inn/email/
 * idEntityCard) и поле для смены адреса API.
 */
public class MainFrame extends JFrame {

    private final ApiClient apiClient = new ApiClient(ApiClient.LAB_URL);
    private MockApiServer mockServer;

    private final JComboBox<DataType> dataTypeCombo = new JComboBox<>(DataType.values());
    private final JTextField apiUrlField = new JTextField(ApiClient.LAB_URL, 28);
    private final JCheckBox useMockCheckBox = new JCheckBox("Использовать локальную заглушку (для отладки без лаборатории)");

    private final JButton fetchButton = new JButton("Получить данные");
    private final JButton sendButton = new JButton("Отправить результат теста");

    private final JLabel valueLabel = new JLabel(" ");
    private final JLabel resultLabel = new JLabel(" ");
    private final JLabel statusBar = new JLabel(" ");

    private String lastFetchedValue = null;
    private DataType lastFetchedType = null;

    public MainFrame() {
        super("Валидация данных");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildConfigPanel(), BorderLayout.NORTH);
        add(buildFormPanel(), BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        wireActions();

        setMinimumSize(new Dimension(620, 320));
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildConfigPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Настройки API"));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row1.add(new JLabel("Тип данных:"));
        row1.add(dataTypeCombo);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row2.add(new JLabel("Адрес API:"));
        row2.add(apiUrlField);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row3.add(useMockCheckBox);

        panel.add(row1);
        panel.add(row2);
        panel.add(row3);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Валидация данных"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Dimension buttonSize = new Dimension(220, 32);
        fetchButton.setPreferredSize(buttonSize);
        sendButton.setPreferredSize(buttonSize);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(fetchButton, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.PLAIN, 14f));
        panel.add(valueLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(sendButton, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(resultLabel, gbc);

        return panel;
    }

    private void wireActions() {
        sendButton.setEnabled(false);

        useMockCheckBox.addActionListener(e -> {
            boolean useMock = useMockCheckBox.isSelected();
            apiUrlField.setEnabled(!useMock);
            if (useMock) {
                startMockServerIfNeeded();
            } else {
                apiUrlField.setText(ApiClient.LAB_URL);
            }
        });

        fetchButton.addActionListener(e -> onFetchData());
        sendButton.addActionListener(e -> onSendResult());
    }

    private void startMockServerIfNeeded() {
        if (mockServer != null) {
            apiUrlField.setText(mockServer.getBaseUrl());
            return;
        }
        try {
            mockServer = new MockApiServer(0); // порт назначится системой
            // com.sun.net.httpserver с портом 0 не всегда доступен предсказуемо,
            // поэтому используем фиксированный резервный порт с ретраем.
        } catch (IOException ignored) {
        }
        int[] candidatePorts = {4444, 4445, 4446, 8089};
        for (int p : candidatePorts) {
            try {
                mockServer = new MockApiServer(p);
                mockServer.start();
                apiUrlField.setText(mockServer.getBaseUrl());
                setStatus("Локальная заглушка API запущена на порту " + p);
                return;
            } catch (IOException ex) {
                mockServer = null;
            }
        }
        setStatus("Не удалось запустить локальную заглушку API (все порты заняты)");
        useMockCheckBox.setSelected(false);
        apiUrlField.setEnabled(true);
    }

    private void onFetchData() {
        DataType type = (DataType) dataTypeCombo.getSelectedItem();
        String url = apiUrlField.getText().trim();
        apiClient.setBaseUrl(url);

        fetchButton.setEnabled(false);
        setStatus("Запрос к API: " + url + "/" + type.getMethodName() + " ...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return apiClient.fetchValue(type.getMethodName());
            }

            @Override
            protected void done() {
                fetchButton.setEnabled(true);
                try {
                    String value = get();
                    lastFetchedValue = value;
                    lastFetchedType = type;
                    valueLabel.setText(value);
                    resultLabel.setText(" ");
                    sendButton.setEnabled(true);
                    setStatus("Данные получены успешно");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    lastFetchedValue = null;
                    sendButton.setEnabled(false);
                    valueLabel.setText("Ошибка получения данных");
                    resultLabel.setText(" ");
                    setStatus(cause.getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this,
                            cause.getMessage(),
                            "Ошибка API",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void onSendResult() {
        if (lastFetchedValue == null || lastFetchedType == null) {
            setStatus("Сначала получите данные");
            return;
        }
        DataValidator.Result result = DataValidator.validate(lastFetchedType, lastFetchedValue);
        resultLabel.setText(result.message());
        resultLabel.setForeground(result.valid() ? new Color(0, 128, 0) : new Color(200, 0, 0));
        setStatus(result.valid() ? "Проверка пройдена" : "Проверка не пройдена");
    }

    private void setStatus(String text) {
        statusBar.setText(text);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new MainFrame().setVisible(true);
        });
    }
}
