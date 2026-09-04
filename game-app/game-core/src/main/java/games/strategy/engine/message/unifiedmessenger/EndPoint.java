package games.strategy.engine.message.unifiedmessenger;

import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.TypedInvocation;
import games.strategy.engine.message.TypedInvocationResult;
import games.strategy.net.INode;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.collections.CollectionUtils;

/**
 * This is where the methods finally get called. An end point contains the implementors for a given
 * name that are local to this node. You can invoke the method and get the results for all the
 * implementors.
 */
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
  private final TypedMessageRegistry typedMessageRegistry;
  private final InvocationExecutionGate executionGate;

  EndPoint(
      final String name,
      final Class<?> remoteClass,
      final boolean singleThreaded,
      final TypedMessageRegistry typedMessageRegistry,
      final InvocationExecutionGate executionGate) {
    this.name = name;
    this.remoteClass = remoteClass;
    this.singleThreaded = singleThreaded;
    this.typedMessageRegistry = typedMessageRegistry;
    this.executionGate = executionGate;
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
   * @return a List of {@link TypedInvocationResult}
   */
  public List<TypedInvocationResult> invokeLocal(
      final TypedInvocation call, final long number, final INode messageOriginator) {
    try {
      if (singleThreaded) {
        waitTillCanBeRun(number);
      }
      return invokeMultiple(call, messageOriginator);
    } finally {
      releaseNumber();
    }
  }

  private List<TypedInvocationResult> invokeMultiple(
      final TypedInvocation call, final INode messageOriginator) {
    return implementors.stream()
        .map(implementor -> invokeSingle(call, implementor, messageOriginator))
        .collect(Collectors.toUnmodifiableList());
  }

  private TypedInvocationResult invokeSingle(
      final TypedInvocation call, final Object implementor, final INode messageOriginator) {
    final WebSocketMessage message = call.getMessage();
    final TypedMessageHandler<WebSocketMessage> handler =
        typedMessageRegistry
            .handlerFor(message)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No typed handler registered for " + message.getClass().getName()));
    // The execution gate (delegate endpoints only) acquires the delegate-execution read lock around
    // the handler so a save cannot serialize game state mid-mutation; it is entered before the try
    // so a failed enter does not trigger a spurious leave, mirroring the old inbound proxy wrapper.
    executionGate.enter();
    MessageContext.setSenderNodeForThread(messageOriginator);
    try {
      return new TypedInvocationResult(handler.handle(message, implementor));
    } catch (final Throwable t) {
      // Any failure the handler raises returns to the caller through the latch as an exception
      // result rather than escaping on the delegate/thread-pool thread.
      return new TypedInvocationResult(t);
    } finally {
      MessageContext.setSenderNodeForThread(null);
      executionGate.leave();
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
