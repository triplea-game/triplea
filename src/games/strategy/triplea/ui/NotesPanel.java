package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.util.SoftJEditorPane;

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
	protected final SoftJEditorPane m_gameNotesPane;
	protected final GameData m_data;
	final JButton m_refresh = new JButton("Refresh Notes");
	
	// we now require passing a JEditorPane containing the notes in it, because we do not want to have multiple copies of it in memory for all the different ways the user can access the game notes
	// so instead we keep the main copy in the BasicGameMenuBar, and then give it to the notes tab. this prevents out of memory errors for maps with large images in their games notes.
	public NotesPanel(final GameData data, final SoftJEditorPane gameNotesPane)
	{
		m_data = data;
		m_gameNotesPane = gameNotesPane;
		initLayout();
	}
	
	protected void initLayout()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		m_refresh.setAlignmentY(Component.CENTER_ALIGNMENT);
		m_refresh.addActionListener(new AbstractAction("Refresh Notes")
		{
			private static final long serialVersionUID = 8439704398303765832L;
			
			public void actionPerformed(final ActionEvent e)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						layoutNotes();
					}
				});
			}
		});
		// layoutNotes();
		removeNotes();
	}
	
	void removeNotes()
	{
		NotesPanel.this.removeAll();
		NotesPanel.this.add(new JLabel(" "));
		NotesPanel.this.add(m_refresh);
		NotesPanel.this.add(new JLabel(" "));
		// NotesPanel.this.invalidate();
	}
	
	void layoutNotes()
	{
		if (m_gameNotesPane == null)
			return;
		NotesPanel.this.removeAll();
		NotesPanel.this.add(new JLabel(" "));
		NotesPanel.this.add(m_refresh);
		NotesPanel.this.add(new JLabel(" "));
		final JEditorPane pane = m_gameNotesPane.getComponent();
		final JScrollPane scroll = new JScrollPane(pane);
		scroll.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
		NotesPanel.this.add(scroll);
		// NotesPanel.this.invalidate();
	}
	
	public boolean isEmpty()
	{
		return m_gameNotesPane == null || m_gameNotesPane.getText() == null || m_gameNotesPane.getText().length() <= 0;
	}
}
