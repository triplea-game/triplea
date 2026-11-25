package games.strategy.triplea.odds.calculator.adapter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import games.strategy.engine.data.GameData;
import org.junit.jupiter.api.Test;

/**
 * Build-gates the lock-free bake decision (design §3): the bounded-context bake reads the live
 * {@code GameData} without holding a lock, guarding against a mid-walk mutation by shape (snapshot
 * plus retry-once) rather than by blocking. Turning that into a check means a stray {@code
 * acquireReadLock}/{@code acquireWriteLock} in the adapter or the calculator fails the build
 * instead of silently reintroducing the whole-game blocking the seam removed.
 */
class NoLockingArchTest {

  private static final String ADAPTER = "games.strategy.triplea.odds.calculator.adapter..";
  private static final String CALCULATOR =
      "games.strategy.triplea.odds.calculator.BoundedContextBattleCalculator";

  @Test
  void bakePathNeverLocksTheGame() {
    final JavaClasses classes =
        new ClassFileImporter().importPackages("games.strategy.triplea.odds.calculator");

    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(ADAPTER)
            .or()
            .haveFullyQualifiedName(CALCULATOR)
            .should()
            .callMethod(GameData.class, "acquireReadLock")
            .orShould()
            .callMethod(GameData.class, "acquireWriteLock")
            .because(
                "the bounded-context bake is lock-free by design; it snapshots the caller's forces "
                    + "and retries once on a concurrent-modification rather than blocking the live "
                    + "game, so taking a GameData lock here would undo that decision");

    rule.check(classes);
  }
}
