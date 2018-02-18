package games.strategy.engine.message;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

class RemoteInterfaceHelper {
  static int getNumber(final String methodName, final Class<?>[] argTypes, final Class<?> remoteInterface) {
    final Method[] methods = remoteInterface.getMethods();
    Arrays.sort(methods, methodComparator);

    return IntStream.range(0, methods.length)
        .filter(i -> methods[i].getName().equals(methodName))
        .filter(i -> Arrays.equals(argTypes, methods[i].getParameterTypes()))
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Method not found: " + methodName));
  }

  static Method getMethod(final int methodNumber, final Class<?> remoteInterface) {
    final Method[] methods = remoteInterface.getMethods();
    Arrays.sort(methods, methodComparator);
    return methods[methodNumber];
  }

  /**
   * get methods does not guarantee an order, so sort.
   */
  private static final Comparator<Method> methodComparator = Comparator
      .comparing(Method::getName)
      .thenComparing(Method::getParameterTypes,
          Comparator.comparingInt((final Class<?>[] a) -> a.length)
              .thenComparing((o1, o2) -> {
                for (int i = 0; i < o1.length; i++) {
                  final int compareValue = o1[i].getName().compareTo(o2[i].getName());
                  if (compareValue != 0) {
                    return compareValue;
                  }
                }
                return 0;
              }));
}
