package games.strategy.triplea.formatter;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import games.strategy.engine.data.DefaultNamed;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.util.UnitOwner;
import games.strategy.util.IntegerMap;

/**
 * Provides useful methods for converting things to text.
 */
public class MyFormatter {
  /**
   * Some exceptions to the rules.
   */
  private static final Map<String, String> plural;

  static {
    plural = new HashMap<>();
    plural.put("armour", "armour");
    plural.put("infantry", "infantry");
    plural.put("artillery", "artilleries");
    plural.put("factory", "factories");
  }

  public static String unitsToTextNoOwner(final Collection<Unit> units) {
    return unitsToTextNoOwner(units, null);
  }

  public static String unitsToTextNoOwner(final Collection<Unit> units, final PlayerID owner) {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    for (final Unit unit : units) {
      if ((owner == null) || owner.equals(unit.getOwner())) {
        map.add(unit.getType(), 1);
      }
    }
    final StringBuilder buf = new StringBuilder();
    // sort on unit name
    final List<UnitType> sortedList = new ArrayList<>(map.keySet());
    Collections.sort(sortedList, Comparator.comparing(UnitType::getName));
    int count = map.keySet().size();
    for (final UnitType type : sortedList) {
      final int quantity = map.getInt(type);
      buf.append(quantity);
      buf.append(" ");
      buf.append((quantity > 1) ? pluralize(type.getName()) : type.getName());
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
   * Converts the specified unit collection to a textual representation that describes the quantity of each distinct
   * unit type owned by each distinct player.
   *
   * @param units The collection of units.
   *
   * @return A textual representation of the specified unit collection.
   */
  public static String unitsToText(final Collection<Unit> units) {
    checkNotNull(units);

    final Map<UnitOwner, Long> quantitiesByOwner = units.stream()
        .map(UnitOwner::new)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    final StringBuilder buf = new StringBuilder();
    final AtomicInteger countRef = new AtomicInteger(quantitiesByOwner.size());
    quantitiesByOwner.forEach((owner, quantity) -> {
      buf.append(quantity);
      buf.append(" ");
      buf.append((quantity > 1) ? pluralize(owner.getType().getName()) : owner.getType().getName());
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
    return ((quantity == -1) || (quantity == 1)) ? in : pluralize(in);
  }

  public static String pluralize(final String in) {
    if (plural.containsKey(in)) {
      return plural.get(in);
    }
    if (in.endsWith("man")) {
      return in.substring(0, in.lastIndexOf("man")) + "men";
    }
    return in + "s";
  }

  public static String attachmentNameToText(final String attachmentGetName) {
    String toText = attachmentGetName;
    if (attachmentGetName.startsWith(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME)) {
      toText = attachmentGetName.replaceFirst(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME, "Relationship Type ");
    } else if (attachmentGetName.startsWith(Constants.TECH_ATTACHMENT_NAME)) {
      toText = attachmentGetName.replaceFirst(Constants.TECH_ATTACHMENT_NAME, "Player Techs ");
    } else if (attachmentGetName.startsWith(Constants.UNIT_ATTACHMENT_NAME)) {
      toText = attachmentGetName.replaceFirst(Constants.UNIT_ATTACHMENT_NAME, "Unit Type Properties ");
    } else if (attachmentGetName.startsWith(Constants.TERRITORY_ATTACHMENT_NAME)) {
      toText = attachmentGetName.replaceFirst(Constants.TERRITORY_ATTACHMENT_NAME, "Territory Properties ");
    } else if (attachmentGetName.startsWith(Constants.CANAL_ATTACHMENT_PREFIX)) {
      toText = attachmentGetName.replaceFirst(Constants.CANAL_ATTACHMENT_PREFIX, "Canal ");
    } else if (attachmentGetName.startsWith(Constants.TERRITORYEFFECT_ATTACHMENT_NAME)) {
      toText = attachmentGetName.replaceFirst(Constants.TERRITORYEFFECT_ATTACHMENT_NAME, "Territory Effect ");
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
      toText = attachmentGetName.replaceFirst(Constants.PLAYER_ATTACHMENT_NAME, "Player Properties ");
    } else if (attachmentGetName.startsWith(Constants.POLITICALACTION_ATTACHMENT_PREFIX)) {
      toText = attachmentGetName.replaceFirst(Constants.POLITICALACTION_ATTACHMENT_PREFIX, "Political Action ");
    } else if (attachmentGetName.startsWith(Constants.POLITICALACTION_ATTACHMENT_PREFIX)) {
      toText = attachmentGetName.replaceFirst(Constants.USERACTION_ATTACHMENT_PREFIX, "Action ");
    } else if (attachmentGetName.startsWith(Constants.TECH_ABILITY_ATTACHMENT_NAME)) {
      toText = attachmentGetName.replaceFirst(Constants.TECH_ABILITY_ATTACHMENT_NAME, "Tech Properties ");
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
    return listOfArrays.stream()
        .map(Arrays::toString)
        .collect(Collectors.joining(",","[","]"));
  }

  public static String asDice(final DiceRoll roll) {
    if ((roll == null) || (roll.size() == 0)) {
      return "none";
    }
    final StringBuilder buf = new StringBuilder();
    for (int i = 0; i < roll.size(); i++) {
      buf.append(roll.getDie(i).getValue() + 1);
      if ((i + 1) < roll.size()) {
        buf.append(",");
      }
    }
    return buf.toString();
  }

  public static String asDice(final int[] rolls) {
    if ((rolls == null) || (rolls.length == 0)) {
      return "none";
    }
    final StringBuilder buf = new StringBuilder(rolls.length * 2);
    for (int i = 0; i < rolls.length; i++) {
      buf.append(rolls[i] + 1);
      if ((i + 1) < rolls.length) {
        buf.append(",");
      }
    }
    return buf.toString();
  }

  public static String asDice(final List<Die> rolls) {
    if ((rolls == null) || (rolls.size() == 0)) {
      return "none";
    }
    final StringBuilder buf = new StringBuilder(rolls.size() * 2);
    for (int i = 0; i < rolls.size(); i++) {
      buf.append(rolls.get(i).getValue() + 1);
      if ((i + 1) < rolls.size()) {
        buf.append(",");
      }
    }
    return buf.toString();
  }

  public static String defaultNamedToTextList(final Collection<? extends DefaultNamed> list) {
    return defaultNamedToTextList(list, ", ", false);
  }

  public static String defaultNamedToTextList(final Collection<? extends DefaultNamed> list, final String seperator,
      final boolean showQuantity) {
    final IntegerMap<DefaultNamed> map = new IntegerMap<>();
    for (final DefaultNamed unit : list) {
      if ((unit == null) || (unit.getName() == null)) {
        throw new IllegalStateException("Unit or Resource no longer exists?!?");
      }
      map.add(unit, 1);
    }
    final StringBuilder buf = new StringBuilder();
    // sort on unit name
    final List<DefaultNamed> sortedList = new ArrayList<>(map.keySet());
    Collections.sort(sortedList, Comparator.comparing(DefaultNamed::getName));
    int count = map.keySet().size();
    for (final DefaultNamed type : sortedList) {
      if (showQuantity) {
        final int quantity = map.getInt(type);
        buf.append(quantity);
        buf.append(" ");
        buf.append((quantity > 1) ? pluralize(type.getName()) : type.getName());
      } else {
        buf.append(type.getName());
      }
      count--;
      if (count > 1) {
        buf.append(seperator);
      }
      if (count == 1) {
        buf.append(" and ");
      }
    }
    return buf.toString();
  }

  public static String integerDefaultNamedMapToString(final IntegerMap<? extends DefaultNamed> map,
      final String separator, final String assignment, final boolean valueBeforeKey) {
    final StringBuilder buf = new StringBuilder("");
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

  public static String integerUnitMapToString(final IntegerMap<? extends Unit> map, final String separator,
      final String assignment, final boolean valueBeforeKey) {
    final StringBuilder buf = new StringBuilder("");
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

  /** Creates a new instance of MyFormatter. */
  private MyFormatter() {}
}


