package games.strategy.engine.data; /*
                                      Usage:
                                      - Drop this file into your project (any package or default package).
                                      - From a debug menu action, a breakpoint, or any place running on the application JVM,
                                        call: ScrollableTextFieldInspector.runInspection();
                                      - It schedules the inspection on the EDT and prints the component tree and
                                        per-stepper-button details to System.out so you can compare the battleship entry
                                        with a working entry.

                                      Note: This file uses only standard AWT/Swing APIs and reflection-safe checks
                                      (it does not require a compile-time dependency on ScrollableTextField).
                                    */

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class ScrollableTextFieldInspector {
  private ScrollableTextFieldInspector() {}

  public static void runInspection() {
    // Schedule on the EDT so we read consistent Swing state
    if (SwingUtilities.isEventDispatchThread()) {
      inspectAllWindows();
    } else {
      SwingUtilities.invokeLater(ScrollableTextFieldInspector::inspectAllWindows);
    }
  }

  private static void inspectAllWindows() {
    try {
      StringBuilder sb = new StringBuilder();
      sb.append("==== ScrollableTextField / Stepper inspection ====\n");
      sb.append("UIManager keys (sample):\n");
      printUiColor(sb, "Button.disabledText");
      printUiColor(sb, "Button.disabledForeground");
      printUiColor(sb, "Label.disabledForeground");
      sb.append("\n");

      java.awt.Window[] wins = java.awt.Window.getWindows();
      if (wins == null || wins.length == 0) {
        sb.append("No windows found in this JVM.\n");
      }
      for (java.awt.Window w : wins) {
        sb.append("Window: ").append(w.getClass().getName());
        String title = tryGetWindowTitle(w);
        if (title != null && !title.isEmpty()) sb.append(" title=").append(title);
        sb.append(" visible=")
            .append(w.isVisible())
            .append(" bounds=")
            .append(w.getBounds())
            .append("\n");
        dumpContainer(w, sb, 1);
      }
      System.out.println(sb.toString());
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private static void printUiColor(StringBuilder sb, String key) {
    try {
      Object val = UIManager.get(key);
      sb.append("  ").append(key).append(" = ").append(val).append("\n");
    } catch (Throwable t) {
      sb.append("  ").append(key).append(" = <error reading>\n");
    }
  }

  private static String tryGetWindowTitle(java.awt.Window w) {
    try {
      // many windows are java.awt.Window without getTitle; try reflection for JFrame/JDialog
      Method m = w.getClass().getMethod("getTitle");
      Object r = m.invoke(w);
      return r == null ? null : r.toString();
    } catch (Exception ignored) {
      return null;
    }
  }

  private static void dumpContainer(Container c, StringBuilder sb, int depth) {
    String indent = "  ".repeat(Math.max(0, depth));
    Component[] comps = c.getComponents();
    for (Component comp : comps) {
      sb.append(indent)
          .append(comp.getClass().getSimpleName())
          .append(" visible=")
          .append(comp.isVisible())
          .append(" enabled=")
          .append(comp.isEnabled())
          .append(" bounds=")
          .append(comp.getBounds())
          .append("\n");

      // Detect ScrollableTextField by class name to avoid compile-time dependency
      String compClassName = comp.getClass().getName();
      if ("games.strategy.ui.ScrollableTextField".equals(compClassName)
          || compClassName.endsWith("ScrollableTextField")) {
        sb.append(indent).append("  >>> Detected ScrollableTextField instance <<<\n");
        dumpScrollableTextField(comp, sb, depth + 2);
      } else if (comp instanceof Container) {
        // Recurse into containers
        dumpContainer((Container) comp, sb, depth + 1);
      } else {
        // leaf node
      }
    }
  }

  private static void dumpScrollableTextField(Component stf, StringBuilder sb, int depth) {
    String indent = "  ".repeat(Math.max(0, depth));
    if (!(stf instanceof Container)) {
      sb.append(indent).append("Not a Container, skipping children\n");
      return;
    }
    Container container = (Container) stf;
    Component[] children = container.getComponents();
    for (Component child : children) {
      if (child instanceof JButton) {
        JButton button = (JButton) child;
        sb.append(indent)
            .append("JButton: class=")
            .append(button.getClass().getName())
            .append(" visible=")
            .append(button.isVisible())
            .append(" enabled=")
            .append(button.isEnabled())
            .append(" opaque=")
            .append(button.isOpaque())
            .append(" fg=")
            .append(button.getForeground())
            .append(" bg=")
            .append(button.getBackground())
            .append(" preferred=")
            .append(button.getPreferredSize())
            .append(" actual=")
            .append(button.getSize())
            .append(" bounds=")
            .append(button.getBounds())
            .append("\n");
        try {
          sb.append(indent)
              .append("  ui=")
              .append(button.getUI().getClass().getName())
              .append("\n");
        } catch (Throwable t) {
          sb.append(indent).append("  ui=<error>").append(t).append("\n");
        }

        Icon icon = button.getIcon();
        sb.append(indent)
            .append("  icon=")
            .append(icon == null ? "null" : icon.getClass().getName());
        if (icon != null) {
          sb.append(" size=").append(icon.getIconWidth()).append("x").append(icon.getIconHeight());
        }
        sb.append("\n");

        // Client properties of interest
        Object bt = button.getClientProperty("JButton.buttonType");
        Object mw = button.getClientProperty("JComponent.minimumWidth");
        sb.append(indent)
            .append("  clientProps: JButton.buttonType=")
            .append(bt)
            .append(", JComponent.minimumWidth=")
            .append(mw)
            .append("\n");

      } else {
        sb.append(indent)
            .append(child.getClass().getSimpleName())
            .append(" visible=")
            .append(child.isVisible())
            .append(" bounds=")
            .append(child.getBounds())
            .append("\n");
      }

      // Recurse into the child if it's a container to find nested buttons (up/down nested panels)
      if (child instanceof Container) {
        dumpContainer((Container) child, sb, depth + 1);
      }
    }

    // For additional context, check the preferred/minimum sizes set on the ScrollableTextField
    // itself
    sb.append(indent)
        .append("ScrollableTextField preferred=")
        .append(container.getPreferredSize())
        .append(" minimum=")
        .append(container.getMinimumSize())
        .append(" size=")
        .append(container.getSize())
        .append("\n");
  }

  // Optional helper if user wants a compact check to see whether clicking the area works:
  // You can trigger this method (on EDT) to print whether a click in the first
  // ScrollableTextField's
  // up-button bounds would be inside the button and whether the button is enabled.
  public static void simulatePointProbe() {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(ScrollableTextFieldInspector::simulatePointProbe);
      return;
    }
    java.awt.Window[] wins = java.awt.Window.getWindows();
    for (java.awt.Window w : wins) {
      Component[] comps = w.getComponents();
      for (Component c : comps) {
        if (c instanceof Container) {
          Container root = (Container) c;
          Component target = findFirstByClassName(root, "games.strategy.ui.ScrollableTextField");
          if (target != null && target instanceof Container) {
            Container stf = (Container) target;
            // find first JButton child
            Component[] children = stf.getComponents();
            for (Component child : children) {
              if (child instanceof JButton) {
                JButton b = (JButton) child;
                Rectangle r = b.getBounds();
                System.out.println(
                    "Found first stepper button: enabled="
                        + b.isEnabled()
                        + " bounds="
                        + r
                        + " fg="
                        + b.getForeground()
                        + " bg="
                        + b.getBackground());
                return;
              }
            }
          }
        }
      }
    }
    System.out.println("simulatePointProbe: no ScrollableTextField found.");
  }

  private static Component findFirstByClassName(Container root, String className) {
    for (Component comp : root.getComponents()) {
      if (comp.getClass().getName().equals(className)
          || comp.getClass().getName().endsWith("ScrollableTextField")) {
        return comp;
      }
      if (comp instanceof Container) {
        Component nested = findFirstByClassName((Container) comp, className);
        if (nested != null) return nested;
      }
    }
    return null;
  }

  // Small convenience so running from a headless test doesn't hang: main will run only if not
  // headless.
  public static void main(String[] args) {
    if (GraphicsEnvironment.isHeadless()) {
      System.err.println("Headless environment - cannot inspect Swing windows.");
      return;
    }
    runInspection();
  }
}
