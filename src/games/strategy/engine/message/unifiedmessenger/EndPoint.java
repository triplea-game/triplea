package games.strategy.engine.message.unifiedmessenger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteMethodCallResults;
import games.strategy.net.INode;

/**
 * This is where the methods finally get called.
 * An endpoint contains the implementors for a given name that are local to this
 * node.
 * You can invoke the method and get the results for all the implementors.
 */
class EndPoint {
  // the next number we are going to give
  private final AtomicLong m_nextGivenNumber = new AtomicLong();
  // the next number we can run
  private long m_currentRunnableNumber = 0;
  private final Object m_numberMutext = new Object();
  private final Object m_implementorsMutext = new Object();
  private final String m_name;
  private final Class<?> m_remoteClass;
  private final List<Object> m_implementors = new ArrayList<>();
  private final boolean m_singleThreaded;

  public EndPoint(final String name, final Class<?> remoteClass, final boolean singleThreaded) {
    m_name = name;
    m_remoteClass = remoteClass;
    m_singleThreaded = singleThreaded;
  }

  public Object getFirstImplementor() {
    synchronized (m_implementorsMutext) {
      if (m_implementors.size() != 1) {
        throw new IllegalStateException("Invalid implementor count, " + m_implementors);
      }
      return m_implementors.get(0);
    }
  }

  public long takeANumber() {
    return m_nextGivenNumber.getAndIncrement();
  }

  private void waitTillCanBeRun(final long aNumber) {
    synchronized (m_numberMutext) {
      while (aNumber > m_currentRunnableNumber) {
        try {
          m_numberMutext.wait();
        } catch (final InterruptedException e) {
          ClientLogger.logQuietly(e);
        }
      }
    }
  }

  private void releaseNumber() {
    synchronized (m_numberMutext) {
      m_currentRunnableNumber++;
      m_numberMutext.notifyAll();
    }
  }

  /**
   * @return is this the first implementor
   */
  public boolean addImplementor(final Object implementor) {
    if (!m_remoteClass.isAssignableFrom(implementor.getClass())) {
      throw new IllegalArgumentException(m_remoteClass + " is not assignable from " + implementor.getClass());
    }
    synchronized (m_implementorsMutext) {
      final boolean rVal = m_implementors.isEmpty();
      m_implementors.add(implementor);
      return rVal;
    }
  }

  public int getLocalImplementorCount() {
    synchronized (m_implementorsMutext) {
      return m_implementors.size();
    }
  }

  /**
   * @return - we have no more implementors
   */
  boolean removeImplementor(final Object implementor) {
    synchronized (m_implementorsMutext) {
      if (!m_implementors.remove(implementor)) {
        throw new IllegalStateException("Not removed, impl:" + implementor + " have " + m_implementors);
      }
      return m_implementors.isEmpty();
    }
  }

  public String getName() {
    return m_name;
  }

  public Class<?> getRemoteClass() {
    return m_remoteClass;
  }

  /*
   * @param number - like the number you get in a bank line, if we are single
   * threaded, then the method will not run until the number comes up. Acquire
   * with getNumber() @return a List of RemoteMethodCallResults
   */
  public List<RemoteMethodCallResults> invokeLocal(final RemoteMethodCall call, final long number,
      final INode messageOriginator) {
    try {
      if (m_singleThreaded) {
        waitTillCanBeRun(number);
      }
      return invokeMultiple(call, messageOriginator);
    } finally {
      releaseNumber();
    }
  }

  /**
   * @param call
   * @param rVal
   */
  private List<RemoteMethodCallResults> invokeMultiple(final RemoteMethodCall call, final INode messageOriginator) {
    // copy the implementors
    List<Object> implementorsCopy;
    synchronized (m_implementorsMutext) {
      implementorsCopy = new ArrayList<>(m_implementors);
    }
    final List<RemoteMethodCallResults> results = new ArrayList<>(implementorsCopy.size());
    for (final Object implementor : implementorsCopy) {
      results.add(invokeSingle(call, implementor, messageOriginator));
    }
    return results;
  }

  /**
   * @param call
   * @param implementor
   */
  private RemoteMethodCallResults invokeSingle(final RemoteMethodCall call, final Object implementor,
      final INode messageOriginator) {
    call.resolve(m_remoteClass);
    Method method;
    try {
      method = implementor.getClass().getMethod(call.getMethodName(), call.getArgTypes());
      method.setAccessible(true);
    } catch (final SecurityException | NoSuchMethodException e) {
      ClientLogger.logQuietly(e);
      throw new IllegalStateException(e.getMessage());
    }
    MessageContext.setSenderNodeForThread(messageOriginator);
    try {
      final Object methodRVal = method.invoke(implementor, call.getArgs());
      return new RemoteMethodCallResults(methodRVal);
    } catch (final InvocationTargetException e) {
      return new RemoteMethodCallResults(e.getTargetException());
    } catch (final IllegalAccessException e) {
      ClientLogger.logQuietly("error in call:" + call, e);
      return new RemoteMethodCallResults(e);
    } catch (final IllegalArgumentException e) {
      ClientLogger.logQuietly("error in call:" + call, e);
      return new RemoteMethodCallResults(e);
    } finally {
      MessageContext.setSenderNodeForThread(null);
    }
  }


  @Override
  public String toString() {
    return "Name:" + m_name + " singleThreaded:" + m_singleThreaded + " implementors:" + m_implementors;
  }
}
