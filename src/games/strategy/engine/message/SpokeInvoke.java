package games.strategy.engine.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import games.strategy.engine.message.unifiedmessenger.Invoke;
import games.strategy.net.GUID;
import games.strategy.net.INode;
import games.strategy.net.Node;

public class SpokeInvoke extends Invoke {
  private INode m_invoker;

  public SpokeInvoke() {
    super();
  }

  public SpokeInvoke(final GUID methodCallID, final boolean needReturnValues, final RemoteMethodCall call,
      final INode invoker) {
    super(methodCallID, needReturnValues, call);
    m_invoker = invoker;
  }

  public INode getInvoker() {
    return m_invoker;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    m_invoker = new Node();
    ((Node) m_invoker).readExternal(in);
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    super.writeExternal(out);
    ((Node) m_invoker).writeExternal(out);
  }
}
