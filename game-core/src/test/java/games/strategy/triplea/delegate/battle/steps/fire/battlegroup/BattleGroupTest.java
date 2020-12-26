package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Test;

class BattleGroupTest {

  @Test
  void useOffenseReturnFireByDefault() {
    final BattleGroup battleGroup =
        BattleGroup.builder().casualtiesOnOffenseReturnFire(false).build();

    assertThat(
        "The predicate should use the passed in return fire value",
        battleGroup.getCasualtiesOnOffenseReturnFirePredicate().test(List.of()),
        is(false));
  }

  @Test
  void useOffenseReturnFirePredicateIfSet() {
    final BattleGroup battleGroup =
        BattleGroup.builder()
            .casualtiesOnOffenseReturnFire(false)
            .casualtiesOnOffenseReturnFirePredicate(units -> true)
            .build();

    assertThat(
        "The predicate should use the passed in predicate function",
        battleGroup.getCasualtiesOnOffenseReturnFirePredicate().test(List.of()),
        is(true));
  }

  @Test
  void useDefenseReturnFireByDefault() {
    final BattleGroup battleGroup =
        BattleGroup.builder().casualtiesOnDefenseReturnFire(false).build();

    assertThat(
        "The predicate should use the passed in return fire value",
        battleGroup.getCasualtiesOnDefenseReturnFirePredicate().test(List.of()),
        is(false));
  }

  @Test
  void useDefenseReturnFirePredicateIfSet() {
    final BattleGroup battleGroup =
        BattleGroup.builder()
            .casualtiesOnDefenseReturnFire(false)
            .casualtiesOnDefenseReturnFirePredicate(units -> true)
            .build();

    assertThat(
        "The predicate should use the passed in predicate function",
        battleGroup.getCasualtiesOnDefenseReturnFirePredicate().test(List.of()),
        is(true));
  }
}
