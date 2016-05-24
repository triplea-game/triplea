package games.strategy.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class ClientLoginHelper {
  private final IConnectionLogin login;
  private final SocketStreams streams;
  private String clientName;

  public ClientLoginHelper(final IConnectionLogin login, final SocketStreams streams, final String clientName) {
    this.login = login;
    this.streams = streams;
    this.clientName = clientName;
  }

  @SuppressWarnings("unchecked")
  public boolean login() {
    try {
      final ObjectOutputStream out = new ObjectOutputStream(streams.getBufferedOut());
      out.writeObject(clientName);
      // write the object output streams magic number
      out.flush();
      final ObjectInputStream in = new ObjectInputStream(streams.getBufferedIn());
      Map<String, String> challenge = (Map<String, String>) in.readObject();
      // the degenerate case
      if (challenge == null) {
        out.writeObject(null);
        out.flush();
        return true;
      }
      final Set<Entry<String,String>> entries = challenge.entrySet();//TODO someone needs to rewrite this in order to remove the Supressed Warning
      for (final Map.Entry<?,?> entry : entries) {
        // check what we read is a string
        if (!(entry.getKey() instanceof String) && !(entry.getValue() instanceof String)) {
          throw new IllegalStateException("Value must be a String");
        }
      }
      if (login == null) {
        throw new IllegalStateException("Challenged, but no login generator");
      }
      final Map<String, String> props = login.getProperties((Map<String,String>)challenge);
      out.writeObject(props);
      out.flush();
      final String response = (String) in.readObject();
      if (response == null) {
        clientName = (String) in.readObject();
        return true;
      }
      login.notifyFailedLogin(response);
      return false;
    } catch (final Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public String getClientName() {
    return clientName;
  }
}
