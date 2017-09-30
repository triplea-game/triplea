package games.strategy.triplea.ui;

import java.awt.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import com.google.common.annotations.VisibleForTesting;

import swinglib.JLabelBuilder;
import swinglib.JPanelBuilder;

/**
 * A UI-Utility class that can be used to prompt the user for a ban or mute time.
 */
public class TimespanDialog {

  /**
   * The possible time units and corresponding mappings.
   */
  @VisibleForTesting
  enum TimeUnit {
    MINUTES("Minutes", i -> Instant.now().plus(i, ChronoUnit.MINUTES)),
    HOURS("Hours", i -> Instant.now().plus(i, ChronoUnit.HOURS)),
    DAYS("Days", i -> Instant.now().plus(i, ChronoUnit.DAYS)),
    WEEKS("Weeks", i -> LocalDateTime.now(ZoneOffset.UTC).plus(i, ChronoUnit.WEEKS).toInstant(ZoneOffset.UTC)),
    MONTHS("Months", i -> LocalDateTime.now(ZoneOffset.UTC).plus(i, ChronoUnit.MONTHS).toInstant(ZoneOffset.UTC)),
    YEARS("Years", i -> LocalDateTime.now(ZoneOffset.UTC).plus(i, ChronoUnit.YEARS).toInstant(ZoneOffset.UTC)),
    FOREVER("Forever", i -> null);

    private final String name;
    private final Function<Integer, Instant> function;

    private TimeUnit(final String name, final Function<Integer, Instant> function) {
      this.name = name;
      this.function = function;
    }

    @Override
    public String toString() {
      return name;
    }

    @VisibleForTesting
    Instant getInstant(final Integer integer) {
      return function.apply(integer);
    }
  }

  /**
   * Prompts the user to enter a timespan.
   * If the operation is not cancelled, the action Consumer is run.
   * Not that the Date passed to the consumer can be null if the user chose forever.
   */
  public static void prompt(final Component parent, final String title, final String infoMessage,
      final Consumer<Date> action) {
    final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    final JComboBox<TimeUnit> comboBox = new JComboBox<>(TimeUnit.values());
    comboBox.addActionListener(e -> spinner.setEnabled(!comboBox.getSelectedItem().equals(TimeUnit.FOREVER)));
    final int returnValue = JOptionPane.showConfirmDialog(parent, JPanelBuilder.builder()
        .addNorth(JLabelBuilder.builder()
            .text(infoMessage)
            .border(new EmptyBorder(0, 0, 5, 0))
            .build())
        .addSouth(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .add(spinner)
            .add(comboBox)
            .build())
        .build(), title, JOptionPane.OK_CANCEL_OPTION);
    if (returnValue == JOptionPane.OK_OPTION) {
      final Instant instant = ((TimeUnit) comboBox.getSelectedItem()).getInstant((Integer) spinner.getValue());
      action.accept(instant == null ? null : Date.from(instant));
    }
  }
}
