package games.strategy.engine.data;

import java.util.Objects;
import java.util.Properties;

import games.strategy.engine.delegate.IDelegate;

/**
 * A single step in a game.
 *
 * <p>
 * Typically turn based strategy games are composed of a set of distinct phases (in chess this would be two, white move,
 * black move).
 * </p>
 */
public class GameStep extends GameDataComponent {
  private static final long serialVersionUID = -7944468945162840931L;
  private final String m_name;
  private final String m_displayName;
  private final PlayerID m_player;
  private final String m_delegate;
  private int m_runCount = 0;
  private int m_maxRunCount = -1;
  private final Properties m_properties;

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
  }

  /**
   * Creates new GameStep.
   *
   * @param name
   *        name of the game step
   * @param displayName
   *        name that gets displayed
   * @param player
   *        player who executes the game step
   * @param delegate
   *        delegate for the game step
   * @param data
   *        game data
   * @param stepProperties
   *        properties of the game step
   */
  public GameStep(final String name, final String displayName, final PlayerID player, final IDelegate delegate,
      final GameData data, final Properties stepProperties) {
    super(data);
    m_name = name;
    m_displayName = displayName;
    m_player = player;
    m_delegate = delegate.getName();
    m_properties = stepProperties;
  }

  public String getName() {
    return m_name;
  }

  public PlayerID getPlayerId() {
    return m_player;
  }

  public IDelegate getDelegate() {
    return getData().getDelegateList().getDelegate(m_delegate);
  }

  @Override
  public boolean equals(final Object o) {
    if ((o == null) || !(o instanceof GameStep)) {
      return false;
    }
    final GameStep other = (GameStep) o;
    return other.m_name.equals(this.m_name) && other.m_delegate.equals(this.m_delegate)
        && other.m_player.equals(this.m_player);
  }

  public boolean hasReachedMaxRunCount() {
    if (m_maxRunCount == -1) {
      return false;
    }
    return m_maxRunCount <= m_runCount;
  }

  public int getRunCount() {
    return m_runCount;
  }

  public void incrementRunCount() {
    m_runCount++;
  }

  public void setMaxRunCount(final int count) {
    m_maxRunCount = count;
  }

  public int getMaxRunCount() {
    return m_maxRunCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_name, m_delegate, m_player);
  }

  public String getDisplayName() {
    if (m_displayName == null) {
      return getDelegate().getDisplayName();
    }
    return m_displayName;
  }

  /**
   * Returns the properties of the game step.
   * Allowed Properties so far:<br>
   * EndTurn delegates -> skipPosting = true/false<br>
   * EndTurn delegates -> turnSummaryPlayers = colon separated list of players for this turn summary<br>
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
   * Purchase delegates -> repairPlayers = colon separated list of players which you can repair for<br>
   * Move delegates -> combinedTurns = colon separated list of players which have intermeshed phases<br>
   */
  public Properties getProperties() {
    return m_properties;
  }

  @Override
  public String toString() {
    return "GameStep:" + m_name + " delegate:" + m_delegate + " player:" + m_player + " displayName:" + m_displayName;
  }
}
