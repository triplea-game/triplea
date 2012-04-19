package games.strategy.engine.framework.startup.ui.editors;

import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.engine.pbem.NullForumPoster;
import games.strategy.ui.ProgressWindow;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;

/**
 * A class for selecting which Forum poster to use
 * 
 * @author Klaus Groenbaek
 */
public class ForumPosterEditor extends EditorPanel
{
	private static final long serialVersionUID = -6069315084412575053L;
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	private final JButton m_viewPosts = new JButton("View Forum");
	private final JButton m_testForum = new JButton("Test Post");
	
	private final JLabel m_loginLabel = new JLabel("Login:");
	private final JLabel m_passwordLabel = new JLabel("Password:");
	private final JTextField m_login = new JTextField();
	private final JTextField m_password = new JPasswordField();
	
	private final JTextField m_topicIdField = new JTextField();
	private final JLabel m_topicIdLabel = new JLabel("Topic Id:");
	
	private final JCheckBox m_includeSaveGame = new JCheckBox("Attach save game to summary");
	private final IForumPoster m_bean;
	
	// -----------------------------------------------------------------------
	// constructors
	// -----------------------------------------------------------------------
	
	public ForumPosterEditor(final IForumPoster bean)
	{
		m_bean = bean;
		
		final int bottomSpace = 1;
		final int labelSpace = 2;
		int row = 0;
		if (m_bean.getCanViewPosted())
		{
			add(m_topicIdLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
			add(m_topicIdField, new GridBagConstraints(1, row, 1, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
			m_topicIdField.setText(m_bean.getTopicId());
			add(m_viewPosts, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 2, bottomSpace, 0), 0, 0));
			row++;
		}
		
		add(m_loginLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
		add(m_login, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
		m_login.setText(m_bean.getUsername());
		row++;
		
		add(m_passwordLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
		add(m_password, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
		m_password.setText(m_bean.getPassword());
		row++;
		
		if (m_bean.supportsSaveGame())
		{
			add(m_includeSaveGame, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			m_includeSaveGame.setSelected(m_bean.getIncludeSaveGame());
			add(m_testForum, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		}
		else
		{
			add(m_testForum, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		}
		
		setupListeners();
	}
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	/**
	 * Configures the listeners for the gui components
	 */
	private void setupListeners()
	{
		
		m_viewPosts.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				((IForumPoster) getBean()).viewPosted();
			}
		});
		
		m_testForum.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				testForum();
			}
		});
		
		// add a document listener which will validate input when the content of any input field is changed
		final DocumentListener docListener = new EditorChangedFiringDocumentListener();
		m_login.getDocument().addDocumentListener(docListener);
		m_password.getDocument().addDocumentListener(docListener);
		m_topicIdField.getDocument().addDocumentListener(docListener);
	}
	
	/**
	 * Tests the Forum poster
	 */
	void testForum()
	{
		final IForumPoster poster = (IForumPoster) getBean();
		final ProgressWindow progressWindow = new ProgressWindow(MainFrame.getInstance(), poster.getTestMessage());
		progressWindow.setVisible(true);
		
		final Runnable runnable = new Runnable()
		{
			public void run()
			{
				
				if (poster.getIncludeSaveGame())
				{
					try
					{
						final File f = File.createTempFile("123", "test");
						f.deleteOnExit();
						
						/* For .txt use this:
						final FileOutputStream fout = new FileOutputStream(f);
						fout.write("Test upload".getBytes());
						fout.close();
						poster.addSaveGame(f, "test.txt");
						*/

						// For .jpg use this:
						final BufferedImage image = new BufferedImage(130, 40, BufferedImage.TYPE_INT_RGB);
						final Graphics g = image.getGraphics();
						g.drawString("Testing file upload", 10, 20);
						try
						{
							ImageIO.write(image, "jpg", f);
						} catch (final IOException e)
						{
							// ignore
						}
						
						poster.addSaveGame(f, "Test.jpg");
					} catch (final IOException e)
					{
						// ignore
					}
					
				}
				poster.postTurnSummary("Test summary from TripleA " + new SimpleDateFormat("HH:mm:ss").format(new Date()), "Testing Forum poster");
				progressWindow.setVisible(false);
				
				// now that we have a result, marshall it back unto the swing thread
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						try
						{
							JOptionPane.showMessageDialog(MainFrame.getInstance(), m_bean.getTurnSummaryRef(), "Test Turn Summary Post", JOptionPane.INFORMATION_MESSAGE);
						} catch (final HeadlessException e)
						{
							// should never happen in a GUI app
						}
					}
				});
			}
		};
		// start a background thread
		final Thread t = new Thread(runnable);
		t.start();
	}
	
	@Override
	public boolean isBeanValid()
	{
		if (m_bean instanceof NullForumPoster)
		{
			return true;
		}
		final boolean loginValid = validateTextFieldNotEmpty(m_login, m_loginLabel);
		final boolean passwordValid = validateTextFieldNotEmpty(m_password, m_passwordLabel);
		
		boolean idValid = true;
		if (m_bean.getCanViewPosted())
		{
			idValid = validateTextFieldNotEmpty(m_topicIdField, m_topicIdLabel);
			m_viewPosts.setEnabled(idValid);
		}
		else
		{
			m_topicIdLabel.setForeground(m_labelColor);
			m_viewPosts.setEnabled(false);
		}
		
		final boolean allValid = loginValid && passwordValid && idValid;
		m_testForum.setEnabled(allValid);
		return allValid;
	}
	
	@Override
	public IBean getBean()
	{
		m_bean.setTopicId(m_topicIdField.getText());
		m_bean.setUsername(m_login.getText());
		m_bean.setPassword(m_password.getText());
		m_bean.setIncludeSaveGame(m_includeSaveGame.isSelected());
		return m_bean;
	}
}
