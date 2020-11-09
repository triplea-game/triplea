package games.strategy.triplea.delegate;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAttachment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.triplea.util.Tuple;

/** Superclass for all technology advances. */
public abstract class TechAdvance extends NamedAttachable {
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
  public static final String TECH_NAME_INCREASED_FACTORY_PRODUCTION =
      "Increased Factory Production";
  public static final String TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION =
      "increasedFactoryProduction";
  public static final String TECH_NAME_WAR_BONDS = "War Bonds";
  public static final String TECH_PROPERTY_WAR_BONDS = "warBonds";
  public static final String TECH_NAME_MECHANIZED_INFANTRY = "Mechanized Infantry";
  public static final String TECH_PROPERTY_MECHANIZED_INFANTRY = "mechanizedInfantry";
  public static final String TECH_NAME_INDUSTRIAL_TECHNOLOGY = "Industrial Technology";
  public static final String TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY = "industrialTechnology";
  public static final String TECH_NAME_DESTROYER_BOMBARD = "Destroyer Bombard";
  public static final String TECH_PROPERTY_DESTROYER_BOMBARD = "destroyerBombard";
  public static final List<String> ALL_PREDEFINED_TECHNOLOGY_NAMES =
      Collections.unmodifiableList(
          List.of(
              TECH_NAME_SUPER_SUBS,
              TECH_NAME_JET_POWER,
              TECH_NAME_IMPROVED_SHIPYARDS,
              TECH_NAME_AA_RADAR,
              TECH_NAME_LONG_RANGE_AIRCRAFT,
              TECH_NAME_HEAVY_BOMBER,
              TECH_NAME_IMPROVED_ARTILLERY_SUPPORT,
              TECH_NAME_ROCKETS,
              TECH_NAME_PARATROOPERS,
              TECH_NAME_INCREASED_FACTORY_PRODUCTION,
              TECH_NAME_WAR_BONDS,
              TECH_NAME_MECHANIZED_INFANTRY,
              TECH_NAME_INDUSTRIAL_TECHNOLOGY,
              TECH_NAME_DESTROYER_BOMBARD));
  private static final long serialVersionUID = -1076712297024403156L;
  private static final Class<?>[] preDefinedTechConstructorParameter =
      new Class<?>[] {GameData.class};
  private static final Map<String, Class<? extends TechAdvance>> ALL_PREDEFINED_TECHNOLOGIES =
      newPredefinedTechnologyMap();

  public TechAdvance(final String name, final GameData data) {
    super(name, data);
  }

  private static Map<String, Class<? extends TechAdvance>> newPredefinedTechnologyMap() {
    final Map<String, Class<? extends TechAdvance>> preDefinedTechMap = new HashMap<>();
    preDefinedTechMap.put(TECH_PROPERTY_SUPER_SUBS, SuperSubsAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_JET_POWER, JetPowerAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_IMPROVED_SHIPYARDS, ImprovedShipyardsAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_AA_RADAR, AaRadarAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_LONG_RANGE_AIRCRAFT, LongRangeAircraftAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_HEAVY_BOMBER, HeavyBomberAdvance.class);
    preDefinedTechMap.put(
        TECH_PROPERTY_IMPROVED_ARTILLERY_SUPPORT, ImprovedArtillerySupportAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_ROCKETS, RocketsAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_PARATROOPERS, ParatroopersAdvance.class);
    preDefinedTechMap.put(
        TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION, IncreasedFactoryProductionAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_WAR_BONDS, WarBondsAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_MECHANIZED_INFANTRY, MechanizedInfantryAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY, IndustrialTechnologyAdvance.class);
    preDefinedTechMap.put(TECH_PROPERTY_DESTROYER_BOMBARD, DestroyerBombardTechAdvance.class);
    return Collections.unmodifiableMap(preDefinedTechMap);
  }

  public abstract String getProperty();

