package tools.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ToolArgumentsTest {

  @AfterEach
  void clearProperties() {
    for (final ToolArguments argument : ToolArguments.values()) {
      System.clearProperty(argument.toString());
    }
  }

  @Test
  void setsDoublePropertyFromTextFieldInput() {
    ToolArguments.UNIT_ZOOM.setSystemProperty("1.25");

    assertThat(System.getProperty(ToolArguments.UNIT_ZOOM.toString())).isEqualTo("1.25");
  }

  @Test
  void setsIntegerPropertyFromTextFieldInput() {
    ToolArguments.UNIT_WIDTH.setSystemProperty("48");

    assertThat(System.getProperty(ToolArguments.UNIT_WIDTH.toString())).isEqualTo("48");
  }

  @Test
  void setsStringPropertyFromTextFieldInput() {
    ToolArguments.MAP_FOLDER.setSystemProperty("/tmp/maps");

    assertThat(System.getProperty(ToolArguments.MAP_FOLDER.toString())).isEqualTo("/tmp/maps");
  }

  @Test
  void clampsIntegerPropertyToConfiguredRange() {
    ToolArguments.UNIT_HEIGHT.setSystemProperty("0");

    assertThat(System.getProperty(ToolArguments.UNIT_HEIGHT.toString())).isEqualTo("1");
  }

  @Test
  void clampsDoublePropertyToConfiguredRange() {
    ToolArguments.UNIT_ZOOM.setSystemProperty("9.5");

    assertThat(System.getProperty(ToolArguments.UNIT_ZOOM.toString())).isEqualTo("4.0");
  }

  @Test
  void leavesPropertyUntouchedForMalformedNumber() {
    ToolArguments.UNIT_ZOOM.setSystemProperty("1.5");

    ToolArguments.UNIT_ZOOM.setSystemProperty("not-a-number");

    assertThat(System.getProperty(ToolArguments.UNIT_ZOOM.toString())).isEqualTo("1.5");
  }

  @Test
  void clearsPropertyForNullValue() {
    ToolArguments.UNIT_WIDTH.setSystemProperty("48");

    ToolArguments.UNIT_WIDTH.setSystemProperty(null);

    assertThat(System.getProperty(ToolArguments.UNIT_WIDTH.toString())).isNull();
  }
}
