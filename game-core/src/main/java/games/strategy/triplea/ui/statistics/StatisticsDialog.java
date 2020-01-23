package games.strategy.triplea.ui.statistics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.stats.Statistics;
import games.strategy.engine.stats.StatisticsAggregator;
import javax.swing.JPanel;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.triplea.swing.JTabbedPaneBuilder;

public class StatisticsDialog extends JPanel {
  public StatisticsDialog(final GameData game) {
    final Statistics statistics = StatisticsAggregator.aggregate(game);
    final JTabbedPaneBuilder tabbedPane = JTabbedPaneBuilder.builder();
    tabbedPane.addTab("Lines", createDummyXyGraph(statistics));
    tabbedPane.addTab("Pie", createDummyPieChart(statistics));
    this.add(tabbedPane.build());
  }

  private JPanel createDummyXyGraph(final Statistics statistics) {
    final XYChart chart =
        new XYChartBuilder()
            .theme(Styler.ChartTheme.Matlab)
            .title("Sample Chart: " + statistics.toString())
            .build();
    chart.addSeries("some value1", new double[] {2.0, 0.0, 40.0});
    chart.addSeries("some value2", new double[] {3.0, 1.0, 41.0});
    chart.addSeries("some value3", new double[] {4.0, 2.0, 42.0});
    chart.addSeries("some value4", new double[] {5.0, 3.0, 43.0});
    chart.addSeries("some value5", new double[] {6.0, 4.0, 44.0});
    chart.addSeries("some value6", new double[] {7.0, 5.0, 45.0});
    chart.addSeries("some value7", new double[] {8.0, 6.0, 46.0});
    return new XChartPanel<>(chart);
  }

  private JPanel createDummyPieChart(final Statistics statistics) {
    final PieChart chart =
        new PieChartBuilder()
            .theme(Styler.ChartTheme.XChart)
            .title("Sample Chart: " + statistics.toString())
            .build();
    chart.addSeries("Value 1", 27);
    chart.addSeries("Value 2", 63);
    chart.addSeries("Value 3", 1);
    chart.addSeries("Value 4", 9);
    chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSE);
    return new XChartPanel<>(chart);
  }
}
