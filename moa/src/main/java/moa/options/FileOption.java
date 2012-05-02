/*
 *    FileOption.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.options;

import java.io.File;

import javax.swing.JComponent;

import moa.gui.FileOptionEditComponent;

/**
 * File option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class FileOption extends StringOption {

    private static final long serialVersionUID = 1L;

    protected String defaultFileExtension;

    protected boolean isOutputFile;

    public FileOption(String name, char cliChar, String purpose,
            String defaultFileName, String defaultExtension, boolean isOutput) {
        super(name, cliChar, purpose, defaultFileName);
        this.defaultFileExtension = defaultExtension;
        this.isOutputFile = isOutput;
    }

    public String getDefaultFileExtension() {
        return this.defaultFileExtension;
    }

    public boolean isOutputFile() {
        return this.isOutputFile;
    }

    public File getFile() {
        if ((getValue() != null) && (getValue().length() > 0)) {
            return new File(getValue());
        }
        return null;
    }

    @Override
    public JComponent getEditComponent() {
        return new FileOptionEditComponent(this);
    }
}
