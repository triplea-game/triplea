package games.strategy.engine.message;

import java.lang.reflect.Method;
import java.util.Arrays;

final class RemoteInterfaceHelper {

  private RemoteInterfaceHelper() {}

  static int getNumber(final Method method) {
    final RemoteActionCode annotation = method.getAnnotation(RemoteActionCode.class);

    if (annotation == null) {
      throw new IllegalArgumentException(
          "Method " + method + " must be annotated with @RemoteActionCode");
    }

    return annotation.value();
  }

  static Method getMethod(final int methodNumber, final Class<?> remoteInterface) {
    return Arrays.stream(remoteInterface.getMethods())
        .filter(method -> getNumber(method) == methodNumber)
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Class "
                        + remoteInterface
                        + " does not contain method annotated with @RemoteActionCode("
                        + methodNumber
                        + ")"));
  }
}
