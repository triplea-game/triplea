package games.strategy.triplea.attachments;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.triplea.java.collections.CollectionUtils;

/**
 * An attachment for instances of {@link Territory}.
 *
 * <p>Note: Empty collection fields default to null to minimize memory use and serialization size.
 */
public class TerritoryAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 9102862080104655281L;

  private @Nullable String capital = null;
  private boolean originalFactory = false;
  // "setProduction" will set both production and unitProduction.
  // While "setProductionOnly" sets only production.
  @Getter private int production = 0;
  @Getter private int victoryCity = 0;
  private boolean isImpassable = false;
  @Getter private GamePlayer originalOwner = null;
  private boolean convoyRoute = false;
  private @Nullable Set<Territory> convoyAttached = null;
  private @Nullable List<GamePlayer> changeUnitOwners = null;
  private @Nullable List<GamePlayer> captureUnitOnEnteringBy = null;
  private boolean navalBase = false;
  private boolean airBase = false;
  private boolean kamikazeZone = false;
  @Getter private int unitProduction = 0;
  private boolean blockadeZone = false;
  private @Nullable List<TerritoryEffect> territoryEffect = null;
  private @Nullable List<String> whenCapturedByGoesTo = null;
  private @Nullable ResourceCollection resources = null;

  public TerritoryAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static boolean doWeHaveEnoughCapitalsToProduce(
      final GamePlayer player, final GameMap gameMap) {
    final List<Territory> capitalsListOriginal =
        TerritoryAttachment.getAllCapitals(player, gameMap);
    final List<Territory> capitalsListOwned =
        TerritoryAttachment.getAllCurrentlyOwnedCapitals(player, gameMap);
    final PlayerAttachment pa = PlayerAttachment.get(player);

    if (pa == null) {
      return capitalsListOriginal.isEmpty() || !capitalsListOwned.isEmpty();
    }
    return pa.getRetainCapitalProduceNumber() <= capitalsListOwned.size();
  }

  /**
   * If we own one of our capitals, return the first one found, otherwise return the first capital
   * we find that we don't own. If a capital has no neighbor connections, it will be sent last.
   */
  public static @Nullable Territory getFirstOwnedCapitalOrFirstUnownedCapital(
      final GamePlayer player, final GameMap gameMap) {
    final List<Territory> capitals = new ArrayList<>();
    final List<Territory> noNeighborCapitals = new ArrayList<>();
    for (final Territory current : gameMap.getTerritories()) {
      final TerritoryAttachment ta = TerritoryAttachment.get(current);
      if (ta != null && ta.getCapital() != null) {
        if (player.getName().equals(ta.getCapital())) {
          if (player.equals(current.getOwner())) {
            if (!gameMap.getNeighbors(current).isEmpty()) {
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
      return CollectionUtils.getAny(capitals);
    }
    if (!noNeighborCapitals.isEmpty()) {
      return CollectionUtils.getAny(noNeighborCapitals);
    }
    // Added check for optional players- no error thrown for them
    if (player.getOptional()) {
      return null;
    }
    throw new IllegalStateException("Capital not found for:" + player);
  }

  /** will return empty list if none controlled, never returns null. */
  public static List<Territory> getAllCapitals(final GamePlayer player, final GameMap gameMap) {
    final List<Territory> capitals = new ArrayList<>();
    for (final Territory current : gameMap.getTerritories()) {
      final TerritoryAttachment ta = TerritoryAttachment.get(current);
      if (ta != null && ta.getCapital() != null) {
        if (player.getName().equals(ta.getCapital())) {
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

  /** will return empty list if none controlled, never returns null. */
  public static List<Territory> getAllCurrentlyOwnedCapitals(
      final GamePlayer player, final GameMap gameMap) {
    final List<Territory> capitals = new ArrayList<>();
    for (final Territory current : gameMap.getTerritories()) {
      final TerritoryAttachment ta = TerritoryAttachment.get(current);
      if (ta != null && ta.getCapital() != null) {
        if (player.getName().equals(ta.getCapital()) && player.equals(current.getOwner())) {
          capitals.add(current);
        }
      }
    }
    return capitals;
  }

  /** Convenience method. Can return null. */
  public static @Nullable TerritoryAttachment get(final Territory t) {
    return (TerritoryAttachment) t.getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
  }

  static TerritoryAttachment get(final Territory t, final String nameOfAttachment) {
    final TerritoryAttachment territoryAttachment =
        (TerritoryAttachment) t.getAttachment(nameOfAttachment);
    if (territoryAttachment == null && !t.isWater()) {
      throw new IllegalStateException(
          "No territory attachment for:" + t.getName() + " with name:" + nameOfAttachment);
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

  /** Convenience method since TerritoryAttachment.get could return null. */
  public static int getProduction(final Territory t) {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    if (ta == null) {
      return 0;
    }
    return ta.getProduction();
  }

  /** Convenience method since TerritoryAttachment.get could return null. */
  public static int getUnitProduction(final Territory t) {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    if (ta == null) {
      return 0;
    }
    return ta.getUnitProduction();
  }

  private void setResources(final String value) throws GameParseException {
    if (resources == null) {
      resources = new ResourceCollection(getData());
    }
    final String[] s = splitOnColon(value);
    final int amount = getInt(s[0]);
    if (s[1].equals(Constants.PUS)) {
      throw new GameParseException(
          "Please set PUs using production, not resource" + thisErrorMsg());
    }
    final Resource resource = getData().getResourceList().getResource(s[1]);
    if (resource == null) {
      throw new GameParseException("No resource named: " + s[1] + thisErrorMsg());
    }
    resources.putResource(resource, amount);
  }

  private void setResources(final ResourceCollection value) {
    resources = value;
  }

  public @Nullable ResourceCollection getResources() {
    return resources;
  }

  private void resetResources() {
    resources = null;
  }

  private void setIsImpassable(final String value) {
    setIsImpassable(getBool(value));
  }

  private void setIsImpassable(final boolean value) {
    isImpassable = value;
  }

  public boolean getIsImpassable() {
    return isImpassable;
  }

  private void resetIsImpassable() {
    isImpassable = false;
  }

  public void setCapital(final String value) throws GameParseException {
    getPlayerOrThrow(value);
    capital = value;
  }

  public boolean isCapital() {
    return capital != null;
  }

  public @Nullable String getCapital() {
    return capital;
  }

  private void resetCapital() {
    capital = null;
  }

  private void setVictoryCity(final int value) {
    victoryCity = value;
  }

  private void setOriginalFactory(final String value) {
    setOriginalFactory(getBool(value));
  }

  private void setOriginalFactory(final boolean value) {
    originalFactory = value;
  }

  public boolean getOriginalFactory() {
    return originalFactory;
  }

  private void resetOriginalFactory() {
    originalFactory = false;
  }

  /**
   * Sets production and unitProduction (or just "production" in a map xml) of a territory to be
   * equal to the string value passed. This method is used when parsing game XML since it passes
   * string values.
   */
  private void setProduction(final String value) {
    production = getInt(value);
    // do NOT remove. unitProduction should always default to production
    unitProduction = production;
  }

  /**
   * Sets production and unitProduction (or just "production" in a map xml) of a territory to be
   * equal to the Integer value passed. This method is used when working with game history since it
   * passes Integer values.
   */
  private void setProduction(final Integer value) {
    production = value;
    // do NOT remove. unitProduction should always default to production
    unitProduction = production;
  }

  /**
   * Resets production and unitProduction (or just "production" in a map xml) of a territory to the
   * default value.
   */
  private void resetProduction() {
    production = 0;
    // do NOT remove. unitProduction should always default to production
    unitProduction = production;
  }

  /** Sets only production. */
  private void setProductionOnly(final String value) {
    production = getInt(value);
  }

  private void setUnitProduction(final int value) {
    unitProduction = value;
  }

  /**
   * Should not be set by a game xml during attachment parsing, but CAN be set by initialization
   * parsing.
   */
  public void setOriginalOwner(final GamePlayer player) {
    originalOwner = player;
  }

  private void setOriginalOwner(final String player) throws GameParseException {
    originalOwner = getPlayerOrThrow(player);
  }

  private void resetOriginalOwner() {
    originalOwner = null;
  }

  private void setConvoyRoute(final String value) {
    convoyRoute = getBool(value);
  }

  private void setConvoyRoute(final Boolean value) {
    convoyRoute = value;
  }

  public boolean getConvoyRoute() {
    return convoyRoute;
  }

  private void resetConvoyRoute() {
    convoyRoute = false;
  }

  private void setChangeUnitOwners(final String value) throws GameParseException {
    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
      changeUnitOwners = null;
    } else {
      changeUnitOwners = parsePlayerList(value, changeUnitOwners);
    }
  }

  private void setChangeUnitOwners(final List<GamePlayer> value) {
    changeUnitOwners = value;
  }

  public List<GamePlayer> getChangeUnitOwners() {
    return getListProperty(changeUnitOwners);
  }

  private void resetChangeUnitOwners() {
    changeUnitOwners = null;
  }

  private void setCaptureUnitOnEnteringBy(final String value) throws GameParseException {
    captureUnitOnEnteringBy = parsePlayerList(value, captureUnitOnEnteringBy);
  }

  private void setCaptureUnitOnEnteringBy(final List<GamePlayer> value) {
    captureUnitOnEnteringBy = value;
  }

  public List<GamePlayer> getCaptureUnitOnEnteringBy() {
    return getListProperty(captureUnitOnEnteringBy);
  }

  private void resetCaptureUnitOnEnteringBy() {
    captureUnitOnEnteringBy = null;
  }

  @VisibleForTesting
  void setWhenCapturedByGoesTo(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException(
          "whenCapturedByGoesTo must have 2 player names separated by a colon" + thisErrorMsg());
    }
    for (final String name : s) {
      getPlayerOrThrow(name);
    }
    if (whenCapturedByGoesTo == null) {
      whenCapturedByGoesTo = new ArrayList<>();
    }
    whenCapturedByGoesTo.add(value.intern());
  }

  private void setWhenCapturedByGoesTo(final List<String> value) {
    whenCapturedByGoesTo = value;
  }

  private List<String> getWhenCapturedByGoesTo() {
    return getListProperty(whenCapturedByGoesTo);
  }

  private void resetWhenCapturedByGoesTo() {
    whenCapturedByGoesTo = null;
  }

  public Collection<CaptureOwnershipChange> getCaptureOwnershipChanges() {
    return getWhenCapturedByGoesTo().stream()
        .map(this::parseCaptureOwnershipChange)
        .collect(Collectors.toList());
  }

  private CaptureOwnershipChange parseCaptureOwnershipChange(
      final String encodedCaptureOwnershipChange) {
    final String[] tokens = splitOnColon(encodedCaptureOwnershipChange);
    assert tokens.length == 2;
    return new CaptureOwnershipChange(
        getData().getPlayerList().getPlayerId(tokens[0]),
        getData().getPlayerList().getPlayerId(tokens[1]));
  }

  private void setTerritoryEffect(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    for (final String name : s) {
      final TerritoryEffect effect = getData().getTerritoryEffectList().get(name);
      if (effect != null) {
        if (territoryEffect == null) {
          territoryEffect = new ArrayList<>();
        }
        territoryEffect.add(effect);
      } else {
        throw new GameParseException("No TerritoryEffect named: " + name + thisErrorMsg());
      }
    }
  }

  private void setTerritoryEffect(final List<TerritoryEffect> value) {
    territoryEffect = value;
  }

  public List<TerritoryEffect> getTerritoryEffect() {
    return getListProperty(territoryEffect);
  }

  private void resetTerritoryEffect() {
    territoryEffect = null;
  }

  private void setConvoyAttached(final String value) throws GameParseException {
    if (value.length() <= 0) {
      return;
    }
    for (final String subString : splitOnColon(value)) {
      final Territory territory = getTerritoryOrThrow(subString);
      if (convoyAttached == null) {
        convoyAttached = new HashSet<>();
      }
      convoyAttached.add(territory);
    }
  }

  private void setConvoyAttached(final Set<Territory> value) {
    convoyAttached = value;
  }

  public Set<Territory> getConvoyAttached() {
    return getSetProperty(convoyAttached);
  }

  private void resetConvoyAttached() {
    convoyAttached = null;
  }

  public static boolean hasNavalBase(final Territory t) {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    return ta != null && ta.getNavalBase();
  }

  private void setNavalBase(final String value) {
    navalBase = getBool(value);
  }

  private void setNavalBase(final Boolean value) {
    navalBase = value;
  }

  public boolean getNavalBase() {
    return navalBase;
  }

  private void resetNavalBase() {
    navalBase = false;
  }

  public static boolean hasAirBase(final Territory t) {
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    return ta != null && ta.getAirBase();
  }

  private void setAirBase(final String value) {
    airBase = getBool(value);
  }

  private void setAirBase(final Boolean value) {
    airBase = value;
  }

  public boolean getAirBase() {
    return airBase;
  }

  private void resetAirBase() {
    airBase = false;
  }

  private void setKamikazeZone(final String value) {
    kamikazeZone = getBool(value);
  }

  private void setKamikazeZone(final Boolean value) {
    kamikazeZone = value;
  }

  public boolean getKamikazeZone() {
    return kamikazeZone;
  }

  private void resetKamikazeZone() {
    kamikazeZone = false;
  }

  private void setBlockadeZone(final String value) {
    blockadeZone = getBool(value);
  }

  private void setBlockadeZone(final Boolean value) {
    blockadeZone = value;
  }

  public boolean getBlockadeZone() {
    return blockadeZone;
  }

  private void resetBlockadeZone() {
    blockadeZone = false;
  }

  /**
   * Returns the collection of territories that make up the convoy route containing the specified
   * territory or returns an empty collection if the specified territory is not part of a convoy
   * route.
   */
  public static Collection<Territory> getWhatTerritoriesThisIsUsedInConvoysFor(
      final Territory territory, final GameState data) {
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    if (ta == null || !ta.getConvoyRoute()) {
      return new HashSet<>();
    }

    final Collection<Territory> territories = new HashSet<>();
    for (final Territory current : data.getMap().getTerritories()) {
      final TerritoryAttachment cta = TerritoryAttachment.get(current);
      if (cta == null || !cta.getConvoyRoute()) {
        continue;
      }
      if (cta.getConvoyAttached().contains(territory)) {
        territories.add(current);
      }
    }
    return territories;
  }

  /**
   * Returns a string suitable for display describing the territory to which this attachment is
   * attached.
   */
  public String toStringForInfo(final boolean useHtml, final boolean includeAttachedToName) {
    final StringBuilder sb = new StringBuilder();
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
      final GamePlayer owner = t.getOwner();
      if (owner != null && !owner.isNull()) {
        sb.append("Current Owner: ").append(t.getOwner().getName());
        sb.append(br);
      }
      final GamePlayer originalOwner = getOriginalOwner();
      if (originalOwner != null) {
        sb.append("Original Owner: ").append(originalOwner.getName());
        sb.append(br);
      }
    }
    if (isImpassable) {
      sb.append("Is Impassable");
      sb.append(br);
    }
    if (capital != null && capital.length() > 0) {
      sb.append("A Capital of ").append(capital);
      sb.append(br);
    }
    if (victoryCity != 0) {
      sb.append("Is a Victory location");
      sb.append(br);
    }
    if (kamikazeZone) {
      sb.append("Is Kamikaze Zone");
      sb.append(br);
    }
    if (blockadeZone) {
      sb.append("Is a Blockade Zone");
      sb.append(br);
    }
    if (navalBase) {
      sb.append("Is a Naval Base");
      sb.append(br);
    }
    if (airBase) {
      sb.append("Is an Air Base");
      sb.append(br);
    }
    if (convoyRoute) {
      if (!getConvoyAttached().isEmpty()) {
        sb.append("Needs: ").append(MyFormatter.defaultNamedToTextList(convoyAttached)).append(br);
      }
      final Collection<Territory> requiredBy =
          getWhatTerritoriesThisIsUsedInConvoysFor(t, getData());
      if (!requiredBy.isEmpty()) {
        sb.append("Required By: ")
            .append(MyFormatter.defaultNamedToTextList(requiredBy))
            .append(br);
      }
    }
    if (!getChangeUnitOwners().isEmpty()) {
      sb.append("Units May Change Ownership Here");
      sb.append(br);
    }
    if (!getCaptureUnitOnEnteringBy().isEmpty()) {
      sb.append("May Allow The Capture of Some Units");
      sb.append(br);
    }
    if (!getWhenCapturedByGoesTo().isEmpty()) {
      sb.append("Captured By -> Ownership Goes To");
      sb.append(br);
      for (final String value : whenCapturedByGoesTo) {
        final String[] s = splitOnColon(value);
        sb.append(s[0]).append(" -> ").append(s[1]);
        sb.append(br);
      }
    }
    sb.append(br);
    if (!t.isWater()
        && unitProduction > 0
        && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
            getData().getProperties())) {
      sb.append("Base Unit Production: ");
      sb.append(unitProduction);
      sb.append(br);
    }
    if (production > 0 || (resources != null && resources.toString().length() > 0)) {
      sb.append("Production: ");
      sb.append(br);
      if (production > 0) {
        sb.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(production).append(" PUs");
        sb.append(br);
      }
      if (resources != null) {
        if (useHtml) {
          sb.append("&nbsp;&nbsp;&nbsp;&nbsp;")
              .append(
                  resources.toStringForHtml().replaceAll("<br>", "<br>&nbsp;&nbsp;&nbsp;&nbsp;"));
        } else {
          sb.append(resources);
        }
        sb.append(br);
      }
    }
    if (!getTerritoryEffect().isEmpty()) {
      sb.append("Territory Effects: ");
      sb.append(br);
      sb.append(
          territoryEffect.stream()
              .map(TerritoryEffect::getName)
              .map(name -> "&nbsp;&nbsp;&nbsp;&nbsp;" + name + br)
              .collect(Collectors.joining()));
    }
    return sb.toString();
  }

  @Override
  public void validate(final GameState data) {}

  @Override
  public @Nullable MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "capital":
        return MutableProperty.ofString(this::setCapital, this::getCapital, this::resetCapital);
      case "originalFactory":
        return MutableProperty.of(
            this::setOriginalFactory,
            this::setOriginalFactory,
            this::getOriginalFactory,
            this::resetOriginalFactory);
      case "production":
        return MutableProperty.of(
            this::setProduction, this::setProduction, this::getProduction, this::resetProduction);
      case "productionOnly":
        return MutableProperty.ofWriteOnlyString(this::setProductionOnly);
      case "victoryCity":
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt, this::setVictoryCity, this::getVictoryCity, () -> 0);
      case "isImpassable":
        return MutableProperty.of(
            this::setIsImpassable,
            this::setIsImpassable,
            this::getIsImpassable,
            this::resetIsImpassable);
      case "originalOwner":
        return MutableProperty.of(
            this::setOriginalOwner,
            this::setOriginalOwner,
            this::getOriginalOwner,
            this::resetOriginalOwner);
      case "convoyRoute":
        return MutableProperty.of(
            this::setConvoyRoute,
            this::setConvoyRoute,
            this::getConvoyRoute,
            this::resetConvoyRoute);
      case "convoyAttached":
        return MutableProperty.of(
            this::setConvoyAttached,
            this::setConvoyAttached,
            this::getConvoyAttached,
            this::resetConvoyAttached);
      case "changeUnitOwners":
        return MutableProperty.of(
            this::setChangeUnitOwners,
            this::setChangeUnitOwners,
            this::getChangeUnitOwners,
            this::resetChangeUnitOwners);
      case "captureUnitOnEnteringBy":
        return MutableProperty.of(
            this::setCaptureUnitOnEnteringBy,
            this::setCaptureUnitOnEnteringBy,
            this::getCaptureUnitOnEnteringBy,
            this::resetCaptureUnitOnEnteringBy);
      case "navalBase":
        return MutableProperty.of(
            this::setNavalBase, this::setNavalBase, this::getNavalBase, this::resetNavalBase);
      case "airBase":
        return MutableProperty.of(
            this::setAirBase, this::setAirBase, this::getAirBase, this::resetAirBase);
      case "kamikazeZone":
        return MutableProperty.of(
            this::setKamikazeZone,
            this::setKamikazeZone,
            this::getKamikazeZone,
            this::resetKamikazeZone);
      case "unitProduction":
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt, this::setUnitProduction, this::getUnitProduction, () -> 0);
      case "blockadeZone":
        return MutableProperty.of(
            this::setBlockadeZone,
            this::setBlockadeZone,
            this::getBlockadeZone,
            this::resetBlockadeZone);
      case "territoryEffect":
        return MutableProperty.of(
            this::setTerritoryEffect,
            this::setTerritoryEffect,
            this::getTerritoryEffect,
            this::resetTerritoryEffect);
      case "whenCapturedByGoesTo":
        return MutableProperty.of(
            this::setWhenCapturedByGoesTo,
            this::setWhenCapturedByGoesTo,
            this::getWhenCapturedByGoesTo,
            this::resetWhenCapturedByGoesTo);
      case "resources":
        return MutableProperty.of(
            this::setResources, this::setResources, this::getResources, this::resetResources);
      default:
        return null;
    }
  }

  /**
   * Specifies the player that will take ownership of a territory when it is captured by another
   * player.
   */
  @AllArgsConstructor(access = AccessLevel.PACKAGE)
  @EqualsAndHashCode
  @ToString
  public static final class CaptureOwnershipChange {
    public final GamePlayer capturingPlayer;
    public final GamePlayer receivingPlayer;
  }
}
