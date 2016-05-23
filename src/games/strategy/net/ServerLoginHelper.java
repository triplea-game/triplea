package games.strategy.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

class ServerLoginHelper {
  /**
   * The sequence of events is
   * 1) client writes user name
   * 1) server writes challenge string (or null if no challenge, at which point the communication ends)
   * 2) client writes credentials in response to challenge string
   * 3) server reads credentials, sends null and then client name if login suceeds, otherwise an error message and the
   * connection is closed
   */
  private final static Logger logger = Logger.getLogger(ServerLoginHelper.class.getName());
  private final SocketAddress remoteAddress;
  private final ILoginValidator loginValidator;
  private final SocketStreams streams;
  private String clientName;
  private final ServerMessenger serverMessenger;

  public ServerLoginHelper(final SocketAddress remoteAddress, final ILoginValidator loginValidator,
      final SocketStreams streams, final ServerMessenger messenger) {
    this.remoteAddress = remoteAddress;
    this.loginValidator = loginValidator;
    this.streams = streams;
    serverMessenger = messenger;
  }

  @SuppressWarnings("unchecked")
  public boolean canConnect() {
    try {
      final ObjectOutputStream out = new ObjectOutputStream(streams.getBufferedOut());
      // write the object output streams magic number
      out.flush();
      final ObjectInputStream in = new ObjectInputStream(streams.getBufferedIn());
      clientName = (String) in.readObject();
      // the degenerate case
      if (loginValidator == null) {
        out.writeObject(null);
        out.flush();
        // cast to string to avoid toString() call on random object if
        // it isn't null
        final String read = (String) in.readObject();
        if (read != null) {
          throw new IllegalArgumentException("something non null read in response to null challenge " + read);
        }
        System.out.println("Server done");
        return true;
      }
      final Map<String, String> challenge = loginValidator.getChallengeProperties(clientName, remoteAddress);
      if (challenge == null) {
        throw new IllegalStateException("Challenge can't be null");
      }
      out.writeObject(challenge);
      out.flush();
      final Map<String, String> credentials = (Map<String, String>) in.readObject();//TODO rewrite in order to remove Suppressed Warning
      final Set<Map.Entry<String, String>> entries = credentials.entrySet();
      for (final Map.Entry<?,?> entry : entries) {
        // check what we read is a string
        if (!(entry.getKey() instanceof String) && !(entry.getValue() instanceof String)) {
          throw new IllegalStateException("Value must be a String");
        }
      }
      final String mac = MacFinder.GetHashedMacAddress();
      final String error =
          loginValidator.verifyConnection(challenge, credentials, clientName, mac, remoteAddress);
      if (error == null) {
        out.writeObject(null);
        clientName = serverMessenger.getUniqueName(clientName);
        out.writeObject(clientName);
        out.flush();
        return true;
      }
      out.writeObject(error);
      out.flush();
      return false;
    } catch (final Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      return false;
    }
  }

  public String getClientName() {
    return clientName;
  }
}
