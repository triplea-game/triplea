package games.strategy.triplea.delegate;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.util.Tuple;

public abstract class TechAdvance extends NamedAttachable {
  private static final long serialVersionUID = -1076712297024403156L;
  private static final Class<?>[] preDefinedTechConstructorParameter = new Class<?>[] {GameData.class};
  public static final String TECH_NAME_SUPER_SUBS = "Super subs";
  public static final String TECH_PROPERTY_SUPER_SUBS = "superSub";
  public static final String TECH_NAME_JET_POWER = "Jet Power";
  public static final String TECH_PROPERTY_JET_POWER = "jetPower";
  public static final String TECH_NAME_IMPROVED_SHIPYARDS = "Shipyards";
  public static final String TECH_PROPERTY_IMPROVED_SHIPYARDS = "shipyards";
  public static final String TECH_NAME_AA_RADAR = "AA Radar";
  public static final String TECH_PROPERTY_AA_RADAR = "aARadar";
  public static final String TECH_NAME_LONG_RANGE_AIRCRAFT = "Long Range Aircraft";
  public static final String TECH_PROPERTY_LONG_RANGE_AIRCRAFT = "longRangeAir";
  public static final String TECH_NAME_HEAVY_BOMBER = "Heavy Bomber";
  public static final String TECH_PROPERTY_HEAVY_BOMBER = "heavyBomber";
  public static final String TECH_NAME_IMPROVED_ARTILLERY_SUPPORT = "Improved Artillery Support";
  public static final String TECH_PROPERTY_IMPROVED_ARTILLERY_SUPPORT = "improvedArtillerySupport";
  public static final String TECH_NAME_ROCKETS = "Rockets Advance";
  public static final String TECH_PROPERTY_ROCKETS = "rocket";
  public static final String TECH_NAME_PARATROOPERS = "Paratroopers";
  public static final String TECH_PROPERTY_PARATROOPERS = "paratroopers";
  public static final String TECH_NAME_INCREASED_FACTORY_PRODUCTION = "Increased Factory Production";
  public static final String TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION = "increasedFactoryProduction";
  public static final String TECH_NAME_WAR_BONDS = "War Bonds";
  public static final String TECH_PROPERTY_WAR_BONDS = "warBonds";
  public static final String TECH_NAME_MECHANIZED_INFANTRY = "Mechanized Infantry";
  public static final String TECH_PROPERTY_MECHANIZED_INFANTRY = "mechanizedInfantry";
  public static final String TECH_NAME_INDUSTRIAL_TECHNOLOGY = "Industrial Technology";
  public static final String TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY = "industrialTechnology";
  public static final String TECH_NAME_DESTROYER_BOMBARD = "Destroyer Bombard";
  public static final String TECH_PROPERTY_DESTROYER_BOMBARD = "destroyerBombard";
  public static final List<String> ALL_PREDEFINED_TECHNOLOGY_NAMES = Collections.unmodifiableList(
      Arrays.asList(TECH_NAME_SUPER_SUBS, TECH_NAME_JET_POWER, TECH_NAME_IMPROVED_SHIPYARDS, TECH_NAME_AA_RADAR,
          TECH_NAME_LONG_RANGE_AIRCRAFT, TECH_NAME_HEAVY_BOMBER, TECH_NAME_IMPROVED_ARTILLERY_SUPPORT,
          TECH_NAME_ROCKETS, TECH_NAME_PARATROOPERS, TECH_NAME_INCREASED_FACTORY_PRODUCTION, TECH_NAME_WAR_BONDS,
          TECH_NAME_MECHANIZED_INFANTRY, TECH_NAME_INDUSTRIAL_TECHNOLOGY, TECH_NAME_DESTROYER_BOMBARD));
  private static final Map<String, Class<? extends TechAdvance>> ALL_PREDEFINED_TECHNOLOGIES =
      createPreDefinedTechnologyMap();

  private static Map<String, Class<? extends TechAdvance>> createPreDefinedTechnologyMap() {
    final HashMap<String, Class<? extends TechAdvance>> preDefinedTechMap =
        new HashMap<>();
    preDefinedTechMap.put(TECH_PROPERTY_SUPER_SUBS, SuperSubsAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_JET_POWER, JetPowerAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_IMPROVED_SHIPYARDS, ImprovedShipyardsAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_AA_RADAR, AARadarAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_LONG_RANGE_AIRCRAFT, LongRangeAircraftAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_HEAVY_BOMBER, HeavyBomberAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_IMPROVED_ARTILLERY_SUPPORT, ImprovedArtillerySupportAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_ROCKETS, RocketsAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_PARATROOPERS, ParatroopersAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION, IncreasedFactoryProductionAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_WAR_BONDS, WarBondsAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_MECHANIZED_INFANTRY, MechanizedInfantryAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY, IndustrialTechnologyAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_DESTROYER_BOMBARD, DestroyerBombardTechAdvance.class);
    return Collections.unmodifiableMap(preDefinedTechMap);
  }

