package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility for getting/setting java bean style properties on an object.
 */
public class PropertyUtil {

  public static void set(final String propertyName, final Object value, final Object subject) {
    final Method m = getSetter(propertyName, subject, value);
    try {
      m.setAccessible(true);
      m.invoke(subject, value);
    } catch (final Exception e) {
      throw new IllegalStateException(
          "Could not set property:" + propertyName + " subject:" + subject + " new value:" + value, e);
    }
  }

  private static Field getFieldIncludingFromSuperClasses(final Class<?> c, final String name,
      final boolean justFromSuper) {
    if (!justFromSuper) {
      try {
        return c.getDeclaredField(name); // TODO: unchecked reflection
      } catch (final NoSuchFieldException e) {
        return getFieldIncludingFromSuperClasses(c, name, true);
      }
    }

    if (c.getSuperclass() == null) {
      throw new IllegalStateException("No such Property Field: " + name);
    }
    try {
      return c.getSuperclass().getDeclaredField(name); // TODO: unchecked reflection
    } catch (final NoSuchFieldException e) {
      return getFieldIncludingFromSuperClasses(c.getSuperclass(), name, true);
    }
  }

  public static Object getPropertyFieldObject(final String propertyName, final Object subject) {
    try {
      final Field field = getPropertyField(propertyName, subject);
      field.setAccessible(true);
      return field.get(subject);
    } catch (final Exception e) {
      final String msg =
          "No such Property Field named: " + "m_" + propertyName + ", or: " + propertyName + ", for Subject: "
              + subject.toString();
      throw new IllegalStateException(msg, e);
    }
  }

  private static Field getPropertyField(final String propertyName, final Object subject) {
    return getPropertyField(propertyName, subject.getClass());
  }

  /**
   * Gets the backing field for the property with the specified name in the specified type.
   *
   * @param propertyName The property name.
   * @param type The type that hosts the property.
   *
   * @return The backing field for the specified property.
   *
   * @throws IllegalStateException If no backing field for the specified property exists.
   */
  public static Field getPropertyField(final String propertyName, final Class<?> type) {
    checkNotNull(propertyName);
    checkNotNull(type);

    try {
      return getFieldIncludingFromSuperClasses(type, "m_" + propertyName, false);
    } catch (final IllegalStateException ignored) {
      return getFieldIncludingFromSuperClasses(type, propertyName, false);
    }
  }

  private static String capitalizeFirstLetter(final String str) {
    char first = str.charAt(0);
    first = Character.toUpperCase(first);
    return first + str.substring(1);
  }

  private static Method getSetter(final String propertyName, final Object subject, final Object value) {
    final String setterName = "set" + capitalizeFirstLetter(propertyName);
    // for (final Method m : subject.getClass().getDeclaredMethods())
    for (final Method m : subject.getClass().getMethods()) {
      if (m.getName().equals(setterName)) {
        try {
          final Class<?> argType = value.getClass();
          return subject.getClass().getMethod(setterName, argType);
        } catch (final NoSuchMethodException | NullPointerException e) {
          // TODO: do not catch NPE, that is control flow by exception handling,
          // instead detect the null value and return 'm' at that time.

          // Go ahead and try the first one
          return m;
        }
      }
    }
    throw new IllegalStateException("No method called:" + setterName + " on:" + subject);
  }
}
