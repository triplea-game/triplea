package games.strategy.triplea.odds.calculator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.util.Tuple;

/**
 * Parses and validates 'order of loss' (OOL) strings, which choose casualty order, eg {@code
 * *^infantry;1^artillery}.
 */
final class OrderOfLoss {
  static final char OOL_SEPARATOR = ';';
  static final char OOL_AMOUNT_DESCRIPTOR = '^';
  static final String OOL_ALL = "*";

  private OrderOfLoss() {}

  /** Validates if a given string can be parsed for order of losses. */
  static boolean isValidOrderOfLoss(final String orderOfLoss, final GameData data) {
    if (orderOfLoss == null || orderOfLoss.isBlank()) {
      return true;
    }
    try {
      final UnitTypeList unitTypes;
      try (GameData.Unlocker ignored = data.acquireReadLock()) {
        unitTypes = data.getUnitTypeList();
      }
      for (final String section : splitOrderOfLoss(orderOfLoss)) {
        if (section.isEmpty()) {
          continue;
        }
        final String[] amountThenType = splitOrderOfLossSection(section);
        if (amountThenType.length != 2) {
          return false;
        }
        if (!amountThenType[0].equals(OOL_ALL)) {
          final int amount = Integer.parseInt(amountThenType[0]);
          if (amount <= 0) {
            return false;
          }
        }
        if (unitTypes.getUnitType(amountThenType[1]).isEmpty()) {
          return false;
        }
      }
    } catch (final Exception e) {
      return false;
    }
    return true;
  }

  @VisibleForTesting
  static Iterable<String> splitOrderOfLoss(final String orderOfLoss) {
    return Splitter.on(OOL_SEPARATOR).split(orderOfLoss.trim());
  }

  @VisibleForTesting
  static String[] splitOrderOfLossSection(final String orderOfLossSection) {
    return Iterables.toArray(
        Splitter.on(OOL_AMOUNT_DESCRIPTOR).split(orderOfLossSection), String.class);
  }

  /** Returns units in the same ordering as the 'order of loss' string passed in. */
  static List<Unit> getUnitListByOrderOfLoss(
      final String ool, final Collection<Unit> units, final GameState data) {
    if (ool == null || ool.isBlank()) {
      return null;
    }
    final List<Tuple<Integer, UnitType>> map = new ArrayList<>();
    for (final String section : splitOrderOfLoss(ool)) {
      if (section.isEmpty()) {
        continue;
      }
      final String[] amountThenType = splitOrderOfLossSection(section);
      final int amount =
          amountThenType[0].equals(OOL_ALL)
              ? Integer.MAX_VALUE
              : Integer.parseInt(amountThenType[0]);
      final UnitType type = data.getUnitTypeList().getUnitTypeOrThrow(amountThenType[1]);
      map.add(Tuple.of(amount, type));
    }
    Collections.reverse(map);
    final Set<Unit> unitsLeft = new HashSet<>(units);
    final List<Unit> order = new ArrayList<>();
    for (final Tuple<Integer, UnitType> section : map) {
      final List<Unit> unitsOfType =
          CollectionUtils.getNMatches(
              unitsLeft, section.getFirst(), Matches.unitIsOfType(section.getSecond()));
      order.addAll(unitsOfType);
      unitsLeft.removeAll(unitsOfType);
    }
    Collections.reverse(order);
    return order;
  }
}
