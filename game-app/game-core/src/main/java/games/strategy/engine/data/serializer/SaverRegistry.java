package games.strategy.engine.data.serializer;

import com.google.common.reflect.ClassPath;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps a state/Change/component class to the {@link TextSaver} that serializes it.
 *
 * <p>Savers are discovered <b>additively</b>: on first use the registry scans the {@code
 * serializer} package tree for public {@link TextSaver} implementations with a public no-argument
 * constructor and registers each by its {@link TextSaver#type()}. A new unit therefore only adds its
 * own saver class — no central dispatch file or service index is edited, which is what keeps the
 * fan-out free of merge conflicts on shared infrastructure.
 */
@Slf4j
public final class SaverRegistry {

  private static final String SCAN_PACKAGE = "games.strategy.engine.data.serializer";

  private static final Map<Class<?>, TextSaver<?>> byType = new ConcurrentHashMap<>();
  private static volatile boolean scanned = false;

  private SaverRegistry() {}

  /** Registers a saver explicitly (used by tests; production relies on the classpath scan). */
  public static <T> void register(final TextSaver<T> saver) {
    byType.put(saver.type(), saver);
  }

  /** Returns the saver for exactly {@code type}, if one is registered. */
  @SuppressWarnings("unchecked")
  public static <T> Optional<TextSaver<T>> forType(final Class<T> type) {
    ensureScanned();
    return Optional.ofNullable((TextSaver<T>) byType.get(type));
  }

  /** Returns the saver for {@code value}'s exact runtime class, if one is registered. */
  public static Optional<TextSaver<Object>> forInstance(final @Nullable Object value) {
    if (value == null) {
      return Optional.empty();
    }
    ensureScanned();
    @SuppressWarnings("unchecked")
    final TextSaver<Object> saver = (TextSaver<Object>) byType.get(value.getClass());
    return Optional.ofNullable(saver);
  }

  private static void ensureScanned() {
    if (scanned) {
      return;
    }
    synchronized (SaverRegistry.class) {
      if (scanned) {
        return;
      }
      scanClasspath();
      scanned = true;
    }
  }

  private static void scanClasspath() {
    try {
      final ClassPath classPath = ClassPath.from(SaverRegistry.class.getClassLoader());
      for (final ClassPath.ClassInfo info :
          classPath.getTopLevelClassesRecursive(SCAN_PACKAGE)) {
        registerIfSaver(info);
      }
    } catch (final IOException e) {
      log.error("Failed to scan classpath for TextSaver implementations", e);
    }
  }

  private static void registerIfSaver(final ClassPath.ClassInfo info) {
    final Class<?> clazz;
    try {
      clazz = info.load();
    } catch (final LinkageError e) {
      return;
    }
    if (!TextSaver.class.isAssignableFrom(clazz)
        || clazz.isInterface()
        || Modifier.isAbstract(clazz.getModifiers())) {
      return;
    }
    try {
      final TextSaver<?> saver =
          (TextSaver<?>) clazz.getDeclaredConstructor().newInstance();
      register(saver);
    } catch (final ReflectiveOperationException e) {
      log.warn("TextSaver {} has no usable public no-arg constructor; skipping", clazz.getName(), e);
    }
  }
}
