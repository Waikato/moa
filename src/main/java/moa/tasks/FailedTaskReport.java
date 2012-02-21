/*
 *    FailedTaskReport.java
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
package moa.tasks;

import java.io.PrintWriter;
import java.io.StringWriter;

import moa.AbstractMOAObject;
import moa.core.StringUtils;

/**
 * Class for reporting a failed task.
 * <code>TaskThread</code> returns this class as final result object when a task fails.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class FailedTaskReport extends AbstractMOAObject {

    private static final long serialVersionUID = 1L;

    protected Throwable failureReason;

    public FailedTaskReport(Throwable failureReason) {
        this.failureReason = failureReason;
    }

    public Throwable getFailureReason() {
        return this.failureReason;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        sb.append("Failure reason: ");
        sb.append(this.failureReason.getMessage());
        StringUtils.appendNewlineIndented(sb, indent, "*** STACK TRACE ***");
        StringWriter stackTraceWriter = new StringWriter();
        this.failureReason.printStackTrace(new PrintWriter(stackTraceWriter));
        sb.append(stackTraceWriter.toString());
    }
}
