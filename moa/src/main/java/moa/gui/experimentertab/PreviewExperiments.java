/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.gui.experimentertab;

import javax.swing.JFrame;

/**
 *
 * @author Alberto
 */
public class PreviewExperiments extends JFrame{

    private ExpPreviewPanel previewPanel;

    public PreviewExperiments(ExpPreviewPanel previewPanel) {
        this.previewPanel = previewPanel;
        
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        
        setContentPane(this.previewPanel);

        // Display the window.
        pack();
        setSize(700, 500);
       
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b); //To change body of generated methods, choose Tools | Templates.
        this.repaint();
    }
    
    
    
       
}
