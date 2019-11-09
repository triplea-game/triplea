package org.triplea.debug.console.window;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import org.triplea.swing.JComboBoxBuilder;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;

/**
 * This is a debug window that can be displayed to show log events and has controls for dumping
 * system data, eg: current property values, JVM memory.
 */
class ConsoleWindow implements ConsoleView {

  private final JTextArea textArea = JTextAreaBuilder.builder().rows(20).columns(50).build();

  @Getter(AccessLevel.PACKAGE)
  private final JFrame frame =
      JFrameBuilder.builder().title("TripleA Console").layout(new BorderLayout()).build();

  ConsoleWindow(final ConsoleModel model) {
    frame.getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
    frame.getContentPane().add(createButtonsToolBar(model), BorderLayout.SOUTH);

    SwingUtilities.invokeLater(frame::pack);
    ConsoleModel.setVisibility(this);
  }

  private JToolBar createButtonsToolBar(final ConsoleModel model) {
    final JToolBar buttonsToolBar = new JToolBar(SwingConstants.HORIZONTAL);
    buttonsToolBar.setFloatable(false);
    buttonsToolBar.setLayout(new FlowLayout());
    buttonsToolBar.add(
        SwingAction.of("Enumerate Threads", () -> ConsoleModel.enumerateThreadsAction(this)));
    buttonsToolBar.add(SwingAction.of("Memory", () -> ConsoleModel.memoryAction(this)));
    buttonsToolBar.add(SwingAction.of("Properties", () -> ConsoleModel.propertiesAction(this)));
    buttonsToolBar.add(
        SwingAction.of("Copy to clipboard", () -> model.copyToClipboardAction(this)));
    buttonsToolBar.add(SwingAction.of("Clear", () -> ConsoleModel.clearAction(this)));

    buttonsToolBar.add(
        JComboBoxBuilder.builder(String.class)
            .selectedItem(ConsoleModel.getCurrentLogLevel())
            .items(ConsoleModel.getLogLevelOptions())
            .itemSelectedAction(ConsoleModel::setLogLevel)
            .toolTipText("Increase or decrease log messages printed to console")
            .build());
    return buttonsToolBar;
  }

  @Override
  public String readText() {
    return textArea.getText();
  }

  @Override
  public void setText(final String text) {
    SwingUtilities.invokeLater(() -> textArea.setText(text));
  }

  @Override
  public void setVisible() {
    SwingUtilities.invokeLater(() -> frame.setVisible(true));
  }

  @Override
  public void append(final String s) {
    SwingUtilities.invokeLater(() -> textArea.append(s));
  }

  @Override
  public void addWindowClosedListener(final Runnable closeListener) {
    SwingComponents.addWindowClosedListener(frame, closeListener);
  }
}
