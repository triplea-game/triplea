package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.gameparser.GameParseException;
import org.junit.jupiter.api.Test;

class AbstractConditionsAttachmentTest {

  private final GameData mockData = mock(GameData.class);
  private final Attachable mockAttachable = mock(Attachable.class);
  private final AbstractConditionsAttachment instance =
      new AbstractConditionsAttachment("", mockAttachable, mockData) {
        private static final long serialVersionUID = -40443726954483090L;

        @Override
        public void validate(final GameData data) {}
      };

  @Test
  void setConditionTypeWithValidValues() throws Exception {
    instance.setConditionType("OR");
    assertEquals("OR", instance.conditionType);
    instance.setConditionType("AND");
    assertEquals("AND", instance.conditionType);
    instance.setConditionType("XOR");
    assertEquals("XOR", instance.conditionType);
    instance.setConditionType("00000012345656");
    assertEquals("00000012345656", instance.conditionType);
    instance.setConditionType("0-9");
    assertEquals("0-9", instance.conditionType);
    instance.setConditionType("0987654321-1234567890");
    assertEquals("0987654321-1234567890", instance.conditionType);
  }

  @Test
  void setConditionTypeWithValidLowercase() throws Exception {
    instance.setConditionType("or");
    assertEquals("OR", instance.conditionType);
    instance.setConditionType("and");
    assertEquals("AND", instance.conditionType);
    instance.setConditionType("xor");
    assertEquals("XOR", instance.conditionType);
    instance.setConditionType("123");
    assertEquals("123", instance.conditionType);
    instance.setConditionType("123-456");
    assertEquals("123-456", instance.conditionType);
  }

  @Test
  void setConditionTypeWithInvalidValues() {
    assertThrows(GameParseException.class, () -> instance.setConditionType("XNOR"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("NAND"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("NOR"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("NOT"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("5e10"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("9-"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("0-"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("-9"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("-0"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("0-0"));
    assertThrows(
        GameParseException.class, () -> instance.setConditionType("1234567890-0987654321"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("-1-0"));
    assertThrows(GameParseException.class, () -> instance.setConditionType("1--0"));
  }
}
