package org.triplea.swing.key.binding;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeyCodeTest {
  @Test
  @DisplayName("Verify all key codes can be translated to a awt KeyStroke object")
  void toKeyStroke() {
    assertDoesNotThrow(() -> Arrays.stream(KeyCode.values()).forEach(KeyCode::toKeyStroke));
  }
}
