package games.strategy.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class JTextAreaOptionPane
{
	private final JTextArea m_editor = new JTextArea();
	private final JFrame m_windowFrame = new JFrame();
	private final JButton m_okButton = new JButton();
	private final JLabel m_label = new JLabel();
	private final boolean m_logToSystemOut;
	private final WeakReference<Window> m_parentComponentReference;
	private int m_counter;
	private final CountDownLatch m_countDownLatch;
	
	public JTextAreaOptionPane(final JFrame parentComponent, final String initialEditorText, final String labelText, final String title, final Image icon, final int editorSizeX,
				final int editorSizeY, final boolean logToSystemOut, final int latchCount, final CountDownLatch countDownLatch)
	{
		m_logToSystemOut = logToSystemOut;
		m_countDownLatch = countDownLatch;
		m_counter = latchCount;
		m_parentComponentReference = new WeakReference<Window>(parentComponent);
		m_windowFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		if (icon != null)
			m_windowFrame.setIconImage(icon);
		else if (parentComponent != null && parentComponent.getIconImage() != null)
			m_windowFrame.setIconImage(parentComponent.getIconImage());
		final BorderLayout layout = new BorderLayout();
		layout.setHgap(30);
		layout.setVgap(30);
		m_windowFrame.setLayout(layout);
		m_windowFrame.setTitle(title);
		m_label.setText(labelText);
		m_okButton.setText("OK");
		m_okButton.setEnabled(false);
		m_editor.setEditable(false);
		// m_editor.setContentType("text/html");
		m_editor.setText(initialEditorText);
		if (m_logToSystemOut)
			System.out.println(initialEditorText);
		m_editor.setCaretPosition(0);
		m_windowFrame.setPreferredSize(new Dimension(editorSizeX, editorSizeY));
		m_windowFrame.getContentPane().add(m_label, BorderLayout.NORTH);
		m_windowFrame.getContentPane().add(new JScrollPane(m_editor), BorderLayout.CENTER);
		m_windowFrame.getContentPane().add(m_okButton, BorderLayout.SOUTH);
		m_okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (m_countDownLatch != null)
					m_countDownLatch.countDown();
				dispose();
			}
		});
	}
	
	private void setWidgetActivation()
	{
		if (m_counter <= 0)
			m_okButton.setEnabled(true);
	}
	
	public void show()
	{
		m_windowFrame.pack();
		m_windowFrame.setLocationRelativeTo(m_parentComponentReference.get());
		m_windowFrame.setVisible(true);
	}
	
	public void dispose()
	{
		m_windowFrame.setVisible(false);
		m_windowFrame.dispose();
	}
	
	public void countDown()
	{
		m_counter--;
		setWidgetActivation();
	}
	
	public void append(final String text)
	{
		if (m_logToSystemOut)
			System.out.print(text);
		m_editor.append(text);
		m_editor.setCaretPosition(m_editor.getText().length());
	}
	
	public void appendNewLine(final String text)
	{
		append(text + "\r\n");
	}
	
	/*
	public static void main(final String[] args)
	{
		final JTextAreaOptionPane pane = new JTextAreaOptionPane(null, "initial text\r\n", "label goes here", "testing text area option pane", null, 400, 300, true, 1);
		pane.show();
		for (int i = 0; i <= 20; i++)
		{
			pane.appendNewLine("blah: " + i);
			try
			{
				Thread.sleep(120);
			} catch (final InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		try
		{
			Thread.sleep(1000);
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		}
		pane.countDown();
	}
	*/
}
