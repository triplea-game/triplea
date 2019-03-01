package games.strategy.debug.error.reporting;

import java.awt.Component;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTextAreaBuilder;

class StackTraceReportSwingView implements StackTraceReportView {

  private static final String HELP_TEXT =
      "<html>Any data entered is optional, please use<br/>"
          + "this form to help TripleA support know<br/>"
          + "where and how the error occurred.<br/><br/>"
          + "Uploaded data will be used to create a publicly visible bug report";

  private final JFrame window = JFrameBuilder.builder()
      .title("Upload Error Report to TripleA Support")
      .size(600, 450)
      .minSize(300, 350)
      .alwaysOnTop()
      .build();

  private final JTextArea userDescriptionField = JTextAreaBuilder.builder()
      .toolTip(HELP_TEXT)
      .build();

  private final JButton submitButton = JButtonBuilder.builder()
      .title("Upload")
      .biggerFont()
      .toolTip("Uploads error report to TripleA support")
      .build();

  private final JButton previewButton = JButtonBuilder.builder()
      .title("Preview")
      .toolTip("Shows a preview of the error report")
      .build();

  private final JButton cancelButton = JButtonBuilder.builder()
      .title("Cancel")
      .toolTip("Closes this window")
      .build();

  StackTraceReportSwingView(@Nullable final Component parentWindow) {
    window.setLocationRelativeTo(parentWindow);
    window.getContentPane()
        .add(JPanelBuilder.builder()
            .addNorth(
                JPanelBuilder.builder()
                    .addWest(
                        JLabelBuilder.builder()
                            .border(5)
                            .html("Please describe the error and where it happened:")
                            .toolTip(HELP_TEXT)
                            .build())
                    .addEast(
                        JPanelBuilder.builder()
                            .add(
                                JButtonBuilder.builder()
                                    .title("(?)")
                                    .actionListener(() -> JOptionPane.showMessageDialog(
                                        window,
                                        HELP_TEXT))
                                    .build())
                            .build())
                    .build())
            .addCenter(userDescriptionField)
            .addSouth(buttonPanel())
            .build());
  }

  private JPanel buttonPanel() {
    return JPanelBuilder.builder()
        .border(10)
        .add(
            JPanelBuilder.builder()
                .addWest(
                    JPanelBuilder.builder()
                        .horizontalBoxLayout()
                        .add(submitButton)
                        .add(Box.createHorizontalStrut(30))
                        .add(previewButton)
                        .build())
                .addEast(
                    JPanelBuilder.builder()
                        .horizontalBoxLayout()
                        .add(Box.createHorizontalStrut(70))
                        .add(cancelButton)
                        .build())
                .build())
        .build();
  }

  @Override
  public void bindActions(final StackTraceReportModel viewModel) {
    submitButton.addActionListener(e -> viewModel.submitAction());
    previewButton.addActionListener(e -> viewModel.previewAction());
    cancelButton.addActionListener(e -> viewModel.cancelAction());
  }

  @Override
  public void show() {
    window.setVisible(true);
  }

  @Override
  public void close() {
    window.dispose();
  }

  @Override
  public String readUserDescription() {
    return userDescriptionField.getText();
  }
}
