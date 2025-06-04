package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.stats.IStat;
import games.strategy.engine.stats.ResourceStat;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.ui.mapdata.MapData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
  private static final long serialVersionUID = 2502397606419491543L;
  private IStat[] statsExtended = new IStat[] {};

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
    for (final Resource r : data.getResourceList().getResources()) {
      if (r.getName().equals(Constants.PUS) || r.getName().equals(Constants.TECH_TOKENS)) {
        continue;
      }

      final GenericResourceStat resourceStat = new GenericResourceStat();
      resourceStat.init(r.getName());
      final List<IStat> statsExtended = new ArrayList<>(List.of(this.statsExtended));
      statsExtended.add(resourceStat);
      this.statsExtended = statsExtended.toArray(new IStat[0]);
    }
    // add tech related stuff
    if (Properties.getTechDevelopment(data.getProperties())) {
      // add tech tokens
      if (data.getResourceList().getResourceOptional(Constants.TECH_TOKENS).isPresent()) {
        final List<IStat> statsExtended = new ArrayList<>(List.of(this.statsExtended));
        statsExtended.add(new TechTokenStat());
        this.statsExtended = statsExtended.toArray(new IStat[0]);
      }
      // add number of techs
      final List<IStat> techStatsExtended = new ArrayList<>(List.of(statsExtended));
      techStatsExtended.add(new TechCountStat());
      statsExtended = techStatsExtended.toArray(new IStat[0]);

      // add individual techs
      for (final TechAdvance ta : TechAdvance.getTechAdvances(gameData.getTechnologyFrontier())) {
        final GenericTechNameStat techNameStat = new GenericTechNameStat();
        techNameStat.init(ta);
        final List<IStat> statsExtended = new ArrayList<>(List.of(this.statsExtended));
        statsExtended.add(techNameStat);
        this.statsExtended = statsExtended.toArray(new IStat[0]);
      }
    }
    // now add actual number of each unit type (holy gumdrops batman, this is going to be long!)
    final UnitTypeList allUnitTypes = data.getUnitTypeList();
    for (final UnitType ut : allUnitTypes) {
      final GenericUnitNameStat unitNameStat = new GenericUnitNameStat();
      unitNameStat.init(ut);
      final List<IStat> statsExtended = new ArrayList<>(List.of(this.statsExtended));
      statsExtended.add(unitNameStat);
      this.statsExtended = statsExtended.toArray(new IStat[0]);
    }
  }

  static class TechCountStat implements IStat {
    @Override
    public String getName() {
      return "Techs";
    }

    @Override
    public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
      int count = 0;
      final TechAttachment ta = player.getTechAttachment();
      if (ta.getHeavyBomber()) {
        count++;
      }
      if (ta.getLongRangeAir()) {
        count++;
      }
      if (ta.getJetPower()) {
        count++;
      }
      if (ta.getRocket()) {
        count++;
      }
      if (ta.getIndustrialTechnology()) {
        count++;
      }
      if (ta.getSuperSub()) {
        count++;
      }
      if (ta.getDestroyerBombard()) {
        count++;
      }
      if (ta.getImprovedArtillerySupport()) {
        count++;
      }
      if (ta.getParatroopers()) {
        count++;
      }
      if (ta.getIncreasedFactoryProduction()) {
        count++;
      }
      if (ta.getWarBonds()) {
        count++;
      }
      if (ta.getMechanizedInfantry()) {
        count++;
      }
      if (ta.getAaRadar()) {
        count++;
      }
      if (ta.getShipyards()) {
        count++;
      }
      for (final boolean value : ta.getGenericTech().values()) {
        if (value) {
          count++;
        }
      }
      return count;
    }
  }

  static class GenericResourceStat implements IStat {
    private String name = null;

    public void init(final String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return "Resource: " + name;
    }

    @Override
    public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
      return player.getResources().getQuantity(name);
    }
  }

  static class GenericTechNameStat implements IStat {
    private TechAdvance ta = null;

    public void init(final TechAdvance ta) {
      this.ta = ta;
    }

    @Override
    public String getName() {
      return "TechAdvance: " + ta.getName();
    }

    @Override
    public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
      if (ta.hasTech(player.getTechAttachment())) {
        return 1;
      }
      return 0;
    }
  }

  static class GenericUnitNameStat implements IStat {
    private UnitType ut = null;

    public void init(final UnitType ut) {
      this.ut = ut;
    }

    @Override
    public String getName() {
      return "UnitType: " + ut.getName();
    }

    @Override
    public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
      int matchCount = 0;
      final Predicate<Unit> ownedBy = Matches.unitIsOwnedBy(player).and(Matches.unitIsOfType(ut));
      for (final Territory place : data.getMap().getTerritories()) {
        matchCount += place.getUnitCollection().countMatches(ownedBy);
      }
      return matchCount;
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
