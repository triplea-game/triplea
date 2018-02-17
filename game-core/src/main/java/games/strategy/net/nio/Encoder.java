package games.strategy.net.nio;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.io.IoUtils;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;

/**
 * Encodes data to be written by a writer.
 */
class Encoder {
  private static final Logger logger = Logger.getLogger(Encoder.class.getName());
  private final NioWriter writer;
  private final IObjectStreamFactory objectStreamFactory;
  private final NioSocket nioSocket;

  Encoder(final NioSocket nioSocket, final NioWriter writer, final IObjectStreamFactory objectStreamFactory) {
    this.nioSocket = nioSocket;
    this.writer = writer;
    this.objectStreamFactory = objectStreamFactory;
  }

  void write(final SocketChannel to, final MessageHeader header) {
    if (header.getFrom() == null) {
      throw new IllegalArgumentException("No from node");
    }
    if (to == null) {
      throw new IllegalArgumentException("No to channel!");
    }
    try {
      final byte[] bytes = IoUtils.writeToMemory(os -> write(header, objectStreamFactory.create(os), to));
      final SocketWriteData data = new SocketWriteData(bytes, bytes.length);
      writer.enque(data, to);
    } catch (final IOException e) {
      // we arent doing any io, just writing in memory
      // so something is very wrong
      logger.log(Level.SEVERE, "Error writing object:" + header, e);
      return;
    }
  }

  private void write(final MessageHeader header, final ObjectOutputStream out, final SocketChannel remote)
      throws IOException {
    if (header.getFrom() == null) {
      throw new IllegalArgumentException("null from");
    }
    // a broadcast
    if (header.getFor() == null) {
      out.write(1);
    } else {
      // to a node
      out.write(0);
      // the common case, skip writing the address
      if (header.getFor().equals(nioSocket.getRemoteNode(remote))) {
        out.write(1);
      } else {
        // this message is going to be relayed, write the destination
        out.write(0);
        ((Node) header.getFor()).writeExternal(out);
      }
    }
    if (header.getFrom().equals(nioSocket.getLocalNode())) {
      out.write(1);
    } else if (nioSocket.getLocalNode() == null) {
      out.write(2);
    } else {
      out.write(0);
      ((Node) header.getFrom()).writeExternal(out);
    }
    final byte type = Decoder.getType(header.getMessage());
    out.write(type);
    if (type != Byte.MAX_VALUE) {
      ((Externalizable) header.getMessage()).writeExternal(out);
    } else {
      out.writeObject(header.getMessage());
    }
    out.reset();
  }
}
