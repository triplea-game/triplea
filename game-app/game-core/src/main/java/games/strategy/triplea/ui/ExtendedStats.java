package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.stats.IStat;
import games.strategy.engine.stats.ResourceStat;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.ui.mapdata.MapData;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nls;

/**
 * A UI component that displays an extended set of game statistics beyond those displayed by {@link
 * StatPanel}.
 *
 * <p>The additional statistics include:
 *
 * <ul>
 *   <li>All resources other than PUs.
 *   <li>Number of technologies.
 *   <li>Details about each technology.
 *   <li>Available unit types.
 * </ul>
 */
public class ExtendedStats extends StatPanel {
  @Serial private static final long serialVersionUID = 2502397606419491543L;
  private transient IStat[] statsExtended = new IStat[] {};

  public ExtendedStats(final GameData data, final UiContext uiContext) {
    super(data, uiContext);
  }

  @Override
  protected void initLayout() {
    // no layout necessary
  }

  public IStat[] getStatsExtended(final GameState data) {
    if (statsExtended.length == 0) {
      fillExtendedStats(data);
    }
    return statsExtended;
  }

  private void fillExtendedStats(final GameState data) {
    // add other resources, other than PUs and tech tokens
    final List<IStat> newStatsExtended = new ArrayList<>();
    data.getResourceList().getResources().stream()
        .filter(
            r -> !r.getName().equals(Constants.PUS) && !r.getName().equals(Constants.TECH_TOKENS))
        .forEach(r -> newStatsExtended.add((new GenericResourceStat()).init(r.getName())));
    // add tech related stuff
    if (Properties.getTechDevelopment(data.getProperties())) {
      // add tech tokens
      if (data.getResourceList().getResourceOptional(Constants.TECH_TOKENS).isPresent()) {
        newStatsExtended.add(new TechTokenStat());
      }
      // add number of techs
      newStatsExtended.add(new TechCountStat());

      // add individual techs
      TechAdvance.getTechAdvances(gameData.getTechnologyFrontier())
          .forEach(ta -> newStatsExtended.add((new GenericTechNameStat()).init(ta)));
    }
    // now add actual number of each unit type (holy gumdrops batman, this is going to be long!)
    data.getUnitTypeList()
        .forEach(ut -> newStatsExtended.add((new GenericUnitNameStat()).init(ut)));

    if (!newStatsExtended.isEmpty()) {
      this.statsExtended = newStatsExtended.toArray(new IStat[0]);
    }
  }

  static class TechCountStat implements IStat {
    @Override
    public @Nls String getName() {
      return "Techs";
    }

    @Override
    public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
      int count = 0;
      final TechAttachment ta = player.getTechAttachment();
      final List<Supplier<Boolean>> list =
          List.of(
              ta::getHeavyBomber,
              ta::getLongRangeAir,
              ta::getJetPower,
              ta::getRocket,
              ta::getIndustrialTechnology,
              ta::getSuperSub,
              ta::getDestroyerBombard,
              ta::getImprovedArtillerySupport,
              ta::getParatroopers,
              ta::getIncreasedFactoryProduction,
              ta::getWarBonds,
              ta::getMechanizedInfantry,
              ta::getAaRadar,
              ta::getShipyards);
      count += (int) list.stream().filter(Supplier::get).count();
      count += (int) ta.getGenericTech().values().stream().filter(Boolean.TRUE::equals).count();
      return count;
    }
  }

  interface GenericStat<T> extends IStat {
    GenericStat<T> init(final T stat);
  }

  static class GenericResourceStat implements GenericStat<String> {
    private String name = null;

    public GenericStat<String> init(final String name) {
      this.name = name;
      return this;
    }

    @Override
    public @Nls String getName() {
      return String.format("Resource: %s", name);
    }

    @Override
    public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
      return player.getResources().getQuantity(name);
    }
  }

  static class GenericTechNameStat implements GenericStat<TechAdvance> {
    private TechAdvance ta = null;

    public GenericStat<TechAdvance> init(final TechAdvance ta) {
      this.ta = ta;
      return this;
    }

    @Override
    public @Nls String getName() {
      return String.format("TechAdvance: %s", ta.getName());
    }

    @Override
    public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
      if (ta.hasTech(player.getTechAttachment())) {
        return 1;
      }
      return 0;
    }
  }

  static class GenericUnitNameStat implements GenericStat<UnitType> {
    private UnitType ut = null;

    public GenericStat<UnitType> init(final UnitType ut) {
      this.ut = ut;
      return this;
    }

    @Override
    public @Nls String getName() {
      return String.format("UnitType: %s", ut.getName());
    }

    @Override
    public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
      final Predicate<Unit> ownedBy = Matches.unitIsOwnedBy(player).and(Matches.unitIsOfType(ut));
      return data.getMap().getTerritories().stream()
          .mapToInt(place -> place.getUnitCollection().countMatches(ownedBy))
          .sum();
    }
  }

  class TechTokenStat extends ResourceStat {
    TechTokenStat() {
      super(gameData.getResourceList().getResourceOptional(Constants.TECH_TOKENS).orElse(null));
    }
  }

  public IStat[] getStats() {
    return stats;
  }
}
