package org.triplea.generic.xml.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.jupiter.api.Test;
import org.triplea.generic.xml.reader.AttributeDataCastingTest.TagExample.SingleChild;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;

/** Checks that we can successfully cast attribute types to primitives. */
@SuppressWarnings("UnmatchedTest")
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

    assertThat(singleChild.numberAttribute, is(1));
    assertThat(singleChild.missingNumberAttribute, is(0));

    assertThat(singleChild.integerObjectAttribute, is(-1));
    assertThat(singleChild.missingIntegerObjectAttribute, is(nullValue()));

    assertThat(singleChild.decimalAttribute, is(0.3));
    assertThat(singleChild.decimalWithIntAttribute, is(3.0));
    assertThat(singleChild.missingDecimalAttribute, is(0.0));
    assertThat(singleChild.decimalObjectAttribute, is(10.0));

    assertThat(singleChild.booleanAttribute, is(true));
    assertThat(singleChild.booleanObjectAttribute, is(true));
    assertThat(singleChild.missingBooleanAttribute, is(false));

    assertThat(singleChild.defaultInt, is(100));
    assertThat(singleChild.defaultDouble, is(110.0));
    assertThat(singleChild.defaultBoolean, is(true));
  }
}
