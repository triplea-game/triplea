package games.strategy.engine.chat;

import javax.swing.text.*;

import junit.framework.TestCase;

public class ChatPanelTest extends TestCase
{

    public void testTrim() throws Exception
    {
        StyledDocument doc = new DefaultStyledDocument();
        
        StringBuffer buffer = new StringBuffer();
        for(int i =0; i < 10; i++)
        {
            buffer.append("\n");
        }
        doc.insertString(0, buffer.toString(), null);

        ChatPanel.trimLines(doc, 20);
        assertEquals(doc.getLength(), 10);

        ChatPanel.trimLines(doc, 10);
        assertEquals(doc.getLength(), 10);
        
        ChatPanel.trimLines(doc, 5);
        assertEquals(doc.getLength(), 5);
        
        ChatPanel.trimLines(doc, 1);
        assertEquals(doc.getLength(), 1);
        
    }
    
    
}
