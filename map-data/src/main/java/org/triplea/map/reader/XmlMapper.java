package org.triplea.map.reader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import javax.xml.stream.XMLStreamReader;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class XmlMapper {
  private XMLStreamReader xmlStreamReader;

  public <T> T mapXmlToClass(final Class<T> pojo) {
    try {
      final T instance = pojo.getDeclaredConstructor().newInstance();
      for (final Field field : pojo.getDeclaredFields()) {
        if (field.getAnnotation(Attribute.class) != null) {
          field.setAccessible(true);
          field.set(instance, xmlStreamReader.getAttributeValue(null, field.getName()));
        }
      }
      return instance;
    } catch (final IllegalAccessException
        | NoSuchMethodException
        | InstantiationException
        | InvocationTargetException e) {
      throw new RuntimeException("Class: " + pojo.getCanonicalName(), e);
    }
  }
}
