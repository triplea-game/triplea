package org.triplea.web.socket;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

@ExtendWith(MockitoExtension.class)
class MessageBroadcasterTest {

  @Mock private Session session0;
  @Mock private Session session1;
  @Mock private Session session2;
  @Mock private ServerMessageEnvelope serverEventEnvelope;

  @Mock private BiConsumer<Session, ServerMessageEnvelope> singleMessageSender;
  @InjectMocks private MessageBroadcaster messageBroadcaster;

  @Test
  void accept() {
    when(session0.isOpen()).thenReturn(true);
    when(session1.isOpen()).thenReturn(true);
    when(session2.isOpen()).thenReturn(false);

    messageBroadcaster.accept(Set.of(session0, session1, session2), serverEventEnvelope);

    verify(singleMessageSender).accept(session0, serverEventEnvelope);
    verify(singleMessageSender).accept(session1, serverEventEnvelope);
    // session2 is not open, should not be used
    verify(singleMessageSender, never()).accept(session2, serverEventEnvelope);
  }
}
