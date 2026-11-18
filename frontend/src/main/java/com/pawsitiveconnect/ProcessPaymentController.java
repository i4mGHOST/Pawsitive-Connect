package com.pawsitiveconnect;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ProcessPaymentController {

    @FXML private Label dialogTitleLabel;
    @FXML private Label applicationDetailsLabel;
    @FXML private VBox pendingSelectBox;
    @FXML private ComboBox<Transaction> pendingTxnCombo;
    @FXML private Label summaryCategoryLabel;
    @FXML private Label summaryAmountLabel;
    @FXML private TextField payerNameField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private Label statusLabel;
    @FXML private Button cancelPaymentBtn;
    @FXML private Button payNowBtn;

    private boolean ledgerPendingMode = false;
    private int currentApplicationId = 0;
    private Transaction selectedPendingTxn = null;
    private Runnable onSuccessCallback;

    @FXML
    public void initialize() {
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
                "UPI", "Net Banking / IMPS", "Debit Card", "Credit Card", "Cash"
        ));
        paymentMethodCombo.getSelectionModel().selectFirst();

        if (pendingTxnCombo != null) {
            pendingTxnCombo.setConverter(new StringConverter<>() {
                @Override
                public String toString(Transaction txn) {
                    if (txn == null) return "";
                    return String.format("TXN-%05d • %s • ₹%.2f (%s)",
                            txn.getTransactionId(), txn.getPayerName(), txn.getAmount(), txn.getCategory());
                }

                @Override
                public Transaction fromString(String s) {
                    return null;
                }
            });

            pendingTxnCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldTxn, newTxn) -> {
                if (newTxn != null) {
                    applySelectedPendingTransaction(newTxn);
                }
            });
        }
    }

    /**
     * Mode A: Admin opens "Process Payment" from the Transaction & Ledger page.
     * Loads all PENDING transactions from PostgreSQL into the dropdown.
     */
    public void setPendingLedgerMode(Runnable onSuccessCallback) {
        this.ledgerPendingMode = true;
        this.onSuccessCallback = onSuccessCallback;

        if (dialogTitleLabel != null) dialogTitleLabel.setText("Process Pending Payment");
        if (pendingSelectBox != null) {
            pendingSelectBox.setVisible(true);
            pendingSelectBox.setManaged(true);
        }
        if (payerNameField != null) payerNameField.setEditable(false);
        if (amountField != null) amountField.setEditable(false);

        loadPendingTransactions();
    }

    private void loadPendingTransactions() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/transactions"))
                .GET()
                .build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> Platform.runLater(() -> {
                    List<Transaction> pendingList = new ArrayList<>();
                    try {
                        JSONArray arr = new JSONArray(res.body());
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            String status = obj.optString("status", "").toUpperCase();
                            if ("PENDING".equals(status)) {
                                int id = obj.optInt("transaction_id", obj.optInt("id", i + 1));
                                String payer = obj.optString("payer_name", "Unknown Payer");
                                double amount = obj.optDouble("amount", 0.0);
                                String method = obj.optString("payment_method", "UPI");
                                String date = obj.optString("transaction_date", "Pending");
                                String category = obj.optString("category", "Donation");
                                pendingList.add(new Transaction(id, payer, amount, method, status, date, category));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    pendingTxnCombo.getItems().setAll(pendingList);
                    if (!pendingList.isEmpty()) {
                        pendingTxnCombo.getSelectionModel().selectFirst();
                        payNowBtn.setDisable(false);
                        cancelPaymentBtn.setDisable(false);
                    } else {
                        applicationDetailsLabel.setText("No pending transactions available right now.");
                        summaryCategoryLabel.setText("Queue Clear");
                        summaryAmountLabel.setText("₹0.00");
                        payerNameField.clear();
                        amountField.setText("0.00");
                        payNowBtn.setDisable(true);
                        cancelPaymentBtn.setDisable(true);
                        statusLabel.setStyle("-fx-text-fill: #2C694E;");
                        statusLabel.setText("All recorded transactions are already settled.");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: #D93025;");
                        statusLabel.setText("Could not load pending transactions from server.");
                    });
                    return null;
                });
    }

    private void applySelectedPendingTransaction(Transaction txn) {
        this.selectedPendingTxn = txn;
        String formattedFee = String.format("%.2f", txn.getAmount());

        applicationDetailsLabel.setText(String.format("Processing TXN-%05d | Payer: %s", txn.getTransactionId(), txn.getPayerName()));
        summaryCategoryLabel.setText(txn.getCategory() + " • " + txn.getPayerName());
        summaryAmountLabel.setText("₹" + formattedFee);

        payerNameField.setText(txn.getPayerName());
        amountField.setText(formattedFee);
        amountField.setEditable(false);

        if (txn.getMethod() != null && !txn.getMethod().isBlank()) {
            if (!paymentMethodCombo.getItems().contains(txn.getMethod())) {
                paymentMethodCombo.getItems().add(txn.getMethod());
            }
            paymentMethodCombo.setValue(txn.getMethod());
        }
        statusLabel.setText("");
    }

    /**
     * Mode B: Adopter settles an approved adoption fee from the Adoptions page.
     */
    public void setApplicationData(int appId, String applicantName, Runnable onSuccessCallback) {
        this.ledgerPendingMode = false;
        this.currentApplicationId = appId;
        this.onSuccessCallback = onSuccessCallback;

        if (pendingSelectBox != null) {
            pendingSelectBox.setVisible(false);
            pendingSelectBox.setManaged(false);
        }
        if (dialogTitleLabel != null) dialogTitleLabel.setText("Process Adoption Fee");
        this.applicationDetailsLabel.setText("App #APP-" + String.format("%04d", appId) + " | " + applicantName);
        if (summaryCategoryLabel != null) summaryCategoryLabel.setText("Adoption & Medical Transfer Fee");
        this.payerNameField.setText(applicantName);
        this.payerNameField.setEditable(true);
        this.amountField.setText("1500.00");
        this.amountField.setEditable(false);
        if (summaryAmountLabel != null) {
            this.summaryAmountLabel.setText("₹1500.00");
        }
    }

    public void setApplicationData(int appId, String applicantName, double feeAmount, Runnable onSuccessCallback) {
        setApplicationData(appId, applicantName, onSuccessCallback);
        String formattedFee = String.format("%.2f", feeAmount);
        this.amountField.setText(formattedFee);
        this.amountField.setEditable(false);
        if (this.summaryAmountLabel != null) {
            this.summaryAmountLabel.setText("₹" + formattedFee);
        }
    }

    @FXML
    public void handleProcessPayment(ActionEvent event) {
        String method = paymentMethodCombo.getValue();

        if (ledgerPendingMode) {
            if (selectedPendingTxn == null) {
                statusLabel.setStyle("-fx-text-fill: #D93025;");
                statusLabel.setText("Please select a pending transaction first.");
                return;
            }
            settlePendingTransactionInBackend(selectedPendingTxn.getTransactionId(), method);
            return;
        }

        String payer = payerNameField.getText().trim();
        String amountStr = amountField.getText().trim();

        if (payer.isEmpty() || amountStr.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #D93025;");
            statusLabel.setText("Please fill in all required fields.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            sendAdoptionPaymentToBackend(payer, amount, method);
        } catch (NumberFormatException e) {
            statusLabel.setStyle("-fx-text-fill: #D93025;");
            statusLabel.setText("Invalid amount format.");
        }
    }

    private void settlePendingTransactionInBackend(int transactionId, String method) {
        statusLabel.setStyle("-fx-text-fill: #B8860B;");
        statusLabel.setText("Completing transaction settlement...");

        JSONObject payload = new JSONObject();
        payload.put("payment_method", method);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/transactions/" + transactionId + "/pay"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        if (onSuccessCallback != null) onSuccessCallback.run();
                        closeDialog();
                    } else {
                        statusLabel.setStyle("-fx-text-fill: #D93025;");
                        statusLabel.setText("Server response: " + response.body());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: #D93025;");
                        statusLabel.setText("Could not connect to payment gateway.");
                    });
                    return null;
                });
    }

    private void sendAdoptionPaymentToBackend(String payer, double amount, String method) {
        statusLabel.setStyle("-fx-text-fill: #B8860B;");
        statusLabel.setText("Processing payment settlement...");

        String jsonPayload = String.format(
                "{\"application_id\": %d, \"payer_name\": \"%s\", \"amount\": %.2f, \"payment_method\": \"%s\"}",
                currentApplicationId, payer.replace("\"", "\\\""), amount, method
        );

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/transactions/process"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            if (onSuccessCallback != null) onSuccessCallback.run();
                            closeDialog();
                        } else {
                            statusLabel.setStyle("-fx-text-fill: #D93025;");
                            statusLabel.setText("Server response: " + response.body());
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            statusLabel.setStyle("-fx-text-fill: #D93025;");
                            statusLabel.setText("Could not connect to payment gateway.");
                        });
                        return null;
                    });

        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: #D93025;");
            statusLabel.setText("Failed to dispatch request.");
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        if (ledgerPendingMode && selectedPendingTxn != null) {
            statusLabel.setStyle("-fx-text-fill: #B8860B;");
            statusLabel.setText("Cancelling pending payment...");

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/transactions/" + selectedPendingTxn.getTransactionId() + "/cancel"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (onSuccessCallback != null) onSuccessCallback.run();
                        closeDialog();
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(this::closeDialog);
                        return null;
                    });
        } else {
            closeDialog();
        }
    }

    @FXML
    public void handleDismissDialog(ActionEvent event) {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) payerNameField.getScene().getWindow();
        stage.close();
    }
}