/*
 * Created on Feb 18, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package games.strategy.triplea.ui;

import games.strategy.engine.framework.RandomDestination;
import games.strategy.engine.framework.VerifiedRandomNumbers;
import games.strategy.triplea.formatter.Formatter;

import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.util.List;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author sgb
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
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
        List verified = RandomDestination.getVerifiedRandomNumbers();
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
            tableValues[i][1] = Formatter.asDice(number.getValues());
        }
        return tableValues;
    }

    
    
    
    
}
