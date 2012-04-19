package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

public class WaitWindow extends JWindow
{
	private static final long serialVersionUID = -8134956690669346954L;
	private final Object m_mutex = new Object();
	private Timer m_timer = new Timer();
	
	public WaitWindow(final String waitMessage)
	{
		// super("Game Loading, Please wait");
		// setIconImage(GameRunner.getGameIcon(this));
		setSize(200, 80);
		final WaitPanel mainPanel = new WaitPanel(waitMessage);
		setLocationRelativeTo(null);
		// setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainPanel.setBorder(new LineBorder(Color.BLACK));
		setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
	}
	
	public void showWait()
	{
		final TimerTask task = new TimerTask()
		{
			@Override
			public void run()
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						toFront();
					}
				});
			}
		};
		synchronized (m_mutex)
		{
			if (m_timer != null)
				m_timer.schedule(task, 15, 15);
		}
	}
	
	public void doneWait()
	{
		synchronized (m_mutex)
		{
			if (m_timer != null)
			{
				m_timer.cancel();
				m_timer = null;
			}
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				setVisible(false);
				removeAll();
				dispose();
			}
		});
	}
	
	public static void main(final String[] args)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final WaitWindow window = new WaitWindow("Loading game, please wait.");
				window.setVisible(true);
				window.showWait();
			}
		});
	}
}
