package games.strategy.engine.chat;

import java.util.Collection;
import org.triplea.domain.data.ChatParticipant;

/** Callback interface for a components interested in displaying a list of chat participants. */
public interface ChatPlayerListener {
  void updatePlayerList(Collection<ChatParticipant> players);
}
