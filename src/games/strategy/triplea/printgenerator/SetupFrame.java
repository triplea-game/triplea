package games.strategy.triplea.printgenerator;

import java.io.File;

import games.strategy.engine.data.GameData;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

public class SetupFrame extends BorderPane {

  public SetupFrame(final GameData data) {
    super();
    final Label info1 = new Label();
    final Label info2 = new Label();
    final Label info3 = new Label();
    final Button outDirButton = new Button();
    final Button runButton = new Button();
    TextField outField = new TextField();
    DirectoryChooser outChooser = new DirectoryChooser();
    final RadioButton currentState = new RadioButton();
    final RadioButton originalState = new RadioButton();
    final ToggleGroup radioButtonGroup = new ToggleGroup();
    info1.setText("This utility will export the map's either current or ");
    info2.setText("beginning state exactly like the boardgame, so you ");
    info3.setText("will get Setup Charts, Unit Information, etc.");
    currentState.setText("Current Position/State");
    originalState.setText("Starting Position/State");
    currentState.setToggleGroup(radioButtonGroup);
    originalState.setToggleGroup(radioButtonGroup);
    originalState.setSelected(true);
    outDirButton.setText("Choose the Output Directory");
    outDirButton.setOnAction(e -> {
      final File outDir = outChooser.showDialog(null);
      if (outDir != null) {
        outField.setText(outDir.getAbsolutePath());
      }
    });
    runButton.setText("Generate the Files");
    runButton.setOnAction(e -> {
      if (!outField.getText().equals("")) {
        File outDir = new File(outField.getText());
        final PrintGenerationData printData = new PrintGenerationData();
        printData.setOutDir(outDir);
        printData.setData(data);
        new InitialSetup().run(printData, originalState.isSelected());
        new Alert(AlertType.INFORMATION, "Done!").show();
      } else {
        new Alert(AlertType.ERROR, "You need to select an Output Directory.").show();
      }
    });
    final VBox infoPanel = new VBox();
    final BorderPane textButtonRadioPanel = new BorderPane();
    infoPanel.getChildren().add(info1);
    infoPanel.getChildren().add(info2);
    infoPanel.getChildren().add(info3);
    setTop(infoPanel);
    textButtonRadioPanel.setLeft(outField);
    textButtonRadioPanel.setRight(outDirButton);
    final HBox panel = new HBox();
    panel.getChildren().add(originalState);
    panel.getChildren().add(currentState);
    textButtonRadioPanel.setBottom(panel);
    setCenter(textButtonRadioPanel);
    setBottom(runButton);
  }
}
