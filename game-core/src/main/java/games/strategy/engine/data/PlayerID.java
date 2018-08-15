package games.strategy.engine.data;

import java.util.LinkedHashMap;
import java.util.List;

import com.google.common.base.Splitter;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Tuple;

/**
 * A game player (nation, power, etc.).
 */
public class PlayerID extends NamedAttachable implements NamedUnitHolder {
  private static final long serialVersionUID = -2284878450555315947L;

  private static final String DEFAULT_TYPE_AI = "AI";
  private static final String DEFAULT_TYPE_DOES_NOTHING = "DoesNothing";

  private final boolean m_optional;
  private final boolean m_canBeDisabled;
  private final String defaultType;
  private final boolean isHidden;
  private boolean m_isDisabled = false;
  private final UnitCollection m_unitsHeld;
  private final ResourceCollection m_resources;
  private ProductionFrontier m_productionFrontier;
  private RepairFrontier m_repairFrontier;
  private final TechnologyFrontierList m_technologyFrontiers;
  private String m_whoAmI = "null:no_one";

  public PlayerID(final String name, final GameData data) {
    this(name, false, false, null, false, data);
  }

  public PlayerID(final String name, final boolean optional, final boolean canBeDisabled, final String defaultType,
      final boolean isHidden, final GameData data) {
    super(name, data);
    m_optional = optional;
    m_canBeDisabled = canBeDisabled;
    this.defaultType = defaultType;
    this.isHidden = isHidden;
    m_unitsHeld = new UnitCollection(this, data);
    m_resources = new ResourceCollection(data);
    m_technologyFrontiers = new TechnologyFrontierList(data);
  }

  public boolean getOptional() {
    return m_optional;
  }

  public boolean getCanBeDisabled() {
    return m_canBeDisabled;
  }

  public String getDefaultType() {
    return defaultType;
  }

  public boolean isHidden() {
    return isHidden;
  }

  @Override
  public UnitCollection getUnits() {
    return m_unitsHeld;
  }

  public ResourceCollection getResources() {
    return m_resources;
  }

  public TechnologyFrontierList getTechnologyFrontierList() {
    return m_technologyFrontiers;
  }

  public void setProductionFrontier(final ProductionFrontier frontier) {
    m_productionFrontier = frontier;
  }

  public ProductionFrontier getProductionFrontier() {
    return m_productionFrontier;
  }

  public void setRepairFrontier(final RepairFrontier frontier) {
    m_repairFrontier = frontier;
  }

  public RepairFrontier getRepairFrontier() {
    return m_repairFrontier;
  }

  @Override
  public void notifyChanged() {}

  public boolean isNull() {
    return false;
  }

  public static final PlayerID NULL_PLAYERID =
      new PlayerID(Constants.PLAYER_NAME_NEUTRAL, true, false, null, false, null) {
        // compatible with 0.9.0.2 saved games
        private static final long serialVersionUID = -6596127754502509049L;

        @Override
        public boolean isNull() {
          return true;
        }
      };

  @Override
  public String toString() {
    return "PlayerID named:" + getName();
  }

  @Override
  public String getType() {
    return UnitHolder.PLAYER;
  }

  /**
   * First string is "Human" or "AI" or "null" (case insensitive), while second string is the name of the player;
   * separated with a colon. For example, it could be {@code "AI:Hard (AI)"}.
   */
  public void setWhoAmI(final String encodedTypeAndName) {
    final List<String> tokens = tokenizeTypeAndName(encodedTypeAndName);
    if (tokens.size() != 2) {
      throw new IllegalStateException(String.format("whoAmI '%s' must have two strings, separated by a colon",
          encodedTypeAndName));
    }
    final String type = tokens.get(0);
    if (!("AI".equalsIgnoreCase(type) || "Human".equalsIgnoreCase(type) || "null".equalsIgnoreCase(type))) {
      throw new IllegalStateException("whoAmI first part must be, ai or human or null");
    }
    m_whoAmI = encodedTypeAndName;
  }

  private static List<String> tokenizeTypeAndName(final String encodedTypeAndName) {
    return Splitter.on(':').splitToList(encodedTypeAndName);
  }

  public String getWhoAmI() {
    return m_whoAmI;
  }

  /**
   * Returns a tuple where the first element is the player type and the second element is the player name. The player
   * type will be one of the strings {@code "AI"}, {@code "Human"}, or {@code "null"}, but there is no guarantee of
   * case, so comparisons should be done in a case-insensitive manner.
   */
  public Tuple<String, String> getTypeAndName() {
    final List<String> tokens = tokenizeTypeAndName(m_whoAmI);
    assert tokens.size() == 2;
    return Tuple.of(tokens.get(0), tokens.get(1));
  }

  public boolean isAi() {
    return "AI".equalsIgnoreCase(getTypeAndName().getFirst());
  }

  public void setIsDisabled(final boolean isDisabled) {
    m_isDisabled = isDisabled;
  }

  public boolean getIsDisabled() {
    return m_isDisabled;
  }

  /**
   * If I have no units with movement,
   * And I own zero factories or have have no owned land,
   * then I am basically dead, and therefore should not participate in things like politics.
   */
  public boolean amNotDeadYet(final GameData data) {
    for (final Territory t : data.getMap().getTerritories()) {
      if (t.getUnits().anyMatch(Matches.unitIsOwnedBy(this)
          .and(Matches.unitHasAttackValueOfAtLeast(1))
          .and(Matches.unitCanMove())
          .and(Matches.unitIsLand()))) {
        return true;
      }
      if (t.getOwner().equals(this)
          && t.getUnits().anyMatch(Matches.unitIsOwnedBy(this).and(Matches.unitCanProduceUnits()))) {
        return true;
      }
    }
    return false;
  }

  public static LinkedHashMap<String, String> currentPlayers(final GameData data) {
    final LinkedHashMap<String, String> currentPlayers = new LinkedHashMap<>();
    if (data == null) {
      return currentPlayers;
    }
    for (final PlayerID player : data.getPlayerList().getPlayers()) {
      currentPlayers.put(player.getName(), player.getTypeAndName().getSecond());
    }
    return currentPlayers;
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
}
