package games.strategy.engine.framework.startup.ui.editors;

import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.engine.pbem.NullForumPoster;
import games.strategy.ui.ProgressWindow;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A class for selecting which Forum poster to use
 *
 * @author Klaus Groenbaek
 */
public class ForumPosterEditor extends EditorPanel
{

	//-----------------------------------------------------------------------
	// instance fields
	//-----------------------------------------------------------------------
	private final JButton m_viewPosts = new JButton("View Forum");
	private final JButton m_testForum = new JButton("Test Post");

	private JLabel m_loginLabel = new JLabel("Login:");
	private JLabel m_passwordLabel = new JLabel("Password:");
	private final JTextField m_login = new JTextField();
	private final JTextField m_password = new JPasswordField();

	private JTextField m_forumIdField = new JTextField();
	private JLabel m_forumIdLabel = new JLabel("Forum Id:");

	private JCheckBox m_includeSaveGame = new JCheckBox("Attach save game to summary");
	private IForumPoster m_bean;
	//-----------------------------------------------------------------------
	// constructors
	//-----------------------------------------------------------------------

	public ForumPosterEditor(IForumPoster bean)
	{
		m_bean = bean;

		int bottomSpace = 1;
		int labelSpace = 2;
		int row = 0;
		if (m_bean.getCanViewPosted())
		{
			add(m_forumIdLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
			add(m_forumIdField, new GridBagConstraints(1, row, 1, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
			m_forumIdField.setText(m_bean.getForumId());
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
		} else
		{
			add(m_testForum, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		}

		setupListeners();
	}

	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------


	/**
	 * Configures the listeners for the gui components
	 */
	private void setupListeners()
	{

		m_viewPosts.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_bean.viewPosted();
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
		DocumentListener docListener = new EditorChangedFiringDocumentListener();
		m_login.getDocument().addDocumentListener(docListener);
		m_password.getDocument().addDocumentListener(docListener);
		m_forumIdField.getDocument().addDocumentListener(docListener);
	}

	/**
	 * Tests the Forum poster
	 */
	void testForum()
	{
		final ProgressWindow progressWindow = new ProgressWindow(MainFrame.getInstance(), "Testing Forum Post...");
		progressWindow.setVisible(true);

		Runnable runnable = new Runnable()
		{
			public void run()
			{
				m_bean.postTurnSummary("Test summary");
				progressWindow.setVisible(false);

				// now that we have a result, marshall it back unto the swing thread
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						try
						{
							JOptionPane.showMessageDialog(MainFrame.getInstance(), m_bean.getTurnSummaryRef(), "Test Turn Summary Post", JOptionPane.INFORMATION_MESSAGE);
						} catch (HeadlessException e)
						{
							// should never happen in a GUI app
						}
					}
				});
			}
		};
		// start a background thread
		Thread t = new Thread(runnable);
		t.start();
	}


	public boolean isBeanValid()
	{
		if (m_bean instanceof NullForumPoster)
		{
			return true;
		}
		boolean loginValid = validateTextFieldNotEmpty(m_login, m_loginLabel);
		boolean passwordValid = validateTextFieldNotEmpty(m_password, m_passwordLabel);

		boolean idValid = true;
		if (m_bean.getCanViewPosted())
		{
			idValid = validateTextFieldNotEmpty(m_forumIdField, m_forumIdLabel);
			m_viewPosts.setEnabled(idValid);
		} else
		{
			m_forumIdLabel.setForeground(m_labelColor);
			m_viewPosts.setEnabled(false);
		}

		boolean allValid = loginValid && passwordValid && idValid;
		m_testForum.setEnabled(allValid);
		return allValid;
	}

	@Override
	public IBean getBean()
	{
		m_bean.setForumId(m_forumIdField.getText());
		m_bean.setUsername(m_login.getText());
		m_bean.setPassword(m_password.getText());
		m_bean.setIncludeSaveGame(m_includeSaveGame.isSelected());
		return m_bean;
	}
}
