package games.strategy.engine.data.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.triplea.java.collections.IntegerMap;

/**
 * Shared helpers for {@link TextSaver} implementations: encoding a nested {@link Serializable}
 * sub-state (a delegate's {@code superState} chain, say) as text when a saver exists for it and as
 * a legacy blob otherwise, and encoding an {@link IntegerMap} keyed by game entities as an array of
 * (reference, value) pairs.
 */
public final class SaverSupport {

  static final String ENCODING = "encoding";
  static final String ENCODING_TEXT = "text";
  static final String ENCODING_JAVA = "java";
  static final String ENCODING_NONE = "none";
  static final String STATE_CLASS = "stateClass";
  static final String STATE = "state";
  static final String BYTES = "bytes";

  private SaverSupport() {}

  /**
   * Encodes {@code state} as a self-describing record: natively via its registered {@link
   * TextSaver} when one exists, otherwise as a base64 legacy blob (or {@code none} for null). Read
   * back with {@link #readNested}.
   */
  public static JsonObject writeNested(
      final @Nullable Serializable state, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    if (state == null) {
      json.addProperty(ENCODING, ENCODING_NONE);
      return json;
    }
    final Optional<TextSaver<Object>> saver = SaverRegistry.forInstance(state);
    if (saver.isPresent()) {
      json.addProperty(ENCODING, ENCODING_TEXT);
      json.addProperty(STATE_CLASS, state.getClass().getName());
      json.add(STATE, saver.get().write(state, refs));
    } else {
      json.addProperty(ENCODING, ENCODING_JAVA);
      json.addProperty(BYTES, toBlob(state));
    }
    return json;
  }

  /** Inverse of {@link #writeNested}. */
  public static @Nullable Serializable readNested(
      final JsonObject json, final GameRefResolver refs) {
    final String encoding = json.get(ENCODING).getAsString();
    switch (encoding) {
      case ENCODING_NONE:
        return null;
      case ENCODING_TEXT:
        final Class<?> stateClass = loadClass(json.get(STATE_CLASS).getAsString());
        @SuppressWarnings("unchecked")
        final TextSaver<Object> saver =
            (TextSaver<Object>)
                SaverRegistry.forType(stateClass)
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                "No TextSaver for nested state " + stateClass.getName()));
        return (Serializable) saver.read(json.getAsJsonObject(STATE), refs);
      case ENCODING_JAVA:
        return (Serializable) fromBlob(json.get(BYTES).getAsString());
      default:
        throw new IllegalArgumentException("Unknown nested encoding: " + encoding);
    }
  }

  /**
   * Encodes an {@link IntegerMap} whose keys are game entities as an array of (ref, value) pairs.
   */
  public static JsonArray writeEntityIntegerMap(
      final @Nullable IntegerMap<?> map, final GameRefResolver refs) {
    final JsonArray array = new JsonArray();
    if (map == null) {
      return array;
    }
    for (final Map.Entry<?, Integer> entry : map.entrySet()) {
      final JsonObject element = new JsonObject();
      element.add("ref", refs.writeRef(entry.getKey()));
      element.addProperty("value", entry.getValue());
      array.add(element);
    }
    return array;
  }

  /** Inverse of {@link #writeEntityIntegerMap}; resolves each ref back to a {@code T}. */
  public static <T> IntegerMap<T> readEntityIntegerMap(
      final JsonArray array, final GameRefResolver refs, final Class<T> valueType) {
    final IntegerMap<T> map = new IntegerMap<>();
    for (final var element : array) {
      final JsonObject object = element.getAsJsonObject();
      final T key = valueType.cast(refs.readRef(object.getAsJsonObject("ref")));
      map.put(key, object.get("value").getAsInt());
    }
    return map;
  }

  private static String toBlob(final Serializable value) {
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
        out.writeObject(value);
      }
      return Base64.getEncoder().encodeToString(bos.toByteArray());
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Object fromBlob(final String base64) {
    final byte[] bytes = Base64.getDecoder().decode(base64);
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return in.readObject();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    } catch (final ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Class<?> loadClass(final String name) {
    try {
      return Class.forName(name);
    } catch (final ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
