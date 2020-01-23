package games.strategy.triplea.ui.statistics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.stats.Statistics;
import games.strategy.engine.stats.StatisticsAggregator;
import javax.swing.*;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

public class StatisticsDialog extends JPanel {
  public StatisticsDialog(final GameData game) {
    final Statistics statistics = StatisticsAggregator.aggregate(game);
    // transform statistics object to interesting charts and show them
    this.add(createDummyGraph(statistics));
  }

  private JPanel createDummyGraph(Statistics statistics) {
    XYChart sample_chart =
        new XYChartBuilder()
            .theme(Styler.ChartTheme.Matlab)
            .title("Sample Chart: " + statistics.toString())
            .build();
    sample_chart.addSeries("some value1", new double[] {2.0, 0.0, 40.0});
    sample_chart.addSeries("some value2", new double[] {3.0, 1.0, 41.0});
    sample_chart.addSeries("some value3", new double[] {4.0, 2.0, 42.0});
    sample_chart.addSeries("some value4", new double[] {5.0, 3.0, 43.0});
    sample_chart.addSeries("some value5", new double[] {6.0, 4.0, 44.0});
    sample_chart.addSeries("some value6", new double[] {7.0, 5.0, 45.0});
    sample_chart.addSeries("some value7", new double[] {8.0, 6.0, 46.0});
    return new XChartPanel<>(sample_chart);
  }
}
