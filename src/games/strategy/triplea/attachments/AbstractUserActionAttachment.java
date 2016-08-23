package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.HashMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.util.Match;

/**
 * Abstract class for holding various action/condition things for PoliticalActionAttachment and UserActionAttachment
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
  protected ArrayList<PlayerID> m_actionAccept = new ArrayList<>();

  public static Match<AbstractUserActionAttachment> isSatisfiedMatch(
      final HashMap<ICondition, Boolean> testedConditions) {
    return new Match<AbstractUserActionAttachment>() {
      @Override
      public boolean match(final AbstractUserActionAttachment value) {
        return value.isSatisfied(testedConditions);
      }
    };
  }

  /**
   * @return true if there is no condition to this action or if the condition is satisfied
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
   * @return the Key that is used in politicstext.properties or other .properties for all the texts
   */
  public String getText() {
    return m_text;
  }

  /**
   * @param s
   *        the amount you need to pay to perform the action
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCostPU(final String s) {
    m_costPU = getInt(s);
  }

  /**
   * @return the amount you need to pay to perform the action
   */
  public int getCostPU() {
    return m_costPU;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value
   * @throws GameParseException
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setActionAccept(final String value) throws GameParseException {
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
      if (tempPlayer != null) {
        m_actionAccept.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setActionAccept(final ArrayList<PlayerID> value) {
    m_actionAccept = value;
  }

  /**
   * @return a list of players that must accept this action before it takes effect.
   */
  public ArrayList<PlayerID> getActionAccept() {
    return m_actionAccept;
  }

  /**
   * @param s
   *        the amount of times you can try this Action per Round
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttemptsPerTurn(final String s) {
    m_attemptsPerTurn = getInt(s);
    m_attemptsLeftThisTurn = m_attemptsPerTurn;
  }


  public void resetAttempts(final IDelegateBridge aBridge) {
    if (m_attemptsLeftThisTurn != m_attemptsPerTurn) {
      aBridge.addChange(ChangeFactory.attachmentPropertyChange(this, m_attemptsPerTurn, ATTEMPTS_LEFT_THIS_TURN));
    }
  }

  public void useAttempt(final IDelegateBridge aBridge) {
    aBridge
        .addChange(ChangeFactory.attachmentPropertyChange(this, (m_attemptsLeftThisTurn - 1), ATTEMPTS_LEFT_THIS_TURN));
  }

  public boolean hasAttemptsLeft() {
    return m_attemptsLeftThisTurn > 0;
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
    super.validate(data);
    if (m_text.trim().length() <= 0) {
      throw new GameParseException("value: text can't be empty" + thisErrorMsg());
    }
  }
}
