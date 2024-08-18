package games.strategy.triplea.formatter;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.DefaultNamed;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.util.UnitOwner;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.triplea.java.collections.IntegerMap;

/** Provides useful methods for converting things to text. */
public class MyFormatter {
  /** Some exceptions to the rules. */
  private static final Map<String, String> plural;

  static {
    plural = new HashMap<>();
    plural.put("armour", "armour");
    plural.put("infantry", "infantry");
    plural.put("artillery", "artilleries");
    plural.put("factory", "factories");
  }

  private MyFormatter() {}

  public static String unitsToTextNoOwner(final Collection<Unit> units) {
    return unitsToTextNoOwner(units, null);
  }

  /**
   * Returns a string containing the quantity of each type of unit in {@code units} owned by {@code
   * owner}, but no reference to the owner will be mentioned in the string. The returned string will
   * have the form: {@code <q1> <ut1>, <q2> <ut2>, ... and <qN> <utN>}.
   */
  public static String unitsToTextNoOwner(final Collection<Unit> units, final GamePlayer owner) {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    for (final Unit unit : units) {
      if (owner == null || owner.equals(unit.getOwner())) {
        map.add(unit.getType(), 1);
      }
    }
    final StringBuilder buf = new StringBuilder();
    // sort on unit name
    final List<UnitType> sortedList = new ArrayList<>(map.keySet());
    sortedList.sort(Comparator.comparing(UnitType::getName));
    int count = map.keySet().size();
    for (final UnitType type : sortedList) {
      final int quantity = map.getInt(type);
      buf.append(quantity);
      buf.append(" ");
      buf.append(quantity > 1 ? pluralize(type.getName()) : type.getName());
      count--;
      if (count > 1) {
        buf.append(", ");
      }
      if (count == 1) {
        buf.append(" and ");
      }
    }
    return buf.toString();
  }

  /**
   * Converts the specified unit collection to a textual representation that describes the quantity
   * of each distinct unit type owned by each distinct player.
   *
   * @param units The collection of units.
   * @return A textual representation of the specified unit collection.
   */
  public static String unitsToText(final Collection<Unit> units) {
    checkNotNull(units);

    final Map<UnitOwner, Long> quantitiesByOwner =
        units.stream()
            .map(UnitOwner::new)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    final StringBuilder buf = new StringBuilder();
    final AtomicInteger countRef = new AtomicInteger(quantitiesByOwner.size());
    quantitiesByOwner.forEach(
        (owner, quantity) -> {
          buf.append(quantity);
          buf.append(" ");
          buf.append(
              quantity > 1 ? pluralize(owner.getType().getName()) : owner.getType().getName());
          buf.append(" owned by the ");
          buf.append(owner.getOwner().getName());
          final int count = countRef.decrementAndGet();
          if (count > 1) {
            buf.append(", ");
          } else if (count == 1) {
            buf.append(" and ");
          }
        });
    return buf.toString();
  }

  public static String pluralize(final String in, final int quantity) {
    if (quantity == -1 || quantity == 1) {
      return in;
    }
    return pluralize(in);
  }

  private static String pluralize(final String in) {
    if (plural.containsKey(in)) {
      return plural.get(in);
    }

    if (in.endsWith("man")) {
      return in.substring(0, in.lastIndexOf("man")) + "men";
    }
    return in + "s";
  }

