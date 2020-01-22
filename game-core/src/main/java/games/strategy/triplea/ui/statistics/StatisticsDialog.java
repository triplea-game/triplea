package games.strategy.triplea.ui.statistics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.stats.Statistics;
import games.strategy.engine.stats.StatisticsAggregator;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StatisticsDialog extends JPanel {
  public StatisticsDialog(final GameData game) {
    final Statistics statistics = StatisticsAggregator.aggregate(game);
    // transform statistics object to interesting charts and show them
    this.add(new JLabel("Under construction: " + statistics.toString()));
  }
}
