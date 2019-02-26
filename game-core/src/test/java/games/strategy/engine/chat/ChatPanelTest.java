package games.strategy.engine.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import org.junit.jupiter.api.Test;

public class ChatPanelTest {

  @Test
  public void testTrim() throws Exception {
    final StyledDocument doc = new DefaultStyledDocument();
    final StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < 10; i++) {
      buffer.append("\n");
    }
    doc.insertString(0, buffer.toString(), null);
    ChatMessagePanel.trimLines(doc, 20);
    assertEquals(10, doc.getLength());
    ChatMessagePanel.trimLines(doc, 10);
    assertEquals(10, doc.getLength());
    ChatMessagePanel.trimLines(doc, 5);
    assertEquals(5, doc.getLength());
    ChatMessagePanel.trimLines(doc, 1);
    assertEquals(1, doc.getLength());
  }
}
