package games.strategy.engine.stats;

public interface OverTimeStatisticType {
  String getName();

  enum PredefinedStatistics implements OverTimeStatisticType {
    TUV,
    PRODUCTION,
    UNITS,
    VC;

    @Override
    public String getName() {
      return name();
    }
  }
}
