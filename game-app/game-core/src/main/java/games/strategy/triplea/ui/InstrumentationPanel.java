package games.strategy.triplea.ui;

import static java.lang.System.gc;

import games.strategy.engine.framework.IGame;
import games.strategy.triplea.odds.calculator.InstrumentationMonitor;
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator;
import java.time.Instant;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JComboBoxBuilder;

public class InstrumentationPanel extends JPanel implements InstrumentationMonitor {
  @SuppressWarnings("checkstyle:AnnotationLocation")
  @Nullable
  @Override
  public StochasticBattleCalculator.StateDistribution getStateDistribution() {
    return null;
  }

  @Override
  public void setStateDistribution(
      @Nullable final StochasticBattleCalculator.StateDistribution stateDistribution) {}

  @Override
  public void performanceReport(
      @NotNull final Instant beforeMe,
      @NotNull final Instant beforeBackup,
      @NotNull final Instant afterBackup) {
    stochasticBattleCalculationTime += beforeBackup.toEpochMilli() - beforeMe.toEpochMilli();
    concurrentBattleCalculationTime += afterBackup.toEpochMilli() - beforeBackup.toEpochMilli();
  }

  private long stochasticBattleCalculationTime = 0L;
  private long concurrentBattleCalculationTime = 0L;

  final JTextArea textArea = new JTextArea();
  final JComboBox<StochasticBattleCalculator.WhenCallBackup> whenCallConcurrentCalculator =
      JComboBoxBuilder.builder(StochasticBattleCalculator.WhenCallBackup.class)
          .items(List.of(StochasticBattleCalculator.WhenCallBackup.values()))
          .nullableSelectedItem(StochasticBattleCalculator.Companion.getWhenCallBackup())
          .enableAutoComplete()
          .itemSelectedAction(StochasticBattleCalculator.Companion::setWhenCallBackup)
          .build();

  @SuppressWarnings("unused")
  public InstrumentationPanel() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(textArea);
    textArea.setText("I am the\ntext field!!\n!");
    add(whenCallConcurrentCalculator);
    add(new JButtonBuilder("reset timer").actionListener(c -> resetTimer()).build());
    add(new JButtonBuilder("garbage collection").actionListener(c -> gcAndReport()).build());

    StochasticBattleCalculator.Companion.setInstrumentationMonitor(this);
  }

  private void gcAndReport() {
    gc();
    setText("after garbage collection");
  }

  private void resetTimer() {
    stochasticBattleCalculationTime = 0L;
    concurrentBattleCalculationTime = 0L;
    setText("timer reset");
  }

  @NotNull
  public String getText() {
    return textArea.getText();
  }

  public void setText(@NotNull final String text) {
    final var memoryStatus = InstrumentationMonitor.Companion.memoryStatus();
    final var displayText =
        stochasticBattleCalculationTime == 0L && concurrentBattleCalculationTime == 0L
            ? text + "\n\n" + memoryStatus
            : text + "\n\n" + memoryStatus + "\n\n" + timeReport();

    textArea.setText(displayText);
  }

  private String timeReport() {
    return concurrentBattleCalculationTime == 0L
        ? String.format("stochastic: %,5d", stochasticBattleCalculationTime)
        : String.format(
            "stochastic: %,5d  %d%% of concurrent: %,5d",
            stochasticBattleCalculationTime,
            (int)
                (((double) stochasticBattleCalculationTime)
                    / concurrentBattleCalculationTime
                    * 100),
            concurrentBattleCalculationTime);
  }
}
