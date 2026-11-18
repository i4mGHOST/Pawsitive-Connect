package com.pawsitiveconnect;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainLayoutController {
    private static MainLayoutController instance;
    @FXML private Label userEmailLabel;
    @FXML private Label userRoleLabel;
    @FXML private StackPane contentArea;
    @FXML private Button navPets;

    public static MainLayoutController getInstance() { return instance; }

    @FXML
    public void initialize() {
        instance = this;
        userEmailLabel.setText(UserSession.getEmail());
        userRoleLabel.setText("Shelter Lead");
        showPets(null);
    }

    @FXML
    public void showPets(ActionEvent event) {
        loadView("PetCatalogueView.fxml");
    }

    public Object loadView(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFileName));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            return loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}