  /**
   * Replaces the map XML attachment name at the beginning of the specified string with its
   * corresponding display name followed by a space. In addition, it makes the following
   * replacements:
   *
   * <ul>
   *   <li>An underscore is replaced with a space.
   *   <li>Two consecutive spaces are replaced with a single space.
   *   <li>Trailing whitespace is removed.
   * </ul>
   *
   * <p>For example:
   *
   * <pre>
   * "canalAttachmentFOO  BAR_BAZ "
   * </pre>
   *
   * <p>will be converted to
   *
   * <pre>
   * "Canal FOO BAR BAZ"
   * </pre>
   */
  public static String attachmentNameToText(final String attachmentGetName) {
    String toText = attachmentGetName;
    if (attachmentGetName.startsWith(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME)) {
      toText =
          attachmentGetName.replaceFirst(
              Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME, "Relationship Type ");
    } else if (attachmentGetName.startsWith(Constants.TECH_ATTACHMENT_NAME)) {
      toText = attachmentGetName.replaceFirst(Constants.TECH_ATTACHMENT_NAME, "Player Techs ");
    } else if (attachmentGetName.startsWith(Constants.UNIT_ATTACHMENT_NAME)) {
      toText =
          attachmentGetName.replaceFirst(Constants.UNIT_ATTACHMENT_NAME, "Unit Type Properties ");
    } else if (attachmentGetName.startsWith(Constants.TERRITORY_ATTACHMENT_NAME)) {
      toText =
          attachmentGetName.replaceFirst(
              Constants.TERRITORY_ATTACHMENT_NAME, "Territory Properties ");
    } else if (attachmentGetName.startsWith(Constants.CANAL_ATTACHMENT_PREFIX)) {
      toText = attachmentGetName.replaceFirst(Constants.CANAL_ATTACHMENT_PREFIX, "Canal ");
    } else if (attachmentGetName.startsWith(Constants.TERRITORYEFFECT_ATTACHMENT_NAME)) {
      toText =
          attachmentGetName.replaceFirst(
              Constants.TERRITORYEFFECT_ATTACHMENT_NAME, "Territory Effect ");
    } else if (attachmentGetName.startsWith(Constants.SUPPORT_ATTACHMENT_PREFIX)) {
      toText = attachmentGetName.replaceFirst(Constants.SUPPORT_ATTACHMENT_PREFIX, "Support ");
    } else if (attachmentGetName.startsWith(Constants.RULES_OBJECTIVE_PREFIX)) {
      toText = attachmentGetName.replaceFirst(Constants.RULES_OBJECTIVE_PREFIX, "Objective ");
    } else if (attachmentGetName.startsWith(Constants.RULES_CONDITION_PREFIX)) {
      toText = attachmentGetName.replaceFirst(Constants.RULES_CONDITION_PREFIX, "Condition ");
    } else if (attachmentGetName.startsWith(Constants.TRIGGER_ATTACHMENT_PREFIX)) {
      toText = attachmentGetName.replaceFirst(Constants.TRIGGER_ATTACHMENT_PREFIX, "Trigger ");
    } else if (attachmentGetName.startsWith(Constants.RULES_ATTACHMENT_NAME)) {
      toText = attachmentGetName.replaceFirst(Constants.RULES_ATTACHMENT_NAME, "Rules ");
    } else if (attachmentGetName.startsWith(Constants.PLAYER_ATTACHMENT_NAME)) {
      toText =
          attachmentGetName.replaceFirst(Constants.PLAYER_ATTACHMENT_NAME, "Player Properties ");
    } else if (attachmentGetName.startsWith(Constants.POLITICALACTION_ATTACHMENT_PREFIX)) {
      toText =
          attachmentGetName.replaceFirst(
              Constants.POLITICALACTION_ATTACHMENT_PREFIX, "Political Action ");
    } else if (attachmentGetName.startsWith(Constants.USERACTION_ATTACHMENT_PREFIX)) {
      toText = attachmentGetName.replaceFirst(Constants.USERACTION_ATTACHMENT_PREFIX, "Action ");
    } else if (attachmentGetName.startsWith(Constants.TECH_ABILITY_ATTACHMENT_NAME)) {
      toText =
          attachmentGetName.replaceFirst(
              Constants.TECH_ABILITY_ATTACHMENT_NAME, "Tech Properties ");
    }
    toText = toText.replaceAll("_", " ");
    toText = toText.replaceAll("  ", " ");
    toText = toText.trim();
    return toText;
  }

  public static String listOfArraysToString(final List<String[]> listOfArrays) {
    if (listOfArrays == null) {
      return "null";
    }
    return listOfArrays.stream().map(Arrays::toString).collect(Collectors.joining(",", "[", "]"));
  }

