package games.strategy.engine.data;

import games.strategy.engine.delegate.IDelegate;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nullable;
import lombok.Getter;

/**
 * A single step in a game.
 *
 * <p>Typically turn based strategy games are composed of a set of distinct phases (in chess this
 * would be two, white move, black move).
 */
public class GameStep extends GameDataComponent {
  private static final long serialVersionUID = -7944468945162840931L;

  @Getter @Nullable private final String name;
  @Nullable private final String displayName;
  @Nullable private final GamePlayer player;
  @Getter private final String delegateName;
  private int runCount = 0;
  @Getter private int maxRunCount = -1;

  /**
   * -- GETTER -- Returns the properties of the game step. Allowed Properties so far:<br>
   * EndTurn delegates -> skipPosting = true/false<br>
   * EndTurn delegates -> turnSummaryPlayers = colon separated list of players for this turn summary
   * <br>
   * Move delegates -> airborneMove = true/false<br>
   * Move delegates -> combatMove = true/false<br>
   * Move delegates -> nonCombatMove = true/false<br>
   * Move delegates -> fireRocketsAfter = true/false<br>
   * Move & EndTurn delegates -> repairUnits = true/false<br>
   * Move delegates -> giveBonusMovement = true/false<br>
   * Move & Place delegates -> removeAirThatCanNotLand = true/false<br>
   * Move delegates -> resetUnitStateAtStart = true/false<br>
   * Move delegates -> resetUnitStateAtEnd = true/false<br>
   * Purchase & Place delegates -> bid = true/false<br>
   * Purchase delegates -> repairPlayers = colon separated list of players which you can repair for
   * <br>
   * Move delegates -> combinedTurns = colon separated list of players which have intermeshed phases
   * <br>
   */
  @Getter private final Properties properties;

  /**
   * The keys for all supported game step properties.
   *
   * @see GameStep#getProperties()
   */
  public interface PropertyKeys {
    String SKIP_POSTING = "skipPosting";
    String TURN_SUMMARY_PLAYERS = "turnSummaryPlayers";
    String AIRBORNE_MOVE = "airborneMove";
    String COMBAT_MOVE = "combatMove";
    String NON_COMBAT_MOVE = "nonCombatMove";
    String FIRE_ROCKETS = "fireRockets";
    String REPAIR_UNITS = "repairUnits";
    String GIVE_BONUS_MOVEMENT = "giveBonusMovement";
    String REMOVE_AIR_THAT_CAN_NOT_LAND = "removeAirThatCanNotLand";
    String RESET_UNIT_STATE_AT_START = "resetUnitStateAtStart";
    String RESET_UNIT_STATE_AT_END = "resetUnitStateAtEnd";
    String BID = "bid";
    String COMBINED_TURNS = "combinedTurns";
    String REPAIR_PLAYERS = "repairPlayers";
    String ONLY_REPAIR_IF_DISABLED = "onlyRepairIfDisabled";
  }

  /**
   * Creates new GameStep.
   *
   * @param name name of the game step
   * @param displayName name that gets displayed
   * @param player player who executes the game step
   * @param delegate delegate for the game step
   * @param data game data
   * @param stepProperties properties of the game step
   */
  public GameStep(
      final String name,
      final String displayName,
      final GamePlayer player,
      final IDelegate delegate,
      final GameData data,
      final Properties stepProperties) {
    super(data);
    this.name = name;
    this.displayName = displayName;
    this.player = player;
    delegateName = delegate.getName();
    properties = stepProperties;
  }

  public @Nullable GamePlayer getPlayerId() {
    return player;
  }

  public IDelegate getDelegate() {
    return getData().getDelegate(delegateName);
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof GameStep)) {
      return false;
    }
    final GameStep other = (GameStep) o;
    return other.name.equals(this.name)
        && other.delegateName.equals(this.delegateName)
        && other.player.equals(this.player);
  }

  public boolean hasReachedMaxRunCount() {
    return maxRunCount != -1 && maxRunCount <= runCount;
  }

  public void incrementRunCount() {
    runCount++;
  }

  public void setMaxRunCount(final int count) {
    maxRunCount = count;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, delegateName, player);
  }

  public String getDisplayName() {
    if (displayName == null) {
      IDelegate delegate = getDelegate();
      return delegate != null ? delegate.getDisplayName() : delegateName;
    }
    return displayName;
  }

  @Override
  public String toString() {
    return "GameStep:"
        + name
        + " delegate:"
        + delegateName
        + " player:"
        + player
        + " displayName:"
        + displayName;
  }

  /** Utility method to determine if the current game step is a 'nonCombat' move phase. */
  public boolean isNonCombat() {
    return Optional.ofNullable(properties.getProperty(GameStep.PropertyKeys.NON_COMBAT_MOVE))
            .map(Boolean::parseBoolean)
            .orElse(false)
        || GameStep.isNonCombatMoveStep(name);
  }

  public static boolean isTechStep(final String stepName) {
    return stepName.endsWith("Tech");
  }

  public static boolean isMoveStep(final String stepName) {
    return stepName.endsWith("Move");
  }

  public static boolean isNonCombatMoveStep(final String stepName) {
    return stepName.endsWith("NonCombatMove");
  }

  public static boolean isCombatMoveStep(final String stepName) {
    // NonCombatMove endsWith CombatMove so check for NCM first.
    return !isNonCombatMoveStep(stepName) && stepName.endsWith("CombatMove");
  }

  public static boolean isAirborneCombatMoveStep(final String stepName) {
    return stepName.endsWith("AirborneCombatMove");
  }

  public static boolean isBattleStep(final String stepName) {
    return stepName.endsWith("Battle");
  }

  public static boolean isPoliticsStep(final String stepName) {
    return stepName.endsWith("Politics");
  }

  public static boolean isUserActionsStep(final String stepName) {
    return stepName.endsWith("UserActions");
  }

  public static boolean isEndTurnStep(final String stepName) {
    return stepName.endsWith("EndTurn");
  }

  public static boolean isPurchaseOrBidStep(final String stepName) {
    return isBidStep(stepName) || isPurchaseStep(stepName);
  }

  public static boolean isPurchaseStep(final String stepName) {
    return stepName != null && stepName.endsWith("Purchase");
  }

  public static boolean isBidStep(final String stepName) {
    return stepName != null && stepName.endsWith("Bid");
  }

  public static boolean isBidPlaceStep(final String stepName) {
    return stepName.endsWith("BidPlace");
  }

  public static boolean isPlaceStep(final String stepName) {
    return stepName != null && stepName.endsWith("Place");
  }

  public static boolean isTechActivationStep(final String stepName) {
    return stepName.endsWith("TechActivation");
  }
}
