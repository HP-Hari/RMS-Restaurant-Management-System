package com.rms.desktop;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class POSController {

    @FXML private VBox categoryBox;
    @FXML private TilePane menuGrid;
    @FXML private Label menuHeaderLabel;
    @FXML private Label itemCountLabel;

    // Fulfillment Channel & Customer Controls
    @FXML private Button dineInBtn;
    @FXML private Button takeoutBtn;
    @FXML private Button deliveryBtn;
    @FXML private VBox tableBox;
    @FXML private ComboBox<String> tableSelector;
    @FXML private VBox customerBox;
    @FXML private TextField customerNameInput;
    @FXML private TextField customerPhoneInput;

    // Cart Controls
    @FXML private VBox cartItemsBox;
    @FXML private Label ticketCountLabel;
    
    // Discount & Payment Controls
    @FXML private ComboBox<String> discountSelector;
    @FXML private ComboBox<String> paymentSelector;
    @FXML private HBox discountRow;
    @FXML private Label discountNameLabel;
    @FXML private Label discountValLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;

    // Status & Toasts
    @FXML private Label toastLabel;
    @FXML private Label statusLabel;
    @FXML private Label clockLabel;
    @FXML private Label connectionLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8080";
    private static final double TAX_RATE = 0.10;

    private List<JsonNode> allMenuItems = new ArrayList<>();
    private final LinkedHashMap<String, double[]> cart = new LinkedHashMap<>();
    private final Map<String, String> tableIdMap = new HashMap<>();
    
    private String activeOrderType = "DINE_IN";
    private Button activeCategoryBtn = null;

    @FXML
    public void initialize() {
        setupDiscountsAndPayments();
        loadCategories();
        loadMenuItems();
        loadTables();
        startClock();
        checkConnection();
    }

    private void setupDiscountsAndPayments() {
        // Setup Discount Options
        ObservableList<String> discounts = FXCollections.observableArrayList(
                "None",
                "Staff Discount (10%)",
                "VIP Discount (15%)",
                "Promo Code ($5 Off)"
        );
        discountSelector.setItems(discounts);
        discountSelector.getSelectionModel().selectFirst();
        discountSelector.setOnAction(e -> renderCart());

        // Setup Payment Options
        ObservableList<String> payments = FXCollections.observableArrayList(
                "💵 Cash",
                "💳 Credit / Debit Card",
                "📱 UPI / QR Code"
        );
        paymentSelector.setItems(payments);
        paymentSelector.getSelectionModel().selectFirst();
    }

    // ─── CHANNEL SELECTION ───────────────────────────────────

    @FXML
    public void selectDineIn() {
        activeOrderType = "DINE_IN";
        updateChannelStyles(dineInBtn);
        tableBox.setManaged(true);
        tableBox.setVisible(true);
        customerBox.setManaged(false);
        customerBox.setVisible(false);
    }

    @FXML
    public void selectTakeout() {
        activeOrderType = "TAKEOUT";
        updateChannelStyles(takeoutBtn);
        tableBox.setManaged(false);
        tableBox.setVisible(false);
        customerBox.setManaged(true);
        customerBox.setVisible(true);
    }

    @FXML
    public void selectDelivery() {
        activeOrderType = "DELIVERY";
        updateChannelStyles(deliveryBtn);
        tableBox.setManaged(false);
        tableBox.setVisible(false);
        customerBox.setManaged(true);
        customerBox.setVisible(true);
    }

    private void updateChannelStyles(Button activeBtn) {
        dineInBtn.getStyleClass().remove("channel-btn-active");
        takeoutBtn.getStyleClass().remove("channel-btn-active");
        deliveryBtn.getStyleClass().remove("channel-btn-active");
        activeBtn.getStyleClass().add("channel-btn-active");
    }

    // ─── DATA LOADING ────────────────────────────────────────

    private void loadCategories() {
        httpGet("/api/menu/categories", body -> {
            Platform.runLater(() -> {
                try {
                    JsonNode root = mapper.readTree(body);
                    Button allBtn = createCategoryButton("All Items", "\uD83C\uDF7D");
                    allBtn.getStyleClass().add("nav-btn-active");
                    activeCategoryBtn = allBtn;
                    allBtn.setOnAction(e -> filterByCategory(null, allBtn));
                    categoryBox.getChildren().add(allBtn);

                    String[] icons = {"\uD83C\uDF54", "\uD83C\uDF55", "\uD83E\uDD57", "\uD83C\uDF79", "\uD83C\uDF70", "\uD83E\uDD69", "\uD83C\uDF5D", "\uD83C\uDF5B"};
                    int i = 0;
                    for (JsonNode node : root) {
                        String cat = node.asText();
                        String icon = icons[i % icons.length];
                        Button btn = createCategoryButton(cat, icon);
                        btn.setOnAction(e -> filterByCategory(cat, btn));
                        categoryBox.getChildren().add(btn);
                        i++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private Button createCategoryButton(String text, String icon) {
        Button btn = new Button(icon + "  " + text);
        btn.getStyleClass().add("nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private void loadMenuItems() {
        httpGet("/api/menu", body -> {
            Platform.runLater(() -> {
                try {
                    JsonNode root = mapper.readTree(body);
                    allMenuItems.clear();
                    for (JsonNode node : root) {
                        allMenuItems.add(node);
                    }
                    renderMenuItems(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void loadTables() {
        httpGet("/api/tables", body -> {
            Platform.runLater(() -> {
                try {
                    JsonNode root = mapper.readTree(body);
                    ObservableList<String> items = FXCollections.observableArrayList();
                    for (JsonNode node : root) {
                        int num = node.get("table_number").asInt();
                        int cap = node.get("capacity").asInt();
                        boolean occ = node.has("is_occupied") && node.get("is_occupied").asBoolean();
                        String display = "Table " + num + " (" + cap + " seats)" + (occ ? " - OCCUPIED" : "");
                        items.add(display);
                        tableIdMap.put(display, node.get("id").asText());
                    }
                    tableSelector.setItems(items);
                    if (!items.isEmpty()) {
                        tableSelector.getSelectionModel().selectFirst();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    // ─── CATEGORY FILTERING ──────────────────────────────────

    private void filterByCategory(String category, Button btn) {
        if (activeCategoryBtn != null) {
            activeCategoryBtn.getStyleClass().remove("nav-btn-active");
        }
        btn.getStyleClass().add("nav-btn-active");
        activeCategoryBtn = btn;
        renderMenuItems(category);
    }

    private void renderMenuItems(String category) {
        menuGrid.getChildren().clear();
        int count = 0;
        for (JsonNode node : allMenuItems) {
            if (node.has("is_available") && !node.get("is_available").asBoolean()) continue;
            if (category != null && !category.equals(node.get("category").asText())) continue;

            String name = node.get("name").asText();
            double price = node.get("price").asDouble();
            String cat = node.get("category").asText();

            Button btn = new Button();
            btn.getStyleClass().add("menu-item");

            VBox vbox = new VBox(4);
            vbox.setAlignment(Pos.CENTER);

            Label catLabel = new Label(cat.toUpperCase());
            catLabel.getStyleClass().add("menu-category");

            Label titleLabel = new Label(name);
            titleLabel.getStyleClass().add("menu-title");
            titleLabel.setWrapText(true);

            Label priceLabel = new Label(String.format("$%.2f", price));
            priceLabel.getStyleClass().add("menu-price");

            vbox.getChildren().addAll(catLabel, titleLabel, priceLabel);
            btn.setGraphic(vbox);
            btn.setOnAction(e -> addItem(name, price));

            menuGrid.getChildren().add(btn);
            count++;
        }
        menuHeaderLabel.setText(category == null ? "All Items" : category);
        itemCountLabel.setText(count + " item" + (count != 1 ? "s" : ""));
    }

    // ─── CART MANAGEMENT ─────────────────────────────────────

    private void addItem(String name, double price) {
        if (cart.containsKey(name)) {
            cart.get(name)[1] += 1;
        } else {
            cart.put(name, new double[]{price, 1});
        }
        renderCart();
    }

    private void removeItem(String name) {
        cart.remove(name);
        renderCart();
    }

    private void changeQuantity(String name, int delta) {
        if (!cart.containsKey(name)) return;
        double[] data = cart.get(name);
        data[1] += delta;
        if (data[1] <= 0) {
            cart.remove(name);
        }
        renderCart();
    }

    private void renderCart() {
        cartItemsBox.getChildren().clear();
        double subtotal = 0;
        int totalQty = 0;

        for (Map.Entry<String, double[]> entry : cart.entrySet()) {
            String name = entry.getKey();
            double price = entry.getValue()[0];
            int qty = (int) entry.getValue()[1];
            subtotal += price * qty;
            totalQty += qty;

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("cart-item-row");

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("cart-item-name");
            Label priceInfo = new Label(String.format("$%.2f ea.", price));
            priceInfo.getStyleClass().add("cart-item-price");
            info.getChildren().addAll(nameLabel, priceInfo);

            HBox qtyBox = new HBox(4);
            qtyBox.setAlignment(Pos.CENTER);
            Button minusBtn = new Button("-");
            minusBtn.getStyleClass().add("qty-btn");
            minusBtn.setOnAction(e -> changeQuantity(name, -1));
            Label qtyLabel = new Label(String.valueOf(qty));
            qtyLabel.getStyleClass().add("qty-label");
            Button plusBtn = new Button("+");
            plusBtn.getStyleClass().add("qty-btn");
            plusBtn.setOnAction(e -> changeQuantity(name, 1));
            qtyBox.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

            VBox lineBox = new VBox(2);
            lineBox.setAlignment(Pos.CENTER_RIGHT);
            Label lineTotal = new Label(String.format("$%.2f", price * qty));
            lineTotal.getStyleClass().add("cart-line-total");
            Button removeBtn = new Button("\u2715");
            removeBtn.getStyleClass().add("remove-btn");
            removeBtn.setOnAction(e -> removeItem(name));
            lineBox.getChildren().addAll(lineTotal, removeBtn);

            row.getChildren().addAll(info, qtyBox, lineBox);
            cartItemsBox.getChildren().add(row);
        }

        // Calculate Discount
        double discountAmount = 0.0;
        String selectedDiscount = discountSelector.getValue();
        if ("Staff Discount (10%)".equals(selectedDiscount)) {
            discountAmount = subtotal * 0.10;
        } else if ("VIP Discount (15%)".equals(selectedDiscount)) {
            discountAmount = subtotal * 0.15;
        } else if ("Promo Code ($5 Off)".equals(selectedDiscount) && subtotal > 5.0) {
            discountAmount = 5.00;
        }

        if (discountAmount > 0) {
            discountRow.setManaged(true);
            discountRow.setVisible(true);
            discountNameLabel.setText(selectedDiscount);
            discountValLabel.setText(String.format("-$%.2f", discountAmount));
        } else {
            discountRow.setManaged(false);
            discountRow.setVisible(false);
        }

        double taxableSubtotal = Math.max(0, subtotal - discountAmount);
        double tax = taxableSubtotal * TAX_RATE;
        double total = taxableSubtotal + tax;

        ticketCountLabel.setText(totalQty + " item" + (totalQty != 1 ? "s" : ""));
        subtotalLabel.setText(String.format("$%.2f", subtotal));
        taxLabel.setText(String.format("$%.2f", tax));
        totalLabel.setText(String.format("$%.2f", total));
    }

    @FXML
    public void clearCart() {
        cart.clear();
        renderCart();
    }

    // ─── CHECKOUT ────────────────────────────────────────────

    @FXML
    public void checkout() {
        if (cart.isEmpty()) return;
        
        int tableVal = 0;
        if ("DINE_IN".equals(activeOrderType)) {
            if (tableSelector.getValue() == null) {
                showToast("Please select a table first!", "toast-error");
                return;
            }
            String tableDisplay = tableSelector.getValue();
            String tableNum = tableDisplay.replaceAll("[^0-9]", "").trim();
            if (tableNum.length() > 2) tableNum = tableNum.substring(0, 1);
            try { tableVal = Integer.parseInt(tableNum); } catch (Exception e) {}
        } else {
            if (customerNameInput.getText() == null || customerNameInput.getText().trim().isEmpty()) {
                showToast("Please enter Customer Name!", "toast-error");
                return;
            }
        }

        double subtotal = 0;
        for (double[] data : cart.values()) {
            subtotal += data[0] * data[1];
        }
        
        double discountAmount = 0.0;
        String selectedDiscount = discountSelector.getValue();
        if ("Staff Discount (10%)".equals(selectedDiscount)) discountAmount = subtotal * 0.10;
        else if ("VIP Discount (15%)".equals(selectedDiscount)) discountAmount = subtotal * 0.15;
        else if ("Promo Code ($5 Off)".equals(selectedDiscount) && subtotal > 5.0) discountAmount = 5.00;

        double taxableSubtotal = Math.max(0, subtotal - discountAmount);
        double tax = taxableSubtotal * TAX_RATE;
        double total = taxableSubtotal + tax;

        String rawPayment = paymentSelector.getValue();
        String paymentMethod = "CASH";
        if (rawPayment != null) {
            if (rawPayment.contains("Card")) paymentMethod = "CARD";
            else if (rawPayment.contains("UPI")) paymentMethod = "UPI";
        }

        try {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("order_type", activeOrderType);
            orderMap.put("table_number", tableVal);
            orderMap.put("total_amount", total);
            orderMap.put("discount_amount", discountAmount);
            orderMap.put("discount_name", "None".equals(selectedDiscount) ? "" : selectedDiscount);
            orderMap.put("payment_method", paymentMethod);
            orderMap.put("payment_status", "PAID");
            orderMap.put("customer_name", customerNameInput.getText() != null ? customerNameInput.getText().trim() : "");
            orderMap.put("customer_phone", customerPhoneInput.getText() != null ? customerPhoneInput.getText().trim() : "");
            orderMap.put("status", "PENDING");

            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (Map.Entry<String, double[]> entry : cart.entrySet()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("name", entry.getKey());
                itemMap.put("price", entry.getValue()[0]);
                itemMap.put("quantity", (int) entry.getValue()[1]);
                itemsList.add(itemMap);
            }
            orderMap.put("items_json", mapper.writeValueAsString(itemsList));

            String jsonBody = mapper.writeValueAsString(orderMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/orders"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            final int finalTableVal = tableVal;
            final double finalSubtotal = subtotal;
            final double finalDiscount = discountAmount;
            final double finalTax = tax;
            final double finalTotal = total;
            final String finalPaymentMethod = paymentMethod;

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response.statusCode() == 200 || response.statusCode() == 201) {
                                String createdId = "UNKNOWN";
                                try {
                                    JsonNode orderNode = mapper.readTree(response.body());
                                    createdId = orderNode.has("id") ? orderNode.get("id").asText() : "N/A";
                                } catch (Exception ex) {}

                                generateReceiptFile(createdId, activeOrderType, finalTableVal, customerNameInput.getText(), customerPhoneInput.getText(), finalSubtotal, finalDiscount, finalTax, finalTotal, finalPaymentMethod);

                                cart.clear();
                                renderCart();
                                showToast("Order Placed & Receipt Printed!", "toast-success");
                                statusLabel.setText("Last order placed at " +
                                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                            } else {
                                showToast("Order failed. Try again.", "toast-error");
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> showToast("Connection error!", "toast-error"));
                        return null;
                    });
        } catch (Exception e) {
            showToast("Error: " + e.getMessage(), "toast-error");
        }
    }

    private void generateReceiptFile(String orderId, String orderType, int tableNum, String custName, String custPhone, double subtotal, double discount, double tax, double total, String payMethod) {
        try {
            java.io.File receiptsDir = new java.io.File("/Users/hari/RMS/receipts");
            if (!receiptsDir.exists()) receiptsDir.mkdirs();

            String filename = String.format("receipt_order_%s.txt", orderId);
            java.io.File receiptFile = new java.io.File(receiptsDir, filename);

            java.io.PrintWriter writer = new java.io.PrintWriter(receiptFile);
            writer.println("========================================");
            writer.println("            RMS PREMIUM POS");
            writer.println("         123 Restaurant Blvd.");
            writer.println("========================================");
            writer.println("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("Order ID: " + orderId);
            writer.println("Channel:  " + orderType);
            if ("DINE_IN".equals(orderType)) {
                writer.println("Table #:  " + tableNum);
            } else {
                writer.println("Customer: " + (custName != null ? custName : "Walk-In"));
                if (custPhone != null && !custPhone.isEmpty()) writer.println("Phone:    " + custPhone);
            }
            writer.println("Tender:   " + payMethod + " [PAID]");
            writer.println("----------------------------------------");
            writer.printf("%-4s %-25s %7s\n", "Qty", "Item", "Price");
            writer.println("----------------------------------------");

            for (Map.Entry<String, double[]> entry : cart.entrySet()) {
                String name = entry.getKey();
                double price = entry.getValue()[0];
                int qty = (int) entry.getValue()[1];
                String displayName = name.length() > 25 ? name.substring(0, 22) + "..." : name;
                writer.printf("%-4d %-25s $%6.2f\n", qty, displayName, price * qty);
            }

            writer.println("----------------------------------------");
            writer.printf("%-30s $%6.2f\n", "Subtotal:", subtotal);
            if (discount > 0) {
                writer.printf("%-30s -$%5.2f\n", "Discount:", discount);
            }
            writer.printf("%-30s $%6.2f\n", "Tax (10%):", tax);
            writer.printf("%-30s $%6.2f\n", "TOTAL:", total);
            writer.println("========================================");
            writer.println("       SENT TO KITCHEN / PRINTED");
            writer.println("========================================");
            writer.close();

            System.out.println("Receipt printed to: " + receiptFile.getAbsolutePath());
            sendToPhysicalPrinter(receiptFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendToPhysicalPrinter(java.io.File receiptFile) {
        try {
            javax.print.PrintService defaultPrinter = javax.print.PrintServiceLookup.lookupDefaultPrintService();
            if (defaultPrinter == null) {
                System.out.println("No default print service found. Local copy saved.");
                return;
            }

            javax.print.DocPrintJob job = defaultPrinter.createPrintJob();
            java.io.FileInputStream fis = new java.io.FileInputStream(receiptFile);
            javax.print.DocFlavor flavor = javax.print.DocFlavor.INPUT_STREAM.AUTOSENSE;
            javax.print.Doc doc = new javax.print.SimpleDoc(fis, flavor, null);

            job.print(doc, null);
            fis.close();
            System.out.println("Sent print job to OS default printer: " + defaultPrinter.getName());
        } catch (Exception e) {
            System.out.println("Routing print job to OS spooler failed.");
            e.printStackTrace();
        }
    }

    // ─── TOAST NOTIFICATION ──────────────────────────────────

    private void showToast(String message, String styleClass) {
        toastLabel.setText(message);
        toastLabel.getStyleClass().setAll("toast-label", styleClass);
        toastLabel.setVisible(true);
        toastLabel.setManaged(true);
        toastLabel.setOpacity(1.0);

        PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
        pause.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(500), toastLabel);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(ev -> {
                toastLabel.setVisible(false);
                toastLabel.setManaged(false);
            });
            fade.play();
        });
        pause.play();
    }

    // ─── STATUS BAR & CLOCK ──────────────────────────────────

    private void startClock() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                clockLabel.setText(LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm:ss")));
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void checkConnection() {
        httpGet("/api/menu", body -> {
            Platform.runLater(() -> connectionLabel.setText("\u26AB Connected"));
        });
    }

    private void httpGet(String path, java.util.function.Consumer<String> onSuccess) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(onSuccess)
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        connectionLabel.setText("\uD83D\uDD34 Disconnected");
                        statusLabel.setText("Backend not reachable");
                    });
                    return null;
                });
    }
}
