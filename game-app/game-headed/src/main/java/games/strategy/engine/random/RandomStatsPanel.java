package games.strategy.engine.random;

import games.strategy.engine.data.GamePlayer;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.triplea.java.collections.IntegerMap;

/** Builds the Swing display of a {@link RandomStatsDetails}. */
public final class RandomStatsPanel {
  private RandomStatsPanel() {}

  /** Returns a JPanel displaying information about all Statistics. */
  public static JPanel newPanel(final RandomStatsDetails stats) {
    final Map<GamePlayer, IntegerMap<Integer>> data = stats.getData();
    final Map<GamePlayer, DiceStatistic> playerStats = stats.getPlayerStats();
    final Insets insets = new Insets(2, 2, 2, 2);
    final JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    panel.setBorder(BorderFactory.createEmptyBorder());
    panel.add(
        getStatsDisplay(stats.getTotalMap(), stats.getTotalStats(), "Total"),
        new GridBagConstraints(
            0,
            0,
            1,
            1,
            1,
            1,
            GridBagConstraints.FIRST_LINE_START,
            GridBagConstraints.NONE,
            insets,
            0,
            0));
    if (data.containsKey(null)) {
      panel.add(
          getStatsDisplay(data.get(null), playerStats.get(null), "Null / Other"),
          new GridBagConstraints(
              1,
              0,
              1,
              1,
              1,
              1,
              GridBagConstraints.FIRST_LINE_START,
              GridBagConstraints.NONE,
              insets,
              0,
              0));
    }
    final int rows = Math.max(2, data.size() / 6);
    int x = 0;
    for (final Entry<GamePlayer, IntegerMap<Integer>> entry : data.entrySet()) {
      if (entry.getKey() == null) {
        continue;
      }
      panel.add(
          getStatsDisplay(
              entry.getValue(),
              playerStats.get(entry.getKey()),
              entry.getKey().getName() + " Combat"),
          new GridBagConstraints(
              (x / rows),
              1 + (x % rows),
              1,
              1,
              1,
              1,
              GridBagConstraints.FIRST_LINE_START,
              GridBagConstraints.NONE,
              insets,
              0,
              0));
      x++;
    }
    return panel;
  }

  private static JPanel getStatsDisplay(
      final IntegerMap<Integer> diceRolls, final DiceStatistic diceStats, final String title) {
    final JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEtchedBorder());
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(new JLabel("<html><b>" + title + "</b></html>"));
    for (final int key : new TreeSet<>(diceRolls.keySet())) {
      final int value = diceRolls.getInt(key);
      final JLabel label = new JLabel(key + " was rolled " + value + " times");
      panel.add(label);
    }
    panel.add(new JLabel("  "));
    final DecimalFormat format = new DecimalFormat("#0.000");
    panel.add(new JLabel("Average roll : " + format.format(diceStats.getAverage())));
    panel.add(new JLabel("Median : " + format.format(diceStats.getMedian())));
    panel.add(new JLabel("Variance : " + format.format(diceStats.getVariance())));
    panel.add(new JLabel("Standard Deviation : " + format.format(diceStats.getStdDeviation())));
    panel.add(new JLabel("Total rolls : " + diceStats.getTotal()));
    return panel;
  }
}
