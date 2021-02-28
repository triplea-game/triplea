package games.strategy.triplea.ui.statistics;

import com.google.common.collect.Table;
import games.strategy.engine.data.GameData;
import games.strategy.engine.history.Round;
import games.strategy.engine.stats.Statistics;
import games.strategy.engine.stats.StatisticsAggregator;
import games.strategy.triplea.ui.mapdata.MapData;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.triplea.swing.JTabbedPaneBuilder;

public class StatisticsDialog extends JPanel {

  private final XYChartBuilder xyChartDefaults =
      new XYChartBuilder().theme(Styler.ChartTheme.Matlab).xAxisTitle("#Rounds");

  @Getter
  @RequiredArgsConstructor
  private static class OverTimeChart {
    private final String title;
    private final String axisTitle;
    private final Table<String, Round, Double> data;
  }

  public StatisticsDialog(final GameData game, final MapData mapData) {
    final Statistics statistics = new StatisticsAggregator(game, mapData).aggregate();

    final List<OverTimeChart> overTimeCharts = new ArrayList<>();
    statistics
        .getOverTimeStatistics()
        .forEach(
            (key, value) ->
                overTimeCharts.add(new OverTimeChart(key.getName(), key.getAxisLabel(), value)));

    final JTabbedPaneBuilder tabbedPane = JTabbedPaneBuilder.builder();
    for (final OverTimeChart chartData : overTimeCharts) {
      tabbedPane.addTab(chartData.getTitle(), createChart(chartData));
    }
    this.add(tabbedPane.build());
  }

  private JPanel createChart(final OverTimeChart chartData) {
    final XYChart chart =
        xyChartDefaults.title(chartData.getTitle()).yAxisTitle(chartData.getAxisTitle()).build();
    chartData
        .getData()
        .rowMap()
        .forEach((key, value) -> chart.addSeries(key, new ArrayList<>(value.values())));
    return new XChartPanel<>(chart);
  }
}
