/*
 *    InputStreamProgressMonitor.java
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
package moa.core;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class for monitoring the progress of reading an input stream.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class InputStreamProgressMonitor extends FilterInputStream {

	/** The number of bytes to read in total */
	protected int inputByteSize;

	/** The number of bytes read so far */
	protected int inputBytesRead;

	public InputStreamProgressMonitor(InputStream in) {
		super(in);
		try {
			this.inputByteSize = in.available();
		} catch (IOException ioe) {
			this.inputByteSize = 0;
		}
		this.inputBytesRead = 0;
	}

	public int getBytesRead() {
		return this.inputBytesRead;
	}

	public int getBytesRemaining() {
		return this.inputByteSize - this.inputBytesRead;
	}

	public double getProgressFraction() {
		return ((double) this.inputBytesRead / (double) this.inputByteSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		int c = this.in.read();
		if (c > 0) {
			this.inputBytesRead++;
		}
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException {
		int numread = this.in.read(b);
		if (numread > 0) {
			this.inputBytesRead += numread;
		}
		return numread;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int numread = this.in.read(b, off, len);
		if (numread > 0) {
			this.inputBytesRead += numread;
		}
		return numread;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#skip(long)
	 */
	@Override
	public long skip(long n) throws IOException {
		long numskip = this.in.skip(n);
		if (numskip > 0) {
			this.inputBytesRead += numskip;
		}
		return numskip;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilterInputStream#reset()
	 */
	@Override
	public synchronized void reset() throws IOException {
		this.in.reset();
		this.inputBytesRead = this.inputByteSize - this.in.available();
	}

}
