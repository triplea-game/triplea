package games.strategy.engine.lobby.client.ui.action;

import java.awt.Frame;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/** A UI-Utility class that can be used to prompt the user for a timespan. */
@Builder
public final class ActionDurationDialog {
  private static final long serialVersionUID = 1367343948352548021L;
  private static final String TITLE = "Select Timespan";

  @Builder.Default private int maxDuration = 10_000_000;
  @Builder.Default private List<ActionTimeUnit> timeUnits = List.of(ActionTimeUnit.values());
  @Nonnull private final ActionName actionName;
  @Nonnull private final Frame parent;

  @AllArgsConstructor
  public enum ActionName {
    BAN("ban"),
    MUTE("mute");
    private final String label;
  }

  private enum Result {
    OK,
    CANCEL
  }

  /**
   * Prompts the user to enter a timespan. If the operation is not cancelled, the action Consumer is
   * run. Not that the Date passed to the consumer can be null if the user chose forever.
   */
  public Optional<ActionDuration> prompt() {
    final JDialog dialog = new JDialog(parent, TITLE, true);

    final AtomicReference<Result> result = new AtomicReference<>(Result.CANCEL);

    final JComboBox<ActionTimeUnit> timeUnitComboBox =
        new JComboBox<>(timeUnits.toArray(ActionTimeUnit[]::new));
    final JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(15, 1, maxDuration, 1));

    dialog
        .getContentPane()
        .add(
            new JPanelBuilder()
                .border(10)
                .boxLayoutVertical()
                .add(
                    new JPanelBuilder()
                        .boxLayoutHorizontal()
                        .add(
                            JLabelBuilder.builder()
                                .text("Select duration to " + actionName.label)
                                .build())
                        .addHorizontalGlue()
                        .build())
                .addVerticalStrut(10)
                .add(
                    new JPanelBuilder()
                        .boxLayoutHorizontal()
                        .add(durationSpinner)
                        .addHorizontalStrut(5)
                        .add(timeUnitComboBox)
                        .build())
                .addVerticalStrut(20)
                .add(
                    new JPanelBuilder()
                        .boxLayoutHorizontal()
                        .addHorizontalGlue()
                        .add(
                            new JButtonBuilder()
                                .title("OK")
                                .actionListener(
                                    () -> {
                                      result.set(Result.OK);
                                      close(dialog);
                                    })
                                .build())
                        .addHorizontalStrut(5)
                        .add(
                            new JButtonBuilder()
                                .title("Cancel")
                                .actionListener(
                                    () -> {
                                      result.set(Result.CANCEL);
                                      close(dialog);
                                    })
                                .build())
                        .build())
                .build());
    dialog.pack();
    dialog.setLocationRelativeTo(parent);

    SwingKeyBinding.addKeyBinding(
        dialog.getRootPane(),
        KeyCode.ESCAPE,
        () -> {
          result.set(Result.CANCEL);
          close(dialog);
        });

    dialog.setVisible(true);

    return result.get() == Result.OK
        ? Optional.of(
            new ActionDuration(
                (Integer) durationSpinner.getValue(),
                (ActionTimeUnit) timeUnitComboBox.getSelectedItem()))
        : Optional.empty();
  }

  private void close(final JDialog dialog) {
    dialog.setVisible(false);
    dialog.dispose();
  }
}
