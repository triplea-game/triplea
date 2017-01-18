package games.strategy.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.nio.ClientQuarantineConversation;
import games.strategy.net.nio.NIOSocket;
import games.strategy.net.nio.NIOSocketListener;
import games.strategy.net.nio.QuarantineConversation;
import games.strategy.util.ListenerList;
import games.strategy.util.ThreadUtil;

public class ClientMessenger implements IClientMessenger, NIOSocketListener {
  private INode m_node;
  private final ListenerList<IMessageListener> m_listeners = new ListenerList<>();
  private final ListenerList<IMessengerErrorListener> m_errorListeners = new ListenerList<>();
  private final CountDownLatch m_initLatch = new CountDownLatch(1);
  private Exception m_connectionRefusedError;
  private final NIOSocket m_socket;
  private final SocketChannel m_socketChannel;
  private INode m_serverNode;
  private volatile boolean m_shutDown = false;

  /**
   * Note, the name paramater passed in here may not match the name of the
   * ClientMessenger after it has been constructed.
   */
  public ClientMessenger(final String host, final int port, final String name, final String mac,
      final IConnectionLogin login) throws IOException {
    this(host, port, name, mac, new DefaultObjectStreamFactory(), login);
  }

  /**
   * Note, the name paramater passed in here may not match the name of the
   * ClientMessenger after it has been constructed.
   */
  public ClientMessenger(final String host, final int port, final String name, final String mac)
      throws IOException {
    this(host, port, name, mac, new DefaultObjectStreamFactory());
  }

  /**
   * Note, the name paramater passed in here may not match the name of the
   * ClientMessenger after it has been constructed.
   */
  public ClientMessenger(final String host, final int port, final String name, final String mac,
      final IObjectStreamFactory streamFact) throws IOException {
    this(host, port, name, mac, streamFact, null);
  }

  /**
   * Note, the name paramater passed in here may not match the name of the
   * ClientMessenger after it has been constructed.
   */
  public ClientMessenger(final String host, final int port, final String name, final String mac,
      final IObjectStreamFactory streamFact, final IConnectionLogin login)
      throws IOException {
    m_socketChannel = SocketChannel.open();
    m_socketChannel.configureBlocking(false);
    final InetSocketAddress remote = new InetSocketAddress(host, port);
    if (!m_socketChannel.connect(remote)) {
      // give up after 10 seconds
      int waitTimeMilliseconds = 0;
      while (true) {
        if (waitTimeMilliseconds > 10000) {
          m_socketChannel.close();
          throw new IOException("Connection refused");
        }
        if (m_socketChannel.finishConnect()) {
          break;
        }
        if(!ThreadUtil.sleep(50)) {
          shutDown();
          m_socket = null;
          return;
        }
        waitTimeMilliseconds += 50;
      }
    }
    final Socket socket = m_socketChannel.socket();
    socket.setKeepAlive(true);
    m_socket = new NIOSocket(streamFact, this, name);
    final ClientQuarantineConversation conversation =
        new ClientQuarantineConversation(login, m_socketChannel, m_socket, name, mac);
    m_socket.add(m_socketChannel, conversation);
    // allow the credentials to be shown in this thread
    conversation.showCredentials();
    // wait for the quarantine to end
    try {
      m_initLatch.await();
    } catch (final InterruptedException e) {
      m_connectionRefusedError = e;
      try {
        m_socketChannel.close();
      } catch (final IOException e2) {
        // ignore
      }
    }
    if (conversation.getErrorMessage() != null || m_connectionRefusedError != null) {
      // our socket channel should already be closed
      m_socket.shutDown();
      if (conversation.getErrorMessage() != null) {
        String msg = conversation.getErrorMessage();
        if (m_connectionRefusedError != null) {
          msg += ", " + m_connectionRefusedError;
        }
        login.notifyFailedLogin(msg);
        throw new CouldNotLogInException();
      } else if (m_connectionRefusedError instanceof CouldNotLogInException) {
        throw (CouldNotLogInException) m_connectionRefusedError;
      } else if (m_connectionRefusedError != null) {
        throw new IOException(m_connectionRefusedError.getMessage());
      }
    }
  }

  @Override
  public synchronized void send(final Serializable msg, final INode to) {
    // use our nodes address, this is our network visible address
    final MessageHeader header = new MessageHeader(to, m_node, msg);
    m_socket.send(m_socketChannel, header);
  }

  @Override
  public synchronized void broadcast(final Serializable msg) {
    final MessageHeader header = new MessageHeader(m_node, msg);
    m_socket.send(m_socketChannel, header);
  }

  @Override
  public void addMessageListener(final IMessageListener listener) {
    m_listeners.add(listener);
  }

  @Override
  public void removeMessageListener(final IMessageListener listener) {
    m_listeners.remove(listener);
  }

  @Override
  public void addErrorListener(final IMessengerErrorListener listener) {
    m_errorListeners.add(listener);
  }

