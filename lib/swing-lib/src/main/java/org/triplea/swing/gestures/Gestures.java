package org.triplea.swing.gestures;

import games.strategy.engine.framework.system.SystemProperties;
import java.lang.reflect.Proxy;
import javax.swing.JComponent;

/**
 * Provides support for platform-specific gestures. Currently, supports only the magnification
 * gestures on Mac (e.g. pinch to zoom using the trackpad).
 *
 * <p>Since there is no cross-platform API in Swing for this, it relies on Mac-specific
 * com.apple.eawt classes for the support, which are loaded via reflection in order for the code to
 * compile on all platforms.
 *
 * <p>This implementation is based on the following sources:
 * https://stackoverflow.com/questions/69221217/multi-touch-gestures-in-java-swing-awt-and-java-17
 * https://gist.github.com/alanwhite/42502f20390baf879d093691ebb72066
 *
 * <p>Note: This functionality requires passing the following args to java when running the app:
 * --add-opens java.desktop/com.apple.eawt.event=ALL-UNNAMED
 */
public class Gestures {
  @SuppressWarnings({"rawtypes"})
  public static void registerMagnificationListener(JComponent c, MagnificationListener listener) {
    // The current implementation only supports Mac.
    if (!SystemProperties.isMac()) {
      return;
    }
    // Wrapped in a try, as this may fail if the specific classes or their methods are not found.
    try {
      Class listenerClass = Class.forName("com.apple.eawt.event.MagnificationListener");
      Object listenerProxy =
          Proxy.newProxyInstance(
              listenerClass.getClassLoader(),
              new Class[] {listenerClass},
              (proxy, method, args) -> {
                if (method.getName().equals("magnify")) {
                  try {
                    Object mag = args[0].getClass().getMethod("getMagnification").invoke(args[0]);
                    listener.magnify(1 + (double) mag);
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                }
                return null;
              });

      Class.forName("com.apple.eawt.event.GestureUtilities")
          .getMethod(
              "addGestureListenerTo",
              Class.forName("javax.swing.JComponent"),
              Class.forName("com.apple.eawt.event.GestureListener"))
          .invoke(null, c, listenerProxy);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