  /**
   * Converts the specified dice roll to a string of the form {@code <die1>,<die2>,...,<dieN>}. If
   * the dice roll is {@code null} or contains no dice, {@code none} will be returned.
   */
  public static String asDice(final DiceRoll roll) {
    if (roll == null || roll.isEmpty()) {
      return "none";
    }

    final StringBuilder buf = new StringBuilder();
    for (int i = 0; i < roll.size(); i++) {
      buf.append(roll.getDie(i).getValue() + 1);
      if (i + 1 < roll.size()) {
        buf.append(",");
      }
    }
    return buf.toString();
  }

  /**
   * Converts the specified array of dice rolls to a string of the form {@code
   * <die1>,<die2>,...,<dieN>}. If the array is {@code null} or empty, {@code none} will be
   * returned.
   */
  public static String asDice(final int[] rolls) {
    if (rolls == null || rolls.length == 0) {
      return "none";
    }
    return Arrays.stream(rolls)
        .map(roll -> roll + 1)
        .mapToObj(String::valueOf)
        .collect(Collectors.joining(","));
  }

  /**
   * Converts the specified list of dice rolls to a string of the form {@code
   * <die1>,<die2>,...,<dieN>}. If the list is {@code null} or empty, {@code none} will be returned.
   */
  public static String asDice(final List<Die> rolls) {
    if (rolls == null || rolls.isEmpty()) {
      return "none";
    }
    final StringBuilder buf = new StringBuilder(rolls.size() * 2);
    for (int i = 0; i < rolls.size(); i++) {
      buf.append(rolls.get(i).getValue() + 1);
      if (i + 1 < rolls.size()) {
        buf.append(",");
      }
    }
    return buf.toString();
  }

  public static String defaultNamedToTextList(final Collection<? extends DefaultNamed> list) {
    return defaultNamedToTextList(list, ", ", false);
  }

  /**
   * Computes a histogram of the {@code DefaultNamed} objects in the specified list and converts it
   * to a string for display.
   *
   * <p>If {@code showQuantity} is {@code true}, the returned string will have the form (without
   * line breaks):
   *
   * <pre>
   * &lt;q1> &lt;objectName1>&lt;separator>
   * &lt;q2> &lt;objectName2>&lt;separator>
   * ...
   * and &lt;qN> &lt;objectNameN>
   * </pre>
   *
   * <p>If {@code showQuantity} is {@code false}, the returned string will have the form:
   *
   * <pre>
   * &lt;objectName1>&lt;separator>&lt;objectName2>&lt;separator>... and &lt;objectNameN>
   * </pre>
   *
   * <p>In both cases, the objects will appear in the string in order by name.
   */
  public static String defaultNamedToTextList(
      final Collection<? extends DefaultNamed> list,
      final String separator,
      final boolean showQuantity) {
    final IntegerMap<DefaultNamed> map = new IntegerMap<>();
    for (final DefaultNamed unit : list) {
      if (unit == null || unit.getName() == null) {
        throw new IllegalStateException("Unit or Resource no longer exists?!?");
      }
      map.add(unit, 1);
    }
    final StringBuilder buf = new StringBuilder();
    // sort on unit name
    final List<DefaultNamed> sortedList = new ArrayList<>(map.keySet());
    sortedList.sort(Comparator.comparing(DefaultNamed::getName));
    int count = map.keySet().size();
    for (final DefaultNamed type : sortedList) {
      if (showQuantity) {
        final int quantity = map.getInt(type);
        buf.append(quantity);
        buf.append(" ");
        buf.append(quantity > 1 ? pluralize(type.getName()) : type.getName());
      } else {
        buf.append(type.getName());
      }
      count--;
      if (count > 1) {
        buf.append(separator);
      }
      if (count == 1) {
        buf.append(" and ");
      }
    }
    return buf.toString();
  }

