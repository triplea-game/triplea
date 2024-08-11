package games.strategy.triplea.attachments;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

/**
 * This class is designed to hold common code for holding "conditions". Any attachment that can hold
 * conditions (ie: RulesAttachments), should extend this instead of DefaultAttachment. Note: Empty
 * collection fields default to null to minimize memory use and serialization size.
 */
public abstract class AbstractConditionsAttachment extends DefaultAttachment implements ICondition {
  public static final String TRIGGER_CHANCE_SUCCESSFUL = "Trigger Rolling is a Success!";
  public static final String TRIGGER_CHANCE_FAILURE = "Trigger Rolling is a Failure!";
  protected static final String AND = "AND";
  protected static final String OR = "OR";
  private static final Pattern CONDITION_REGEX = Pattern.compile("AND|OR|\\d+(?:-\\d+)?");
  protected static final String DEFAULT_CHANCE = "1:1";
  protected static final String CHANCE = "chance";
  private static final long serialVersionUID = -9008441256118867078L;
  private static final Splitter HYPHEN_SPLITTER = Splitter.on('-');

  // list of conditions that this condition can contain
  protected @Nullable List<RulesAttachment> conditions = null;
  // conditionType modifies the relationship of conditions
  protected String conditionType = AND;
  // will logically negate the entire condition, including contained conditions
  protected boolean invert = false;
  // chance (x out of y) that this action is successful when attempted, default = 1:1 = always
  // successful
  protected String chance = DEFAULT_CHANCE;
  // if chance fails, we should increment the chance by x
  @Getter protected int chanceIncrementOnFailure = 0;
  // if chance succeeds, we should decrement the chance by x
  @Getter protected int chanceDecrementOnSuccess = 0;

  protected AbstractConditionsAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  protected void setConditions(final String conditions) throws GameParseException {
    if (this.conditions == null) {
      this.conditions = new ArrayList<>();
    }
    for (final String subString : splitOnColon(conditions)) {
      if (subString.isBlank()) {
        continue;
      }
      this.conditions.add(
          getData().getPlayerList().stream()
              .map(p -> p.getAttachment(subString))
              .map(RulesAttachment.class::cast)
              .filter(Objects::nonNull)
              .findAny()
              .orElseThrow(
                  () ->
                      new GameParseException(
                          "Could not find rule. From conditions value: "
                              + conditions
                              + ", expected name: "
                              + subString
                              + thisErrorMsg())));
    }
  }

  private void setConditions(final List<RulesAttachment> value) {
    conditions = value;
  }

  @Override
  public List<RulesAttachment> getConditions() {
    return getListProperty(conditions);
  }

  protected void resetConditions() {
    conditions = null;
  }

  private void setInvert(final boolean s) {
    invert = s;
  }

  private boolean getInvert() {
    return invert;
  }

  @VisibleForTesting
  void setConditionType(final String value) throws GameParseException {
    final String uppercaseValue = value.toUpperCase(Locale.ENGLISH);
    if (CONDITION_REGEX.matcher(uppercaseValue).matches()) {
      final String[] split = splitOnHyphen(uppercaseValue);
      if (split.length != 2 || Integer.parseInt(split[1]) > Integer.parseInt(split[0])) {
        conditionType = uppercaseValue.intern();
        return;
      }
    }
    throw new GameParseException(
        "conditionType must be equal to 'AND' or 'OR' or 'y' or 'y-z' where Y "
            + "and Z are valid positive integers and Z is greater than Y"
            + thisErrorMsg());
  }

  protected static String[] splitOnHyphen(final String value) {
    checkNotNull(value);

    return Iterables.toArray(HYPHEN_SPLITTER.split(value), String.class);
  }

  private String getConditionType() {
    return conditionType;
  }

  private void resetConditionType() {
    conditionType = AND;
  }

  /**
   * Accounts for Invert and conditionType. Only use if testedConditions has already been filled and
   * this conditions has been tested.
   */
  @Override
  public boolean isSatisfied(final Map<ICondition, Boolean> testedConditions) {
    return isSatisfied(testedConditions, null);
  }

