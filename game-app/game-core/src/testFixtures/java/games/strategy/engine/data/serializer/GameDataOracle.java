package games.strategy.engine.data.serializer;

import com.google.gson.JsonObject;
import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.GameDataManager;
import java.io.Serializable;
import java.util.Optional;
import org.triplea.io.IoUtils;

/**
 * The verification gate for the text save format: proves that serializing then deserializing
 * reconstructs equal state. There is no {@code equals} over the {@code GameData} graph, so the
 * oracle uses the serializer under test as its own comparator — two states are "equal" when they
 * serialize to identical JSON.
 *
 * <p>Every text-save unit (a {@link TextSaver} for one state/Change/component class) is expected to
 * pass {@link #assertStateRoundTrips} for its type, and to leave {@link #assertReconstructs} green
 * for a full save that exercises it.
 */
public final class GameDataOracle {

  private GameDataOracle() {}

  /**
   * Asserts that {@code saver} round-trips {@code state}: write → read → write must produce identical
   * JSON. This is the primary per-unit gate and needs no save-file plumbing.
   */
  public static <T> void assertStateRoundTrips(
      final TextSaver<T> saver, final T state, final GameData data) {
    final GameRefResolver refs = new GameRefResolver(data);
    final JsonObject first = saver.write(state, refs);
    final T reread = saver.read(deepCopy(first), refs);
    final JsonObject second = saver.write(reread, refs);
    if (!first.equals(second)) {
      throw new AssertionError(
          "TextSaver round-trip mismatch for "
              + state.getClass().getName()
              + "\n  first write:  "
              + first
              + "\n  second write: "
              + second);
    }
  }

  /**
   * Saves {@code original} through the full text pipeline, reloads it, and asserts the reconstruction
   * matches: same game name, same delegate set, and — for every delegate whose state is written
   * natively as text — identical serialized state. Delegates still written as legacy blobs round-trip
   * through Java serialization and are not re-compared here. Returns the reloaded game for further
   * assertions.
   */
  public static GameData assertReconstructs(final GameData original) {
    final GameData reloaded;
    try {
      final byte[] bytes =
          IoUtils.writeToMemory(os -> GameDataManager.saveGameText(os, original));
      reloaded =
          IoUtils.readFromMemory(bytes, GameDataManager::loadGame)
              .orElseThrow(() -> new AssertionError("Text save failed to reload"));
    } catch (final Exception e) {
      throw new AssertionError("Text save/load round-trip threw", e);
    }

    assertEquals("gameName", original.getGameName(), reloaded.getGameName());

    final GameRefResolver originalRefs = new GameRefResolver(original);
    final GameRefResolver reloadedRefs = new GameRefResolver(reloaded);
    for (final IDelegate originalDelegate : original.getDelegates()) {
      final IDelegate reloadedDelegate = reloaded.getDelegate(originalDelegate.getName());
      if (reloadedDelegate == null) {
        throw new AssertionError("Delegate missing after reload: " + originalDelegate.getName());
      }
      assertTextStateEqual(originalDelegate, reloadedDelegate, originalRefs, reloadedRefs);
    }
    return reloaded;
  }

  private static void assertTextStateEqual(
      final IDelegate original,
      final IDelegate reloaded,
      final GameRefResolver originalRefs,
      final GameRefResolver reloadedRefs) {
    final Serializable originalState = original.saveState();
    if (originalState == null) {
      return;
    }
    final Optional<TextSaver<Object>> saver = SaverRegistry.forInstance(originalState);
    if (saver.isEmpty()) {
      return; // legacy blob delegate; Java serialization is trusted for round-trip.
    }
    final JsonObject expected = saver.get().write(originalState, originalRefs);
    final JsonObject actual = saver.get().write(reloaded.saveState(), reloadedRefs);
    if (!expected.equals(actual)) {
      throw new AssertionError(
          "Delegate state mismatch after reload for '"
              + original.getName()
              + "' ("
              + originalState.getClass().getName()
              + ")\n  expected: "
              + expected
              + "\n  actual:   "
              + actual);
    }
  }

  private static JsonObject deepCopy(final JsonObject json) {
    return json.deepCopy();
  }

  private static void assertEquals(final String field, final Object expected, final Object actual) {
    if (expected == null ? actual != null : !expected.equals(actual)) {
      throw new AssertionError(field + " mismatch: expected " + expected + " but was " + actual);
    }
  }
}
