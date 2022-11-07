package org.triplea.swing.key.binding;

import javax.swing.KeyStroke;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter(AccessLevel.PACKAGE)
public class KeyCombination {

  private final KeyCode keyCode;
  private final ButtonDownMask buttonDownMask;

  public static KeyCombination of(final KeyCode keyCode, final ButtonDownMask buttonDownMask) {
    return new KeyCombination(keyCode, buttonDownMask);
  }

  @SuppressWarnings("MagicConstant")
  KeyStroke toKeyStroke() {
    final KeyStroke result =
        KeyStroke.getKeyStroke(keyCode.getInputEventCode(), buttonDownMask.getInputEventCode());

    if (result.toString().contains("UNKNOWN")) {
      throw new IllegalArgumentException(
          "Be sure to use a constant from 'KeyEvent', unknown key constant: " + this);
    }
    return result;
  }
}
