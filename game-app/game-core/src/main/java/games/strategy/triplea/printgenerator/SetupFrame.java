package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.history.HistoryNode;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;

/** The top-level UI component for configuring the Setup Chart exporter. */
public class SetupFrame extends JPanel {
  private static final long serialVersionUID = 7308943603423170303L;
  private final JTextField outField;
  private final JFileChooser outChooser;
  private final JRadioButton originalState;

  public SetupFrame(final Frame frame, final CompletableFuture<GameData> clonedGameData) {
    super(new BorderLayout());
    outField = new JTextField(15);
    outChooser = new JFileChooser(Path.of(SystemProperties.getUserDir()).toFile());
    outChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    final JButton outDirButton = new JButton("Choose the Output Directory");
    outDirButton.addActionListener(
        e -> {
          final int returnVal = outChooser.showDialog(frame, outDirButton.getText());
          if (returnVal == JFileChooser.APPROVE_OPTION && verifySelectedOutIsEmpty(frame)) {
            final Path outDirSelected = outChooser.getSelectedFile().toPath().toAbsolutePath();
            outField.setText(outDirSelected.toString());
          }
        });

    final ButtonGroup radioButtonGroup = new ButtonGroup();
    originalState = new JRadioButton("Starting Position/State");
    radioButtonGroup.add(originalState);
    originalState.setSelected(true);
    final JRadioButton currentState = new JRadioButton("Current Position/State");
    radioButtonGroup.add(currentState);

    final JButton runButton = getRunButton(frame, clonedGameData);

    final JPanel infoPanel =
        new JPanelBuilder()
            .gridLayout(3, 1)
            .add(
                JLabelBuilder.builder()
                    .text("This utility will export the map's either current or ")
                    .build())
            .add(
                JLabelBuilder.builder()
                    .text("beginning state exactly like the board game, so you ")
                    .build())
            .add(
                JLabelBuilder.builder()
                    .text("will get Setup Charts, Unit Information, etc.")
                    .build())
            .build();

    super.add(infoPanel, BorderLayout.NORTH);

    final JPanel textButtonRadioPanel =
        new JPanelBuilder()
            .borderLayout()
            .addWest(outField)
            .addEast(outDirButton)
            .addSouth(
                new JPanelBuilder().gridLayout(1, 2).add(originalState).add(currentState).build())
            .build();

    super.add(textButtonRadioPanel, BorderLayout.CENTER);

    super.add(runButton, BorderLayout.SOUTH);
  }

  @Nonnull
  private JButton getRunButton(
      final Frame frame, final CompletableFuture<GameData> clonedGameData) {
    final JButton runButton = new JButton("Generate the Files");
    runButton.addActionListener(
        e -> {
          if (!outField.getText().isEmpty()) {
            final Path outDir = Path.of(outField.getText());
            final PrintGenerationData printData =
                PrintGenerationData.builder().outDir(outDir).data(clonedGameData.join()).build();
            generateFiles(printData, originalState.isSelected());
            JOptionPane.showMessageDialog(frame, "Done!", "Done!", JOptionPane.INFORMATION_MESSAGE);
          } else {
            JOptionPane.showMessageDialog(
                null,
                "You need to select an empty Output Directory.",
                "Select an empty Output Directory!",
                JOptionPane.ERROR_MESSAGE);
          }
        });
    return runButton;
  }

  boolean verifySelectedOutIsEmpty(final Frame frame) {
    File selectedFile = outChooser.getSelectedFile();
    if (selectedFile.isDirectory()) {
      try (DirectoryStream<Path> directory = Files.newDirectoryStream(selectedFile.toPath())) {
        if (!directory.iterator().hasNext()) {
          return true; // directory is empty
        }
      } catch (IOException ignored) {
        // following message dialog is sufficient handling
      }
    }
    JOptionPane.showMessageDialog(
        frame,
        "The selection for the empty Output Directory is invalid.",
        "Incorrect directory selected",
        JOptionPane.WARNING_MESSAGE);
    return false;
  }

  private void generateFiles(final PrintGenerationData printData, final boolean useOriginalState) {
    if (useOriginalState) {
      final HistoryNode root = (HistoryNode) printData.getData().getHistory().getRoot();
      printData.getData().getHistory().gotoNode(root);
    }
    new UnitInformation().saveToFile(printData);
    for (final GamePlayer currentPlayer : printData.getData().getPlayerList()) {
      new CountryChart(printData.getOutDir(), currentPlayer).saveToFile(printData);
    }
    new PuInfo().saveToFile(printData);
    new PlayerOrder().saveToFile(printData);
    new PuChart().saveToFiles(printData);
  }
}
