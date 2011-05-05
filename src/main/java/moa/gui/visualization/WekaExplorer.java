package moa.gui.visualization;

import weka.gui.explorer.Explorer;
import weka.core.Memory;
//import weka.gui.LookAndFeel;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import weka.core.Instances;


/**
 *
 * @author jansen
 *
 * Copy of main() from weka.gui.Explorer to start the Explorer with the
 * processed data already loaded
 *
 */
public class WekaExplorer {

    private static Explorer m_explorer;
    /** for monitoring the Memory consumption */
    private static Memory m_Memory = new Memory(true);

    public WekaExplorer(Instances instances) {
        //weka.core.logging.Logger.log(weka.core.logging.Logger.Level.INFO, "Logging started");
        try {
            javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {}

        try {
            // uncomment to disable the memory management:
            //m_Memory.setEnabled(false);

            m_explorer = new Explorer();
            final JFrame jf = new JFrame("Weka Explorer");
            jf.getContentPane().setLayout(new BorderLayout());
            jf.getContentPane().add(m_explorer, BorderLayout.CENTER);
            jf.addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    jf.dispose();
                }
            });
            jf.pack();
            jf.setSize(800, 600);
            jf.setVisible(true);
            Image icon = Toolkit.getDefaultToolkit().
                    getImage(ClassLoader.getSystemResource("weka/gui/weka_icon.gif"));
            jf.setIconImage(icon);

            if(instances !=null){
                m_explorer.getPreprocessPanel().setInstances(instances);
            }

            Thread memMonitor = new Thread() {

                public void run() {
                    while (true) {
                        try {
                            //System.out.println("Before sleeping.");
                            this.sleep(4000);

                            System.gc();

                            if (m_Memory.isOutOfMemory()) {
                                // clean up
                                jf.dispose();
                                m_explorer = null;
                                System.gc();

                                // stop threads
                                m_Memory.stopThreads();

                                // display error
                                System.err.println("\ndisplayed message:");
                                m_Memory.showOutOfMemory();
                                System.err.println("\nexiting");
                                System.exit(-1);
                            }

                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            };

            memMonitor.setPriority(Thread.MAX_PRIORITY);
            memMonitor.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println(ex.getMessage());
        }
    }
}
