package moa.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by me on 6/1/15.
 */
abstract public class MOATabPanel extends AbstractTabPanel {

    public MOATabPanel() {
        super(new BorderLayout());

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JSplitPane js = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
                add(js, BorderLayout.CENTER);

                js.add(getOptionsPanel());
                js.add(getContentPanel());

            }
        });

    }

    abstract JComponent getContentPanel();

    abstract JComponent getOptionsPanel();

}
