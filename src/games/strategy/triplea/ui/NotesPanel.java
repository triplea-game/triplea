package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class NotesPanel extends JPanel
{
	private static final long serialVersionUID = 2746643868463714526L;
	protected static JEditorPane m_textArea = new JEditorPane();
	protected GameData m_data;
	
	public NotesPanel(final GameData data)
	{
		m_data = data;
		initLayout();
	}
	
	protected void initLayout()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		m_textArea.setEditable(false);
		m_textArea.setContentType("text/html");
		/*
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
						fillNotesPane();
					}
				});
			}
		});
		add(refresh);
		*/
		fillNotesPane();
		final JScrollPane scroll = new JScrollPane(m_textArea);
		add(scroll);
	}
	
	private void fillNotesPane()
	{
		final String notes = m_data.getProperties().get("notes", "");
		if (notes == null || notes.trim().length() <= 0)
			m_textArea.setText("");
		else
			m_textArea.setText(notes);
		m_textArea.setCaretPosition(0);
	}
	
	public boolean isEmpty()
	{
		return m_textArea == null || m_textArea.getText() == null || m_textArea.getText().length() <= 0;
	}
}