  public TechAdvance(final String name, final GameData data) {
    super(name, data);
  }

  public abstract String getProperty();

  public abstract void perform(PlayerID id, IDelegateBridge bridge);

  public abstract boolean hasTech(TechAttachment ta);

  private static void createWW2V1Advances(final TechnologyFrontier tf) {
    tf.addAdvance(new JetPowerAdvance(tf.getData()));
    tf.addAdvance(new SuperSubsAdvance(tf.getData()));
    tf.addAdvance(new LongRangeAircraftAdvance(tf.getData()));
    tf.addAdvance(new RocketsAdvance(tf.getData()));
    tf.addAdvance(new IndustrialTechnologyAdvance(tf.getData()));
    tf.addAdvance(new HeavyBomberAdvance(tf.getData()));
  }

  private static void createWW2V2Advances(final TechnologyFrontier tf) {
    tf.addAdvance(new JetPowerAdvance(tf.getData()));
    tf.addAdvance(new SuperSubsAdvance(tf.getData()));
    tf.addAdvance(new LongRangeAircraftAdvance(tf.getData()));
    tf.addAdvance(new RocketsAdvance(tf.getData()));
    tf.addAdvance(new DestroyerBombardTechAdvance(tf.getData()));
    tf.addAdvance(new HeavyBomberAdvance(tf.getData()));
    // tf.addAdvance(new IndustrialTechnologyAdvance(tf.getData()));
  }

  private static void createWW2V3Advances(final TechnologyFrontier tf) {
    tf.addAdvance(new SuperSubsAdvance(tf.getData()));
    tf.addAdvance(new JetPowerAdvance(tf.getData()));
    tf.addAdvance(new ImprovedShipyardsAdvance(tf.getData()));
    tf.addAdvance(new AARadarAdvance(tf.getData()));
    tf.addAdvance(new LongRangeAircraftAdvance(tf.getData()));
    tf.addAdvance(new HeavyBomberAdvance(tf.getData()));
    tf.addAdvance(new ImprovedArtillerySupportAdvance(tf.getData()));
    tf.addAdvance(new RocketsAdvance(tf.getData()));
    tf.addAdvance(new ParatroopersAdvance(tf.getData()));
    tf.addAdvance(new IncreasedFactoryProductionAdvance(tf.getData()));
    tf.addAdvance(new WarBondsAdvance(tf.getData()));
    tf.addAdvance(new MechanizedInfantryAdvance(tf.getData()));
  }

  /**
   * For the game parser only.
   */
  public static void createDefaultTechAdvances(final GameData data) {
    final TechnologyFrontier tf = data.getTechnologyFrontier();
    final boolean ww2v2 = Properties.getWW2V2(data);
    final boolean ww2v3 = Properties.getWW2V3(data);
    if (ww2v2) {
      createWW2V2Advances(tf);
    } else if (ww2v3) {
      createWW2V3Advances(tf);
    } else {
      createWW2V1Advances(tf);
    }
    // now create player tech frontiers
    final List<TechnologyFrontier> frontiers = new ArrayList<>();
    if (ww2v3) {
      final TechnologyFrontier an = new TechnologyFrontier("Air and Naval Advances", data);
      final TechnologyFrontier lp = new TechnologyFrontier("Land and Production Advances", data);
      final Tuple<List<TechAdvance>, List<TechAdvance>> ww2v3advances = getWW2v3CategoriesWithTheirAdvances(data);
      an.addAdvance(ww2v3advances.getFirst());
      lp.addAdvance(ww2v3advances.getSecond());
      frontiers.add(an);
      frontiers.add(lp);
    } else {
      final TechnologyFrontier tas = new TechnologyFrontier("Technology Advances", data);
      tas.addAdvance(new ArrayList<>(tf.getTechs()));
      frontiers.add(tas);
    }
    // add the frontiers
    for (final PlayerID player : data.getPlayerList().getPlayers()) {
      for (final TechnologyFrontier frontier : frontiers) {
        player.getTechnologyFrontierList().addTechnologyFrontier(new TechnologyFrontier(frontier));
      }
    }
  }

