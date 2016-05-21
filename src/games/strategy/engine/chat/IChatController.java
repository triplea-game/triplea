package games.strategy.engine.chat;

import java.util.Map;

import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;
import games.strategy.util.Tuple;

/**
 * A central controller of who is in the chat.
 * <p>
 * When joining you get a list of all the players currently in the chat
 * <p>
 * To handle un-ordered comings and going into the chat, we send a version number with each chat change. The init method
 * is the sum of all
 * changes &lt; the version number returned in the init message.
 * <p>
 */
public interface IChatController extends IRemote {
  /**
   * Join the chat, returns the chatters currently in the chat.
   */
  Tuple<Map<INode, Tag>, Long> joinChat();

  /**
   * Leave the chat, and ask that everyone stops bothering me.
   */
  void leaveChat();

  enum Tag {
    MODERATOR, NONE
  }
}
