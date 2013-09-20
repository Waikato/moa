/*
 * Copyright 2007 University of Waikato.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.  
 */

package com.github.javacliparser;

import java.io.File;

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

    //@Override
    //public JComponent getEditComponent() {
    //    return new FileOptionEditComponent(this);
    //}
}
