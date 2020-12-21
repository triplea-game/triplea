package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Splitter;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/** A game player (nation, power, etc.). */
public class GamePlayer extends NamedAttachable implements NamedUnitHolder {
  private static final long serialVersionUID = -2284878450555315947L;

  private static final String DEFAULT_TYPE_AI = "AI";
  private static final String DEFAULT_TYPE_DOES_NOTHING = "DoesNothing";

  public static final GamePlayer NULL_PLAYERID =
      new GamePlayer(Constants.PLAYER_NAME_NEUTRAL, true, false, null, false, null) {
        private static final long serialVersionUID = -6596127754502509049L;

        @Override
        public boolean isNull() {
          return true;
        }
      };

  private final boolean optional;
  private final boolean canBeDisabled;
  private final String defaultType;
  private final boolean isHidden;
  private boolean isDisabled = false;
  private final UnitCollection unitsHeld;
  private final ResourceCollection resources;
  private ProductionFrontier productionFrontier;
  private RepairFrontier repairFrontier;
  private final TechnologyFrontierList technologyFrontiers;
  private String whoAmI = "null:no_one";

  public GamePlayer(final String name, final GameData data) {
    this(name, false, false, null, false, data);
  }

  public GamePlayer(
      final String name,
      final boolean optional,
      final boolean canBeDisabled,
      final String defaultType,
      final boolean isHidden,
      final GameData data) {
    super(name, data);
    this.optional = optional;
    this.canBeDisabled = canBeDisabled;
    this.defaultType = defaultType;
    this.isHidden = isHidden;
    unitsHeld = new UnitCollection(this, data);
    resources = new ResourceCollection(data);
    technologyFrontiers = new TechnologyFrontierList(data);
  }

  public boolean getOptional() {
    return optional;
  }

  public boolean getCanBeDisabled() {
    return canBeDisabled;
  }

  public boolean isHidden() {
    return isHidden;
  }

  @Override
  public UnitCollection getUnitCollection() {
    return unitsHeld;
  }

  public ResourceCollection getResources() {
    return resources;
  }

  public TechnologyFrontierList getTechnologyFrontierList() {
    return technologyFrontiers;
  }

  public void setProductionFrontier(final ProductionFrontier frontier) {
    productionFrontier = frontier;
  }

  public ProductionFrontier getProductionFrontier() {
    return productionFrontier;
  }

  public void setRepairFrontier(final RepairFrontier frontier) {
    repairFrontier = frontier;
  }

  public RepairFrontier getRepairFrontier() {
    return repairFrontier;
  }

  @Override
  public void notifyChanged() {}

  public boolean isNull() {
    return false;
  }

  @Override
  public String toString() {
    return "PlayerId named:" + getName();
  }

  @Override
  public String getType() {
    return UnitHolder.PLAYER;
  }

  /**
   * First string is "Human" or "AI" or "null" (case insensitive), while second string is the name
   * of the player, separated with a colon. For example, it could be "AI:Hard (AI)".
   *
   * @throws IllegalArgumentException If {@code encodedType} does not contain two strings separated
   *     by a colon; or if the first string is not one of "AI", "Human", or "null" (case
   *     insensitive).
   */
  public void setWhoAmI(final String encodedType) {
    final List<String> tokens = tokenizeEncodedType(encodedType);
    checkArgument(
        tokens.size() == 2,
        "whoAmI '" + encodedType + "' must have two strings, separated by a colon");
    final String typeId = tokens.get(0);
    checkArgument(
        "AI".equalsIgnoreCase(typeId)
            || "Human".equalsIgnoreCase(typeId)
            || "null".equalsIgnoreCase(typeId),
        "whoAmI '" + encodedType + "' first part must be, ai or human or null");
    whoAmI = encodedType;
  }

  private static List<String> tokenizeEncodedType(final String encodedType) {
    return Splitter.on(':').splitToList(encodedType);
  }

  public String getWhoAmI() {
    return whoAmI;
  }

  public Type getPlayerType() {
    final List<String> tokens = tokenizeEncodedType(whoAmI);
    return new Type(tokens.get(0), tokens.get(1));
  }

  public boolean isAi() {
    return "AI".equalsIgnoreCase(getPlayerType().id);
  }

  public void setIsDisabled(final boolean isDisabled) {
    this.isDisabled = isDisabled;
  }

  public boolean getIsDisabled() {
    return isDisabled;
  }

  /**
   * If I have no units with movement, And I own zero factories or have have no owned land, then I
   * am basically dead, and therefore should not participate in things like politics.
   */
  public boolean amNotDeadYet(final GameDataInjections data) {
    for (final Territory t : data.getMap().getTerritories()) {
      if (t.getUnitCollection()
          .anyMatch(
              Matches.unitIsOwnedBy(this)
                  .and(Matches.unitHasAttackValueOfAtLeast(1))
                  .and(Matches.unitCanMove())
                  .and(Matches.unitIsLand()))) {
        return true;
      }
      if (t.getOwner().equals(this)
          && t.getUnitCollection()
              .anyMatch(Matches.unitIsOwnedBy(this).and(Matches.unitCanProduceUnits()))) {
        return true;
      }
    }
    return false;
  }

  public static Map<String, String> currentPlayers(final GameDataInjections data) {
    final LinkedHashMap<String, String> currentPlayers = new LinkedHashMap<>();
    if (data == null) {
      return currentPlayers;
    }
    for (final GamePlayer player : data.getPlayerList().getPlayers()) {
      currentPlayers.put(player.getName(), player.getPlayerType().name);
    }
    return currentPlayers;
  }

  public static Optional<GamePlayer> asOptional(@Nullable final GamePlayer player) {
    if (player == null || player.isNull()) {
      return Optional.empty();
    }
    return Optional.of(player);
  }

  public boolean isDefaultTypeAi() {
    return DEFAULT_TYPE_AI.equals(defaultType);
  }

  public boolean isDefaultTypeDoesNothing() {
    return DEFAULT_TYPE_DOES_NOTHING.equals(defaultType);
  }

  public RulesAttachment getRulesAttachment() {
    return (RulesAttachment) getAttachment(Constants.RULES_ATTACHMENT_NAME);
  }

  public PlayerAttachment getPlayerAttachment() {
    return (PlayerAttachment) getAttachment(Constants.PLAYER_ATTACHMENT_NAME);
  }

  public TechAttachment getTechAttachment() {
    return (TechAttachment) getAttachment(Constants.TECH_ATTACHMENT_NAME);
  }

  /** A player type (e.g. human, AI). */
  @AllArgsConstructor(access = AccessLevel.PACKAGE)
  @EqualsAndHashCode
  public static final class Type {
    /**
     * The type identifier. One of "AI", "Human", or "null". Case is not guaranteed, so comparisons
     * should be case-insensitive.
     */
    public final String id;

    /** The display name of the type (e.g. "Hard (AI)"). */
    public final String name;
  }
}
