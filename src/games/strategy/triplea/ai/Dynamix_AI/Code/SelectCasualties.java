package games.strategy.triplea.ai.Dynamix_AI.Code;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.net.GUID;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.ai.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.ai.Dynamix_AI.CommandCenter.GlobalCenter;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;


public class SelectCasualties {
  private static boolean useDefaultSelectionThisTime = false;

  public static void NotifyCasualtySelectionError(final String error) {
    if (error.equals("Wrong number of casualties selected")) {
      DUtils.Log(Level.FINER, "  Wrong number of casualties selected for current battle, so attempting to use default casualties");
      useDefaultSelectionThisTime = true;
    }
  }

  @SuppressWarnings("unchecked")
  public static CasualtyDetails selectCasualties(final Dynamix_AI ai, final GameData data, final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents, final int count,
      final String message, final DiceRoll dice, final PlayerID hit, final CasualtyList defaultCasualties, final GUID battleID,
      final boolean allowMultipleHitsPerUnit) {
    ai.pause();
    final HashSet<Unit> damaged = new HashSet<Unit>();
    final HashSet<Unit> destroyed = new HashSet<Unit>();
    if (useDefaultSelectionThisTime) {
      useDefaultSelectionThisTime = false;
      damaged.addAll(defaultCasualties.getDamaged());
      destroyed.addAll(defaultCasualties.getKilled());
      /*
       * for (Unit unit : defaultCasualties)
       * {
       * boolean twoHit = UnitAttachment.get(unit.getType()).isTwoHit();
       * //If it appears in casualty list once, it's damaged, if twice, it's damaged and additionally destroyed
       * if (unit.getHits() == 0 && twoHit && !damaged.contains(unit))
       * damaged.add(unit);
       * else
       * destroyed.add(unit);
       * }
       */
    } else {
      while (damaged.size() + destroyed.size() < count) {
        Unit untouchedTwoHitUnit = null;
        for (final Unit unit : selectFrom) {
          final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
          if (allowMultipleHitsPerUnit && ua.getHitPoints() > 1 + unit.getHits() && !damaged.contains(unit)) // If this is an undamaged,
                                                                                                             // un-selected as casualty, two
                                                                                                             // hit unit
          {
            untouchedTwoHitUnit = unit;
            break;
          }
        }
        if (untouchedTwoHitUnit != null) // We try to damage untouched two hit units first, if there are any
        {
          damaged.add(untouchedTwoHitUnit);
          continue;
        }
        Unit highestScoringUnit = null;
        float highestScore = Integer.MIN_VALUE;
        for (final Unit unit : selectFrom) // Problem with calcing for the best unit to select as a casualties is that the battle calculator
                                           // needs to call this very method to calculate the battle, resulting in a never ending loop!
        {
          final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
          // TripleAUnit ta = TripleAUnit.get(unit);
          if (destroyed.contains(unit)) {
            continue;
          }
          float score = 0;
          score -= DUtils.GetTUVOfUnit(unit, GlobalCenter.GetPUResource());
          score -= DUtils.GetValueOfUnits(Collections.singleton(unit)); // Valuable units should get killed later
          if (dependents.containsKey(unit)) {
            score -= 1000;
          }
          if (ua.getHitPoints() > 1 && (unit.getHits() > 0 || damaged.contains(unit))) {
            score -= 100 * ua.getHitPoints();
          }
          if (score > highestScore) {
            highestScore = score;
            highestScoringUnit = unit;
          }
        }
        if (highestScoringUnit != null) {
          destroyed.add(highestScoringUnit);
          continue;
        }
      }
    }
    DUtils.Log(Level.FINER, "  Casualties selected. Damaged: {0}, Destroyed {1}", damaged, destroyed);
    final CasualtyDetails m2 = new CasualtyDetails(DUtils.ToList(destroyed), DUtils.ToList(damaged), false);
    return m2;
  }
}
