package games.strategy.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.util.function.ThrowingConsumer;
import lombok.extern.java.Log;

/**
 * Utility class to hide macOS integration.
 *
 * <p>
 * This class uses reflection to avoid Mac-specific classes being required for compiling on other platforms.
 * </p>
 */
@Log
public final class MacOsIntegration {
  private MacOsIntegration() {}

  /**
   * Adds the specified about handler to the application.
   */
  public static void addAboutHandler(final Runnable handler) {
    checkNotNull(handler);

    addHandler(
        getHandlerClassName("java.awt.desktop.AboutHandler", "com.apple.eawt.AboutHandler"),
        "handleAbout",
        "setAboutHandler",
        args -> handler.run());
  }

  private static String getHandlerClassName(final String java9OrLaterClassName, final String java8ClassName) {
    return isJavaVersionAtLeast9() ? java9OrLaterClassName : java8ClassName;
  }

  private static boolean isJavaVersionAtLeast9() {
    return isJavaVersionAtLeast9(SystemProperties.getJavaSpecificationVersion());
  }

  @VisibleForTesting
  static boolean isJavaVersionAtLeast9(final String encodedJavaSpecificationVersion) {
    try {
      return Float.parseFloat(encodedJavaSpecificationVersion) >= 9.0F;
    } catch (final NumberFormatException e) {
      log.log(Level.WARNING, "Malformed Java specification version: '" + encodedJavaSpecificationVersion + "'", e);
      return false;
    }
  }

  private static void addHandler(
      final String handlerClassName,
      final String handlerMethodName,
      final String addHandlerMethodName,
      final ThrowingConsumer<Object[], Throwable> handler) {
    try {
      final Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
      final Object application = applicationClass.getConstructor((Class[]) null).newInstance((Object[]) null);
      final Class<?> handlerClass = Class.forName(handlerClassName);
      final Object handlerProxy = Proxy.newProxyInstance(
          MacOsIntegration.class.getClassLoader(),
          new Class<?>[] {handlerClass},
          (proxy, method, args) -> {
            if (handlerMethodName.equals(method.getName())) {
              handler.accept(args);
            }
            return null;
          });
      applicationClass.getDeclaredMethod(addHandlerMethodName, handlerClass).invoke(application, handlerProxy);
    } catch (final ReflectiveOperationException e) {
      log.log(Level.SEVERE, "Failed to add macOS application handler (" + handlerClassName + ")", e);
    }
  }

  /**
   * Adds the specified open URI handler to the application.
   */
  public static void addOpenUriHandler(final Consumer<URI> handler) {
    checkNotNull(handler);

    addHandler(
        getHandlerClassName("java.awt.desktop.OpenURIHandler", "com.apple.eawt.OpenURIHandler"),
        "openURI",
        "setOpenURIHandler",
        args -> {
          final Object openUriEvent = args[0];
          final Method getUri = openUriEvent.getClass().getMethod("getURI");
          final URI uri = (URI) getUri.invoke(openUriEvent);
          handler.accept(uri);
        });
  }

  /**
   * Adds the specified quit handler to the application.
   */
  public static void addQuitHandler(final Runnable handler) {
    checkNotNull(handler);

    addHandler(
        getHandlerClassName("java.awt.desktop.QuitHandler", "com.apple.eawt.QuitHandler"),
        "handleQuitRequestWith",
        "setQuitHandler",
        args -> handler.run());
  }
}
