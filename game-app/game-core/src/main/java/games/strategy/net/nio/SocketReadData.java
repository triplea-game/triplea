package games.strategy.net.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import lombok.Getter;

/**
 * A packet of data being read over the network.
 *
 * <p>A Packet does not correspond to a network packet, rather it is the bytes for 1 serialized java
 * object.
 */
class SocketReadData {
  static final int MAX_MESSAGE_SIZE = 1000 * 1000 * 10;
  // as a sanity check to make sure we are talking to another TripleA instance
  // that the upper bits of the packet size we send is 0x9b
  static final int MAGIC = 0x9b000000;

  private int targetSize = -1;
  // we read into here the first four bytes to find out size
  private ByteBuffer sizeBuffer;
  // we read into here after knowing out size
  private ByteBuffer contentBuffer;
  @Getter private final SocketChannel channel;
  @Getter private int readCalls;

  SocketReadData(final SocketChannel channel) {
    this.channel = channel;
  }

  /**
   * Read data from the channel, returning true if this packet is done.
   *
   * <p>If we detect the socket is closed, we will throw an IOException
   */
  public boolean read(final SocketChannel channel) throws IOException {
    readCalls++;
    // we dont know our size, read it
    if (targetSize < 0) {
      // our first read
      // find out how big this packet is
      if (sizeBuffer == null) {
        sizeBuffer = ByteBuffer.allocate(4);
      }
      final int size = channel.read(sizeBuffer);
      if (size == -1) {
        throw new IOException("Socket closed");
      }
      // we have read all four bytes of our size
      if (!sizeBuffer.hasRemaining()) {
        sizeBuffer.flip();
        targetSize = sizeBuffer.getInt();
        if ((targetSize & 0xFF000000) != MAGIC) {
          throw new IOException("Did not write magic!");
        }
        targetSize = targetSize & 0x00ffffff;
        // limit messages to 10MB
        if (targetSize <= 0 || targetSize > MAX_MESSAGE_SIZE) {
          throw new IOException("Invalid triplea packet size: " + targetSize);
        }
        contentBuffer = ByteBuffer.allocate(targetSize);
        sizeBuffer = null;
      } else {
        // we didn't read all 4 bytes, return
        return false;
      }
    }
    // http://javaalmanac.com/egs/java.nio/DetectClosed.html
    final int size = channel.read(contentBuffer);
    if (size == -1) {
      throw new IOException("Socket closed");
    }
    return !contentBuffer.hasRemaining();
  }

  /** Get the data as a byte[]. This method can only be called once. */
  public byte[] getData() {
    final byte[] data = new byte[contentBuffer.capacity()];
    contentBuffer.flip();
    contentBuffer.get(data);
    contentBuffer = null;
    return data;
  }

  public int size() {
    // add 4 to count the bytes used to send our size
    return targetSize + 4;
  }
}
