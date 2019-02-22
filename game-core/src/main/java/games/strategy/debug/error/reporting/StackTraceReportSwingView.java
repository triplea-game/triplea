package games.strategy.debug.error.reporting;

import java.awt.Component;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTextAreaBuilder;

class StackTraceReportSwingView implements StackTraceReportView {

  private static final String TOOL_TIP =
      "<html>This information is used by the TripleA support team to help pinpoint<br/>"
          + "and solve the error that occurred.  Please describe the sequence of actions and<br/>"
          + "game events leading up to the error and include any additional information that would<br/>"
          + "be helpful for TripleA support to solve this problem.<br/><br/>"
          + "For example: \"Game crashed during combat after rolling dice for defending subs.\"";

  private final JFrame window = JFrameBuilder.builder()
      .title("Upload Error Report to TripleA Support")
      .size(600, 450)
      .minSize(300, 350)
      .alwaysOnTop()
      .build();

  private final JTextArea userDescriptionField = JTextAreaBuilder.builder()
      .toolTip(TOOL_TIP)
      .build();

  private final JButton submitButton = JButtonBuilder.builder()
      .title("Upload")
      .biggerFont()
      .toolTip("Uploads error report to TripleA support")
      .build();

  private final JButton cancelButton = JButtonBuilder.builder()
      .title("Cancel")
      .toolTip("Closes this window")
      .build();

  StackTraceReportSwingView(@Nullable final Component parentWindow) {
    window.setLocationRelativeTo(parentWindow);
    window.getContentPane()
        .add(JPanelBuilder.builder()
            .addNorth(JLabelBuilder.builder()
                .border(5)
                .html("Please describe when and where the error happened:<br/>"
                    + "(Error message details will be included automatically in the upload)")
                .toolTip(TOOL_TIP)
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
                .borderLayout()
                .addHorizontalStrut(10)
                .addWest(submitButton)
                .addHorizontalStrut(30)
                .addEast(cancelButton)
                .build())
        .build();
  }

  @Override
  public void bindActions(final StackTraceReportModel viewModel) {
    submitButton.addActionListener(e -> viewModel.submitAction());
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
