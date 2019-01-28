package games.strategy.debug.console.window;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.java.Log;
import swinglib.JComboBoxBuilder;
import swinglib.JFrameBuilder;
import swinglib.JTextAreaBuilder;

@Log
class ConsoleWindow implements ConsoleView {

  private final JTextArea textArea = JTextAreaBuilder.builder()
      .rows(20)
      .columns(50)
      .build();

  @Getter(AccessLevel.PACKAGE)
  private final JFrame frame = JFrameBuilder.builder()
      .title("TripleA Console")
      .layout(new BorderLayout())
      .build();


  ConsoleWindow() {
    final ConsoleModel model = new ConsoleModel(this);

    frame.getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
    frame.getContentPane().add(createButtonsToolBar(model), BorderLayout.SOUTH);

    SwingUtilities.invokeLater(frame::pack);
  }

  private static JToolBar createButtonsToolBar(final ConsoleModel model) {
    final JToolBar buttonsToolBar = new JToolBar(SwingConstants.HORIZONTAL);
    buttonsToolBar.setFloatable(false);
    buttonsToolBar.setLayout(new FlowLayout());
    buttonsToolBar.add(SwingAction.of("Enumerate Threads", model::enumerateThreadsAction));
    buttonsToolBar.add(SwingAction.of("Memory", model::memoryAction));
    buttonsToolBar.add(SwingAction.of("Properties", model::propertiesAction));
    buttonsToolBar.add(SwingAction.of("Copy to clipboard", model::copyToClipboardAction));
    buttonsToolBar.add(SwingAction.of("Clear", model::clearAction));

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
  public void setVisible(final boolean visible) {
    SwingUtilities.invokeLater(() -> frame.setVisible(visible));
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
