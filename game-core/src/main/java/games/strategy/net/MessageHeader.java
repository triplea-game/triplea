package games.strategy.net;

import java.io.Serializable;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The envelope for a message consisting of both the header and payload. The header specifies the source and
 * destination nodes of the message.
 */
@Getter
@AllArgsConstructor
public class MessageHeader {
  // to can be null if we are a broadcast
  @Nullable
  private final INode to;
  private final Serializable message;
  // from can be null if the sending node doesnt know its own address
  @Nullable
  private final INode from;
}
