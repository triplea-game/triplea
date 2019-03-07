package games.strategy.net.nio;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

import com.google.common.base.Preconditions;

import games.strategy.io.IoUtils;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

/**
 * Encodes data to be written by a writer.
 */
@Log
@AllArgsConstructor
class Encoder {
  private final NioWriter writer;
  private final IObjectStreamFactory objectStreamFactory;

  void write(final SocketChannel to, final MessageHeader header) {
    checkNotNull(to);
    if (header.getFrom() == null) {
      throw new IllegalArgumentException("No from node");
    }
    try {
      final byte[] bytes = IoUtils.writeToMemory(os -> write(header, objectStreamFactory.create(os)));
      final SocketWriteData data = new SocketWriteData(bytes, bytes.length);
      writer.enque(data, to);
    } catch (final IOException e) {
      // we aren't doing any I/O, just writing in memory so something is very wrong
      log.log(Level.SEVERE, "Error writing object:" + header, e);
    }
  }

  private void write(final MessageHeader header, final ObjectOutputStream out) throws IOException {
    Preconditions.checkNotNull(header.getFrom());
    out.writeObject(header);
    out.reset();
  }
}
