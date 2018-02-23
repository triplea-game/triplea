package games.strategy.engine.lobby.client.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Frame;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JLabelBuilder;
import swinglib.JPanelBuilder;

/**
 * A UI-Utility class that can be used to prompt the user for a ban or mute time.
 */
public final class TimespanDialog extends JDialog {
  private static final long serialVersionUID = 1367343948352548021L;

  @VisibleForTesting
  static final int MAX_DURATION = 99_999_999;

  private final JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(1, 1, MAX_DURATION, 1));
  private final JComboBox<TimeUnit> timeUnitComboBox = new JComboBox<>(TimeUnit.values());
  private Result result = Result.CANCEL;

  private TimespanDialog(final Frame owner, final String title, final String message) {
    super(owner, title, true);

    add(JPanelBuilder.builder()
        .borderEmpty(10)
        .verticalBoxLayout()
        .add(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .add(JLabelBuilder.builder()
                .text(message)
                .build())
            .addHorizontalGlue()
            .build())
        .addVerticalStrut(10)
        .add(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .add(durationSpinner)
            .addHorizontalStrut(5)
            .add(timeUnitComboBox)
            .build())
        .addVerticalStrut(20)
        .add(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .addHorizontalGlue()
            .add(JButtonBuilder.builder()
                .title("OK")
                .actionListener(() -> close(Result.OK))
                .build())
            .addHorizontalStrut(5)
            .add(JButtonBuilder.builder()
                .title("Cancel")
                .actionListener(() -> close(Result.CANCEL))
                .build())
            .build())
        .build());
    pack();
    setLocationRelativeTo(owner);

    SwingComponents.addEscapeKeyListener(this, () -> close(Result.CANCEL));
    timeUnitComboBox.addActionListener(e -> updateComponents());
  }

  private void close(final Result result) {
    setVisible(false);
    dispose();
    this.result = result;
  }

  private void updateComponents() {
    durationSpinner.setEnabled(TimeUnit.FOREVER != timeUnitComboBox.getSelectedItem());
  }

  Optional<Timespan> open() {
    setVisible(true);

    switch (result) {
      case OK:
        return Optional.of(new Timespan(
            (Integer) durationSpinner.getValue(),
            (TimeUnit) timeUnitComboBox.getSelectedItem()));
      case CANCEL:
        return Optional.empty();
      default:
        throw new AssertionError("unknown result: " + result);
    }
  }

  private enum Result {
    OK, CANCEL
  }

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

    private final String displayName;
    private final Function<Integer, Instant> function;

    private TimeUnit(final String displayName, final Function<Integer, Instant> function) {
      this.displayName = displayName;
      this.function = function;
    }

    @Override
    public String toString() {
      return displayName;
    }

    @VisibleForTesting
    @Nullable
    Instant getInstant(final Integer integer) {
      return function.apply(integer);
    }
  }

  @VisibleForTesting
  static final class Timespan {
    final Integer duration;
    final TimeUnit timeUnit;

    Timespan(final Integer duration, final TimeUnit timeUnit) {
      this.duration = duration;
      this.timeUnit = timeUnit;
    }
  }

  /**
   * Prompts the user to enter a timespan.
   * If the operation is not cancelled, the action Consumer is run.
   * Not that the Date passed to the consumer can be null if the user chose forever.
   */
  public static void prompt(final Frame owner, final String title, final String message, final Consumer<Date> action) {
    checkNotNull(owner);
    checkNotNull(title);
    checkNotNull(message);
    checkNotNull(action);

    runAction(action, new TimespanDialog(owner, title, message).open());
  }

  @VisibleForTesting
  static void runAction(final Consumer<Date> action, final Optional<Timespan> timespan) {
    timespan.ifPresent(it -> {
      final @Nullable Instant instant = it.timeUnit.getInstant(it.duration);
      action.accept((instant == null) ? null : Date.from(instant));
    });
  }
}
