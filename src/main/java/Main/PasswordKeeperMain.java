package Main;

import Controller.MainWindowController;
import Controller.SignInController;
import Util.WindowCreationManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.lang.module.ModuleDescriptor;

public class PasswordKeeperMain extends Application {

    private final static double mainSceneWidth = Screen.getPrimary().getBounds().getWidth() * 0.6;
    private final static double mainSceneHeight = Screen.getPrimary().getBounds().getHeight() * 0.8;
    private final static WindowCreationManager windowCreationManager = new WindowCreationManager();
    private static Stage mainStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/window_structure/MainWindow.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("Secure Password Keeper - Profile view");
        primaryStage.setScene(new Scene(root, mainSceneWidth, mainSceneHeight));
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.getScene().setFill(Color.TRANSPARENT);
        primaryStage.getScene().getStylesheets().add(this.getClass().getResource("/css/mainWindowStyle.css").toExternalForm());
        mainStage = primaryStage;
        windowCreationManager.showSignInWindow(primaryStage.getScene().getWindow());
        MainWindowController mainWindowController = fxmlLoader.getController();
        continueProgramOrTerminate(mainWindowController);
    }

    public static double getMainSceneWidth() {
        return mainSceneWidth;
    }

    public static double getMainSceneHeight() {
        return mainSceneHeight;
    }

    public static WindowCreationManager getWindowCreationManager() {
        return windowCreationManager;
    }

    public static void continueProgramOrTerminate(MainWindowController mainWindowController) {
        if (SignInController.isCloseApp()) {
            mainStage.close();
            Platform.exit();
        } else {
            mainWindowController.initializeAfterSigningIn();
            mainStage.show();
        }
    }

    public static void main(String[] args) {
        PasswordKeeperMain.launch(args);
    }
}
