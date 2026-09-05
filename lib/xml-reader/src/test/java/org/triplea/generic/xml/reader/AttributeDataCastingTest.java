package org.triplea.generic.xml.reader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.triplea.generic.xml.reader.AttributeDataCastingTest.TagExample.SingleChild;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;

/** Checks that we can successfully cast attribute types to primitives. */
public class AttributeDataCastingTest extends AbstractXmlMapperTest {
  AttributeDataCastingTest() {
    super("simple-tag.xml");
  }

  public static class TagExample {
    @Tag private SingleChild singleChild;

    public static class SingleChild {
      @Attribute int numberAttribute;
      @Attribute int missingNumberAttribute;
      @Attribute Integer integerObjectAttribute;
      @Attribute Integer missingIntegerObjectAttribute;

      @Attribute double decimalAttribute;
      @Attribute double decimalWithIntAttribute;
      @Attribute double missingDecimalAttribute;
      @Attribute Double decimalObjectAttribute;

      @Attribute boolean booleanAttribute;
      @Attribute boolean booleanObjectAttribute;
      @Attribute boolean missingBooleanAttribute;

      @Attribute(defaultInt = 100)
      int defaultInt;

      @Attribute(defaultDouble = 110.0)
      double defaultDouble;

      @Attribute(defaultBoolean = true)
      boolean defaultBoolean;
    }
  }

  @Test
  void verifyPrimitiveTypeCasting() throws Exception {

    final SingleChild singleChild = xmlMapper.mapXmlToObject(TagExample.class).singleChild;

    assertThat(singleChild.numberAttribute).isEqualTo(1);
    assertThat(singleChild.missingNumberAttribute).isEqualTo(0);

    assertThat(singleChild.integerObjectAttribute).isEqualTo(-1);
    assertThat(singleChild.missingIntegerObjectAttribute).isNull();

    assertThat(singleChild.decimalAttribute).isEqualTo(0.3);
    assertThat(singleChild.decimalWithIntAttribute).isEqualTo(3.0);
    assertThat(singleChild.missingDecimalAttribute).isEqualTo(0.0);
    assertThat(singleChild.decimalObjectAttribute).isEqualTo(10.0);

    assertThat(singleChild.booleanAttribute).isTrue();
    assertThat(singleChild.booleanObjectAttribute).isTrue();
    assertThat(singleChild.missingBooleanAttribute).isFalse();

    assertThat(singleChild.defaultInt).isEqualTo(100);
    assertThat(singleChild.defaultDouble).isEqualTo(110.0);
    assertThat(singleChild.defaultBoolean).isTrue();
  }
}
