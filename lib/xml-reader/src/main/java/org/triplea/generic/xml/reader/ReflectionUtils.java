package org.triplea.generic.xml.reader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import lombok.experimental.UtilityClass;
import org.triplea.generic.xml.reader.exceptions.JavaDataModelException;

@UtilityClass
class ReflectionUtils {
  <T> T newInstance(final Class<T> pojo) throws JavaDataModelException {
    try {
      final Constructor<T> constructor = pojo.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (final IllegalAccessException e) {
      throw new JavaDataModelException(
          "Unexpected illegal access error while constructing class", e);
    } catch (final NoSuchMethodException e) {
      throw new JavaDataModelException(
          "Cannot instantiate, make sure class is static if it is a nested class, "
              + "and has a no-args constructor.",
          e);
    } catch (final InvocationTargetException e) {
      throw new JavaDataModelException(
          "An exception was thrown when invoking the no arg constructor. This not expected, the "
              + "no-args constructor is expected to a no-op, a simple default constructor.",
          e);
    } catch (final InstantiationException e) {
      throw new JavaDataModelException(
          "Unable to instantiate class, check that it is not marked as abstract "
              + "and is not an interface",
          e);
    }
  }

  @SuppressWarnings("unchecked")
  <T> Class<T> getGenericType(final Field field) {
    final ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
    return (Class<T>) parameterizedType.getActualTypeArguments()[0];
  }
}
