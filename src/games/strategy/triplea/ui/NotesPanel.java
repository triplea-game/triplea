package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class NotesPanel extends JPanel
{
	private static final long serialVersionUID = 2746643868463714526L;
	protected final JEditorPane m_textArea;
	protected final GameData m_data;
	
	// we now require passing a JEditorPane containing the notes in it, because we do not want to have multiple copies of it in memory for all the different ways the user can access the game notes
	// so instead we keep the main copy in the BasicGameMenuBar, and then give it to the notes tab. this prevents out of memory errors for maps with large images in their games notes.
	public NotesPanel(final GameData data, final JEditorPane gameNotesPane)
	{
		m_data = data;
		m_textArea = gameNotesPane;
		initLayout();
	}
	
	protected void initLayout()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		// m_textArea.setEditable(false);
		// m_textArea.setContentType("text/html");
		
		final JButton refresh = new JButton("Refresh Notes");
		refresh.setAlignmentY(Component.CENTER_ALIGNMENT);
		refresh.addActionListener(new AbstractAction("Refresh Notes")
		{
			private static final long serialVersionUID = 8439704398303765832L;
			
			public void actionPerformed(final ActionEvent e)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						NotesPanel.this.removeAll();
						NotesPanel.this.add(new JLabel(" "));
						NotesPanel.this.add(refresh);
						NotesPanel.this.add(new JLabel(" "));
						m_textArea.setCaretPosition(0);
						final JScrollPane scroll = new JScrollPane(m_textArea);
						scroll.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
						NotesPanel.this.add(scroll);
						NotesPanel.this.invalidate();
					}
				});
			}
		});
		add(new JLabel(" "));
		add(refresh);
		add(new JLabel(" "));
		
		// fillNotesPane();
		m_textArea.setCaretPosition(0);
		final JScrollPane scroll = new JScrollPane(m_textArea);
		scroll.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
		add(scroll);
	}
	
	/*
	private void fillNotesPane()
	{
		final String notes = m_data.getProperties().get("notes", "");
		if (notes == null || notes.trim().length() <= 0)
		{
			m_textArea.setText("");
		}
		else
		{
			m_textArea.setText(LocalizeHTML.localizeImgLinksInHTML(notes.trim()));
		}
		m_textArea.setCaretPosition(0);
	}
	*/

	public boolean isEmpty()
	{
		return m_textArea == null || m_textArea.getText() == null || m_textArea.getText().length() <= 0;
	}
}
