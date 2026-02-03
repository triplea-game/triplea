package games.strategy.engine.message;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.annotation.Nullable;

/**
 * Implementation of {@link InvocationHandler} that delegates the {@link Object#equals(Object)},
 * {@link Object#hashCode()}, and {@link Object#toString()} methods to the specified object.
 */
public class WrappedInvocationHandler implements InvocationHandler {
  private final Object delegate;

  public WrappedInvocationHandler(final Object delegate) {
    checkNotNull(delegate);

    this.delegate = delegate;
  }

  private boolean wrappedEquals(final Object other) {
    if (other == this) {
      return true;
    }
    if (Proxy.isProxyClass(other.getClass())
        && Proxy.getInvocationHandler(other) instanceof WrappedInvocationHandler otherWrapped) {
      return otherWrapped.delegate.equals(delegate);
    }
    return false;
  }

  protected boolean shouldHandle(final Method method, final Object[] args) {
    return (method.getName().equals("equals") && args != null && args.length == 1)
        || (method.getName().equals("hashCode") && args == null)
        || (method.getName().equals("toString") && args == null);
  }

  protected Object handle(final Method method, final Object[] args) {
    if (method.getName().equals("equals") && args != null && args.length == 1) {
      return wrappedEquals(args[0]);
    } else if (method.getName().equals("hashCode") && args == null) {
      return delegate.hashCode();
    } else if (method.getName().equals("toString") && args == null) {
      return delegate.toString();
    } else {
      throw new IllegalStateException("how did we get here");
    }
  }

  @Override
  @Nullable
  public Object invoke(final Object proxy, final Method method, final Object[] args)
      throws Throwable {
    if (shouldHandle(method, args)) {
      return handle(method, args);
    }
    throw new IllegalStateException("not configured");
  }
}
