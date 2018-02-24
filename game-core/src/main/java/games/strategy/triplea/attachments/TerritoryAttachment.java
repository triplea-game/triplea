package games.strategy.triplea.attachments;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.formatter.MyFormatter;

@MapSupport
public class TerritoryAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 9102862080104655281L;


  public static boolean doWeHaveEnoughCapitalsToProduce(final PlayerID player, final GameData data) {
    final List<Territory> capitalsListOriginal = new ArrayList<>(TerritoryAttachment.getAllCapitals(player, data));
    final List<Territory> capitalsListOwned =
        new ArrayList<>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(player, data));
    final PlayerAttachment pa = PlayerAttachment.get(player);

    if (pa == null) {
      return capitalsListOriginal.isEmpty() || !capitalsListOwned.isEmpty();
    }
    return pa.getRetainCapitalProduceNumber() <= capitalsListOwned.size();
  }

  /**
   * If we own one of our capitals, return the first one found, otherwise return the first capital we find that we don't
   * own.
   * If a capital has no neighbor connections, it will be sent last.
   */
  public static Territory getFirstOwnedCapitalOrFirstUnownedCapital(final PlayerID player, final GameData data) {
    final List<Territory> capitals = new ArrayList<>();
    final List<Territory> noNeighborCapitals = new ArrayList<>();
    for (final Territory current : data.getMap().getTerritories()) {
      final TerritoryAttachment ta = TerritoryAttachment.get(current);
      if (ta != null && ta.getCapital() != null) {
        final PlayerID whoseCapital = data.getPlayerList().getPlayerId(ta.getCapital());
        if (whoseCapital == null) {
          throw new IllegalStateException("Invalid capital for player name:" + ta.getCapital());
        }
        if (player.equals(whoseCapital)) {
          if (player.equals(current.getOwner())) {
            if (data.getMap().getNeighbors(current).size() > 0) {
              return current;
            }
            noNeighborCapitals.add(current);
          } else {
            capitals.add(current);
          }
        }
      }
    }
    if (!capitals.isEmpty()) {
      return capitals.iterator().next();
    }
    if (!noNeighborCapitals.isEmpty()) {
      return noNeighborCapitals.iterator().next();
    }
    // Added check for optional players- no error thrown for them
    if (player.getOptional()) {
      return null;
    }
    throw new IllegalStateException("Capital not found for:" + player);
  }

  /**
   * will return empty list if none controlled, never returns null.
   */
  public static List<Territory> getAllCapitals(final PlayerID player, final GameData data) {
    final List<Territory> capitals = new ArrayList<>();
    for (final Territory current : data.getMap().getTerritories()) {
      final TerritoryAttachment ta = TerritoryAttachment.get(current);
      if (ta != null && ta.getCapital() != null) {
        final PlayerID whoseCapital = data.getPlayerList().getPlayerId(ta.getCapital());
        if (whoseCapital == null) {
          throw new IllegalStateException("Invalid capital for player name:" + ta.getCapital());
        }
        if (player.equals(whoseCapital)) {
          capitals.add(current);
        }
      }
    }
    if (!capitals.isEmpty()) {
      return capitals;
    }
    // Added check for optional players- no error thrown for them
    if (player.getOptional()) {
      return capitals;
    }
    throw new IllegalStateException("Capital not found for:" + player);
  }

  /**
   * will return empty list if none controlled, never returns null.
   */
  public static List<Territory> getAllCurrentlyOwnedCapitals(final PlayerID player, final GameData data) {
    final List<Territory> capitals = new ArrayList<>();
    for (final Territory current : data.getMap().getTerritories()) {
      final TerritoryAttachment ta = TerritoryAttachment.get(current);
      if (ta != null && ta.getCapital() != null) {
        final PlayerID whoseCapital = data.getPlayerList().getPlayerId(ta.getCapital());
        if (whoseCapital == null) {
          throw new IllegalStateException("Invalid capital for player name:" + ta.getCapital());
        }
        if (player.equals(whoseCapital) && player.equals(current.getOwner())) {
          capitals.add(current);
        }
      }
    }
    return capitals;
  }

  /**
   * Convenience method. Can return null.
   */
  public static TerritoryAttachment get(final Territory t) {
    return (TerritoryAttachment) t.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
  }

  static TerritoryAttachment get(final Territory t, final String nameOfAttachment) {
    final TerritoryAttachment territoryAttachment = (TerritoryAttachment) t.getAttachment(nameOfAttachment);
    if (territoryAttachment == null && !t.isWater()) {
      throw new IllegalStateException("No territory attachment for:" + t.getName() + " with name:" + nameOfAttachment);
    }
    return territoryAttachment;
  }

  /**
   * Adds the specified territory attachment to the specified territory.
   *
   * @param territory The territory that will receive the territory attachment.
   * @param territoryAttachment The territory attachment.
   */
  public static void add(final Territory territory, final TerritoryAttachment territoryAttachment) {
    checkNotNull(territory);
    checkNotNull(territoryAttachment);

    territory.addAttachment(Constants.TERRITORY_ATTACHMENT_NAME, territoryAttachment);
  }

  /**
   * Removes any territory attachment from the specified territory.
   *
   * @param territory The territory from which the attachment will be removed.
   */
  public static void remove(final Territory territory) {
    checkNotNull(territory);

    territory.removeAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
  }

  /**
   * Convenience method since TerritoryAttachment.get could return null.
   */
  public static int getProduction(final Territory t) {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    if (ta == null) {
      return 0;
    }
    return ta.getProduction();
  }

  public int getProduction() {
    return m_production;
  }

  /**
   * Convenience method since TerritoryAttachment.get could return null.
   */
  public static int getUnitProduction(final Territory t) {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    if (ta == null) {
      return 0;
    }
    return ta.getUnitProduction();
  }

  public int getUnitProduction() {
    return m_unitProduction;
  }

  private String m_capital = null;
  private boolean m_originalFactory = false;
  // "setProduction" will set both m_production and m_unitProduction.
  // While "setProductionOnly" sets only m_production.
  private int m_production = 0;
  private int m_victoryCity = 0;
  private boolean m_isImpassable = false;
  private PlayerID m_originalOwner = null;
  private boolean m_convoyRoute = false;
  private HashSet<Territory> m_convoyAttached = new HashSet<>();
  private ArrayList<PlayerID> m_changeUnitOwners = new ArrayList<>();
  private ArrayList<PlayerID> m_captureUnitOnEnteringBy = new ArrayList<>();
  private boolean m_navalBase = false;
  private boolean m_airBase = false;
  private boolean m_kamikazeZone = false;
  private int m_unitProduction = 0;
  private boolean m_blockadeZone = false;
  private ArrayList<TerritoryEffect> m_territoryEffect = new ArrayList<>();
  private ArrayList<String> m_whenCapturedByGoesTo = new ArrayList<>();
  private ResourceCollection m_resources = null;

  /** Creates new TerritoryAttachment. */
  public TerritoryAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setResources(final String value) throws GameParseException {
    if (value == null) {
      m_resources = null;
      return;
    }
    if (m_resources == null) {
      m_resources = new ResourceCollection(getData());
    }
    final String[] s = value.split(":");
    final int amount = getInt(s[0]);
    if (s[1].equals(Constants.PUS)) {
      throw new GameParseException("Please set PUs using production, not resource" + thisErrorMsg());
    }
    final Resource resource = getData().getResourceList().getResource(s[1]);
    if (resource == null) {
      throw new GameParseException("No resource named: " + s[1] + thisErrorMsg());
    }
    m_resources.putResource(resource, amount);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setResources(final ResourceCollection value) {
    m_resources = value;
  }

  public ResourceCollection getResources() {
    return m_resources;
  }

  public void clearResources() {
    m_resources = new ResourceCollection(getData());
  }

  public void resetResources() {
    m_resources = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsImpassable(final String value) {
    setIsImpassable(getBool(value));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setIsImpassable(final boolean value) {
    m_isImpassable = value;
  }

  public boolean getIsImpassable() {
    return m_isImpassable;
  }

  public void resetIsImpassable() {
    m_isImpassable = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCapital(final String value) throws GameParseException {
    if (value == null) {
      m_capital = null;
      return;
    }
    final PlayerID p = getData().getPlayerList().getPlayerId(value);
    if (p == null) {
      throw new GameParseException("No Player named: " + value + thisErrorMsg());
    }
    m_capital = value;
  }

  public boolean isCapital() {
    return m_capital != null;
  }

  public String getCapital() {
    return m_capital;
  }

  public void resetCapital() {
    m_capital = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setVictoryCity(final String value) {
    setVictoryCity(getInt(value));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setVictoryCity(final int value) {
    m_victoryCity = value;
  }

  public int getVictoryCity() {
    return m_victoryCity;
  }

  public void resetVictoryCity() {
    m_victoryCity = 0;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setOriginalFactory(final String value) {
    setOriginalFactory(getBool(value));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setOriginalFactory(final boolean value) {
    m_originalFactory = value;
  }

  public boolean getOriginalFactory() {
    return m_originalFactory;
  }

  public void resetOriginalFactory() {
    m_originalFactory = false;
  }

  /**
   * Sets production and unitProduction (or just "production" in a map xml)
   * of a territory to be equal to the string value passed. This method is
   * used when parsing game XML since it passes string values.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setProduction(final String value) {
    m_production = getInt(value);
    // do NOT remove. unitProduction should always default to production
    m_unitProduction = m_production;
  }

  /**
   * Sets production and unitProduction (or just "production" in a map xml)
   * of a territory to be equal to the Integer value passed. This method is
   * used when working with game history since it passes Integer values.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setProduction(final Integer value) {
    m_production = value;
    // do NOT remove. unitProduction should always default to production
    m_unitProduction = m_production;
  }

  /**
   * Resets production and unitProduction (or just "production" in a map xml) of a territory to the default value.
   */
  public void resetProduction() {
    m_production = 0;
    // do NOT remove. unitProduction should always default to production
    m_unitProduction = m_production;
  }

  /**
   * Sets only m_production.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false, virtual = true)
  public void setProductionOnly(final String value) {
    m_production = getInt(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setUnitProduction(final String value) {
    setUnitProduction(getInt(value));
  }


  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setUnitProduction(final int value) {
    m_unitProduction = value;
  }

  public void resetUnitProduction() {
    m_unitProduction = 0;
  }

  /**
   * Should not be set by a game xml during attachment parsing, but CAN be set by initialization parsing and/or Property
   * Utils.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setOriginalOwner(final PlayerID player) {
    m_originalOwner = player;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setOriginalOwner(final String player) throws GameParseException {
    if (player == null) {
      m_originalOwner = null;
    }
    final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(player);
    if (tempPlayer == null) {
      throw new GameParseException("No player named: " + player + thisErrorMsg());
    }
    m_originalOwner = tempPlayer;
  }

  public PlayerID getOriginalOwner() {
    return m_originalOwner;
  }

  public void resetOriginalOwner() {
    m_originalOwner = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setConvoyRoute(final String value) {
    m_convoyRoute = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setConvoyRoute(final Boolean value) {
    m_convoyRoute = value;
  }

  public boolean getConvoyRoute() {
    return m_convoyRoute;
  }

  public void resetConvoyRoute() {
    m_convoyRoute = false;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setChangeUnitOwners(final String value) throws GameParseException {
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_changeUnitOwners.add(tempPlayer);
      } else if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
        m_changeUnitOwners.clear();
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setChangeUnitOwners(final ArrayList<PlayerID> value) {
    m_changeUnitOwners = value;
  }

  public ArrayList<PlayerID> getChangeUnitOwners() {
    return m_changeUnitOwners;
  }

  public void clearChangeUnitOwners() {
    m_changeUnitOwners.clear();
  }

  public void resetChangeUnitOwners() {
    m_changeUnitOwners = new ArrayList<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setCaptureUnitOnEnteringBy(final String value) throws GameParseException {
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_captureUnitOnEnteringBy.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCaptureUnitOnEnteringBy(final ArrayList<PlayerID> value) {
    m_captureUnitOnEnteringBy = value;
  }

  public ArrayList<PlayerID> getCaptureUnitOnEnteringBy() {
    return m_captureUnitOnEnteringBy;
  }

  public void clearCaptureUnitOnEnteringBy() {
    m_captureUnitOnEnteringBy.clear();
  }

  public void resetCaptureUnitOnEnteringBy() {
    m_captureUnitOnEnteringBy = new ArrayList<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setWhenCapturedByGoesTo(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length != 2) {
      throw new GameParseException(
          "whenCapturedByGoesTo must have 2 player names separated by a colon" + thisErrorMsg());
    }
    for (final String name : s) {
      final PlayerID player = getData().getPlayerList().getPlayerId(name);
      if (player == null) {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
    m_whenCapturedByGoesTo.add(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setWhenCapturedByGoesTo(final ArrayList<String> value) {
    m_whenCapturedByGoesTo = value;
  }

  public ArrayList<String> getWhenCapturedByGoesTo() {
    return m_whenCapturedByGoesTo;
  }

  public void clearWhenCapturedByGoesTo() {
    m_whenCapturedByGoesTo.clear();
  }

  public void resetWhenCapturedByGoesTo() {
    m_whenCapturedByGoesTo = new ArrayList<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setTerritoryEffect(final String value) throws GameParseException {
    final String[] s = value.split(":");
    for (final String name : s) {
      final TerritoryEffect effect = getData().getTerritoryEffectList().get(name);
      if (effect != null) {
        m_territoryEffect.add(effect);
      } else {
        throw new GameParseException("No TerritoryEffect named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setTerritoryEffect(final ArrayList<TerritoryEffect> value) {
    m_territoryEffect = value;
  }

  public ArrayList<TerritoryEffect> getTerritoryEffect() {
    return m_territoryEffect;
  }

  public void clearTerritoryEffect() {
    m_territoryEffect.clear();
  }

  public void resetTerritoryEffect() {
    m_territoryEffect = new ArrayList<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setConvoyAttached(final String value) throws GameParseException {
    if (value.length() <= 0) {
      return;
    }
    for (final String subString : value.split(":")) {
      final Territory territory = getData().getMap().getTerritory(subString);
      if (territory == null) {
        throw new GameParseException("No territory called:" + subString + thisErrorMsg());
      }
      m_convoyAttached.add(territory);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setConvoyAttached(final HashSet<Territory> value) {
    m_convoyAttached = value;
  }

  public HashSet<Territory> getConvoyAttached() {
    return m_convoyAttached;
  }

  public void clearConvoyAttached() {
    m_convoyAttached.clear();
  }

  public void resetConvoyAttached() {
    m_convoyAttached = new HashSet<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setNavalBase(final String value) {
    m_navalBase = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setNavalBase(final Boolean value) {
    m_navalBase = value;
  }

  public boolean getNavalBase() {
    return m_navalBase;
  }

  public void resetNavalBase() {
    m_navalBase = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirBase(final String value) {
    m_airBase = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirBase(final Boolean value) {
    m_airBase = value;
  }

  public boolean getAirBase() {
    return m_airBase;
  }

  public void resetAirBase() {
    m_airBase = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setKamikazeZone(final String value) {
    m_kamikazeZone = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setKamikazeZone(final Boolean value) {
    m_kamikazeZone = value;
  }

  public boolean getKamikazeZone() {
    return m_kamikazeZone;
  }

  public void resetKamikazeZone() {
    m_kamikazeZone = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBlockadeZone(final String value) {
    m_blockadeZone = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBlockadeZone(final Boolean value) {
    m_blockadeZone = value;
  }

  public boolean getBlockadeZone() {
    return m_blockadeZone;
  }

  public void resetBlockadeZone() {
    m_blockadeZone = false;
  }

  public static Set<Territory> getWhatTerritoriesThisIsUsedInConvoysFor(final Territory t, final GameData data) {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    if (ta == null || !ta.getConvoyRoute()) {
      return null;
    }
    final Set<Territory> territories = new HashSet<>();
    for (final Territory current : data.getMap().getTerritories()) {
      final TerritoryAttachment cta = TerritoryAttachment.get(current);
      if (cta == null || !cta.getConvoyRoute()) {
        continue;
      }
      if (cta.getConvoyAttached().contains(t)) {
        territories.add(current);
      }
    }
    return territories;
  }

  public String toStringForInfo(final boolean useHtml, final boolean includeAttachedToName) {
    final StringBuilder sb = new StringBuilder("");
    final String br = (useHtml ? "<br>" : ", ");
    final Territory t = (Territory) this.getAttachedTo();
    if (t == null) {
      return sb.toString();
    }
    if (includeAttachedToName) {
      sb.append(t.getName());
      sb.append(br);
      if (t.isWater()) {
        sb.append("Water Territory");
      } else {
        sb.append("Land Territory");
      }
      sb.append(br);
      final PlayerID owner = t.getOwner();
      if (owner != null && !owner.isNull()) {
        sb.append("Current Owner: ").append(t.getOwner().getName());
        sb.append(br);
      }
      final PlayerID originalOwner = getOriginalOwner();
      if (originalOwner != null) {
        sb.append("Original Owner: ").append(originalOwner.getName());
        sb.append(br);
      }
    }
    if (m_isImpassable) {
      sb.append("Is Impassable");
      sb.append(br);
    }
    if (m_capital != null && m_capital.length() > 0) {
      sb.append("A Capital of ").append(m_capital);
      sb.append(br);
    }
    if (m_victoryCity != 0) {
      sb.append("Is a Victory location");
      sb.append(br);
    }
    if (m_kamikazeZone) {
      sb.append("Is Kamikaze Zone");
      sb.append(br);
    }
    if (m_blockadeZone) {
      sb.append("Is a Blockade Zone");
      sb.append(br);
    }
    if (m_convoyRoute) {
      if (!m_convoyAttached.isEmpty()) {
        sb.append("Needs: ").append(MyFormatter.defaultNamedToTextList(m_convoyAttached)).append(br);
      }
      final Set<Territory> requiredBy = getWhatTerritoriesThisIsUsedInConvoysFor(t, getData());
      if (!requiredBy.isEmpty()) {
        sb.append("Required By: ").append(MyFormatter.defaultNamedToTextList(requiredBy)).append(br);
      }
    }
    if (m_changeUnitOwners != null && !m_changeUnitOwners.isEmpty()) {
      sb.append("Units May Change Ownership Here");
      sb.append(br);
    }
    if (m_captureUnitOnEnteringBy != null && !m_captureUnitOnEnteringBy.isEmpty()) {
      sb.append("May Allow The Capture of Some Units");
      sb.append(br);
    }
    if (m_whenCapturedByGoesTo != null && !m_whenCapturedByGoesTo.isEmpty()) {
      sb.append("Captured By -> Ownership Goes To");
      sb.append(br);
      for (final String value : m_whenCapturedByGoesTo) {
        final String[] s = value.split(":");
        sb.append(s[0]).append(" -> ").append(s[1]);
        sb.append(br);
      }
    }
    sb.append(br);
    if (!t.isWater() && m_unitProduction > 0
        && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(getData())) {
      sb.append("Base Unit Production: ");
      sb.append(m_unitProduction);
      sb.append(br);
    }
    if (m_production > 0 || (m_resources != null && m_resources.toString().length() > 0)) {
      sb.append("Production: ");
      sb.append(br);
      if (m_production > 0) {
        sb.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(m_production).append(" PUs");
        sb.append(br);
      }
      if (m_resources != null) {
        if (useHtml) {
          sb.append("&nbsp;&nbsp;&nbsp;&nbsp;")
              .append((m_resources.toStringForHtml()).replaceAll("<br>", "<br>&nbsp;&nbsp;&nbsp;&nbsp;"));
        } else {
          sb.append(m_resources.toString());
        }
        sb.append(br);
      }
    }
    if (!m_territoryEffect.isEmpty()) {
      sb.append("Territory Effects: ");
      sb.append(br);
    }
    sb.append(m_territoryEffect.stream()
        .map(TerritoryEffect::getName)
        .map(name -> "&nbsp;&nbsp;&nbsp;&nbsp;" + name + br)
        .collect(Collectors.joining()));

    return sb.toString();
  }

  @Override
  public void validate(final GameData data) {}

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("capital",
            MutableProperty.ofString(
                this::setCapital,
                this::getCapital,
                this::resetCapital))
        .put("originalFactory",
            MutableProperty.ofBoolean(
                this::setOriginalFactory,
                this::setOriginalFactory,
                this::getOriginalFactory,
                this::resetOriginalFactory))
        .put("production",
            MutableProperty.ofInteger(
                this::setProduction,
                this::setProduction,
                this::getProduction,
                this::resetProduction))
        .put("productionOnly",
            MutableProperty.ofWriteOnlyString(
                this::setProductionOnly))
        .put("victoryCity",
            MutableProperty.ofInteger(
                this::setVictoryCity,
                this::setVictoryCity,
                this::getVictoryCity,
                this::resetVictoryCity))
        .put("isImpassable",
            MutableProperty.ofBoolean(
                this::setIsImpassable,
                this::setIsImpassable,
                this::getIsImpassable,
                this::resetIsImpassable))
        .put("originalOwner",
            MutableProperty.of(
                PlayerID.class,
                this::setOriginalOwner,
                this::setOriginalOwner,
                this::getOriginalOwner,
                this::resetOriginalOwner))
        .put("convoyRoute",
            MutableProperty.ofBoolean(
                this::setConvoyRoute,
                this::setConvoyRoute,
                this::getConvoyRoute,
                this::resetConvoyRoute))
        .put("convoyAttached",
            MutableProperty.of(
                HashSet.class,
                this::setConvoyAttached,
                this::setConvoyAttached,
                this::getConvoyAttached,
                this::resetConvoyAttached))
        .put("changeUnitOwners",
            MutableProperty.of(
                ArrayList.class,
                this::setChangeUnitOwners,
                this::setChangeUnitOwners,
                this::getChangeUnitOwners,
                this::resetChangeUnitOwners))
        .put("captureUnitOnEnteringBy",
            MutableProperty.of(
                ArrayList.class,
                this::setCaptureUnitOnEnteringBy,
                this::setCaptureUnitOnEnteringBy,
                this::getCaptureUnitOnEnteringBy,
                this::resetCaptureUnitOnEnteringBy))
        .put("navalBase",
            MutableProperty.ofBoolean(
                this::setNavalBase,
                this::setNavalBase,
                this::getNavalBase,
                this::resetNavalBase))
        .put("airBase",
            MutableProperty.ofBoolean(
                this::setAirBase,
                this::setAirBase,
                this::getAirBase,
                this::resetAirBase))
        .put("kamikazeZone",
            MutableProperty.ofBoolean(
                this::setKamikazeZone,
                this::setKamikazeZone,
                this::getKamikazeZone,
                this::resetKamikazeZone))
        .put("unitProduction",
            MutableProperty.ofInteger(
                this::setUnitProduction,
                this::setUnitProduction,
                this::getUnitProduction,
                this::resetUnitProduction))
        .put("blockadeZone",
            MutableProperty.ofBoolean(
                this::setBlockadeZone,
                this::setBlockadeZone,
                this::getBlockadeZone,
                this::resetBlockadeZone))
        .put("territoryEffect",
            MutableProperty.of(
                ArrayList.class,
                this::setTerritoryEffect,
                this::setTerritoryEffect,
                this::getTerritoryEffect,
                this::resetTerritoryEffect))
        .put("whenCapturedByGoesTo",
            MutableProperty.of(
                ArrayList.class,
                this::setWhenCapturedByGoesTo,
                this::setWhenCapturedByGoesTo,
                this::getWhenCapturedByGoesTo,
                this::resetWhenCapturedByGoesTo))
        .put("resources",
            MutableProperty.of(
                ResourceCollection.class,
                this::setResources,
                this::setResources,
                this::getResources,
                this::resetResources))
        .build();
  }
}
