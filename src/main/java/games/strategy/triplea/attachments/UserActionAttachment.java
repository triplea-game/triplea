package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

/**
 * A class of attachments that can be "activated" during a user action delegate.
 * For now they will just be conditions that can then fire triggers.
 */
@MapSupport
public class UserActionAttachment extends AbstractUserActionAttachment {
  private static final long serialVersionUID = 5268397563276055355L;

  public UserActionAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static Collection<UserActionAttachment> getUserActionAttachments(final PlayerID player) {
    final ArrayList<UserActionAttachment> returnList = new ArrayList<>();
    final Map<String, IAttachment> map = player.getAttachments();
    for (final Entry<String, IAttachment> entry : map.entrySet()) {
      final IAttachment a = entry.getValue();
      if (a.getName().startsWith(Constants.USERACTION_ATTACHMENT_PREFIX) && a instanceof UserActionAttachment) {
        returnList.add((UserActionAttachment) a);
      }
    }
    return returnList;
  }

  static UserActionAttachment get(final PlayerID player, final String nameOfAttachment) {
    final UserActionAttachment rVal = (UserActionAttachment) player.getAttachment(nameOfAttachment);
    if (rVal == null) {
      throw new IllegalStateException(
          "UserActionAttachment: No attachment for:" + player.getName() + " with name: " + nameOfAttachment);
    }
    return rVal;
  }

  // instance variables:
  private ArrayList<Tuple<String, String>> m_activateTrigger = new ArrayList<>();

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   * (same as one in TriggerAttachment)
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setActivateTrigger(final String value) throws GameParseException {
    // triggerName:numberOfTimes:useUses:testUses:testConditions:testChance
    final String[] s = value.split(":");
    if (s.length != 6) {
      throw new GameParseException(
          "activateTrigger must have 6 parts: triggerName:numberOfTimes:useUses:testUses:testConditions:testChance"
              + thisErrorMsg());
    }
    TriggerAttachment trigger = null;
    for (final PlayerID player : getData().getPlayerList().getPlayers()) {
      for (final TriggerAttachment ta : TriggerAttachment.getTriggers(player, getData(), null)) {
        if (ta.getName().equals(s[0])) {
          trigger = ta;
          break;
        }
      }
      if (trigger != null) {
        break;
      }
    }
    if (trigger == null) {
      throw new GameParseException("No TriggerAttachment named: " + s[0] + thisErrorMsg());
    }
    String options = value;
    options = options.replaceFirst((s[0] + ":"), "");
    final int numberOfTimes = getInt(s[1]);
    if (numberOfTimes < 0) {
      throw new GameParseException(
          "activateTrigger must be positive for the number of times to fire: " + s[1] + thisErrorMsg());
    }
    getBool(s[2]);
    getBool(s[3]);
    getBool(s[4]);
    getBool(s[5]);
    m_activateTrigger.add(Tuple.of(s[0], options));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setActivateTrigger(final ArrayList<Tuple<String, String>> value) {
    m_activateTrigger = value;
  }

  public ArrayList<Tuple<String, String>> getActivateTrigger() {
    return m_activateTrigger;
  }

  public void clearActivateTrigger() {
    m_activateTrigger.clear();
  }

  public void resetActivateTrigger() {
    m_activateTrigger = new ArrayList<>();
  }

  public static void fireTriggers(final UserActionAttachment actionAttachment,
      final HashMap<ICondition, Boolean> testedConditionsSoFar, final IDelegateBridge aBridge) {
    final GameData data = aBridge.getData();
    for (final Tuple<String, String> tuple : actionAttachment.getActivateTrigger()) {
      // numberOfTimes:useUses:testUses:testConditions:testChance
      TriggerAttachment toFire = null;
      for (final PlayerID player : data.getPlayerList().getPlayers()) {
        for (final TriggerAttachment ta : TriggerAttachment.getTriggers(player, data, null)) {
          if (ta.getName().equals(tuple.getFirst())) {
            toFire = ta;
            break;
          }
        }
        if (toFire != null) {
          break;
        }
      }
      final HashSet<TriggerAttachment> toFireSet = new HashSet<>();
      toFireSet.add(toFire);
      final String[] options = tuple.getSecond().split(":");
      final int numberOfTimesToFire = getInt(options[0]);
      final boolean useUsesToFire = getBool(options[1]);
      final boolean testUsesToFire = getBool(options[2]);
      final boolean testConditionsToFire = getBool(options[3]);
      final boolean testChanceToFire = getBool(options[4]);
      if (testConditionsToFire) {
        if (!testedConditionsSoFar.containsKey(toFire)) {
          // this should directly add the new tests to testConditionsToFire...
          TriggerAttachment.collectTestsForAllTriggers(toFireSet, aBridge,
              new HashSet<>(testedConditionsSoFar.keySet()), testedConditionsSoFar);
        }
        if (!AbstractTriggerAttachment.isSatisfiedMatch(testedConditionsSoFar).match(toFire)) {
          continue;
        }
      }
      for (int i = 0; i < numberOfTimesToFire; ++i) {
        aBridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(actionAttachment.getName())
            + " activates a trigger called: " + MyFormatter.attachmentNameToText(toFire.getName()));
        TriggerAttachment.fireTriggers(toFireSet, testedConditionsSoFar, aBridge, null, null, useUsesToFire,
            testUsesToFire, testChanceToFire, false);
      }
    }
  }

  public Set<PlayerID> getOtherPlayers() {
    final HashSet<PlayerID> otherPlayers = new HashSet<>();
    otherPlayers.add((PlayerID) this.getAttachedTo());
    otherPlayers.addAll(m_actionAccept);
    return otherPlayers;
  }

  /**
   * @return gets the valid actions for this player.
   */
  public static Collection<UserActionAttachment> getValidActions(final PlayerID player,
      final HashMap<ICondition, Boolean> testedConditions, final GameData data) {
    return Match.getMatches(getUserActionAttachments(player), Match.all(
        Matches.abstractUserActionAttachmentCanBeAttempted(testedConditions)));
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
    super.validate(data);
  }
}
