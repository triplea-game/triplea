package games.strategy.triplea.attachments;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.triplea.java.collections.IntegerMap;

/**
 * Abstract class for holding various action/condition things for PoliticalActionAttachment and
 * UserActionAttachment.
 */
public abstract class AbstractUserActionAttachment extends AbstractConditionsAttachment {
  public static final String ATTEMPTS_LEFT_THIS_TURN = "attemptsLeftThisTurn";
  private static final long serialVersionUID = 3569461523853104614L;

  // a key referring to politicaltexts.properties or other .properties for all the UI messages
  // belonging to this action.
  protected String text = "";
  /**
   * The cost in PUs to attempt this action.
   *
   * @deprecated Replaced by costResources. The value is here only for backward compatibility with
   *     possibly old map downloads that still have this value.
   */
  @Deprecated protected int costPu = 0;
  // cost in any resources to attempt this action
  protected IntegerMap<Resource> costResources = new IntegerMap<>();
  // how many times can you perform this action each round?
  protected int attemptsPerTurn = 1;
  // how many times are left to perform this action each round?
  protected int attemptsLeftThisTurn = 1;
  // which players should accept this action? this could be the player who is the target of this
  // action in the case of
  // proposing a treaty or the players in your 'alliance' in case you want to declare war...
  // especially for actions such as when france declares war on germany and it automatically causes
  // UK to declare war as
  // well. it is good to set "actionAccept" to "UK" so UK can accept this action to go through.
  protected List<GamePlayer> actionAccept = new ArrayList<>();

  protected AbstractUserActionAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /** Indicates there is no condition to this action or if the condition is satisfied. */
  public boolean canPerform(final Map<ICondition, Boolean> testedConditions) {
    return conditions == null || isSatisfied(testedConditions);
  }

  private void setText(final String text) {
    this.text = text;
  }

  /**
   * Returns the Key that is used in politicstext.properties or other .properties for all the texts.
   */
  public String getText() {
    return text;
  }

  private void resetText() {
    text = "";
  }

  @Deprecated
  private void setCostPu(final String s) {
    setCostPu(getInt(s));
  }

  @Deprecated
  public void setCostPu(final Integer s) {
    final Resource r = getData().getResourceList().getResource(Constants.PUS);
    costResources.put(r, s);
  }

  @Deprecated
  public int getCostPu() {
    return costPu;
  }

  @Deprecated
  private void resetCostPu() {
    costPu = 0;
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
      throw new GameParseException(
          "costResources: No resource called: " + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    costResources.put(r, n);
  }

  public void setCostResources(final IntegerMap<Resource> value) {
    costResources = value;
  }

  public IntegerMap<Resource> getCostResources() {
    return costResources;
  }

  private void resetCostResources() {
    costResources = new IntegerMap<>();
  }

  private void setActionAccept(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final GamePlayer tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        actionAccept.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setActionAccept(final List<GamePlayer> value) {
    actionAccept = value;
  }

  /** Returns a list of players that must accept this action before it takes effect. */
  public List<GamePlayer> getActionAccept() {
    return actionAccept;
  }

  private void resetActionAccept() {
    actionAccept = new ArrayList<>();
  }

  /**
   * Sets the amount of times you can try this Action per Round.
   *
   * @param s the amount of times you can try this Action per Round.
   */
  private void setAttemptsPerTurn(final String s) {
    attemptsPerTurn = getInt(s);
    setAttemptsLeftThisTurn(attemptsPerTurn);
  }

  private void setAttemptsPerTurn(final Integer s) {
    attemptsPerTurn = s;
    setAttemptsLeftThisTurn(attemptsPerTurn);
  }

  /** Returns the amount of times you can try this Action per Round. */
  private int getAttemptsPerTurn() {
    return attemptsPerTurn;
  }

  private void resetAttemptsPerTurn() {
    attemptsPerTurn = 1;
  }

  private void setAttemptsLeftThisTurn(final int attempts) {
    attemptsLeftThisTurn = attempts;
  }

  private void setAttemptsLeftThisTurn(final String attempts) {
    setAttemptsLeftThisTurn(getInt(attempts));
  }

  private int getAttemptsLeftThisTurn() {
    return attemptsLeftThisTurn;
  }

  private void resetAttemptsLeftThisTurn() {
    attemptsLeftThisTurn = 1;
  }

  public void resetAttempts(final IDelegateBridge bridge) {
    if (attemptsLeftThisTurn != attemptsPerTurn) {
      bridge.addChange(
          ChangeFactory.attachmentPropertyChange(this, attemptsPerTurn, ATTEMPTS_LEFT_THIS_TURN));
    }
  }

  public void useAttempt(final IDelegateBridge bridge) {
    bridge.addChange(
        ChangeFactory.attachmentPropertyChange(
            this, (attemptsLeftThisTurn - 1), ATTEMPTS_LEFT_THIS_TURN));
  }

  public boolean hasAttemptsLeft() {
    return attemptsLeftThisTurn > 0;
  }

  @Override
  public void validate(final GameDataInjections data) throws GameParseException {
    if (text.trim().length() <= 0) {
      throw new GameParseException("value: text can't be empty" + thisErrorMsg());
    }
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .put("text", MutableProperty.ofString(this::setText, this::getText, this::resetText))
        .put(
            "costPU",
            MutableProperty.of(
                this::setCostPu, this::setCostPu, this::getCostPu, this::resetCostPu))
        .put(
            "costResources",
            MutableProperty.of(
                this::setCostResources,
                this::setCostResources,
                this::getCostResources,
                this::resetCostResources))
        .put(
            "attemptsPerTurn",
            MutableProperty.of(
                this::setAttemptsPerTurn,
                this::setAttemptsPerTurn,
                this::getAttemptsPerTurn,
                this::resetAttemptsPerTurn))
        .put(
            "attemptsLeftThisTurn",
            MutableProperty.of(
                this::setAttemptsLeftThisTurn,
                this::setAttemptsLeftThisTurn,
                this::getAttemptsLeftThisTurn,
                this::resetAttemptsLeftThisTurn))
        .put(
            "actionAccept",
            MutableProperty.of(
                this::setActionAccept,
                this::setActionAccept,
                this::getActionAccept,
                this::resetActionAccept))
        .build();
  }
}
