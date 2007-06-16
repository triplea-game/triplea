package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;

import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class SetupFrame extends JPanel
{
    private JLabel m_info1;
    private JLabel m_info2;
    private JLabel m_info3;
    private JButton m_outDirButton;
    private JButton m_runButton;
    private JTextField m_outField;
    private JFileChooser m_outChooser;
    private JRadioButton m_currentState;
    private JRadioButton m_originalState;
    private ButtonGroup m_radioButtonGroup;

    private GameData m_data;

    private File m_outDir;

    
    /**
     * @param data
     */
    public SetupFrame(GameData data)
    {
        super(new GridLayout(6, 2));

        m_info1 = new JLabel();
        m_info2 = new JLabel();
        m_info3 = new JLabel();

        m_data = data;

        m_outDirButton = new JButton();
        m_runButton = new JButton();

        m_outField = new JTextField();

        m_outChooser = new JFileChooser();
        m_outChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        m_currentState = new JRadioButton();
        m_originalState = new JRadioButton();
        m_radioButtonGroup = new ButtonGroup();

        m_info1.setText("This utility will export the map's either current");
        m_info2.setText("or beginning state exactly like the boardgame,");
        m_info3.setText("So you will get Setup Charts, Unit Information, etc.");

        m_currentState.setText("Current Position/State");
        m_originalState.setText("Starting Position/State");


        m_radioButtonGroup.add(m_currentState);
        m_radioButtonGroup.add(m_originalState);
        
        m_originalState.setSelected(true);

        m_outDirButton.setText("Choose the Output Directory");
        m_outDirButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                int returnVal = m_outChooser.showOpenDialog(null);

                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    File outDir = m_outChooser.getSelectedFile();
                    m_outField.setText(outDir.getAbsolutePath());
                }
            }
        });

        m_runButton.setText("Generate the Files");
        m_runButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!m_outField.getText().equals(""))
                {
                    m_outDir = new File(m_outField.getText());
                    InitialSetup initialSetup = new InitialSetup();
                    PrintGenerationData.setOutDir(m_outDir);
                    initialSetup.run(m_data, m_originalState
                            .isSelected());
                } else
                {
                    JOptionPane.showMessageDialog(null,
                            "You need to select an Output Directory.",
                            "Select an Output Directory!",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        super.add(m_info1);
        super.add(new Label(""));
        super.add(m_info2);
        super.add(new Label(""));
        super.add(m_info3);
        super.add(new Label(""));
        super.add(m_outField);
        super.add(m_outDirButton);
        super.add(m_originalState);
        super.add(m_currentState);
        super.add(m_runButton);
        super.add(new Label(""));
    }
}
