package org.triplea.generic.xml.reader;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.lang.reflect.Field;
import java.util.Optional;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.exceptions.XmlDataException;

class AttributeValueCasting {

  private final Field field;
  private final Attribute attributeAnnotation;

  AttributeValueCasting(final Field field) {
    this.field = field;
    attributeAnnotation = Preconditions.checkNotNull(field.getAnnotation(Attribute.class));
  }

  Object castAttributeValue(final String attributeValue) throws XmlDataException {
    if (field.getType() == Integer.class || field.getType() == Integer.TYPE) {
      return castToInt(attributeValue);
    } else if (field.getType() == Double.class || field.getType() == Double.TYPE) {
      return castToDouble(attributeValue);
    } else if (field.getType() == Boolean.class || field.getType() == Boolean.TYPE) {
      return castToBoolean(attributeValue);
    } else {
      // type is a String
      return Strings.emptyToNull(
          Optional.ofNullable(attributeValue).orElseGet(attributeAnnotation::defaultValue));
    }
  }

  /**
   * Cast attribute value to an int. If the value is null we return a default. If the field is a
   * "Integer" and if the attribute value is null and the default is not set then we return null. If
   * the field is an "int" and if the attribute value is null then we return 0.
   *
   * @throws XmlDataException Thrown if attributeValue is present but cannot be cast to an int.
   */
  private Integer castToInt(final String attributeValue) throws XmlDataException {
    if (attributeValue == null
        && field.getType() == Integer.class
        && field.getAnnotation(Attribute.class).defaultInt() == 0) {
      return null;
    } else if (attributeValue == null) {
      return field.getAnnotation(Attribute.class).defaultInt();
    } else {
      try {
        return Integer.valueOf(attributeValue);
      } catch (final NumberFormatException e) {
        throw new XmlDataException(
            field, "Invalid value: " + attributeValue + ", required an integer number");
      }
    }
  }

  private Double castToDouble(final String attributeValue) throws XmlDataException {
    if (attributeValue == null
        && field.getType() == Double.class
        && field.getAnnotation(Attribute.class).defaultDouble() == 0.0) {
      return null;
    } else if (attributeValue == null) {
      return field.getAnnotation(Attribute.class).defaultDouble();
    } else {
      try {
        return Double.valueOf(attributeValue);
      } catch (final NumberFormatException e) {
        throw new XmlDataException(
            field, "Invalid value: " + attributeValue + ", required a number");
      }
    }
  }

  private Boolean castToBoolean(final String attributeValue) throws XmlDataException {
    if (attributeValue == null
        && field.getType() == Boolean.class
        && !field.getAnnotation(Attribute.class).defaultBoolean()) {
      return null;
    } else if (attributeValue == null) {
      return field.getAnnotation(Attribute.class).defaultBoolean();
    } else {
      if (!attributeValue.equalsIgnoreCase("true") && !attributeValue.equalsIgnoreCase("false")) {
        throw new XmlDataException(
            field, "Invalid value: " + attributeValue + ", required either 'true' or 'false'");
      }
      return Boolean.valueOf(attributeValue);
    }
  }
}
