package games.strategy.triplea.odds.calculator.context;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * Mechanically build-gates the battle-calc core's island invariant — the core stays engine-free
 * today only by discipline, so this replaces a grep with a check that fails the build if the
 * decoupling ever erodes, preserving the seam that lets the calc run GameData-free and, later,
 * extract into its own Gradle module.
 */
class BoundedContextBoundaryTest {

  // TODO: map-XML differential fuzzing is deferred until the adapter bakes full fidelity; until
  //  then BattleCalcDifferentialTest is the standing correctness gate over the core.

  private static final String CORE = "games.strategy.triplea.odds.calculator.context..";

  @Test
  void coreNeverDependsOnTheGameEngine() {
    final JavaClasses calc =
        new ClassFileImporter().importPackages("games.strategy.triplea.odds.calculator");

    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(CORE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "games.strategy.engine..",
                "games.strategy.triplea.delegate..",
                "games.strategy.triplea.odds.calculator.adapter..")
            .because(
                "the calc core is a self-contained island that may reference only java.* and its own "
                    + "package; GameDataBattleAdapter is the sole bridge to engine types, so a core "
                    + "class reaching an engine class — directly or through the adapter — breaks the "
                    + "boundary that keeps the calc GameData-free");

    rule.check(calc);
  }
}
