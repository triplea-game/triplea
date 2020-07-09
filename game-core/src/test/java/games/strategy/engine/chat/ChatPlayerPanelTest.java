package games.strategy.engine.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChatPlayerPanelTest {

  @Mock private PlayerChatRenderer renderer;

  @Test
  public void setPlayerRendererWhenChatIsNull() {

    final ChatPlayerPanel chatPlayerPanel = new ChatPlayerPanel(null);

    when(renderer.getMaxIconCounter()).thenReturn(1);
    chatPlayerPanel.setPlayerRenderer(renderer);

    verify(renderer, times(1)).getMaxIconCounter();
    assertEquals(chatPlayerPanel.getPreferredSize(), new Dimension(54, 80));
  }
}
