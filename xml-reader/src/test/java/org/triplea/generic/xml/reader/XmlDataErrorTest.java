package org.triplea.generic.xml.reader;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;

/** Checks that we can successfully cast attribute types to primitives. */
@SuppressWarnings({"unused", "UnmatchedTest"})
public class XmlDataErrorTest extends AbstractXmlMapperTest {
  XmlDataErrorTest() {
    super("simple-tag.xml");
  }

  public static class BadNumber {
    @Tag private SingleChild singleChild;

    public static class SingleChild {
      // this has a 'string' value in the XML
      @Attribute int attribute;
    }
  }

  public static class BadDouble {
    @Tag private SingleChild singleChild;

    public static class SingleChild {
      // this has a 'string' value in the XML
      @Attribute double attribute;
    }
  }

  public static class BadBoolean {
    @Tag private SingleChild singleChild;

    public static class SingleChild {
      // this has a 'string' value in the XML
      @Attribute boolean attribute;
    }
  }

  @ParameterizedTest
  @ValueSource(classes = {BadNumber.class, BadDouble.class, BadBoolean.class})
  void verifyPrimitiveTypeCasting(final Class<?> badValueInTheXmlClass) {
    assertThrows(XmlParsingException.class, () -> xmlMapper.mapXmlToObject(badValueInTheXmlClass));
  }
}
