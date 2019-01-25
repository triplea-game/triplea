package games.strategy.util;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.function.Consumer;

import com.google.common.annotations.VisibleForTesting;

/**
 * A utility for interacting with the system clipboard, humble object pattern.
 */
public class SystemClipboard {

  private static Consumer<String> clipboardSetter = contents -> {
    final StringSelection select = new StringSelection(contents);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(select, select);
  };

  @VisibleForTesting
  public static void setTestBehavior(final Consumer<String> clipboardSetter) {
    SystemClipboard.clipboardSetter = clipboardSetter;
  }

  public static void setClipboardContents(final String contents) {
    clipboardSetter.accept(contents);
  }
}