  /**
   * Converts the specified {@code DefaultNamed} to {@code Integer} map to a string for display.
   *
   * <p>If {@code valueBeforeKey} is {@code true}, the returned string will have the form (without
   * line breaks):
   *
   * <pre>
   * &lt;int1>&lt;assignment>&lt;objectName1>&lt;separator>
   * &lt;int2>&lt;assignment>&lt;objectName2>&lt;separator>
   * ...
   * &lt;intN>&lt;assignment>&lt;objectNameN>
   * </pre>
   *
   * <p>If {@code valueBeforeKey} is {@code false}, the returned string will have the form (without
   * line breaks):
   *
   * <pre>
   * &lt;objectName1>&lt;assignment>&lt;int1>&lt;separator>
   * &lt;objectName2>&lt;assignment>&lt;int2>&lt;separator>
   * ...
   * &lt;objectNameN>&lt;assignment>&lt;intN>
   * </pre>
   */
  public static String integerDefaultNamedMapToString(
      final IntegerMap<? extends DefaultNamed> map,
      final String separator,
      final String assignment,
      final boolean valueBeforeKey) {
    final StringBuilder buf = new StringBuilder();
    for (final Entry<? extends DefaultNamed, Integer> entry : map.entrySet()) {
      buf.append(separator);
      final DefaultNamed current = entry.getKey();
      final int val = entry.getValue();
      if (valueBeforeKey) {
        buf.append(val).append(assignment).append(current.getName());
      } else {
        buf.append(current.getName()).append(assignment).append(val);
      }
    }
    return buf.toString().replaceFirst(separator, "");
  }

  /**
   * Converts the specified {@code Unit} to {@code Integer} map to a string for display.
   *
   * <p>If {@code valueBeforeKey} is {@code true}, the returned string will have the form (without
   * line breaks):
   *
   * <pre>
   * &lt;int1>&lt;assignment>&lt;unitTypeName1>&lt;separator>
   * &lt;int2>&lt;assignment>&lt;unitTypeName2>&lt;separator>
   * ...
   * &lt;intN>&lt;assignment>&lt;unitTypeNameN>
   * </pre>
   *
   * <p>If {@code valueBeforeKey} is {@code false}, the returned string will have the form (without
   * line breaks):
   *
   * <pre>
   * &lt;unitTypeName1>&lt;assignment>&lt;int1>&lt;separator>
   * &lt;unitTypeName2>&lt;assignment>&lt;int2>&lt;separator>
   * ...
   * &lt;unitTypeNameN>&lt;assignment>&lt;intN>
   * </pre>
   */
  public static String integerUnitMapToString(
      final IntegerMap<? extends Unit> map,
      final String separator,
      final String assignment,
      final boolean valueBeforeKey) {
    final StringBuilder buf = new StringBuilder();
    for (final Entry<? extends Unit, Integer> entry : map.entrySet()) {
      buf.append(separator);
      final Unit current = entry.getKey();
      final int val = entry.getValue();
      if (valueBeforeKey) {
        buf.append(val).append(assignment).append(current.getType().getName());
      } else {
        buf.append(current.getType().getName()).append(assignment).append(val);
      }
    }
    return buf.toString().replaceFirst(separator, "");
  }

  /**
   * Adds HTML line breaks and indentation to a string, so it wraps for things like long tooltips.
   *
   * <pre>
   * string part 1
   *           string part 2
   *           ...
   *           string part X
   * </pre>
   */
  public static String addHtmlBreaksAndIndents(
      final String target, final int firstLineMaxLength, final int maxLength) {
    final StringBuilder sb = new StringBuilder();
    final BreakIterator breakIterator = BreakIterator.getLineInstance();
    breakIterator.setText(target);
    int start = breakIterator.first();
    int end = breakIterator.next();
    int lineLength = 0;
    int currentMaxLength = firstLineMaxLength;
    while (end != BreakIterator.DONE) {
      final String word = target.substring(start, end);
      lineLength = lineLength + word.length();
      if (lineLength >= currentMaxLength) {
        sb.append("<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        lineLength = word.length() + 5; // Add 5 for the indent
        currentMaxLength = maxLength;
      }
      sb.append(word);
      start = end;
      end = breakIterator.next();
    }
    return sb.toString();
  }
}
