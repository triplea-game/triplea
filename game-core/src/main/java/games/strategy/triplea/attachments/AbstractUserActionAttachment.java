package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;

/**
 * Abstract class for holding various action/condition things for PoliticalActionAttachment and UserActionAttachment.
 */
public abstract class AbstractUserActionAttachment extends AbstractConditionsAttachment {
  private static final long serialVersionUID = 3569461523853104614L;
  public static final String ATTEMPTS_LEFT_THIS_TURN = "attemptsLeftThisTurn";


  public AbstractUserActionAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  // a key referring to politicaltexts.properties or other .properties for all the UI messages belonging to this action.
  protected String m_text = "";
  // cost in PU to attempt this action
  protected int m_costPU = 0;
  // how many times can you perform this action each round?
  protected int m_attemptsPerTurn = 1;
  // how many times are left to perform this action each round?
  @InternalDoNotExport
  // Do Not Export (do not include in IAttachment).
  protected int m_attemptsLeftThisTurn = 1;
  // which players should accept this action? this could be the player who is the target of this action in the case of
  // proposing a treaty or
  // the players in your 'alliance' in case you want to declare war...
  // especially for actions such as when france declares war on germany and it automatically causes UK to declare war as
  // well. it is good to
  // set "actionAccept" to "UK" so UK can accept this action to go through.
  protected List<PlayerID> m_actionAccept = new ArrayList<>();

  /**
   * @return true if there is no condition to this action or if the condition is satisfied.
   */
  public boolean canPerform(final HashMap<ICondition, Boolean> testedConditions) {
    return m_conditions == null || isSatisfied(testedConditions);
  }

  /**
   * @param text
   *        the Key that is used in politicstext.properties or other .properties for all the texts
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setText(final String text) {
    m_text = text;
  }

  /**
   * @return The Key that is used in politicstext.properties or other .properties for all the texts.
   */
  public String getText() {
    return m_text;
  }

  public void resetText() {
    m_text = "";
  }

  /**
   * @param s
   *        the amount you need to pay to perform the action.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCostPU(final String s) {
    m_costPU = getInt(s);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCostPU(final Integer s) {
    m_costPU = s;
  }

  /**
   * @return The amount you need to pay to perform the action.
   */
  public int getCostPU() {
    return m_costPU;
  }

  public void resetCostPU() {
    m_costPU = 0;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setActionAccept(final String value) throws GameParseException {
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_actionAccept.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setActionAccept(final List<PlayerID> value) {
    m_actionAccept = value;
  }

  /**
   * @return a list of players that must accept this action before it takes effect.
   */
  public List<PlayerID> getActionAccept() {
    return m_actionAccept;
  }

  public void clearActionAccept() {
    m_actionAccept.clear();
  }

  public void resetActionAccept() {
    m_actionAccept = new ArrayList<>();
  }

  /**
   * @param s
   *        the amount of times you can try this Action per Round.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttemptsPerTurn(final String s) {
    m_attemptsPerTurn = getInt(s);
    setAttemptsLeftThisTurn(m_attemptsPerTurn);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttemptsPerTurn(final Integer s) {
    m_attemptsPerTurn = s;
    setAttemptsLeftThisTurn(m_attemptsPerTurn);
  }

  /**
   * @return The amount of times you can try this Action per Round.
   */
  public int getAttemptsPerTurn() {
    return m_attemptsPerTurn;
  }

  public void resetAttemptsPerTurn() {
    m_attemptsPerTurn = 1;
  }

  /**
   * @param attempts
   *        left this turn.
   */
  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setAttemptsLeftThisTurn(final int attempts) {
    m_attemptsLeftThisTurn = attempts;
  }

  @GameProperty(xmlProperty = false, gameProperty = true, adds = false)
  public void setAttemptsLeftThisTurn(final String attempts) {
    setAttemptsLeftThisTurn(getInt(attempts));
  }

  /**
   * @return attempts that are left this turn.
   */
  public int getAttemptsLeftThisTurn() {
    return m_attemptsLeftThisTurn;
  }

  public void resetAttemptsLeftThisTurn() {
    m_attemptsLeftThisTurn = 1;
  }

  public void resetAttempts(final IDelegateBridge bridge) {
    if (m_attemptsLeftThisTurn != m_attemptsPerTurn) {
      bridge.addChange(ChangeFactory.attachmentPropertyChange(this, m_attemptsPerTurn, ATTEMPTS_LEFT_THIS_TURN));
    }
  }

  public void useAttempt(final IDelegateBridge bridge) {
    bridge
        .addChange(ChangeFactory.attachmentPropertyChange(this, (m_attemptsLeftThisTurn - 1), ATTEMPTS_LEFT_THIS_TURN));
  }

  public boolean hasAttemptsLeft() {
    return m_attemptsLeftThisTurn > 0;
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
    if (m_text.trim().length() <= 0) {
      throw new GameParseException("value: text can't be empty" + thisErrorMsg());
    }
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .put("text",
            MutableProperty.ofString(
                this::setText,
                this::getText,
                this::resetText))
        .put("costPU",
            MutableProperty.ofInteger(
                this::setCostPU,
                this::setCostPU,
                this::getCostPU,
                this::resetCostPU))
        .put("attemptsPerTurn",
            MutableProperty.ofInteger(
                this::setAttemptsPerTurn,
                this::setAttemptsPerTurn,
                this::getAttemptsPerTurn,
                this::resetAttemptsPerTurn))
        .put("attemptsLeftThisTurn",
            MutableProperty.ofInteger(
                this::setAttemptsLeftThisTurn,
                this::setAttemptsLeftThisTurn,
                this::getAttemptsLeftThisTurn,
                this::resetAttemptsLeftThisTurn))
        .put("actionAccept",
            MutableProperty.of(
                List.class,
                this::setActionAccept,
                this::setActionAccept,
                this::getActionAccept,
                this::resetActionAccept))
        .build();
  }
}
