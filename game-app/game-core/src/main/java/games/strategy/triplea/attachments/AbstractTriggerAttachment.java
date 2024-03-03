package games.strategy.triplea.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.Getter;
import org.triplea.java.Interruptibles;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.util.Tuple;

/**
 * Superclass for all attachments that trigger an action based on an event. Note: Empty collection
 * fields default to null to minimize memory use and serialization size.
 *
 * <p>TODO: Merge with {@link TriggerAttachment}, as that is the only subclass.
 */
@RemoveOnNextMajorRelease
public abstract class AbstractTriggerAttachment extends AbstractConditionsAttachment {
  public static final String NOTIFICATION = "Notification";
  public static final String AFTER = "after";
  public static final String BEFORE = "before";
  public static final Predicate<TriggerAttachment> availableUses = t -> t.getUses() != 0;
  private static final long serialVersionUID = 5866039180681962697L;

  // "setTrigger" is also a valid setter, and it just calls "setConditions" in
  // AbstractConditionsAttachment. Kept for
  // backwards compatibility.
  @Getter private int uses = -1;
  private boolean usedThisRound = false;
  private @Nullable String notification = null;
  private @Nullable List<Tuple<String, String>> when = null;

  protected AbstractTriggerAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Returns a new change that marks all trigger attachments for the specified player has having
   * been used this round. If any trigger attachment has already been marked as used, it will not be
   * modified.
   */
  public static CompositeChange triggerSetUsedForThisRound(final GamePlayer player) {
    final CompositeChange change = new CompositeChange();
    for (final TriggerAttachment ta :
        TriggerAttachment.getTriggers(player, ta -> ta.getUsedThisRound() && ta.getUses() > 0)) {
      change.add(
          ChangeFactory.attachmentPropertyChange(ta, Integer.toString(ta.getUses() - 1), "uses"));
      change.add(ChangeFactory.attachmentPropertyChange(ta, false, "usedThisRound"));
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
    uses = u;
  }

  private void setUsedThisRound(final String s) {
    usedThisRound = getBool(s);
  }

  public void setUsedThisRound(final boolean usedThisRound) {
    this.usedThisRound = usedThisRound;
  }

  protected boolean getUsedThisRound() {
    return usedThisRound;
  }

  private void resetUsedThisRound() {
    usedThisRound = false;
  }

  private void setWhen(final String when) throws GameParseException {
    final String[] s = splitOnColon(when);
    if (s.length != 2) {
      throw new GameParseException(
          "when must exist in 2 parts: \"before/after:stepName\"." + thisErrorMsg());
    }
    if (!(s[0].equals(AFTER) || s[0].equals(BEFORE))) {
      throw new GameParseException(
          "when must start with: " + BEFORE + " or " + AFTER + thisErrorMsg());
    }
    if (this.when == null) {
      this.when = new ArrayList<>();
    }
    this.when.add(Tuple.of(s[0].intern(), s[1].intern()));
  }

  private void setWhen(final List<Tuple<String, String>> value) {
    when = value;
  }

  protected List<Tuple<String, String>> getWhen() {
    return getListProperty(when);
  }

  private void resetWhen() {
    when = null;
  }

  private void setNotification(final String notification) {
    this.notification = notification;
  }

  protected @Nullable String getNotification() {
    return notification;
  }

  private void resetNotification() {
    notification = null;
  }

  protected void use(final IDelegateBridge bridge) {
    // instead of using up a "use" with every action, we will instead use up a "use" if the trigger
    // is fired during this round
    // this is in order to let a trigger that contains multiple actions, fire all of them in a
    // single use
    // we only do this for things that do not have when set. triggers with when set have their uses
    // modified elsewhere.
    if (!usedThisRound && uses > 0 && getWhen().isEmpty()) {
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
    // there is an issue with maps using thousands of chance triggers: they are causing the cypted
    // random source (ie: live and pbem games) to lock up or error out so we need to slow them down
    // a bit, until we come up with a better solution (like aggregating all the chances together,
    // then getting a ton of random numbers at once instead of one at a time)
    if (PbemMessagePoster.gameDataHasPlayByEmailOrForumMessengers(getData())) {
      Interruptibles.sleep(100);
    }
    final int rollResult =
        bridge.getRandom(
                diceSides,
                null,
                DiceType.ENGINE,
                "Attempting the Trigger: " + MyFormatter.attachmentNameToText(this.getName()))
            + 1;
    final boolean testChance = rollResult <= hitTarget;
    final String notificationMessage =
        (testChance ? TRIGGER_CHANCE_SUCCESSFUL : TRIGGER_CHANCE_FAILURE)
            + " (Rolled at "
            + hitTarget
            + " out of "
            + diceSides
            + " Result: "
            + rollResult
            + "  for "
            + MyFormatter.attachmentNameToText(this.getName())
            + ")";
    bridge.getHistoryWriter().startEvent(notificationMessage);
    changeChanceDecrementOrIncrementOnSuccessOrFailure(bridge, testChance, true);
    bridge
        .getRemotePlayer(bridge.getGamePlayer())
        .reportMessage(notificationMessage, notificationMessage);
    return testChance;
  }

  public static Predicate<TriggerAttachment> isSatisfiedMatch(
      final Map<ICondition, Boolean> testedConditions) {
    return t -> t.isSatisfied(testedConditions);
  }

  /**
   * If t.getWhen() is empty, and beforeOrAfter and stepName are both null, then this returns true.
   * Otherwise, all must be not null, and one of when's values must match the arguments.
   *
   * @param beforeOrAfter can be null, or must be "before" or "after"
   * @param stepName can be null, or must be exact name of a specific stepName
   * @return true if when and both args are null, and true if all are not null and when matches the
   *     args, otherwise false
   */
  public static Predicate<TriggerAttachment> whenOrDefaultMatch(
      final String beforeOrAfter, final String stepName) {
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
    return sb.toString().intern();
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
  public void validate(final GameState data) {}

  @Override
  public @Nullable MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "uses":
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt, this::setUses, this::getUses, () -> -1);
      case "usedThisRound":
        return MutableProperty.of(
            this::setUsedThisRound,
            this::setUsedThisRound,
            this::getUsedThisRound,
            this::resetUsedThisRound);
      case "notification":
        return MutableProperty.ofString(
            this::setNotification, this::getNotification, this::resetNotification);
      case "when":
        return MutableProperty.of(this::setWhen, this::setWhen, this::getWhen, this::resetWhen);
      case "trigger":
        return MutableProperty.of(
            l -> {
              throw new IllegalStateException("Can't set trigger directly");
            },
            this::setTrigger,
            this::getTrigger,
            this::resetTrigger);
      default:
        return super.getPropertyOrNull(propertyName);
    }
  }
}
