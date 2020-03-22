package games.strategy.engine.stats;

import games.strategy.engine.data.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface OverTimeStatisticType {
  String getName();

  String getAxisLabel();

  @Getter(onMethod_ = @Override)
  @RequiredArgsConstructor
  enum PredefinedStatistics implements OverTimeStatisticType {
    TUV("TUV", "TUV"),
    PRODUCTION("Production", "Production from territories"),
    UNITS("Units", "Units"),
    VC("VC", "Victory Cities");

    private final String name;
    private final String axisLabel;
  }

  @RequiredArgsConstructor
  class ResourceStatistic implements OverTimeStatisticType {

    private final Resource resource;

    @Override
    public String getName() {
      return String.format("%s Overview", resource.getName());
    }

    @Override
    public String getAxisLabel() {
      return String.format("%ss", resource.getName());
    }
  }
}
