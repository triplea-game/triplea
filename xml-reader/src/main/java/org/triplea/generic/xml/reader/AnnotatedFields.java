package org.triplea.generic.xml.reader;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.BodyText;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;
import org.triplea.generic.xml.reader.exceptions.JavaDataModelException;

/**
 * Sorts fields, validates fields, and sets fields to accessible. Field validation will check that
 * we see the right annotations on the right class types, for example that '@TagList' is on only
 * 'java.util.List'.
 *
 * @param <T> Java model object class type (essentially the tag we are currently creating as a java
 *     object).
 */
@Getter
class AnnotatedFields<T> {
  private final List<Field> attributeFields = new ArrayList<>();
  private final List<Field> tagFields = new ArrayList<>();
  private final List<Field> tagListFields = new ArrayList<>();
  private final List<Field> bodyTextFields = new ArrayList<>();

  AnnotatedFields(final Class<T> pojo) throws JavaDataModelException {
    for (final Field field : pojo.getDeclaredFields()) {
      validateAnnotations(field);
      if (field.getAnnotation(Attribute.class) != null) {
        field.setAccessible(true);
        attributeFields.add(field);
      } else if (field.getAnnotation(Tag.class) != null) {
        field.setAccessible(true);
        tagFields.add(field);
      } else if (field.getAnnotation(TagList.class) != null) {
        field.setAccessible(true);
        tagListFields.add(field);
      } else if (field.getAnnotation(BodyText.class) != null) {
        field.setAccessible(true);
        bodyTextFields.add(field);
      }
    }
    if (bodyTextFields.size() > 1) {
      throw new JavaDataModelException(
          "Too many body text fields, can only have one on any given class");
    }
    if (!bodyTextFields.isEmpty() && (!tagFields.isEmpty() && !tagListFields.isEmpty())) {
      throw new JavaDataModelException(
          "Illegal combination of annoations, may only have attributes and a body text,"
              + "or attributes and tags (or taglist), but may not have both body text and tags.");
    }
  }

  private static <T> void validateAnnotations(final Field field) throws JavaDataModelException {

    int annotationCount = 0;

    if (field.getAnnotation(Attribute.class) != null) {
      if (field.getType() != String.class
          && field.getType() != Boolean.TYPE
          && field.getType() != Boolean.class
          && field.getType() != Integer.TYPE
          && field.getType() != Integer.class
          && field.getType() != Double.TYPE
          && field.getType() != Double.class) {
        throw new JavaDataModelException(
            field,
            "Illegal location of @Attribute, may only be placed on types: "
                + "String, int, Integer, double, Double, boolean, and Boolean");
      }
      final Attribute attributeAnnotation = field.getAnnotation(Attribute.class);

      if ((field.getType() != Boolean.TYPE && field.getType() != Boolean.class)
          && attributeAnnotation.defaultBoolean()) {
        throw new JavaDataModelException(
            field, "Illegal defaultBoolean set on non boolean field: " + field.getType());
      } else if ((field.getType() != Integer.TYPE && field.getType() != Integer.class)
          && attributeAnnotation.defaultInt() != 0) {
        throw new JavaDataModelException(
            field, "Illegal defaultInteger set on non integer field: " + field.getType());
      } else if ((field.getType() != Double.TYPE && field.getType() != Double.class)
          && attributeAnnotation.defaultDouble() != 0.0) {
        throw new JavaDataModelException(
            field, "Illegal defaultDouble set on non double field: " + field.getType());
      }
      annotationCount++;
    }
    if (field.getAnnotation(Tag.class) != null) {
      final Class<?> fieldType = field.getType();

      if (fieldType == String.class
          || fieldType == Integer.TYPE
          || fieldType == Double.TYPE
          || fieldType == Boolean.TYPE
          || fieldType.isAssignableFrom(Collection.class)) {

        throw new JavaDataModelException(
            field,
            "Illegal location of @Tag, must be on object types (not primitives) "
                + "and not on a collection type");
      }

      annotationCount++;
    }
    if (field.getAnnotation(TagList.class) != null) {
      if (field.getType() != List.class) {
        throw new JavaDataModelException(
            field, "Illegal location of @TagList, may only be on List type");
      }
      annotationCount++;
    }

    if (field.getAnnotation(BodyText.class) != null) {
      if (field.getType() != String.class) {
        throw new JavaDataModelException(
            field, "Illegal location of @BodyText, may only be placed on String types");
      }
      annotationCount++;
    }

    if (annotationCount > 1) {
      throw new JavaDataModelException(
          field,
          "Too may annotations on field, can only have one of: "
              + "@Tag, or @TagList, or @Attribute, or @BodyText");
    }
  }
}
