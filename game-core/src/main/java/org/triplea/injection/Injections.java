package org.triplea.injection;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import org.triplea.util.Version;

/**
 * Injections is a dependency-injection like object that is initialized at a top level sub-project,
 * early in a main method, and then can be accessed by lower sub-systems that need implementation
 * details.
 *
 * <p>For example, if we have headed and headless code that both need to show an error message, we
 * can have the game-headed main method inject a GUI dependent strategy to do this and respectively
 * the same for game-headless. This way we can simply access the error message strategy and use it
 * rather than do any checks for if the current game instance is headless or not.
 *
 * <p>Design note, favor injecting data from 'Injections' into constructors, do not use Injections
 * in a static way and avoid injecting the full 'Injections' object. For example:
 *
 * <h3>Do This </h3>
 *
 * <pre><code>
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
 * </code></pre>
 *
 * <h3>Do *Not* Do This </h3>
 *
 * <pre><code>
 *   @AllArgsConstructor
 *   class SomeClass {
 *     void showError() {
 *       String error = "some error message";
 *
 *       // bad, this is an example of static coupling
 *       Injections.getInstance().getErrorMessageStrategy().showErrorMessage(error);
 *     }
 *   }
 *   </code></pre>
 *
 * *
 *
 * <h3>Do *Not* Do This </h3>
 *
 * <pre><code>
 *   class SomeClass {
 *     // this is bad as any test has to new-up a full Injections object.
 *     private final Injections injections;
 *  }
 *  </code></pre>
 */
@Builder
@Getter
public final class Injections {
  @Getter private static Injections instance;

  private final Version engineVersion;

  public static synchronized void init(final Injections injections) {
    Preconditions.checkState(getInstance() == null);
    instance = injections;
  }
}
