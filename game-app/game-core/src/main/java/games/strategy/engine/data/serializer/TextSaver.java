package games.strategy.engine.data.serializer;

import com.google.gson.JsonObject;

/**
 * An explicit text (JSON) serializer for a single type {@code T}, used by the text save format to
 * replace Java serialization. Each implementation owns exactly one type and defines an explicit
 * contract for its persisted state, independent of the type's Java field layout.
 *
 * <p>Implementations are discovered additively by {@link SaverRegistry}: adding a new saver is a
 * matter of adding a class to the {@code serializer} package tree with a public no-argument
 * constructor. No central dispatch file is edited, which is what lets the format be built out by
 * many independent units without merge conflicts.
 *
 * <p>References to other game entities must be written and read through {@link GameRefResolver} (by
 * name for static entities, by UUID for {@code Unit}), never by embedding the referenced entity's
 * own state.
 *
 * @param <T> the type this saver handles.
 */
public interface TextSaver<T> {

  /** The exact runtime type this saver handles; used as the registry key. */
  Class<T> type();

  /** Writes the persisted state of {@code value} to a fresh JSON object. */
  JsonObject write(T value, GameRefResolver refs);

  /** Reconstructs a {@code T} from JSON previously produced by {@link #write}. */
  T read(JsonObject json, GameRefResolver refs);
}
