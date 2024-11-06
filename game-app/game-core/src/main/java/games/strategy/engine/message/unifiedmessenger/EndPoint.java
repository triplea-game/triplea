package games.strategy.engine.message.unifiedmessenger;

import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteMethodCallResults;
import games.strategy.net.INode;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.collections.CollectionUtils;

/**
 * This is where the methods finally get called. An end point contains the implementors for a given
 * name that are local to this node. You can invoke the method and get the results for all the
 * implementors.
 */
@Slf4j
class EndPoint {
  // the next number we are going to give
  private final AtomicLong nextGivenNumber = new AtomicLong();
  // the next number we can run
  private long currentRunnableNumber = 0;
  private final Object numberMutex = new Object();
  private final String name;
  private final Class<?> remoteClass;
  private final Set<Object> implementors = new CopyOnWriteArraySet<>();
  private final boolean singleThreaded;

  EndPoint(final String name, final Class<?> remoteClass, final boolean singleThreaded) {
    this.name = name;
    this.remoteClass = remoteClass;
    this.singleThreaded = singleThreaded;
  }

  /**
   * Returns the implementor if this class only holds a single implementor.
   *
   * @throws IllegalStateException If this class has less or more than 1 implementor.
   */
  public Object getOnlyImplementor() {
    if (!hasSingleImplementor()) {
      throw new IllegalStateException("Invalid implementor count, " + implementors);
    }
    return CollectionUtils.getAny(implementors);
  }

  public long takeANumber() {
    return nextGivenNumber.getAndIncrement();
  }

  private void waitTillCanBeRun(final long number) {
    synchronized (numberMutex) {
      while (number > currentRunnableNumber) {
        try {
          numberMutex.wait();
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private void releaseNumber() {
    synchronized (numberMutex) {
      currentRunnableNumber++;
      numberMutex.notifyAll();
    }
  }

  /** Adds the specified implementation of this end point's remote interface. */
  public void addImplementor(final Object implementor) {
    if (!remoteClass.isAssignableFrom(implementor.getClass())) {
      throw new IllegalArgumentException(
          remoteClass + " is not assignable from " + implementor.getClass());
    }
    implementors.add(implementor);
  }

  public boolean hasSingleImplementor() {
    return implementors.size() == 1;
  }

  /**
   * Removes the specified implementation of this end point's remote interface.
   *
   * @return we have no more implementors.
   */
  boolean removeImplementor(final Object implementor) {
    if (!implementors.remove(implementor)) {
      throw new IllegalStateException(
          "Not removed, impl: " + implementor + " have " + implementors);
    }
    return implementors.isEmpty();
  }

  /**
   * @param number - like the number you get in a bank line, if we are single threaded, then the
   *     method will not run until the number comes up. Acquire with {@link #takeANumber()}
   * @return a List of RemoteMethodCallResults
   */
  public List<RemoteMethodCallResults> invokeLocal(
      final RemoteMethodCall call, final long number, final INode messageOriginator) {
    try {
      if (singleThreaded) {
        waitTillCanBeRun(number);
      }
      return invokeMultiple(call, messageOriginator);
    } finally {
      releaseNumber();
    }
  }

  private List<RemoteMethodCallResults> invokeMultiple(
      final RemoteMethodCall call, final INode messageOriginator) {
    return implementors.stream()
        .map(implementor -> invokeSingle(call, implementor, messageOriginator))
        .collect(Collectors.toUnmodifiableList());
  }

  private RemoteMethodCallResults invokeSingle(
      final RemoteMethodCall call, final Object implementor, final INode messageOriginator) {
    call.resolve(remoteClass);
    final Method method;
    try {
      method = implementor.getClass().getMethod(call.getMethodName(), call.getArgTypes());
      method.setAccessible(true);
    } catch (final NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
    MessageContext.setSenderNodeForThread(messageOriginator);
    try {
      final Object methodRVal = method.invoke(implementor, call.getArgs());
      return new RemoteMethodCallResults(methodRVal);
    } catch (final InvocationTargetException e) {
      return new RemoteMethodCallResults(e.getTargetException());
    } catch (final IllegalAccessException | IllegalArgumentException e) {
      log.error("error in call: " + call, e);
      return new RemoteMethodCallResults(e);
    } finally {
      MessageContext.setSenderNodeForThread(null);
    }
  }

  @Override
  public String toString() {
    return "Name: "
        + name
        + " singleThreaded: "
        + singleThreaded
        + " implementors: "
        + implementors;
  }
}
