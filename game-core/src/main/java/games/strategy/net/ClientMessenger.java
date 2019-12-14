package games.strategy.net;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.framework.HeadlessAutoSaveType;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.nio.ClientQuarantineConversation;
import games.strategy.net.nio.ForgotPasswordConversation;
import games.strategy.net.nio.NioSocket;
import games.strategy.net.nio.NioSocketListener;
import games.strategy.net.nio.QuarantineConversation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.triplea.domain.data.SystemId;
import org.triplea.java.Interruptibles;

/** Default implementation of {@link IClientMessenger}. */
@Log
public class ClientMessenger implements IClientMessenger, NioSocketListener {
  private INode node;
  private final List<IMessageListener> listeners = new CopyOnWriteArrayList<>();
  private final List<IMessengerErrorListener> errorListeners = new CopyOnWriteArrayList<>();
  private final CountDownLatch initLatch = new CountDownLatch(1);
  private Exception connectionRefusedError;
  private final NioSocket nioSocket;
  private final SocketChannel socketChannel;
  private INode serverNode;
  private volatile boolean shutDown = false;

  /**
   * Note, the name parameter passed in here may not match the name of the ClientMessenger after it
   * has been constructed.
   */
  @VisibleForTesting
  public ClientMessenger(
      final String host, final int port, final String name, final SystemId systemId)
      throws IOException {
    this(host, port, name, systemId, new DefaultObjectStreamFactory(), null);
  }

  /**
   * Note, the name parameter passed in here may not match the name of the ClientMessenger after it
   * has been constructed.
   */
  public ClientMessenger(
      final String host,
      final int port,
      final String name,
      final SystemId systemId,
      final IObjectStreamFactory streamFact,
      final IConnectionLogin login)
      throws IOException {
    Preconditions.checkNotNull(systemId);
    socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(false);
    final InetSocketAddress remote = new InetSocketAddress(host, port);
    if (!socketChannel.connect(remote)) {
      // give up after 10 seconds
      int waitTimeMilliseconds = 0;
      while (true) {
        if (waitTimeMilliseconds > 10000) {
          socketChannel.close();
          throw new IOException("Connection refused");
        }
        try {
          if (socketChannel.finishConnect()) {
            break;
          }
        } catch (final ConnectException e) {
          throw new RuntimeException(
              String.format("Could not connect host: %s, port: %s", host, port), e);
        }
        if (!Interruptibles.sleep(50)) {
          shutDown();
          nioSocket = null;
          return;
        }
        waitTimeMilliseconds += 50;
      }
    }
    final Socket socket = socketChannel.socket();
    socket.setKeepAlive(true);
    nioSocket = new NioSocket(streamFact, this);
    final ClientQuarantineConversation conversation =
        new ClientQuarantineConversation(login, socketChannel, nioSocket, name, systemId);

    nioSocket.add(socketChannel, conversation);
    // allow the credentials to be shown in this thread
    conversation.showCredentials();
    // wait for the quarantine to end
    try {
      initLatch.await();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      connectionRefusedError = e;
      try {
        socketChannel.close();
      } catch (final IOException e2) {
        // ignore
      }
    }

    if (conversation.getErrorMessage() != null
        && conversation
            .getErrorMessage()
            .equals(ForgotPasswordConversation.TEMP_PASSWORD_GENERATED_RESPONSE)) {
      return;
    }

    if (conversation.getErrorMessage() != null || connectionRefusedError != null) {
      // our socket channel should already be closed
      nioSocket.shutDown();
      if (conversation.getErrorMessage() != null) {
        String msg = conversation.getErrorMessage();
        if (connectionRefusedError != null) {
          msg += ", " + connectionRefusedError;
        }
        throw new CouldNotLogInException(msg);
      } else if (connectionRefusedError instanceof CouldNotLogInException) {
        throw (CouldNotLogInException) connectionRefusedError;
      } else if (connectionRefusedError != null) {
        throw new IOException(connectionRefusedError);
      }
    }
  }

  @Override
  public synchronized void send(final Serializable msg, final INode to) {
    // use our nodes address, this is our network visible address
    final MessageHeader header = new MessageHeader(to, node, msg);
    nioSocket.send(socketChannel, header);
  }

  @Override
  public void addMessageListener(final IMessageListener listener) {
    listeners.add(listener);
  }

  @Override
  public void addErrorListener(final IMessengerErrorListener listener) {
    errorListeners.add(listener);
  }

  @Override
  public void removeErrorListener(final IMessengerErrorListener listener) {
    errorListeners.remove(listener);
  }

  @Override
  public boolean isConnected() {
    return socketChannel.isConnected();
  }

  @Override
  public void shutDown() {
    shutDown = true;
    if (nioSocket != null) {
      nioSocket.shutDown();
    }
    try {
      socketChannel.close();
    } catch (final IOException e) {
      // ignore
    }
  }

  @Override
  public void messageReceived(final MessageHeader msg, final SocketChannel channel) {
    if (msg.getTo() != null && !msg.getTo().equals(node)) {
      throw new IllegalStateException("msg not for me:" + msg);
    }
    for (final IMessageListener listener : listeners) {
      listener.messageReceived(msg.getMessage(), msg.getFrom());
    }
  }

  @Override
  public INode getLocalNode() {
    return node;
  }

  @Override
  public INode getServerNode() {
    return serverNode;
  }

  @Override
  public boolean isServer() {
    return false;
  }

  @Override
  public void socketUnquarantined(
      final SocketChannel channel, final QuarantineConversation quarantineConversation) {
    final var conversation = (ClientQuarantineConversation) quarantineConversation;
    // all ids are based on the socket address of nodes in the network
    // but the address of a node changes depending on who is looking at it
    // ie, sometimes it is the loopback address if connecting locally,
    // sometimes the client or server will be behind a NAT
    // so all node ids are defined as what the server sees the address as
    // we are still in the decode thread at this point, set our nodes now
    // before the socket is unquarantined
    node = new Node(conversation.getLocalName(), conversation.getNetworkVisibleAddress());
    serverNode = new Node(conversation.getServerName(), conversation.getServerLocalAddress());
    initLatch.countDown();
  }

  @Override
  public void socketError(final SocketChannel channel, final Exception error) {
    if (shutDown) {
      return;
    }
    // if an error occurs during set up we need to return in the constructor
    // otherwise this is harmless
    connectionRefusedError = error;
    for (final IMessengerErrorListener errorListener : errorListeners) {
      errorListener.messengerInvalid(error);
    }
    shutDown();
    initLatch.countDown();
  }

  @Override
  public INode getRemoteNode(final SocketChannel channel) {
    // we only have one channel
    return serverNode;
  }

  @Override
  public void addConnectionChangeListener(final IConnectionChangeListener listener) {}

  @Override
  public void removeConnectionChangeListener(final IConnectionChangeListener listener) {}

  private void bareBonesSendMessageToServer(final String methodName, final Object... messages) {
    final List<Object> args = new ArrayList<>();
    final Class<?>[] argTypes = new Class<?>[messages.length];
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
  public void changeToLatestAutosave(final HeadlessAutoSaveType typeOfAutosave) {
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
      log.log(Level.SEVERE, "Failed to read file: " + file, e);
    }
    return bytes;
  }

  @Override
  public String toString() {
    return "ClientMessenger LocalNode:" + node + " ServerNodes:" + serverNode;
  }
}
