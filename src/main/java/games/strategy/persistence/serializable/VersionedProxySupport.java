package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import games.strategy.util.TaskUtil;

/**
 * This is a utility class that can be used by serializable proxies that support versioning.
 *
 * <p>
 * In order to support versioning, a proxy must implement the {@link Externalizable} interface due to the
 * class compatibility limitations imposed by the Java serialization framework. Without implementing
 * {@link Externalizable}, the evolution of a proxy will be severely constrained.
 * </p>
 *
 * <p>
 * A versioned proxy must provide a method with the following exact signature for each version it wishes to read:
 * </p>
 *
 * <pre>
 * private void readExternalV&lt;version&gt;(ObjectInput in)
 *   throws IOException, ClassNotFoundException
 * </pre>
 *
 * <p>
 * where {@code <version>} is a positive integer representing the version to read.
 * </p>
 *
 * <p>
 * A versioned proxy must provide a method with the following exact signature for each version it wishes to write:
 * </p>
 *
 * <pre>
 * private void writeExternalV&lt;version&gt;(ObjectOutput out)
 *   throws IOException
 * </pre>
 *
 * <p>
 * where {@code <version>} is a positive integer representing the version to write. Typically, versioned proxies
 * will only support writing the current (latest) version.
 * </p>
 */
public final class VersionedProxySupport {
  private final Externalizable proxy;

  /**
   * Initializes a new instance of the {@code VersionedProxySupport} class.
   *
   * @param proxy The serializable proxy; must not be {@code null}.
   */
  public VersionedProxySupport(final Externalizable proxy) {
    checkNotNull(proxy);

    this.proxy = proxy;
  }

  /**
   * Reads the contents of the proxy from the specified stream.
   *
   * @param in The stream from which the proxy contents will be read; must not be {@code null}.
   *
   * @throws IOException If an I/O error occurs.
   * @throws ClassNotFoundException If the class for an object being restored cannot be found.
   *
   * @see Externalizable#readExternal(ObjectInput)
   */
  public void read(final ObjectInput in) throws IOException, ClassNotFoundException {
    checkNotNull(in);

    final long version = in.readLong();
    final Method method = getProxyReadHandlerForVersion(version);
    try {
      method.invoke(proxy, in);
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw (IOException) cause;
      } else if (cause instanceof ClassNotFoundException) {
        throw (ClassNotFoundException) cause;
      }
      throw TaskUtil.launderThrowable(cause);
    } catch (final IllegalAccessException e) {
      throw new AssertionError("should never occur (access checks suppressed)", e);
    }
  }

  private Method getProxyReadHandlerForVersion(final long version) throws IOException {
    return getProxyIoHandlerForVersion("read", version, ObjectInput.class);
  }

  private Method getProxyIoHandlerForVersion(
      final String prefix,
      final long version,
      final Class<?>... parameterTypes) throws IOException {
    try {
      final String methodName = String.format("%sExternalV%d", prefix, version);
      final Method method = proxy.getClass().getDeclaredMethod(methodName, parameterTypes);
      method.setAccessible(true);
      if (Modifier.isPrivate(method.getModifiers())
          && !Modifier.isStatic(method.getModifiers())
          && Void.TYPE.equals(method.getReturnType())) {
        return method;
      }
    } catch (final NoSuchMethodException e) {
      // fall through
    }

    throw new IOException(String.format("proxy class '%s' does not support version %d",
        proxy.getClass().getName(), version));
  }

  /**
   * Writes the contents of the proxy to the specified stream.
   *
   * @param out The stream to which the proxy contents will be written; must not be {@code null}.
   * @param version The version of the proxy contents to write.
   *
   * @throws IOException If an I/O error occurs.
   * @throws IllegalArgumentException If {@code version} is not positive.
   *
   * @see Externalizable#writeExternal(ObjectOutput)
   */
  public void write(final ObjectOutput out, final long version) throws IOException {
    checkNotNull(out);
    checkArgument(version > 0, "version must be positive");

    out.writeLong(version);
    final Method method = getProxyWriteHandlerForVersion(version);
    try {
      method.invoke(proxy, out);
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw (IOException) cause;
      }
      throw TaskUtil.launderThrowable(cause);
    } catch (final IllegalAccessException e) {
      throw new AssertionError("should never occur (access checks suppressed)", e);
    }
  }

  private Method getProxyWriteHandlerForVersion(final long version) throws IOException {
    return getProxyIoHandlerForVersion("write", version, ObjectOutput.class);
  }
}
