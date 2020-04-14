package games.strategy.engine.lobby.client.ui.action;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Frame;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/** A UI-Utility class that can be used to prompt the user for a ban or mute time. */
public final class BanDurationDialog extends JDialog {
  static final int MAX_DURATION = 10_000_000;
  private static final long serialVersionUID = 1367343948352548021L;

  private static final String TITLE = "Select Timespan";
  private static final String MESSAGE =
      "Please consult other admins before banning longer than 1 day. \n"
          + "And please remember to report this ban.";

  private final JSpinner durationSpinner =
      new JSpinner(new SpinnerNumberModel(1, 1, MAX_DURATION, 1));
  private final JComboBox<BanTimeUnit> timeUnitComboBox = new JComboBox<>(BanTimeUnit.values());
  private Result result = Result.CANCEL;

  private BanDurationDialog(final Frame owner) {
    super(owner, TITLE, true);

    add(
        new JPanelBuilder()
            .border(10)
            .boxLayoutVertical()
            .add(
                new JPanelBuilder()
                    .boxLayoutHorizontal()
                    .add(JLabelBuilder.builder().text(MESSAGE).build())
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
                            .actionListener(() -> close(Result.OK))
                            .build())
                    .addHorizontalStrut(5)
                    .add(
                        new JButtonBuilder()
                            .title("Cancel")
                            .actionListener(() -> close(Result.CANCEL))
                            .build())
                    .build())
            .build());
    pack();
    setLocationRelativeTo(owner);

    SwingKeyBinding.addKeyBinding(this, KeyCode.ESCAPE, () -> close(Result.CANCEL));
    timeUnitComboBox.addActionListener(e -> updateComponents());
  }

  private void close(final Result result) {
    setVisible(false);
    dispose();
    this.result = result;
  }

  private void updateComponents() {
    durationSpinner.setEnabled(BanTimeUnit.FOREVER != timeUnitComboBox.getSelectedItem());
  }

  private Optional<BanDuration> open() {
    setVisible(true);

    switch (result) {
      case OK:
        return Optional.of(
            new BanDuration(
                (Integer) durationSpinner.getValue(),
                (BanTimeUnit) timeUnitComboBox.getSelectedItem()));
      case CANCEL:
        return Optional.empty();
      default:
        throw new AssertionError("unknown result: " + result);
    }
  }

  private enum Result {
    OK,
    CANCEL
  }

  /**
   * Prompts the user to enter a timespan. If the operation is not cancelled, the action Consumer is
   * run. Not that the Date passed to the consumer can be null if the user chose forever.
   */
  public static void prompt(final Frame owner, final Consumer<BanDuration> action) {
    checkNotNull(owner);
    checkNotNull(action);

    new BanDurationDialog(owner).open().ifPresent(action);
  }
}
