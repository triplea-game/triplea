package org.triplea.injection;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import java.util.Collection;
import lombok.Builder;
import lombok.Getter;
import org.triplea.util.Version;

/**
 * Injections is a DIY dependency-injection mechanism. It is useful for injecting behavior or data
 * from a top level subproject into a subproject at a lower layer. For example, let's say a
 * top-level project has a UI and a bottom layer project does not. We can create an interface at the
 * top-level that sends data to the UI, bind that in Injections, then use the Injections binding at
 * the lower layer to send data to the UI (even though the lower layer does not have access to the
 * UI).
 *
 * <p>Example:
 *
 * <pre>{@code
 * class Injections {
 *    private final Consumer<String> errorReporter;
 * }
 *
 *
 * ## In a top level sub-project
 *  Injections.builder()
 *     .errorReporter(errorMessage -> doStuffSpecificToTopLevelProjectLikeUiWork(errorMessage);
 *     :
 *     :
 *
 *
 * ## Using injections
 *
 * @AllArgsConstructor
 * class UsesInjections {
 *    private final Consumer<String> errorReporter;
 *
 *    void foo() {
 *       errorReporter.accept(
 *          "This is an error message that will be displayed in UI and this sup-project does " +
 *          "not depend or know about UI");
 *    }
 * }
 * }</pre>
 *
 * <h2>Note on constructor Injection</h2>
 *
 * Generaly, Injections.getInsance().getXXX() should only be used to pass arguments to constructors.
 * Injections.getInstance() should never be used outside of passing parameters to a constructor.
 *
 * <p>Do *not* use Injections as a singleton, do *not* use Injections to create static coupling.
 *
 * <h3>Example of constructor injection - Do This </h3>
 *
 * <pre>{@code
 * @AllArgsConstructor
 * class SomeClass {
 *   private final Consumer<String> errorMessageStrategy;
 *
 *   void showError() {
 *     String error = "some error message";
 *     injections.getErrorMessageStrategy().showErrorMessage(error);
 *   }
 *
 *   static SomeClass factoryMethod() {
 *     return new SomeClass(Injectisons.getInstance().getErrorMessageStrategy());
 *   }
 * }
 * }</pre>
 *
 * <h3>Do *Not* Do This </h3>
 *
 * <pre>{@code
 * @AllArgsConstructor
 * class SomeClass {
 *   void showError() {
 *     String error = "some error message";
 *
 *     // bad, this is an example of static coupling
 *     Injections.getInstance().getErrorMessageStrategy().showErrorMessage(error);
 *   }
 * }
 * }</pre>
 */
@Builder
@Getter
public final class Injections {
  @Getter private static Injections instance;

  private final Version engineVersion;
  private final Collection<PlayerTypes.Type> playerTypes;

  public static synchronized void init(final Injections injections) {
    Preconditions.checkState(getInstance() == null);
    instance = injections;
  }
}
