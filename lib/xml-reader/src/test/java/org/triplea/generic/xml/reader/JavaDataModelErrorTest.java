package org.triplea.generic.xml.reader;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import lombok.NoArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.BodyText;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;

/** Tests to verify validation is working as expected. */
@SuppressWarnings("unused")
public class JavaDataModelErrorTest extends AbstractXmlMapperTest {

  JavaDataModelErrorTest() {
    super("simple-tag.xml");
  }

  @NoArgsConstructor
  static class Example {
    @SuppressWarnings("InnerClassMayBeStatic")
    public class NestedClass {}
  }

  interface ExampleInterface {}

  abstract static class ExampleAbstractClass {}

  @ParameterizedTest
  @ValueSource(
      classes = {Example.NestedClass.class, ExampleInterface.class, ExampleAbstractClass.class})
  void cannotInstantiateCases(final Class<?> cannotInstantiate) {
    assertThrows(XmlParsingException.class, () -> xmlMapper.mapXmlToObject(cannotInstantiate));
  }

  static class TagListOnNonList {
    @TagList private String value;
  }

  static class TagAndAttributeAnnotations {
    @Attribute @Tag private Object value;
  }

  static class TagAndTagListAnnotations {
    @TagList @Tag private List<Object> value;
  }

  static class TagListAndAttributeAnnotations {
    @TagList @Tag private List<Object> value;
  }

  static class BodyTextAndAttribute {
    @BodyText @Attribute private String value;
  }

  static class AttributeOnObject {
    @Attribute private Object attribute;
  }

  static class BodyTextOnNonString {
    @BodyText private Object notAString;
  }

  @ParameterizedTest
  @ValueSource(
      classes = {
        TagListOnNonList.class,
        TagAndAttributeAnnotations.class,
        TagAndTagListAnnotations.class,
        TagListAndAttributeAnnotations.class,
        BodyTextAndAttribute.class,
        AttributeOnObject.class,
        BodyTextOnNonString.class
      })
  void badAnnotationCases(final Class<?> badAnnotations) {
    assertThrows(XmlParsingException.class, () -> xmlMapper.mapXmlToObject(badAnnotations));
  }

  static class DefaultBooleanOnNonBoolean {
    @Attribute(defaultBoolean = true)
    private String value;
  }

  static class DefaultIntegerOnNonInteger {
    @Attribute(defaultInt = 1)
    private double value;
  }

  static class DefaultDoubleOnNonDouble {
    @Attribute(defaultDouble = 1.0)
    private int value;
  }

  @ParameterizedTest
  @ValueSource(
      classes = {
        DefaultBooleanOnNonBoolean.class,
        DefaultIntegerOnNonInteger.class,
        DefaultDoubleOnNonDouble.class
      })
  void badDefaultCases(final Class<?> badDefaults) {
    assertThrows(XmlParsingException.class, () -> xmlMapper.mapXmlToObject(badDefaults));
  }
}
