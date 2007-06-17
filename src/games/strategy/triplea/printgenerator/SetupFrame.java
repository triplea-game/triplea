/*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;

import java.awt.BorderLayout;
import java.awt.GridLayout;
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
    private JPanel m_infoPanel;
    private JPanel m_textButtonRadioPanel;
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
        super(new BorderLayout());

        m_info1 = new JLabel();
        m_info2 = new JLabel();
        m_info3 = new JLabel();

        m_data = data;

        m_outDirButton = new JButton();
        m_runButton = new JButton();

        m_outField = new JTextField(15);

        m_outChooser = new JFileChooser();
        m_outChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        m_currentState = new JRadioButton();
        m_originalState = new JRadioButton();
        m_radioButtonGroup = new ButtonGroup();

        m_info1.setText("This utility will export the map's either current or ");
        m_info2.setText("beginning state exactly like the boardgame, so you ");
        m_info3.setText("will get Setup Charts, Unit Information, etc.");

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
                    PrintGenerationData printData=new PrintGenerationData();
                    printData.setOutDir(m_outDir);
                    printData.setData(m_data);
                    new InitialSetup().run(printData, m_originalState
                            .isSelected());
                    JOptionPane.showMessageDialog(null, "Done!", "Done!",
                            JOptionPane.INFORMATION_MESSAGE);
                } else
                {
                    JOptionPane.showMessageDialog(null,
                            "You need to select an Output Directory.",
                            "Select an Output Directory!",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        m_infoPanel=new JPanel(new GridLayout(3,1));
        m_textButtonRadioPanel=new JPanel(new BorderLayout());

        m_infoPanel.add(m_info1);
        m_infoPanel.add(m_info2);
        m_infoPanel.add(m_info3);
        super.add(m_infoPanel, BorderLayout.NORTH);
        
       
        m_textButtonRadioPanel.add(m_outField, BorderLayout.WEST);
        m_textButtonRadioPanel.add(m_outDirButton, BorderLayout.EAST);
        JPanel panel=new JPanel(new GridLayout(1,2));
        panel.add(m_originalState);
        panel.add(m_currentState);
        m_textButtonRadioPanel.add(panel, BorderLayout.SOUTH);
        super.add(m_textButtonRadioPanel, BorderLayout.CENTER);
        super.add(m_runButton, BorderLayout.SOUTH);
        

    }
}