  public static TechAdvance findDefinedAdvanceAndCreateAdvance(final String s, final GameData data) {
    final Class<? extends TechAdvance> clazz = ALL_PREDEFINED_TECHNOLOGIES.get(s);
    if (clazz == null) {
      throw new IllegalArgumentException(s + " is not a valid technology");
    }
    final TechAdvance ta;
    try {
      final Constructor<? extends TechAdvance> constructor = clazz.getConstructor(preDefinedTechConstructorParameter);
      ta = constructor.newInstance(data);
    } catch (final Exception e) {
      throw new IllegalStateException(s + " is not a valid technology or could not be instantiated", e);
    }
    if (ta == null) {
      throw new IllegalStateException(s + " is not a valid technology or could not be instantiated");
    }
    return ta;
  }

  static TechAdvance findAdvance(final String propertyString, final GameData data, final PlayerID player) {
    for (final TechAdvance t : getTechAdvances(data, player)) {
      if (t.getProperty().equals(propertyString)) {
        return t;
      }
    }
    throw new IllegalArgumentException(propertyString + " is not a valid technology");
  }

  /**
   * @return first is air&naval, second is land&production.
   */
  private static Tuple<List<TechAdvance>, List<TechAdvance>> getWW2v3CategoriesWithTheirAdvances(final GameData data) {
    data.acquireReadLock();
    List<TechAdvance> allAdvances;
    try {
      allAdvances = new ArrayList<>(data.getTechnologyFrontier().getTechs());
    } finally {
      data.releaseReadLock();
    }
    final List<TechAdvance> airAndNaval = new ArrayList<>();
    final List<TechAdvance> landAndProduction = new ArrayList<>();
    for (final TechAdvance ta : allAdvances) {
      final String propertyString = ta.getProperty();
      if (propertyString.equals(TECH_PROPERTY_SUPER_SUBS) || propertyString.equals(TECH_PROPERTY_JET_POWER)
          || propertyString.equals(TECH_PROPERTY_IMPROVED_SHIPYARDS) || propertyString.equals(TECH_PROPERTY_AA_RADAR)
          || propertyString.equals(TECH_PROPERTY_LONG_RANGE_AIRCRAFT)
          || propertyString.equals(TECH_PROPERTY_HEAVY_BOMBER)) {
        airAndNaval.add(ta);
      } else if (propertyString.equals(TECH_PROPERTY_IMPROVED_ARTILLERY_SUPPORT)
          || propertyString.equals(TECH_PROPERTY_ROCKETS) || propertyString.equals(TECH_PROPERTY_PARATROOPERS)
          || propertyString.equals(TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION)
          || propertyString.equals(TECH_PROPERTY_WAR_BONDS)
          || propertyString.equals(TECH_PROPERTY_MECHANIZED_INFANTRY)) {
        landAndProduction.add(ta);
      } else {
        throw new IllegalStateException(
            "We should not be using ww2v3 categories if we have custom techs: " + propertyString);
      }
    }
    return Tuple.of(airAndNaval, landAndProduction);
  }

  /**
   * Returns all tech advances possible in this game.
   */
  public static List<TechAdvance> getTechAdvances(final GameData data) {
    return getTechAdvances(data, null);
  }

  /**
   * Returns all tech advances that this player can possibly research. (Or if Player is null, returns all techs
   * available in the game).
   */
  public static List<TechAdvance> getTechAdvances(final GameData data, final PlayerID player) {
    final TechnologyFrontier technologyFrontier = data.getTechnologyFrontier();
    if ((technologyFrontier != null) && !technologyFrontier.isEmpty()) {
      return (player != null) ? player.getTechnologyFrontierList().getAdvances() : technologyFrontier.getTechs();
    }
    // the game has no techs, just return empty list
    return new ArrayList<>();
  }

  /**
   * Returns all possible tech categories for this player.
   */
  public static List<TechnologyFrontier> getPlayerTechCategories(final PlayerID player) {
    if (player != null) {
      return player.getTechnologyFrontierList().getFrontiers();
    }
    throw new IllegalStateException("Player cannot be null");
  }

  @Override
  public boolean equals(final Object o) {
    if ((o == null) || !(o instanceof TechAdvance)) {
      return false;
    }
    final TechAdvance other = (TechAdvance) o;
    if ((other.getName() == null) || (getName() == null)) {
      return false;
    }
    return getName().equals(other.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getName());
  }

  @Override
  public String toString() {
    return getName();
  }
}
