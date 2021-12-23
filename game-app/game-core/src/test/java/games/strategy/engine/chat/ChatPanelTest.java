package games.strategy.engine.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;

class ChatPanelTest {

  @Test
  void testTrim() throws Exception {
    final StyledDocument doc = new DefaultStyledDocument();
    doc.insertString(0, "\n".repeat(10), null);
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
