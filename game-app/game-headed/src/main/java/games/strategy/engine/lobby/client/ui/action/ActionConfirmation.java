package games.strategy.engine.lobby.client.ui.action;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class ActionConfirmation {
  private final JFrame parent;

  boolean confirm(final String question) {
    final int selectionOption =
        JOptionPane.showConfirmDialog(
            JOptionPane.getFrameForComponent(parent),
            question,
            "Question",
            JOptionPane.OK_CANCEL_OPTION);
    return selectionOption == JOptionPane.OK_OPTION;
  }
}
