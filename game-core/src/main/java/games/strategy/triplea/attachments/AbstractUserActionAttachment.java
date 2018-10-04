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
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

/**
 * Abstract class for holding various action/condition things for PoliticalActionAttachment and UserActionAttachment.
 */
public abstract class AbstractUserActionAttachment extends AbstractConditionsAttachment {
  private static final long serialVersionUID = 3569461523853104614L;
  public static final String ATTEMPTS_LEFT_THIS_TURN = "attemptsLeftThisTurn";

  // a key referring to politicaltexts.properties or other .properties for all the UI messages belonging to this action.
  protected String m_text = "";
  /**
   * The cost in PUs to attempt this action.
   *
   * @deprecated Replaced by costResources.
   */
  @Deprecated
  protected int m_costPU = 0;
  // cost in any resources to attempt this action
  protected IntegerMap<Resource> m_costResources = new IntegerMap<>();
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

  protected AbstractUserActionAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Indicates there is no condition to this action or if the condition is satisfied.
   */
  public boolean canPerform(final HashMap<ICondition, Boolean> testedConditions) {
    return m_conditions == null || isSatisfied(testedConditions);
  }

  private void setText(final String text) {
    m_text = text;
  }

  /**
   * Returns the Key that is used in politicstext.properties or other .properties for all the texts.
   */
  public String getText() {
    return m_text;
  }

  private void resetText() {
    m_text = "";
  }

  @Deprecated
  private void setCostPu(final String s) {
    setCostPu(getInt(s));
  }

  @Deprecated
  public void setCostPu(final Integer s) {
    final Resource r = getData().getResourceList().getResource(Constants.PUS);
    m_costResources.put(r, s);
  }

  @Deprecated
  public int getCostPu() {
    return m_costPU;
  }

  @Deprecated
  private void resetCostPu() {
    m_costPU = 0;
  }

  private void setCostResources(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length <= 0 || s.length > 2) {
      throw new GameParseException(
          "costResources cannot be empty or have more than two fields: " + value + thisErrorMsg());
    }
    final String resourceToProduce = s[1];
    final Resource r = getData().getResourceList().getResource(resourceToProduce);
    if (r == null) {
      throw new GameParseException("costResources: No resource called: " + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    m_costResources.put(r, n);
  }

  public void setCostResources(final IntegerMap<Resource> value) {
    m_costResources = value;
  }

  public IntegerMap<Resource> getCostResources() {
    return m_costResources;
  }

  private void resetCostResources() {
    m_costResources = new IntegerMap<>();
  }

  private void setActionAccept(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_actionAccept.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setActionAccept(final List<PlayerID> value) {
    m_actionAccept = value;
  }

  /**
   * Returns a list of players that must accept this action before it takes effect.
   */
  public List<PlayerID> getActionAccept() {
    return m_actionAccept;
  }

  private void resetActionAccept() {
    m_actionAccept = new ArrayList<>();
  }

  /**
   * Sets the amount of times you can try this Action per Round.
   *
   * @param s the amount of times you can try this Action per Round.
   */
  private void setAttemptsPerTurn(final String s) {
    m_attemptsPerTurn = getInt(s);
    setAttemptsLeftThisTurn(m_attemptsPerTurn);
  }

  private void setAttemptsPerTurn(final Integer s) {
    m_attemptsPerTurn = s;
    setAttemptsLeftThisTurn(m_attemptsPerTurn);
  }

  /**
   * Returns the amount of times you can try this Action per Round.
   */
  private int getAttemptsPerTurn() {
    return m_attemptsPerTurn;
  }

  private void resetAttemptsPerTurn() {
    m_attemptsPerTurn = 1;
  }

  private void setAttemptsLeftThisTurn(final int attempts) {
    m_attemptsLeftThisTurn = attempts;
  }

  private void setAttemptsLeftThisTurn(final String attempts) {
    setAttemptsLeftThisTurn(getInt(attempts));
  }

  private int getAttemptsLeftThisTurn() {
    return m_attemptsLeftThisTurn;
  }

  private void resetAttemptsLeftThisTurn() {
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
            MutableProperty.of(
                this::setCostPu,
                this::setCostPu,
                this::getCostPu,
                this::resetCostPu))
        .put("costResources",
            MutableProperty.of(
                this::setCostResources,
                this::setCostResources,
                this::getCostResources,
                this::resetCostResources))
        .put("attemptsPerTurn",
            MutableProperty.of(
                this::setAttemptsPerTurn,
                this::setAttemptsPerTurn,
                this::getAttemptsPerTurn,
                this::resetAttemptsPerTurn))
        .put("attemptsLeftThisTurn",
            MutableProperty.of(
                this::setAttemptsLeftThisTurn,
                this::setAttemptsLeftThisTurn,
                this::getAttemptsLeftThisTurn,
                this::resetAttemptsLeftThisTurn))
        .put("actionAccept",
            MutableProperty.of(
                this::setActionAccept,
                this::setActionAccept,
                this::getActionAccept,
                this::resetActionAccept))
        .build();
  }
}
