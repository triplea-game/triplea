package tools.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** A collection of argument names common to all support tools. */
@Slf4j
@AllArgsConstructor
public enum ToolArguments {
  MAP_FOLDER("triplea.map.folder", String.class, ""),
  UNIT_HEIGHT("triplea.unit.height", Integer.class, 0),
  UNIT_WIDTH("triplea.unit.width", Integer.class, 0),
  UNIT_ZOOM("triplea.unit.zoom", Double.class, 0);

  private final String value;

  public static Optional<Path> getPropertyMapFolderPath() {
    final @Nullable String value = System.getProperty(MAP_FOLDER.value);
    if (value != null && !value.isEmpty()) {
      final Path mapFolder = Path.of(value);
      if (Files.exists(mapFolder)) {
        return Optional.of(mapFolder);
      } else {
        log.info("Could not find directory: {}", value);
      }
    }
    return Optional.empty();
  }

  public static void ifMapFolder(Consumer<? super Path> action) {
    getPropertyMapFolderPath().ifPresent(action);
  }

  public static void ifUnitWidth(Consumer<? super Integer> action) {
    getPropertyInteger(UNIT_WIDTH)
        .ifPresent(
            unitWidth -> {
              log.info("Unit Width to use: {}", unitWidth);
              action.accept(unitWidth);
            });
  }

  public static void ifUnitHeight(Consumer<? super Integer> action) {
    getPropertyInteger(UNIT_HEIGHT)
        .ifPresent(
            unitHeight -> {
              log.info("Unit Height to use: {}", unitHeight);
              action.accept(unitHeight);
            });
  }

  public static void ifUnitZoom(Consumer<? super Double> action) {
    getPropertyUnitZoom()
        .ifPresent(
            unitZoom -> {
              log.info("Unit Zoom Percent to use: {}", unitZoom);
              action.accept(unitZoom);
            });
  }

  private static Optional<Integer> getPropertyInteger(ToolArguments enumConstant) {
    final Integer value = enumConstant.getSystemPropertyValue();
    if (value == enumConstant.type.cast(enumConstant.invalidDefaultValue)) {
      return Optional.empty();
    } else {
      return Optional.of(value);
    }
  }

  public static Optional<Double> getPropertyUnitZoom() {
    final Double value = UNIT_ZOOM.getSystemPropertyValue();
    if (value == UNIT_ZOOM.type.cast(UNIT_ZOOM.invalidDefaultValue)) {
      return Optional.empty();
    } else {
      return Optional.of(value);
    }
  }

  private final Class<?> type;
  private final Object invalidDefaultValue;

  @Override
  public String toString() {
    return value;
  }

  public String getSystemProperty() {
    return System.getProperty(value);
  }

  @SuppressWarnings("unchecked")
  private <T> T getSystemPropertyValue() {
    final String prop = System.getProperty(value);
    if (prop == null) return (T) invalidDefaultValue;

    try {
      if (type == Integer.class) {
        return (T) Integer.valueOf(prop);
      } else if (type == Double.class) {
        return (T) Double.valueOf(prop);
      }
    } catch (NumberFormatException e) {
      log.error("System property value {} with unexpected format {}", value, prop, e);
      return (T) invalidDefaultValue;
    }

    // default: treat as string
    return (T) prop;
  }

  /** Generic setter that enforces correct type */
  public void setSystemProperty(Object newValue) {
    if (newValue == null) {
      System.clearProperty(value);
      return;
    }

    if (!type.isInstance(newValue)) {
      throw new IllegalArgumentException(
          "Invalid type for " + value + ": expected " + type.getSimpleName());
    }

    System.setProperty(value, newValue.toString());
  }
}
