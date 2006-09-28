package games.strategy.triplea.oddsCalculator.ta;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UIContext;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

public class OddsCalculatorDialog extends JDialog
{
    private OddsCalculatorPanel m_panel;
    
    public static void show(final TripleAFrame taFrame, Territory t)
    {
        final OddsCalculatorDialog dialog = new OddsCalculatorDialog(taFrame.getGame().getData(), taFrame.getUIContext(), taFrame, t);
        dialog.pack();
        int maxHeight = 600;
        if(dialog.getHeight() > maxHeight )
            dialog.setSize(new Dimension(dialog.getWidth(), maxHeight));
        
        dialog.addWindowListener(new WindowAdapter()
        {
            public void windowClosed(WindowEvent e)
            {
                if(taFrame != null && taFrame.getUIContext() != null)
                    taFrame.getUIContext().removeShutdownWindow(dialog);
            }                
        });
        
        //close when hitting the escape key
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        Action closeAction = new AbstractAction()
        {
        
            public void actionPerformed(ActionEvent arg0)
            {
                dialog.setVisible(false);
        
            }
        
        };
        dialog.getRootPane().registerKeyboardAction(closeAction, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        dialog.setLocationRelativeTo(taFrame);
        dialog.setVisible(true);
        
        taFrame.getUIContext().addShutdownWindow(dialog);
    }
    
    OddsCalculatorDialog(GameData data, UIContext context, JFrame parent, Territory location)
    {
        super(parent, "Odds Calculator");
        
        m_panel = new OddsCalculatorPanel(data, context, location, this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(m_panel, BorderLayout.CENTER);
        pack();
    }
    
    
}
