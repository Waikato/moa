/*
 *    ParamGraphAxes.java
 *    Copyright (C) Copyright (C) 2017 Otto-von-Guericke-University, Magdeburg, Germany
 *    @author Tim Sabsch (tim.sabsch@ovgu.de)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */
package moa.gui.visualization;

import java.awt.Color;
import java.awt.Graphics;
import java.text.DecimalFormat;

/**
 * ParamGraphAxes is an implementation of AbstractGraphAxes, drawing x labels
 * based on a parameter.
 * 
 * @author Tim Sabsch (tim.sabsch@ovgu.de)
 * @version $Revision: 1 $
 * @see AbstractGraphAxes
 */
public class ParamGraphAxes extends AbstractGraphAxes {

    private static final long serialVersionUID = 1L;

    @Override
    protected void drawXLabels(Graphics g) {
        g.setColor(Color.BLACK);

        // x-axis markers + labels
        DecimalFormat d = new DecimalFormat("0.00");

        double numLabels = Math.min(Math.pow(2, (int) x_resolution), 32);
        /*
         * technically, this is numLabels-1, but as we're iterating 0 <= i <=
         * numLabels, we need the extra label. Also don't draw more than 32
         * labels.
         */

        for (int i = 0; i <= numLabels; i++) {
            double fraction = i / numLabels;
            double value = lower_x_value + fraction * (upper_x_value - lower_x_value);
            String label = d.format(value);
            int str_length = g.getFontMetrics().stringWidth(label);

            g.drawString(label,
                    (int) (fraction * width) + X_OFFSET_LEFT - str_length / 2,
                    height + Y_OFFSET_TOP + 18);
            g.drawLine((int) (fraction * width) + X_OFFSET_LEFT,
                    height + Y_OFFSET_TOP,
                    (int) (fraction * width) + X_OFFSET_LEFT,
                    height + Y_OFFSET_TOP + 5);
        }
    }

}
