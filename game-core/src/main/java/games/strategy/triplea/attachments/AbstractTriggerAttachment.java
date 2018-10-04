package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.util.Interruptibles;
import games.strategy.util.Tuple;

public abstract class AbstractTriggerAttachment extends AbstractConditionsAttachment {
  private static final long serialVersionUID = 5866039180681962697L;

  public static final String NOTIFICATION = "Notification";
  public static final String AFTER = "after";
  public static final String BEFORE = "before";

  // "setTrigger" is also a valid setter, and it just calls "setConditions" in AbstractConditionsAttachment. Kept for
  // backwards
  // compatibility.
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private int m_uses = -1;
  @InternalDoNotExport
  // Do Not Export (do not include in IAttachment).
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private boolean m_usedThisRound = false;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_notification = null;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private List<Tuple<String, String>> m_when = new ArrayList<>();

  protected AbstractTriggerAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static CompositeChange triggerSetUsedForThisRound(final PlayerID player) {
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment ta : TriggerAttachment.getTriggers(player, null)) {
      if (ta.getUsedThisRound()) {
        final int currentUses = ta.getUses();
        if (currentUses > 0) {
          change.add(ChangeFactory.attachmentPropertyChange(ta, Integer.toString(currentUses - 1), "uses"));
          change.add(ChangeFactory.attachmentPropertyChange(ta, false, "usedThisRound"));
        }
      }
    }
    return change;
  }

  /**
   * DO NOT REMOVE THIS (or else you will break a lot of older xmls).
   *
   * @deprecated please use setConditions, getConditions, clearConditions, instead.
   */
  @Deprecated
  private void setTrigger(final String conditions) throws GameParseException {
    setConditions(conditions);
  }

  /**
   * Returns the attached rule attachments.
   *
   * @deprecated please use setConditions, getConditions, clearConditions, instead.
   */
  @Deprecated
  private List<RulesAttachment> getTrigger() {
    return getConditions();
  }

  /**
   * Resets (clears) the attached rule attachments.
   *
   * @deprecated please use setConditions, getConditions, clearConditions, instead.
   */
  @Deprecated
  private void resetTrigger() {
    resetConditions();
  }

  public void setUses(final int u) {
    m_uses = u;
  }

  private void setUsedThisRound(final String s) {
    m_usedThisRound = getBool(s);
  }

  public void setUsedThisRound(final boolean usedThisRound) {
    m_usedThisRound = usedThisRound;
  }

  protected boolean getUsedThisRound() {
    return m_usedThisRound;
  }

  private void resetUsedThisRound() {
    m_usedThisRound = false;
  }

  public int getUses() {
    return m_uses;
  }

  private void setWhen(final String when) throws GameParseException {
    final String[] s = splitOnColon(when);
    if (s.length != 2) {
      throw new GameParseException("when must exist in 2 parts: \"before/after:stepName\"." + thisErrorMsg());
    }
    if (!(s[0].equals(AFTER) || s[0].equals(BEFORE))) {
      throw new GameParseException("when must start with: " + BEFORE + " or " + AFTER + thisErrorMsg());
    }
    m_when.add(Tuple.of(s[0], s[1]));
  }

  private void setWhen(final List<Tuple<String, String>> value) {
    m_when = value;
  }

  protected List<Tuple<String, String>> getWhen() {
    return m_when;
  }

  private void resetWhen() {
    m_when = new ArrayList<>();
  }

  private void setNotification(final String notification) {
    if (notification == null) {
      m_notification = null;
      return;
    }
    m_notification = notification;
  }

  protected String getNotification() {
    return m_notification;
  }

  private void resetNotification() {
    m_notification = null;
  }

  protected void use(final IDelegateBridge bridge) {
    // instead of using up a "use" with every action, we will instead use up a "use" if the trigger is fired during this
    // round
    // this is in order to let a trigger that contains multiple actions, fire all of them in a single use
    // we only do this for things that do not have m_when set. triggers with m_when set have their uses modified
    // elsewhere.
    if (!m_usedThisRound && m_uses > 0 && m_when.isEmpty()) {
      bridge.addChange(ChangeFactory.attachmentPropertyChange(this, true, "usedThisRound"));
    }
  }

  protected boolean testChance(final IDelegateBridge bridge) {
    // "chance" should ALWAYS be checked last! (always check all other conditions first)
    final int hitTarget = getChanceToHit();
    final int diceSides = getChanceDiceSides();
    if (diceSides <= 0 || hitTarget >= diceSides) {
      changeChanceDecrementOrIncrementOnSuccessOrFailure(bridge, true, false);
      return true;
    } else if (hitTarget <= 0) {
      changeChanceDecrementOrIncrementOnSuccessOrFailure(bridge, false, false);
      return false;
    }
    // there is an issue with maps using thousands of chance triggers: they are causing the cypted random source (ie:
    // live and pbem games) to lock up or error out
    // so we need to slow them down a bit, until we come up with a better solution (like aggregating all the chances
    // together, then getting a ton of random numbers at once instead of one at a time)
    Interruptibles.sleep(100);
    final int rollResult = bridge.getRandom(diceSides, null, DiceType.ENGINE,
        "Attempting the Trigger: " + MyFormatter.attachmentNameToText(this.getName())) + 1;
    final boolean testChance = rollResult <= hitTarget;
    final String notificationMessage =
        (testChance ? TRIGGER_CHANCE_SUCCESSFUL : TRIGGER_CHANCE_FAILURE) + " (Rolled at " + hitTarget + " out of "
            + diceSides + " Result: " + rollResult + "  for " + MyFormatter.attachmentNameToText(this.getName()) + ")";
    bridge.getHistoryWriter().startEvent(notificationMessage);
    changeChanceDecrementOrIncrementOnSuccessOrFailure(bridge, testChance, true);
    ((ITripleAPlayer) bridge.getRemotePlayer(bridge.getPlayerId())).reportMessage(notificationMessage,
        notificationMessage);
    return testChance;
  }

  public static Predicate<TriggerAttachment> isSatisfiedMatch(final HashMap<ICondition, Boolean> testedConditions) {
    return t -> t.isSatisfied(testedConditions);
  }

  /**
   * If t.getWhen() is empty, and beforeOrAfter and stepName are both null, then this returns true.
   * Otherwise, all must be not null, and one of when's values must match the arguments.
   *
   * @param beforeOrAfter can be null, or must be "before" or "after"
   * @param stepName can be null, or must be exact name of a specific stepName
   * @return true if when and both args are null, and true if all are not null and when matches the args, otherwise
   *         false
   */
  public static Predicate<TriggerAttachment> whenOrDefaultMatch(final String beforeOrAfter, final String stepName) {
    return t -> {
      if (beforeOrAfter == null && stepName == null && t.getWhen().isEmpty()) {
        return true;
      } else if (beforeOrAfter != null && stepName != null && !t.getWhen().isEmpty()) {
        for (final Tuple<String, String> w : t.getWhen()) {
          if (beforeOrAfter.equals(w.getFirst()) && stepName.equals(w.getSecond())) {
            return true;
          }
        }
      }
      return false;
    };
  }

  public static final Predicate<TriggerAttachment> availableUses = t -> t.getUses() != 0;

  public static Predicate<TriggerAttachment> notificationMatch() {
    return t -> t.getNotification() != null;
  }

  protected static String getValueFromStringArrayForAllExceptLastSubstring(final String[] s) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length - 1; i++) {
      sb.append(":");
      sb.append(s[i]);
    }
    // remove leading colon
    if (sb.length() > 0 && sb.substring(0, 1).equals(":")) {
      sb.replace(0, 1, "");
    }
    return sb.toString();
  }

  public static int getEachMultiple(final AbstractTriggerAttachment t) {
    int eachMultiple = 1;
    for (final RulesAttachment condition : t.getConditions()) {
      final int tempEach = condition.getEachMultiple();
      if (tempEach > eachMultiple) {
        eachMultiple = tempEach;
      }
    }
    return eachMultiple;
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
    if (m_conditions == null) {
      throw new GameParseException("must contain at least one condition: " + thisErrorMsg());
    }
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .put("uses",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setUses,
                this::getUses,
                () -> -1))
        .put("usedThisRound",
            MutableProperty.of(
                this::setUsedThisRound,
                this::setUsedThisRound,
                this::getUsedThisRound,
                this::resetUsedThisRound))
        .put("notification",
            MutableProperty.ofString(
                this::setNotification,
                this::getNotification,
                this::resetNotification))
        .put("when",
            MutableProperty.of(
                this::setWhen,
                this::setWhen,
                this::getWhen,
                this::resetWhen))
        .put("trigger",
            MutableProperty.of(
                l -> {
                  throw new IllegalStateException("Can't set trigger directly");
                },
                this::setTrigger,
                this::getTrigger,
                this::resetTrigger))
        .build();
  }
}