  /**
   * Accounts for Invert and conditionType. IDelegateBridge is not used so can be null, this is
   * because we have already tested all the conditions.
   */
  @Override
  public boolean isSatisfied(
      final Map<ICondition, Boolean> testedConditions, final IDelegateBridge delegateBridge) {
    checkNotNull(testedConditions);

    if (testedConditions.containsKey(this)) {
      return testedConditions.get(this);
    }
    return areConditionsMet(new ArrayList<>(getConditions()), testedConditions, getConditionType())
        != getInvert();
  }

  /**
   * Anything that implements ICondition (currently RulesAttachment, TriggerAttachment, and
   * PoliticalActionAttachment) can use this to get all the conditions that must be checked for the
   * object to be 'satisfied'. <br>
   * Since anything implementing ICondition can contain other ICondition, this must recursively
   * search through all conditions and contained conditions to get the final list.
   */
  public static Set<ICondition> getAllConditionsRecursive(
      final Set<ICondition> startingListOfConditions,
      final Set<ICondition> initialAllConditionsNeededSoFar) {
    final Set<ICondition> allConditionsNeededSoFar =
        Optional.ofNullable(initialAllConditionsNeededSoFar).orElseGet(HashSet::new);
    allConditionsNeededSoFar.addAll(startingListOfConditions);
    for (final ICondition condition : startingListOfConditions) {
      for (final ICondition subCondition : condition.getConditions()) {
        if (!allConditionsNeededSoFar.contains(subCondition)) {
          allConditionsNeededSoFar.addAll(
              getAllConditionsRecursive(Set.of(subCondition), allConditionsNeededSoFar));
        }
      }
    }
    return allConditionsNeededSoFar;
  }

  /**
   * Takes the list of ICondition that getAllConditionsRecursive generates, and tests each of them,
   * mapping them one by one to their boolean value.
   */
  public static Map<ICondition, Boolean> testAllConditionsRecursive(
      final Set<ICondition> rules,
      final Map<ICondition, Boolean> initialAllConditionsTestedSoFar,
      final IDelegateBridge delegateBridge) {
    final Map<ICondition, Boolean> allConditionsTestedSoFar =
        Optional.ofNullable(initialAllConditionsTestedSoFar).orElseGet(HashMap::new);
    for (final ICondition c : rules) {
      if (!allConditionsTestedSoFar.containsKey(c)) {
        testAllConditionsRecursive(
            new HashSet<>(c.getConditions()), allConditionsTestedSoFar, delegateBridge);
        allConditionsTestedSoFar.put(c, c.isSatisfied(allConditionsTestedSoFar, delegateBridge));
      }
    }
    return allConditionsTestedSoFar;
  }

  /**
   * Accounts for all listed rules, according to the conditionType. Takes the mapped conditions
   * generated by testAllConditions and uses it to know which conditions are true and which are
   * false. There is no testing of conditions done in this method.
   */
  public static boolean areConditionsMet(
      final List<ICondition> rulesToTest,
      final Map<ICondition, Boolean> testedConditions,
      final String conditionType) {
    boolean met = false;
    switch (conditionType) {
      case AND:
        for (final ICondition c : rulesToTest) {
          met = testedConditions.get(c);
          if (!met) {
            break;
          }
        }
        break;
      case OR:
        for (final ICondition c : rulesToTest) {
          met = testedConditions.get(c);
          if (met) {
            break;
          }
        }
        break;
      default:
        final String[] nums = splitOnHyphen(conditionType);
        if (nums.length == 1) {
          final int start = Integer.parseInt(nums[0]);
          int count = 0;
          for (final ICondition c : rulesToTest) {
            met = testedConditions.get(c);
            if (met) {
              count++;
            }
          }
          met = (count == start);
        } else if (nums.length == 2) {
          final int start = Integer.parseInt(nums[0]);
          final int end = Integer.parseInt(nums[1]);
          int count = 0;
          for (final ICondition c : rulesToTest) {
            met = testedConditions.get(c);
            if (met) {
              count++;
            }
          }
          met = (count >= start && count <= end);
        }
        break;
    }
    return met;
  }

