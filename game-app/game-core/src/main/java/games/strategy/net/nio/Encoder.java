package games.strategy.net.nio;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.IoUtils;

/** Encodes data to be written by a writer. */
@Slf4j
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
      final byte[] bytes =
          IoUtils.writeToMemory(os -> write(header, objectStreamFactory.create(os)));
      final SocketWriteData data = new SocketWriteData(bytes);
      writer.enque(data, to);
    } catch (final IOException e) {
      // we aren't doing any I/O, just writing in memory so something is very wrong
      log.error("Error writing object: " + header, e);
    }
  }

  private void write(final MessageHeader header, final ObjectOutputStream out) throws IOException {
    checkNotNull(header.getFrom());
    out.writeObject(header);
    out.reset();
  }
}
