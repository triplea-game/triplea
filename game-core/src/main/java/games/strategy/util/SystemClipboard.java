package games.strategy.util;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * A utility for interacting with the system clipboard, humble object pattern.
 */
public class SystemClipboard {

  public static void setClipboardContents(final String contents) {
    final StringSelection select = new StringSelection(contents);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(select, select);
  }
}