  @Override
  public void removeErrorListener(final IMessengerErrorListener listener) {
    m_errorListeners.remove(listener);
  }

  @Override
  public boolean isConnected() {
    return m_socketChannel.isConnected();
  }

  @Override
  public void shutDown() {
    m_shutDown = true;
    if(m_socket != null) {
      m_socket.shutDown();
    }
    try {
      m_socketChannel.close();
    } catch (final IOException e) {
      // ignore
    }
  }

  public boolean isShutDown() {
    return m_shutDown;
  }

  @Override
  public void messageReceived(final MessageHeader msg, final SocketChannel channel) {
    if (msg.getFor() != null && !msg.getFor().equals(m_node)) {
      throw new IllegalStateException("msg not for me:" + msg);
    }
    for (final IMessageListener listener : m_listeners) {
      listener.messageReceived(msg.getMessage(), msg.getFrom());
    }
  }

  /**
   * Get the local node
   */
  @Override
  public INode getLocalNode() {
    return m_node;
  }

  @Override
  public INode getServerNode() {
    return m_serverNode;
  }

  @Override
  public boolean isServer() {
    return false;
  }

  @Override
  public void socketUnqaurantined(final SocketChannel channel, final QuarantineConversation converstaion2) {
    final ClientQuarantineConversation conversation = (ClientQuarantineConversation) converstaion2;
    // all ids are based on the socket adress of nodes in the network
    // but the adress of a node changes depending on who is looking at it
    // ie, sometimes it is the loopback adress if connecting locally,
    // sometimes the client or server will be behind a NAT
    // so all node ids are defined as what the server sees the adress as
    // we are still in the decode thread at this point, set our nodes now
    // before the socket is unquarantined
    m_node = new Node(conversation.getLocalName(), conversation.getNetworkVisibleSocketAdress());
    m_serverNode = new Node(conversation.getServerName(), conversation.getServerLocalAddress());
    m_initLatch.countDown();
  }

  @Override
  public void socketError(final SocketChannel channel, final Exception error) {
    if (m_shutDown) {
      return;
    }
    // if an error occurs during set up
    // we need to return in the constructor
    // otherwise this is harmless
    m_connectionRefusedError = error;
    for (final IMessengerErrorListener errorListener : m_errorListeners) {
      errorListener.messengerInvalid(ClientMessenger.this, error);
    }
    shutDown();
    m_initLatch.countDown();
  }

  @Override
  public INode getRemoteNode(final SocketChannel channel) {
    // we only have one channel
    return m_serverNode;
  }

  @Override
  public InetSocketAddress getRemoteServerSocketAddress() {
    return (InetSocketAddress) m_socketChannel.socket().getRemoteSocketAddress();
  }

  private void bareBonesSendMessageToServer(final String methodName, final Object... messages) {
    final List<Object> args = new ArrayList<>();
    final Class<? extends Object>[] argTypes = new Class<?>[messages.length];
    for (int i = 0; i < messages.length; i++) {
      final Object message = messages[i];
      args.add(message);
      argTypes[i] = args.get(i).getClass();
    }
    final RemoteName rn = ServerModel.SERVER_REMOTE_NAME;
    final RemoteMethodCall call =
        new RemoteMethodCall(rn.getName(), methodName, args.toArray(), argTypes, rn.getClazz());
    final HubInvoke hubInvoke = new HubInvoke(null, false, call);
    send(hubInvoke, getServerNode());
  }

  @Override
  public void changeServerGameTo(final String gameName) {
    bareBonesSendMessageToServer("changeServerGameTo", gameName);
  }

  @Override
  public void changeToLatestAutosave(final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave) {
    bareBonesSendMessageToServer("changeToLatestAutosave", typeOfAutosave);
  }

  @Override
  public void changeToGameSave(final byte[] bytes, final String fileName) {
    bareBonesSendMessageToServer("changeToGameSave", bytes, fileName);
  }

  @Override
  public void changeToGameSave(final File saveGame, final String fileName) {
    final byte[] bytes = getBytesFromFile(saveGame);
    if (bytes == null || bytes.length == 0) {
      return;
    }
    changeToGameSave(bytes, fileName);
  }

  private static byte[] getBytesFromFile(final File file) {
    if (file == null || !file.exists()) {
      return null;
    }
    // Get the size of the file
    final long length = file.length();
    if (length > Integer.MAX_VALUE) {
      return null;
    }
    // Create the byte array to hold the data
    final byte[] bytes = new byte[(int) length];
    try (InputStream is = new FileInputStream(file)) {
      is.read(bytes);
    } catch (final IOException e) {
      ClientLogger.logQuietly("Failed to read file: " + file);
      ClientLogger.logQuietly(e);
    }
    return bytes;
  }

  @Override
  public String toString() {
    return "ClientMessenger LocalNode:" + m_node + " ServerNodes:" + m_serverNode;
  }
}
