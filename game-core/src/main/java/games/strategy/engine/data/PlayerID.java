package games.strategy.engine.data;

import java.util.LinkedHashMap;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.Matches;

public class PlayerID extends NamedAttachable implements NamedUnitHolder {
  private static final long serialVersionUID = -2284878450555315947L;
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

  /** Creates new PlayerID. */
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
   * First string is "Human" or "AI", while second string is the name of the player. Separated with a colon.
   */
  public void setWhoAmI(final String encodedPlayerTypeAndName) {
    // so for example, it should be "AI:Hard (AI)"
    final String[] s = encodedPlayerTypeAndName.split(":");
    if (s.length != 2) {
      throw new IllegalStateException("whoAmI must have two strings, separated by a colon");
    }
    if (!(s[0].equalsIgnoreCase("AI") || s[0].equalsIgnoreCase("Human") || s[0].equalsIgnoreCase("null"))) {
      throw new IllegalStateException("whoAmI first part must be, ai or human or client");
    }
    m_whoAmI = encodedPlayerTypeAndName;
  }

  public String getWhoAmI() {
    return m_whoAmI;
  }

  public boolean isAi() {
    return m_whoAmI.split(":")[0].equalsIgnoreCase("AI");
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
      currentPlayers.put(player.getName(), player.getWhoAmI().split(":")[1]);
    }
    return currentPlayers;
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
