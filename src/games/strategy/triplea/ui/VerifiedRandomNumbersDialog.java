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
package games.strategy.triplea.ui;

import games.strategy.engine.framework.VerifiedRandomNumbers;
import games.strategy.engine.random.RemoteRandom;
import games.strategy.triplea.formatter.MyFormatter;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * @author Sean Bridges
 */
public class VerifiedRandomNumbersDialog extends JDialog
{
    public VerifiedRandomNumbersDialog(Component parent)
    {
        super(JOptionPane.getFrameForComponent(parent), "Verified Random Numbers", true);
        init();
        pack();
    }
    
    private void init()
    {
        List verified = RemoteRandom.getVerifiedRandomNumbers();
        String[][] tableValues = getTableValues(verified);
        
        DefaultTableModel model = new DefaultTableModel(tableValues, new String[] {"Reason","Dice Rolls"});
        
        JTable table = new JTable(model);
                
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.CENTER));
        getContentPane().add(buttons, BorderLayout.SOUTH);
        
        JButton close = new JButton("Close");
        close.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        setVisible(false);
                    }
                }	
       );
        
        buttons.add(close);
    }

    /**
     * @param verified
     * @return
     */
    private String[][] getTableValues(List verified)
    {
        if(verified.isEmpty())
            return new String[][] {{"",""}};
        
        String[][] tableValues = new String[verified.size()][2] ;
        for(int i = 0; i < verified.size(); i++)
        {
            VerifiedRandomNumbers number = (VerifiedRandomNumbers) verified.get(i);
            tableValues[i][0]= number.getAnnotation();
            tableValues[i][1] = MyFormatter.asDice(number.getValues());
        }
        return tableValues;
    }

    
    
    
    
}
