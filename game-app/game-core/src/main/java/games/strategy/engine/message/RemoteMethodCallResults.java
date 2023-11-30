package games.strategy.engine.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import lombok.Getter;

/**
 * The results of a method execution. Note that either one of returnValue or exception will be null,
 * since the method can either throw or return.
 */
public class RemoteMethodCallResults implements Externalizable {
  private static final long serialVersionUID = 4562274411264858613L;
  private Object returnValue;
  // throwable implements Serializable
  @Getter private Throwable exception;

  public RemoteMethodCallResults() {}

  public RemoteMethodCallResults(final Object returnValue) {
    this.returnValue = returnValue;
    exception = null;
  }

  public RemoteMethodCallResults(final Throwable exception) {
    returnValue = null;
    this.exception = exception;
  }

  public Object getRVal() {
    return returnValue;
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    if (returnValue != null) {
      out.write(1);
      out.writeObject(returnValue);
    } else {
      out.write(0);
      out.writeObject(exception);
    }
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    final boolean hasReturnValue = in.read() == 1;
    if (hasReturnValue) {
      returnValue = in.readObject();
    } else {
      exception = (Throwable) in.readObject();
    }
  }

  @Override
  public String toString() {
    final String exceptionMsg = (exception == null) ? "none" : exception.toString();
    return "Return value: '" + returnValue + "', exception: " + exceptionMsg;
  }
}
