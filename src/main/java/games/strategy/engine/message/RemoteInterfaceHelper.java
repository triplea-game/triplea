package games.strategy.engine.message;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

import games.strategy.util.Tuple;

class RemoteInterfaceHelper {
  static int getNumber(final String methodName, final Class<?>[] argTypes, final Class<?> remoteInterface) {
    final Method[] methods = remoteInterface.getMethods();
    Arrays.sort(methods, methodComparator);

    for (int i = 0; i < methods.length; i++) {
      if (methods[i].getName().equals(methodName)) {
        final Class<?>[] types = methods[i].getParameterTypes();
        if (Arrays.equals(types, argTypes)) {
          return i;
        }
      }
    }
    throw new IllegalStateException("Method not found");
  }

  static Tuple<String, Class<?>[]> getMethodInfo(final int methodNumber, final Class<?> remoteInterface) {
    final Method[] methods = remoteInterface.getMethods();
    Arrays.sort(methods, methodComparator);
    return Tuple.of(methods[methodNumber].getName(), methods[methodNumber].getParameterTypes());
  }

  /**
   * get methods does not guarantee an order, so sort.
   */
  private static final Comparator<Method> methodComparator = (o1, o2) -> {
    if (o1 == o2) {
      return 0;
    }
    if (!o1.getName().equals(o2.getName())) {
      return o1.getName().compareTo(o2.getName());
    }
    final Class<?>[] t1 = o1.getParameterTypes();
    final Class<?>[] t2 = o2.getParameterTypes();
    if ((t1 == null) && (t2 == null)) {
      return 0;
    }
    if (t1 == null) {
      return -1;
    }
    if (t2 == null) {
      return 1;
    }
    if (t1.length != t2.length) {
      return t1.length - t2.length;
    }
    for (int i = 0; i < t1.length; i++) {
      if (!t1[i].getName().equals(t2[i].getName())) {
        return t1[i].getName().compareTo(t2[i].getName());
      }
    }
    return 0;
  };
}
