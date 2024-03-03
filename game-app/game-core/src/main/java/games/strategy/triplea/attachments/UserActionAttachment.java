package games.strategy.triplea.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.util.Tuple;

/**
 * A class of attachments that can be "activated" during a user action delegate. For now they will
 * just be conditions that can then fire triggers. Note: Empty collection fields default to null to
 * minimize memory use and serialization size.
 */
public class UserActionAttachment extends AbstractUserActionAttachment {
  private static final long serialVersionUID = 5268397563276055355L;

  private @Nullable List<Tuple<String, String>> activateTrigger = null;

  public UserActionAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static Collection<UserActionAttachment> getUserActionAttachments(final GamePlayer player) {
    return player.getAttachments().values().stream()
        .filter(a -> a.getName().startsWith(Constants.USERACTION_ATTACHMENT_PREFIX))
        .filter(UserActionAttachment.class::isInstance)
        .map(UserActionAttachment.class::cast)
        .collect(Collectors.toList());
  }

  static UserActionAttachment get(final GamePlayer player, final String nameOfAttachment) {
    return getAttachment(player, nameOfAttachment, UserActionAttachment.class);
  }

  private void setActivateTrigger(final String value) throws GameParseException {
    // triggerName:numberOfTimes:useUses:testUses:testConditions:testChance
    final String[] s = splitOnColon(value);
    if (s.length != 6) {
      throw new GameParseException(
          "activateTrigger must have 6 parts: "
              + "triggerName:numberOfTimes:useUses:testUses:testConditions:testChance"
              + thisErrorMsg());
    }
    TriggerAttachment trigger = null;
    for (final GamePlayer player : getData().getPlayerList().getPlayers()) {
      final TriggerAttachment ta = (TriggerAttachment) player.getAttachment(s[0]);
      if (ta != null) {
        trigger = ta;
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
          "activateTrigger must be positive for the number of times to fire: "
              + s[1]
              + thisErrorMsg());
    }
    getBool(s[2]);
    getBool(s[3]);
    getBool(s[4]);
    getBool(s[5]);
    if (activateTrigger == null) {
      activateTrigger = new ArrayList<>();
    }
    activateTrigger.add(Tuple.of(s[0].intern(), options.intern()));
  }

  private void setActivateTrigger(final List<Tuple<String, String>> value) {
    activateTrigger = value;
  }

  private List<Tuple<String, String>> getActivateTrigger() {
    return getListProperty(activateTrigger);
  }

  private void resetActivateTrigger() {
    activateTrigger = null;
  }

  /**
   * Fires all trigger attachments associated with the specified user action attachment that satisfy
   * the specified conditions.
   */
  public static void fireTriggers(
      final UserActionAttachment actionAttachment,
      final Map<ICondition, Boolean> testedConditionsSoFar,
      final IDelegateBridge bridge) {
    final GameState data = bridge.getData();
    for (final Tuple<String, String> tuple : actionAttachment.getActivateTrigger()) {
      // numberOfTimes:useUses:testUses:testConditions:testChance
      final Optional<TriggerAttachment> optionalTrigger =
          data.getPlayerList().getPlayers().stream()
              .map(player -> (TriggerAttachment) player.getAttachment(tuple.getFirst()))
              .filter(Objects::nonNull)
              .findAny();
      if (optionalTrigger.isEmpty()) {
        continue;
      }
      final TriggerAttachment toFire = optionalTrigger.get();
      final Set<TriggerAttachment> toFireSet = new HashSet<>();
      toFireSet.add(toFire);
      final String[] options = splitOnColon(tuple.getSecond());
      final int numberOfTimesToFire = getInt(options[0]);
      final boolean useUsesToFire = getBool(options[1]);
      final boolean testUsesToFire = getBool(options[2]);
      final boolean testConditionsToFire = getBool(options[3]);
      final boolean testChanceToFire = getBool(options[4]);
      if (testConditionsToFire) {
        if (!testedConditionsSoFar.containsKey(toFire)) {
          // this should directly add the new tests to testConditionsToFire...
          TriggerAttachment.collectTestsForAllTriggers(
              toFireSet,
              bridge,
              new HashSet<>(testedConditionsSoFar.keySet()),
              testedConditionsSoFar);
        }
        if (!AbstractTriggerAttachment.isSatisfiedMatch(testedConditionsSoFar).test(toFire)) {
          continue;
        }
      }
      final FireTriggerParams fireTriggerParams =
          new FireTriggerParams(null, null, useUsesToFire, testUsesToFire, testChanceToFire, false);
      for (int i = 0; i < numberOfTimesToFire; ++i) {
        bridge
            .getHistoryWriter()
            .startEvent(
                MyFormatter.attachmentNameToText(actionAttachment.getName())
                    + " activates a trigger called: "
                    + MyFormatter.attachmentNameToText(toFire.getName()));
        TriggerAttachment.fireTriggers(toFireSet, testedConditionsSoFar, bridge, fireTriggerParams);
      }
    }
  }

  public Set<GamePlayer> getOtherPlayers() {
    final Set<GamePlayer> otherPlayers = new HashSet<>();
    otherPlayers.add((GamePlayer) this.getAttachedTo());
    otherPlayers.addAll(getActionAccept());
    return otherPlayers;
  }

  /** Returns the valid actions for this player. */
  public static Collection<UserActionAttachment> getValidActions(
      final GamePlayer player, final Map<ICondition, Boolean> testedConditions) {
    return CollectionUtils.getMatches(
        getUserActionAttachments(player),
        Matches.abstractUserActionAttachmentCanBeAttempted(testedConditions));
  }

  @Override
  public @Nullable MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "activateTrigger":
        return MutableProperty.of(
            this::setActivateTrigger,
            this::setActivateTrigger,
            this::getActivateTrigger,
            this::resetActivateTrigger);
      default:
        return super.getPropertyOrNull(propertyName);
    }
  }
}