  public abstract void perform(GamePlayer gamePlayer, IDelegateBridge bridge);

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
    tf.addAdvance(new AaRadarAdvance(tf.getData()));
    tf.addAdvance(new LongRangeAircraftAdvance(tf.getData()));
    tf.addAdvance(new HeavyBomberAdvance(tf.getData()));
    tf.addAdvance(new ImprovedArtillerySupportAdvance(tf.getData()));
    tf.addAdvance(new RocketsAdvance(tf.getData()));
    tf.addAdvance(new ParatroopersAdvance(tf.getData()));
    tf.addAdvance(new IncreasedFactoryProductionAdvance(tf.getData()));
    tf.addAdvance(new WarBondsAdvance(tf.getData()));
    tf.addAdvance(new MechanizedInfantryAdvance(tf.getData()));
  }

  /** For the game parser only. */
  public static void createDefaultTechAdvances(final GameData data) {
    final TechnologyFrontier tf = data.getTechnologyFrontier();
    final boolean ww2v2 = Properties.getWW2V2(data.getProperties());
    final boolean ww2v3 = Properties.getWW2V3(data.getProperties());
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
      final Tuple<List<TechAdvance>, List<TechAdvance>> ww2v3advances =
          getWW2v3CategoriesWithTheirAdvances(data);
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
    for (final GamePlayer player : data.getPlayerList().getPlayers()) {
      for (final TechnologyFrontier frontier : frontiers) {
        player.getTechnologyFrontierList().addTechnologyFrontier(new TechnologyFrontier(frontier));
      }
    }
  }

  /**
   * Creates and returns an instance of the well-known technology advance with the specified class
   * name.
   *
   * @param technologyName The fully-qualified name of the technology advance class.
   * @throws IllegalArgumentException If {@code s} is not a well-known technology advance.
   * @throws IllegalStateException If an error occurs while creating the new technology advance
   *     instance.
   */
  public static TechAdvance findDefinedAdvanceAndCreateAdvance(
      final String technologyName, final GameData data) {
    final Class<? extends TechAdvance> clazz = ALL_PREDEFINED_TECHNOLOGIES.get(technologyName);
    if (clazz == null) {
      throw new IllegalArgumentException(technologyName + " is not a valid technology");
    }

    try {
      return clazz.getDeclaredConstructor(preDefinedTechConstructorParameter).newInstance(data);
    } catch (final Exception e) {
      throw new IllegalStateException(
          technologyName + " is not a valid technology or could not be instantiated", e);
    }
  }

  static TechAdvance findAdvance(
      final String propertyString, final GameData data, final GamePlayer player) {
    for (final TechAdvance t : getTechAdvances(data, player)) {
      if (t.getProperty().equals(propertyString)) {
        return t;
      }
    }
    throw new IllegalArgumentException(propertyString + " is not a valid technology");
  }

  /**
   * Returns a tuple, where the first element contains air &amp; naval tech advances, and the second
   * element contains land &amp; production tech advances.
   */
  private static Tuple<List<TechAdvance>, List<TechAdvance>> getWW2v3CategoriesWithTheirAdvances(
      final GameData data) {
    data.acquireReadLock();
    final List<TechAdvance> allAdvances;
    try {
      allAdvances = new ArrayList<>(data.getTechnologyFrontier().getTechs());
    } finally {
      data.releaseReadLock();
    }
    final List<TechAdvance> airAndNaval = new ArrayList<>();
    final List<TechAdvance> landAndProduction = new ArrayList<>();
    for (final TechAdvance ta : allAdvances) {
      final String propertyString = ta.getProperty();
      switch (propertyString) {
        case TECH_PROPERTY_SUPER_SUBS:
        case TECH_PROPERTY_JET_POWER:
        case TECH_PROPERTY_IMPROVED_SHIPYARDS:
        case TECH_PROPERTY_AA_RADAR:
        case TECH_PROPERTY_LONG_RANGE_AIRCRAFT:
        case TECH_PROPERTY_HEAVY_BOMBER:
          airAndNaval.add(ta);
          break;
        case TECH_PROPERTY_IMPROVED_ARTILLERY_SUPPORT:
        case TECH_PROPERTY_ROCKETS:
        case TECH_PROPERTY_PARATROOPERS:
        case TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION:
        case TECH_PROPERTY_WAR_BONDS:
        case TECH_PROPERTY_MECHANIZED_INFANTRY:
          landAndProduction.add(ta);
          break;
        default:
          throw new IllegalStateException(
              "We should not be using ww2v3 categories if we have custom techs: " + propertyString);
      }
    }
    return Tuple.of(airAndNaval, landAndProduction);
  }

  /** Returns all tech advances possible in this game. */
  public static List<TechAdvance> getTechAdvances(final GameData data) {
    return getTechAdvances(data, null);
  }

  /**
   * Returns all tech advances that this player can possibly research. (Or if Player is null,
   * returns all techs available in the game).
   */
  public static List<TechAdvance> getTechAdvances(final GameData data, final GamePlayer player) {
    final TechnologyFrontier technologyFrontier = data.getTechnologyFrontier();
    if (technologyFrontier != null && !technologyFrontier.isEmpty()) {
      return (player != null)
          ? player.getTechnologyFrontierList().getAdvances()
          : technologyFrontier.getTechs();
    }
    // the game has no techs, just return empty list
    return new ArrayList<>();
  }

  /** Returns all possible tech categories for this player. */
  public static List<TechnologyFrontier> getPlayerTechCategories(final GamePlayer player) {
    checkNotNull(player);

    return player.getTechnologyFrontierList().getFrontiers();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof TechAdvance)) {
      return false;
    }
    final TechAdvance other = (TechAdvance) o;
    return other.getName() != null && getName() != null && getName().equals(other.getName());
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
