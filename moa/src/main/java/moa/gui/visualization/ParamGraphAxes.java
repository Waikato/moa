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
		// TODO implement
    	DecimalFormat d = new DecimalFormat("0.00");
    	for (double i = 0.0; i <= 1.0; i += 0.2) {
    		int x = (int) (this.width * i) + X_OFFSET_LEFT;
    		g.drawLine(x, this.height + Y_OFFSET_TOP, x, this.height+Y_OFFSET_TOP + 5);
    		String label = d.format(i);
    		int str_length = g.getFontMetrics().stringWidth(label);
            g.drawString(label, x - str_length/2, this.height + Y_OFFSET_TOP + 18);
    	}
	}

}
