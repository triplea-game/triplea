package org.triplea.generic.xml.reader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class XmlMapper {
  private XMLStreamReader xmlStreamReader;

  public <T> T mapXmlToClass(final Class<T> pojo) throws XMLStreamException {
    // Todo: do validation on which kinds of tags and types we have.
    // for example, a '@Tag' should not be on a list.

    try {
      Constructor<T> constructor = pojo.getDeclaredConstructor();
      constructor.setAccessible(true);
      final T instance = constructor.newInstance();

      XmlParser.TagParser tagParser = new XmlParser.TagParser(pojo.getSimpleName());
      boolean hasNestedTags = false;

      for (final Field field : pojo.getDeclaredFields()) {
        if (field.getAnnotation(Attribute.class) != null) {
          field.setAccessible(true);

          final String value =
              Optional.ofNullable(xmlStreamReader.getAttributeValue(null, field.getName()))
                  .orElseGet(() -> field.getAnnotation(Attribute.class).defaultValue());
          field.set(instance, value);
        }
        if (field.getAnnotation(Tag.class) != null) {
          field.setAccessible(true);
          tagParser.childTagHandler(
              field.getName(), () -> field.set(instance, mapXmlToClass(field.getType())));
          hasNestedTags = true;
        } else if (field.getAnnotation(TagList.class) != null) {
          field.setAccessible(true);
          if (field.getType() == List.class) {
            final List tagList = new ArrayList();
            field.setAccessible(true);
            field.set(instance, tagList);

            final Class<?> listType = field.getAnnotation(TagList.class).value();
            tagParser.childTagHandler(
                listType.getSimpleName(), () -> tagList.add(mapXmlToClass(listType)));

            hasNestedTags = true;
          }
        }
      }
      if (hasNestedTags) {
        tagParser.parse(xmlStreamReader);
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