  protected void setChance(final String chance) throws GameParseException {
    final String[] s = splitOnColon(chance);
    try {
      final int i = getInt(s[0]);
      final int j = getInt(s[1]);
      if (i > j || i < 0 || i > 120 || j > 120) {
        throw new GameParseException(
            "chance should have a format of \"x:y\" where x is <= y and "
                + "both x and y are >=0 and <=120"
                + thisErrorMsg());
      }
    } catch (final IllegalArgumentException iae) {
      throw new GameParseException(
          "Invalid chance declaration: "
              + chance
              + " format: \"1:10\" for 10% chance"
              + thisErrorMsg());
    }
    this.chance = chance.intern();
  }

  /**
   * Returns the number you need to roll to get the action to succeed format "1:10" for 10% chance.
   */
  private String getChance() {
    return chance;
  }

  private void resetChance() {
    chance = DEFAULT_CHANCE;
  }

  public int getChanceToHit() {
    return getInt(splitOnColon(getChance())[0]);
  }

  public int getChanceDiceSides() {
    return getInt(splitOnColon(getChance())[1]);
  }

  private void setChanceIncrementOnFailure(final int value) {
    chanceIncrementOnFailure = value;
  }

  private void setChanceDecrementOnSuccess(final int value) {
    chanceDecrementOnSuccess = value;
  }

  /**
   * Adjusts the chance to hit for this condition.
   *
   * @param success {@code true} to decrement the chance to hit for successful conditions; {@code
   *     false} to increment the chance to hit for failed conditions.
   */
  public void changeChanceDecrementOrIncrementOnSuccessOrFailure(
      final IDelegateBridge delegateBridge, final boolean success, final boolean historyChild) {
    if (success) {
      if (chanceDecrementOnSuccess == 0) {
        return;
      }
      final int oldToHit = getChanceToHit();
      final int diceSides = getChanceDiceSides();
      final int newToHit = Math.max(0, Math.min(diceSides, (oldToHit - chanceDecrementOnSuccess)));
      if (newToHit == oldToHit) {
        return;
      }
      @NonNls final String newChance = newToHit + ":" + diceSides;
      delegateBridge
          .getHistoryWriter()
          .startEvent(
              "Success changes chance for "
                  + MyFormatter.attachmentNameToText(getName())
                  + " to "
                  + newChance);
      delegateBridge.addChange(ChangeFactory.attachmentPropertyChange(this, newChance, CHANCE));
    } else {
      if (chanceIncrementOnFailure == 0) {
        return;
      }
      final int oldToHit = getChanceToHit();
      final int diceSides = getChanceDiceSides();
      final int newToHit = Math.max(0, Math.min(diceSides, (oldToHit + chanceIncrementOnFailure)));
      if (newToHit == oldToHit) {
        return;
      }
      @NonNls final String newChance = newToHit + ":" + diceSides;
      if (historyChild) {
        delegateBridge
            .getHistoryWriter()
            .addChildToEvent(
                "Failure changes chance for "
                    + MyFormatter.attachmentNameToText(getName())
                    + " to "
                    + newChance);
      } else {
        delegateBridge
            .getHistoryWriter()
            .startEvent(
                "Failure changes chance for "
                    + MyFormatter.attachmentNameToText(getName())
                    + " to "
                    + newChance);
      }
      delegateBridge.addChange(ChangeFactory.attachmentPropertyChange(this, newChance, CHANCE));
    }
  }

  @Override
  public @Nullable MutableProperty<?> getPropertyOrNull(@NonNls String propertyName) {
    switch (propertyName) {
      case "conditions":
        return MutableProperty.of(
            this::setConditions, this::setConditions, this::getConditions, this::resetConditions);
      case "conditionType":
        return MutableProperty.ofString(
            this::setConditionType, this::getConditionType, this::resetConditionType);
      case "invert":
        return MutableProperty.ofMapper(
            DefaultAttachment::getBool, this::setInvert, this::getInvert, () -> false);
      case CHANCE:
        return MutableProperty.ofString(this::setChance, this::getChance, this::resetChance);
      case "chanceIncrementOnFailure":
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt,
            this::setChanceIncrementOnFailure,
            this::getChanceIncrementOnFailure,
            () -> 0);
      case "chanceDecrementOnSuccess":
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt,
            this::setChanceDecrementOnSuccess,
            this::getChanceDecrementOnSuccess,
            () -> 0);
      default:
        return null;
    }
  }
}
