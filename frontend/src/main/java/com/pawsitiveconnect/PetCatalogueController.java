package com.pawsitiveconnect;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PetCatalogueController {

    @FXML private Label registryCountLabel;
    @FXML private TextField searchField;
    @FXML private Button clearSearchBtn;
    @FXML private Button addPetBtn;

    @FXML private Button filterAllBtn;
    @FXML private Button filterDogsBtn;
    @FXML private Button filterCatsBtn;
    @FXML private Button filterAvailableBtn;
    @FXML private Button filterMedicalBtn;
    @FXML private Button filterPendingBtn;

    @FXML private Label readyBadge;
    @FXML private Label pendingBadge;
    @FXML private Label medicalBadge;

    @FXML private FlowPane petGridPane;
    @FXML private VBox noResultsBox;

    private final List<Pet> allPets = new ArrayList<>();
    private String currentFilter = "all";

    private boolean isAdmin() {
        int roleId = UserSession.getRoleId();
        return roleId == 1 || roleId == 2;
    }

    @FXML
    public void initialize() {
        boolean admin = isAdmin();
        if (addPetBtn != null) {
            addPetBtn.setVisible(admin);
            addPetBtn.setManaged(admin);
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (clearSearchBtn != null) clearSearchBtn.setVisible(!newVal.trim().isEmpty());
                filterAndDisplayPets();
            });
        }
        loadPetsFromBackend();
    }

    public void loadPetsFromBackend() {
        allPets.clear();
        if (registryCountLabel != null) {
            registryCountLabel.setText("SHELTER REGISTRY • SYNCING...");
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/pets"))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(responseBody -> {
                        try {
                            JSONArray petsArray = new JSONArray(responseBody);
                            for (int i = 0; i < petsArray.length(); i++) {
                                JSONObject petObj = petsArray.getJSONObject(i);

                                int id = petObj.optInt("id", petObj.optInt("pet_id", i + 1));
                                String name = petObj.optString("name", "Unnamed");
                                String species = petObj.optString("species", "Dog");
                                String breed = petObj.optString("breed", "Mixed");
                                int age = petObj.optInt("age", 1);
                                String status = petObj.optString("status", "Available");
                                String imageUrl = petObj.isNull("image_url") ? null : petObj.optString("image_url", null);
                                boolean adoptionReady = petObj.optBoolean("adoption_ready", false);
                                double adoptionFee = petObj.isNull("adoption_fee") ? 0.0 : petObj.optDouble("adoption_fee", 0.0);

                                allPets.add(new Pet(id, name, species, breed, age, status, imageUrl, adoptionReady, adoptionFee));
                            }

                            Platform.runLater(() -> {
                                updateLedgerBadges();
                                filterAndDisplayPets();
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            if (registryCountLabel != null) {
                                registryCountLabel.setText("SHELTER REGISTRY • OFFLINE");
                            }
                        });
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterAndDisplayPets() {
        if (petGridPane == null) return;
        petGridPane.getChildren().clear();

        String query = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        int visibleCount = 0;

        for (Pet pet : allPets) {
            boolean matchesQuery = query.isEmpty()
                    || pet.getName().toLowerCase().contains(query)
                    || pet.getBreed().toLowerCase().contains(query);

            boolean matchesFilter = true;
            String speciesLower = pet.getSpecies().toLowerCase();
            String statusLower = pet.getStatus().toLowerCase();

            switch (currentFilter) {
                case "dog" -> matchesFilter = speciesLower.contains("dog");
                case "cat" -> matchesFilter = speciesLower.contains("cat");
                case "available" -> matchesFilter = statusLower.contains("available");
                case "medical" -> matchesFilter = statusLower.contains("medic") || statusLower.contains("recov");
                case "pending" -> matchesFilter = statusLower.contains("pend");
            }

            if (matchesQuery && matchesFilter) {
                petGridPane.getChildren().add(createPetCard(pet));
                visibleCount++;
            }
        }

        boolean hasResults = visibleCount > 0;
        if (noResultsBox != null) {
            noResultsBox.setVisible(!hasResults);
            noResultsBox.setManaged(!hasResults);
        }
        petGridPane.setVisible(hasResults);
        petGridPane.setManaged(hasResults);

        if (registryCountLabel != null) {
            registryCountLabel.setText("SHELTER REGISTRY • " + allPets.size() + " RESIDENTS");
        }
    }

    private Image loadPetImage(Pet pet) {
        String explicitUrl = pet.getImageUrl();

        if (explicitUrl != null && !explicitUrl.isBlank()) {
            explicitUrl = explicitUrl.trim();

            if (explicitUrl.startsWith("http://") || explicitUrl.startsWith("https://")) {
                try {
                    return new Image(explicitUrl, 295, 170, false, true, true);
                } catch (Exception ignored) {}
            }

            File directFile = new File(explicitUrl);
            if (directFile.exists() && directFile.isFile()) {
                return new Image(directFile.toURI().toString(), 295, 170, false, true, true);
            }

            File[] backendUploadCandidates = {
                    new File("../backend/uploads/" + explicitUrl),
                    new File("backend/uploads/" + explicitUrl)
            };
            for (File f : backendUploadCandidates) {
                if (f.exists() && f.isFile()) {
                    return new Image(f.toURI().toString(), 295, 170, false, true, true);
                }
            }

            String[] candidatePaths = {
                    explicitUrl,
                    explicitUrl.startsWith("/") ? explicitUrl : "/" + explicitUrl,
                    "/Assets/" + explicitUrl,
                    "/assets/" + explicitUrl,
                    "/com/pawsitiveconnect/assets/" + explicitUrl
            };
            for (String path : candidatePaths) {
                try {
                    URL res = getClass().getResource(path);
                    if (res != null) {
                        return new Image(res.toExternalForm(), 295, 170, false, true, true);
                    }
                } catch (Exception ignored) {}
            }
        }

        String cleanName = pet.getName().trim().toLowerCase().replaceAll("\\s+", "_");
        String idStr = String.valueOf(pet.getId());
        String[] autoFilenames = {
                cleanName + ".jpg", cleanName + ".png", cleanName + ".jpeg",
                pet.getName().trim() + ".jpg", pet.getName().trim() + ".png",
                idStr + ".jpg", idStr + ".png"
        };

        for (String filename : autoFilenames) {
            File f1 = new File("../backend/uploads/" + filename);
            if (f1.exists()) return new Image(f1.toURI().toString(), 295, 170, false, true, true);
            File f2 = new File("backend/uploads/" + filename);
            if (f2.exists()) return new Image(f2.toURI().toString(), 295, 170, false, true, true);

            for (String path : new String[]{"/Assets/" + filename, "/assets/" + filename, "/com/pawsitiveconnect/assets/" + filename, "/" + filename}) {
                try {
                    URL res = getClass().getResource(path);
                    if (res != null) return new Image(res.toExternalForm(), 295, 170, false, true, true);
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private VBox createPetCard(Pet pet) {
        VBox card = new VBox(10.0);
        card.setPrefWidth(310.0);
        card.setMaxWidth(310.0);
        card.getStyleClass().add("pet-card");

        StackPane mediaArea = new StackPane();
        mediaArea.setPrefHeight(170.0);
        mediaArea.setMaxHeight(170.0);
        mediaArea.setStyle("-fx-background-color: #e9f1de; -fx-background-radius: 14px;");

        Rectangle clip = new Rectangle(295, 170);
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        mediaArea.setClip(clip);

        Image petImage = loadPetImage(pet);
        if (petImage != null && !petImage.isError()) {
            ImageView imageView = new ImageView(petImage);
            imageView.setFitWidth(295);
            imageView.setFitHeight(170);
            imageView.setPreserveRatio(false);
            mediaArea.getChildren().add(imageView);
        } else {
            Label placeholderEmoji = new Label(pet.getSpecies().equalsIgnoreCase("cat") ? "\uD83D\uDC31" : "\uD83D\uDC36");
            placeholderEmoji.setStyle("-fx-font-size: 54px;");
            mediaArea.getChildren().add(placeholderEmoji);
        }

        Label speciesPill = new Label(pet.getSpecies().equalsIgnoreCase("cat") ? "\uD83D\uDC31 Cat" : "\uD83D\uDC36 Dog");
        speciesPill.setStyle("-fx-background-color: rgba(244, 252, 234, 0.92); -fx-text-fill: #171E13; -fx-font-weight: bold; -fx-font-size: 10.5px; -fx-background-radius: 999px; -fx-padding: 3px 8px;");
        StackPane.setAlignment(speciesPill, Pos.TOP_LEFT);
        StackPane.setMargin(speciesPill, new Insets(10));

        Label statusPill = new Label();
        String stLower = pet.getStatus().toLowerCase();
        if (stLower.contains("available") && pet.isAdoptionReady() && pet.getAdoptionFee() > 0) {
            statusPill.setText(String.format("\u25CF Ready • ₹%.0f", pet.getAdoptionFee()));
            statusPill.getStyleClass().add("pill-green");
        } else if (stLower.contains("medic") || stLower.contains("recov")) {
            statusPill.setText("\u25CF In Medical Care");
            statusPill.getStyleClass().add("pill-red");
        } else if (stLower.contains("adopted")) {
            statusPill.setText("\u2713 Adopted");
            statusPill.getStyleClass().add("pill-blue");
        } else {
            statusPill.setText("\u25CF " + pet.getStatus());
            statusPill.getStyleClass().add("pill-gray");
        }
        StackPane.setAlignment(statusPill, Pos.TOP_RIGHT);
        StackPane.setMargin(statusPill, new Insets(10));

        mediaArea.getChildren().addAll(speciesPill, statusPill);

        HBox titleRow = new HBox(8.0);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(2.0);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label nameLabel = new Label(pet.getName());
        nameLabel.getStyleClass().add("text-item-title");
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800;");

        Label tagLabel = new Label("Tag ID: #PC-" + pet.getSpecies().toUpperCase().charAt(0) + String.format("%02d", pet.getId()));
        tagLabel.getStyleClass().add("text-card-muted");
        nameBox.getChildren().addAll(nameLabel, tagLabel);

        Label ageBadge = new Label(pet.getAge() + " Years");
        ageBadge.getStyleClass().add("pill-gray");

        titleRow.getChildren().addAll(nameBox, ageBadge);

        VBox specBox = new VBox(4.0);
        specBox.getStyleClass().add("pet-spec-box");

        HBox breedRow = new HBox();
        Label breedTitle = new Label("Breed");
        breedTitle.getStyleClass().add("text-card-muted");
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Label breedValue = new Label(pet.getBreed());
        breedValue.setStyle("-fx-font-weight: bold; -fx-text-fill: #171E13; -fx-font-size: 11.5px;");
        breedRow.getChildren().addAll(breedTitle, spacer1, breedValue);

        HBox statusRow = new HBox();
        Label statusTitle = new Label("Adoption Phase");
        statusTitle.getStyleClass().add("text-card-muted");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        String phaseText = (pet.isAdoptionReady() && pet.getAdoptionFee() > 0 && stLower.contains("available"))
                ? String.format("Yes (₹%.2f)", pet.getAdoptionFee())
                : "Not Listed (" + pet.getStatus() + ")";
        Label statusValue = new Label(phaseText);
        statusValue.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C694E; -fx-font-size: 11.5px;");
        statusRow.getChildren().addAll(statusTitle, spacer2, statusValue);

        specBox.getChildren().addAll(breedRow, statusRow);

        HBox actionRow = new HBox(8.0);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button viewProfileBtn = new Button("View Profile");
        viewProfileBtn.getStyleClass().add("pet-action-button");
        HBox.setHgrow(viewProfileBtn, Priority.ALWAYS);
        viewProfileBtn.setMaxWidth(Double.MAX_VALUE);
        viewProfileBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Pet Dossier • " + pet.getName());
            alert.setHeaderText(pet.getName() + " (" + pet.getSpecies() + " - " + pet.getBreed() + ")");
            String feeInfo = (pet.isAdoptionReady() && pet.getAdoptionFee() > 0)
                    ? String.format("Approved for Adoption • Fee: ₹%.2f", pet.getAdoptionFee())
                    : "Not currently listed for adoption";
            alert.setContentText(String.format("Tag ID: #PC-%s%02d\nAge: %d Years\nStatus: %s\nAdoption Phase: %s",
                    pet.getSpecies().toUpperCase().substring(0, 1), pet.getId(), pet.getAge(), pet.getStatus(), feeInfo));
            alert.showAndWait();
        });

        Button secondaryBtn = new Button();
        if (isAdmin()) {
            secondaryBtn.setText("\u2699");
            secondaryBtn.getStyleClass().add("pet-heart-button");
            secondaryBtn.setTooltip(new Tooltip("Set Adoption Phase & Fee"));
            secondaryBtn.setOnAction(e -> openAdminAdoptionConfigModal(pet));
        } else {
            secondaryBtn.setText("\u2764");
            secondaryBtn.getStyleClass().add("pet-heart-button");
            boolean canAdopt = pet.isAdoptionReady() && pet.getAdoptionFee() > 0 && stLower.contains("available");
            secondaryBtn.setDisable(!canAdopt);
            secondaryBtn.setTooltip(new Tooltip(canAdopt ? "Apply to Adopt " + pet.getName() : "Not yet cleared for adoption"));
            secondaryBtn.setOnAction(e -> {
                if (MainLayoutController.getInstance() != null) {
                    MainLayoutController.getInstance().showAdoptions(null);
                    Object ctrl = MainLayoutController.getInstance().loadView("AdoptionWorkflowView.fxml");
                    if (ctrl instanceof AdoptionWorkflowController awc) {
                        awc.openNewIntakeForPet(pet.getName());
                    }
                }
            });
        }

        actionRow.getChildren().addAll(viewProfileBtn, secondaryBtn);
        card.getChildren().addAll(mediaArea, titleRow, specBox, actionRow);
        return card;
    }

    private void openAdminAdoptionConfigModal(Pet pet) {
        Dialog<JSONObject> dialog = new Dialog<>();
        dialog.setTitle("Pet Adoption Verdict & Pricing");
        dialog.setHeaderText("Configure adoption availability and fee for " + pet.getName());

        ButtonType saveBtn = new ButtonType("Save Settings", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 40, 10, 10));

        ComboBox<String> phaseCombo = new ComboBox<>();
        phaseCombo.getItems().addAll("Yes - Ready for Adoption", "No - Hold / Not Ready");
        phaseCombo.getSelectionModel().select(pet.isAdoptionReady() ? 0 : 1);

        TextField feeField = new TextField(pet.getAdoptionFee() > 0 ? String.format("%.2f", pet.getAdoptionFee()) : "1500.00");
        feeField.setDisable(!pet.isAdoptionReady());

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Available", "In Medical Care", "Pending Adoption");
        statusCombo.setValue(pet.getStatus());

        phaseCombo.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            boolean ready = newVal.intValue() == 0;
            feeField.setDisable(!ready);
            if (ready) statusCombo.setValue("Available");
        });

        grid.add(new Label("Adoption Phase:"), 0, 0);
        grid.add(phaseCombo, 1, 0);
        grid.add(new Label("Adoption Fee (₹):"), 0, 1);
        grid.add(feeField, 1, 1);
        grid.add(new Label("Health / Care Status:"), 0, 2);
        grid.add(statusCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                boolean ready = phaseCombo.getSelectionModel().getSelectedIndex() == 0;
                double fee = 0.0;
                if (ready) {
                    try {
                        fee = Double.parseDouble(feeField.getText().trim());
                    } catch (NumberFormatException ignored) {}
                    if (fee <= 0) {
                        new Alert(Alert.AlertType.WARNING, "Please enter a valid adoption fee greater than ₹0.00.").showAndWait();
                        return null;
                    }
                }
                JSONObject payload = new JSONObject();
                payload.put("adoption_ready", ready);
                payload.put("adoption_fee", ready ? fee : JSONObject.NULL);
                payload.put("status", statusCombo.getValue());
                return payload;
            }
            return null;
        });

        Optional<JSONObject> res = dialog.showAndWait();
        res.ifPresent(jsonObj -> {
            boolean nextReady = jsonObj.optBoolean("adoption_ready", false);
            String nextStatus = jsonObj.optString("status", "");
            boolean turningUnavailable = !nextReady || "In Medical Care".equalsIgnoreCase(nextStatus);

            HttpClient client = HttpClient.newHttpClient();
            if (turningUnavailable) {
                HttpRequest checkReq = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/pets/" + pet.getId() + "/active-applications"))
                        .GET()
                        .build();

                client.sendAsync(checkReq, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(checkRes -> Platform.runLater(() -> {
                            try {
                                JSONArray activeApps = new JSONArray(checkRes.body());
                                if (activeApps.length() > 0) {
                                    Alert warn = new Alert(Alert.AlertType.CONFIRMATION);
                                    warn.setTitle("Active Adoption Application Warning");
                                    warn.setHeaderText("An adoption form is currently active for " + pet.getName() + ".");
                                    warn.setContentText("Marking this pet as unavailable or in medical care will cancel the active adoption form(s) and set their status to CANCELLED.\n\nIs this an urgency?");

                                    ButtonType yesUrgencyBtn = new ButtonType("Yes, it's an urgency", ButtonBar.ButtonData.OK_DONE);
                                    ButtonType noFineBtn = new ButtonType("No, everything is fine", ButtonBar.ButtonData.CANCEL_CLOSE);
                                    warn.getButtonTypes().setAll(yesUrgencyBtn, noFineBtn);

                                    Optional<ButtonType> choice = warn.showAndWait();
                                    if (choice.isPresent() && choice.get() == yesUrgencyBtn) {
                                        jsonObj.put("cancel_active_applications", true);
                                        submitPetAdoptionUpdate(client, pet.getId(), jsonObj);
                                    }
                                    return;
                                }
                            } catch (Exception ignored) {}
                            submitPetAdoptionUpdate(client, pet.getId(), jsonObj);
                        }));
            } else {
                submitPetAdoptionUpdate(client, pet.getId(), jsonObj);
            }
        });
    }

    private void submitPetAdoptionUpdate(HttpClient client, int petId, JSONObject jsonObj) {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/pets/" + petId + "/adoption"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonObj.toString()))
                .build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> Platform.runLater(this::loadPetsFromBackend));
    }

    private void updateLedgerBadges() {
        long ready = allPets.stream().filter(p -> p.getStatus().toLowerCase().contains("available")).count();
        long pending = allPets.stream().filter(p -> p.getStatus().toLowerCase().contains("pending")).count();
        long medical = allPets.stream().filter(p -> p.getStatus().toLowerCase().contains("medic") || p.getStatus().toLowerCase().contains("recov")).count();

        if (readyBadge != null) readyBadge.setText("\u2713 " + ready + " Ready");
        if (pendingBadge != null) pendingBadge.setText("\u23F1 " + pending + " Pending");
        if (medicalBadge != null) medicalBadge.setText("\u271A " + medical + " Critical");
    }

    @FXML public void handleFilterAll(ActionEvent event) { setActiveFilter(filterAllBtn, "all"); }
    @FXML public void handleFilterDogs(ActionEvent event) { setActiveFilter(filterDogsBtn, "dog"); }
    @FXML public void handleFilterCats(ActionEvent event) { setActiveFilter(filterCatsBtn, "cat"); }
    @FXML public void handleFilterAvailable(ActionEvent event) { setActiveFilter(filterAvailableBtn, "available"); }
    @FXML public void handleFilterMedical(ActionEvent event) { setActiveFilter(filterMedicalBtn, "medical"); }
    @FXML public void handleFilterPending(ActionEvent event) { setActiveFilter(filterPendingBtn, "pending"); }

    private void setActiveFilter(Button activeBtn, String filterKey) {
        Button[] buttons = {filterAllBtn, filterDogsBtn, filterCatsBtn, filterAvailableBtn, filterMedicalBtn, filterPendingBtn};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("filter-pill-active");
                if (!btn.getStyleClass().contains("filter-pill")) btn.getStyleClass().add("filter-pill");
            }
        }
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("filter-pill");
            activeBtn.getStyleClass().add("filter-pill-active");
        }

        this.currentFilter = filterKey;
        filterAndDisplayPets();
    }

    @FXML
    public void handleClearSearch(ActionEvent event) {
        if (searchField != null) searchField.clear();
        if (clearSearchBtn != null) clearSearchBtn.setVisible(false);
    }

    @FXML
    public void handleResetFilters(ActionEvent event) {
        if (searchField != null) searchField.clear();
        setActiveFilter(filterAllBtn, "all");
    }

    @FXML
    public void handleOpenAddPetModal(ActionEvent event) {
        if (!isAdmin()) return;

        Dialog<JSONObject> dialog = new Dialog<>();
        dialog.setTitle("New Pet Intake (Admin)");
        dialog.setHeaderText("Add a unique animal record to the Pawsitive Connect registry.");

        ButtonType saveButtonType = new ButtonType("Save Record", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 40, 10, 10));

        TextField nameInput = new TextField();
        nameInput.setPromptText("Unique name (e.g. Simba)");

        ComboBox<String> speciesCombo = new ComboBox<>();
        speciesCombo.getItems().addAll("Dog", "Cat", "Other Companion");
        speciesCombo.getSelectionModel().selectFirst();

        TextField breedInput = new TextField();
        breedInput.setPromptText("e.g. Golden Retriever");

        TextField ageInput = new TextField();
        ageInput.setPromptText("e.g. 2");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Available", "In Medical Care", "Pending Adoption");
        statusCombo.getSelectionModel().selectFirst();

        TextField imageInput = new TextField();
        imageInput.setPromptText("Paste https:// URL or filename in backend/uploads");

        Button browseImgBtn = new Button("Browse...");
        browseImgBtn.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Pet Photo");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File chosen = fc.showOpenDialog(petGridPane.getScene().getWindow());
            if (chosen != null) {
                try {
                    Path uploadsDir = Paths.get("../backend/uploads");
                    if (!Files.exists(uploadsDir)) uploadsDir = Paths.get("backend/uploads");
                    Files.createDirectories(uploadsDir);
                    String safeFilename = System.currentTimeMillis() + "_" + chosen.getName().replaceAll("\\s+", "_");
                    Files.copy(chosen.toPath(), uploadsDir.resolve(safeFilename), StandardCopyOption.REPLACE_EXISTING);
                    imageInput.setText(safeFilename);
                } catch (Exception ex) {
                    imageInput.setText(chosen.getAbsolutePath());
                }
            }
        });
        HBox imageRow = new HBox(6, imageInput, browseImgBtn);
        HBox.setHgrow(imageInput, Priority.ALWAYS);

        ComboBox<String> adoptionPhaseCombo = new ComboBox<>();
        adoptionPhaseCombo.getItems().addAll("No (Not Ready Yet)", "Yes (Ready for Adoption)");
        adoptionPhaseCombo.getSelectionModel().selectFirst();

        TextField feeInput = new TextField("1500.00");
        feeInput.setDisable(true);
        adoptionPhaseCombo.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            feeInput.setDisable(n.intValue() != 1);
        });

        grid.add(new Label("Unique Animal Name:"), 0, 0);
        grid.add(nameInput, 1, 0);
        grid.add(new Label("Species:"), 0, 1);
        grid.add(speciesCombo, 1, 1);
        grid.add(new Label("Breed:"), 0, 2);
        grid.add(breedInput, 1, 2);
        grid.add(new Label("Age (Years):"), 0, 3);
        grid.add(ageInput, 1, 3);
        grid.add(new Label("Intake Status:"), 0, 4);
        grid.add(statusCombo, 1, 4);
        grid.add(new Label("Photo URL or Local File:"), 0, 5);
        grid.add(imageRow, 1, 5);
        grid.add(new Label("Adoption Phase:"), 0, 6);
        grid.add(adoptionPhaseCombo, 1, 6);
        grid.add(new Label("Adoption Fee (₹):"), 0, 7);
        grid.add(feeInput, 1, 7);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String name = nameInput.getText().trim();
                String breed = breedInput.getText().trim();
                int age = 1;
                try {
                    age = Integer.parseInt(ageInput.getText().trim());
                } catch (NumberFormatException ignored) {}

                boolean ready = adoptionPhaseCombo.getSelectionModel().getSelectedIndex() == 1;
                double fee = 0.0;
                if (ready) {
                    try {
                        fee = Double.parseDouble(feeInput.getText().trim());
                    } catch (NumberFormatException ignored) {}
                }

                if (!name.isEmpty() && !breed.isEmpty()) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", name);
                    obj.put("species", speciesCombo.getValue());
                    obj.put("breed", breed);
                    obj.put("age", age);
                    obj.put("status", statusCombo.getValue());
                    obj.put("image_url", imageInput.getText().trim());
                    obj.put("adoption_ready", ready);
                    obj.put("adoption_fee", ready ? fee : JSONObject.NULL);
                    return obj;
                }
            }
            return null;
        });

        Optional<JSONObject> result = dialog.showAndWait();
        result.ifPresent(jsonObj -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/pets"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonObj.toString()))
                        .build();

                client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(res -> Platform.runLater(() -> {
                            if (res.statusCode() == 409) {
                                new Alert(Alert.AlertType.ERROR, "Duplicate Pet Name: A pet with this name already exists in the registry.").showAndWait();
                            } else if (res.statusCode() >= 400) {
                                new Alert(Alert.AlertType.ERROR, "Could not add pet: " + res.body()).showAndWait();
                            } else {
                                loadPetsFromBackend();
                            }
                        }));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}