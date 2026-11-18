package com.pawsitiveconnect;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class DashboardController {

    @FXML private javafx.scene.control.Button logIntakeBtn;
    @FXML private javafx.scene.control.Button scheduleVetBtn;

    @FXML
    public void initialize() {
        int roleId = UserSession.getRoleId();
        boolean isAdmin = (roleId == 1 || roleId == 2);
        if (logIntakeBtn != null) {
            logIntakeBtn.setVisible(isAdmin);
            logIntakeBtn.setManaged(isAdmin);
        }
        if (scheduleVetBtn != null) {
            scheduleVetBtn.setVisible(isAdmin);
            scheduleVetBtn.setManaged(isAdmin);
        }
    }

    @FXML
    public void handleViewPets(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showPets(event);
        }
    }

    @FXML
    public void handleReviewQueue(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showAdoptions(event);
        }
    }

    @FXML
    public void handleViewMedicalLedger(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showMedical(event);
        }
    }

    @FXML
    public void handleLiveGps(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showRescue(event);
        }
    }

    @FXML
    public void handleEmergencyRescue(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            Object controller = MainLayoutController.getInstance().loadView("RescueDispatchView.fxml");
            if (controller instanceof RescueDispatchController rdc) {
                rdc.handleOpenNewEmergencyModal(event);
            }
        }
    }

    @FXML
    public void handleLogIntake(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            Object controller = MainLayoutController.getInstance().loadView("PetCatalogueView.fxml");
            if (controller instanceof PetCatalogueController pcc) {
                pcc.handleOpenAddPetModal(event);
            }
        }
    }

    @FXML
    public void handleScheduleVet(ActionEvent event) {
        if (MainLayoutController.getInstance() != null) {
            Object controller = MainLayoutController.getInstance().loadView("MedicalHistoryView.fxml");
            if (controller instanceof MedicalHistoryController mhc) {
                mhc.handleOpenAddEntryModal(event);
            }
        }
    }

    @FXML
    public void handleContactDriver(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Field Dispatch Radio");
        alert.setHeaderText("Van Unit 2 Dispatch Contact");
        alert.setContentText("Driver: Officer Rajdeep Mukherjee\nVehicle: Unit 02 (Tata Winger Animal Spec)\nDirect Radio Channel: 154.2 MHz\nCell Hotline: +91 98301 55210\nStatus: En Route to Salt Lake Central Park (ETA 6m)");
        alert.showAndWait();
    }
}