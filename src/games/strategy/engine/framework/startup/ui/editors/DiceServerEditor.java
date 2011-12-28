package games.strategy.engine.framework.startup.ui.editors;

import games.strategy.engine.framework.startup.ui.editors.validators.EmailValidator;
import games.strategy.engine.random.IRemoteDiceServer;
import games.strategy.engine.random.PBEMDiceRoller;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * An class for editing a Dice Server bean
 *
 * @author Klaus Groenbaek
 */
public class DiceServerEditor extends EditorPanel
{
	//-----------------------------------------------------------------------
	// instance fields 
	//-----------------------------------------------------------------------

	private final JButton m_testDiceyButton = new JButton("Test Server");
	private final JTextField m_toAddress = new JTextField();
	private final JTextField m_ccAddress = new JTextField();
	private final JTextField m_gameId = new JTextField();

	private JLabel m_toLabel = new JLabel("To:");
	private JLabel m_ccLabel = new JLabel("Cc:");
	private JLabel m_gameIdLabel = new JLabel("Game ID:");
	private IRemoteDiceServer m_bean;
	//-----------------------------------------------------------------------
	// constructors 
	//-----------------------------------------------------------------------

	/**
	 * Creating a new instance
	 * @param diceServer the DiceServer bean to edit
	 */
	public DiceServerEditor(IRemoteDiceServer diceServer)
	{
		m_bean = diceServer;

		int bottomSpace = 1;
		int labelSpace = 2;
		int row = 0;
		JTextArea instructionText = new JTextArea();
		instructionText.setLineWrap(true);
		instructionText.setWrapStyleWord(true);
		add(instructionText, new GridBagConstraints(0, row, 3, 1, 1.0d, 0.2d, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
		String text = m_bean.getInfoText();
		if (text == null)
		{
			text = "The author of the properties file should set infotext property";
		}
		instructionText.setText(text);
		instructionText.setBackground(getBackground());

		row++;
		if (m_bean.sendsEmail())
		{

			add(m_toLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
			add(m_toAddress, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
			m_toAddress.setText(m_bean.getToAddress());
			row++;

			add(m_ccLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
			add(m_ccAddress, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
			m_ccAddress.setText(m_bean.getCcAddress());
			row++;
		}
		if (m_bean.supportsGameId())
		{
			add(m_gameIdLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
			add(m_gameId, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
			m_gameId.setText(m_bean.getGameId());
			row++;
		}
		add(m_testDiceyButton, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));

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

		m_testDiceyButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				final PBEMDiceRoller random = new PBEMDiceRoller(getDiceServer(), null);
				random.test();
			}
		});

		final DocumentListener docListener = new EditorChangedFiringDocumentListener();
		m_toAddress.getDocument().addDocumentListener(docListener);
		m_ccAddress.getDocument().addDocumentListener(docListener);
	}

	public boolean isBeanValid()
	{
		boolean toValid = true;
		boolean ccValid = true;

		if (getDiceServer().sendsEmail())
		{
			toValid = validateTextField(m_toAddress, m_toLabel, new EmailValidator(false));
			ccValid = validateTextField(m_ccAddress, m_ccLabel, new EmailValidator(true));
		}

		boolean allValid = toValid && ccValid;
		m_testDiceyButton.setEnabled(allValid);
		return allValid;
	}

	@Override
	public IBean getBean()
	{
		return getDiceServer();
	}


	/**
	 * Returns the currently configured dice server
	 * @return the dice server
	 */
	private IRemoteDiceServer getDiceServer()
	{
		if (m_bean.sendsEmail())
		{
			m_bean.setCcAddress(m_ccAddress.getText());
			m_bean.setToAddress(m_toAddress.getText());
		}
		if (m_bean.supportsGameId())
		{
			m_bean.setGameId(m_gameId.getText());
		}
		return m_bean;
	}

}
