package games.strategy.engine.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * The results of a method execution.
 * Note that either one of m_rVal or m_exception will be null,
 * since the method can either throw or return
 */
public class RemoteMethodCallResults implements Externalizable {
  private static final long serialVersionUID = 4562274411264858613L;
  private Object m_rVal;
  // throwable implements Serializable
  private Throwable m_exception;

  public RemoteMethodCallResults() {}

  public RemoteMethodCallResults(final Object rVal) {
    m_rVal = rVal;
    m_exception = null;
  }

  public RemoteMethodCallResults(final Throwable exception) {
    m_rVal = null;
    m_exception = exception;
  }

  public Throwable getException() {
    return m_exception;
  }

  public Object getRVal() {
    return m_rVal;
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    if (m_rVal != null) {
      out.write(1);
      out.writeObject(m_rVal);
    } else {
      out.write(0);
      out.writeObject(m_exception);
    }
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    final boolean rVal = in.read() == 1;
    if (rVal) {
      m_rVal = in.readObject();
    } else {
      m_exception = (Throwable) in.readObject();
    }
  }
}
