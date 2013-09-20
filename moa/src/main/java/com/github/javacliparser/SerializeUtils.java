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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class implementing some serialize utility methods.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class SerializeUtils {

    public static class ByteCountingOutputStream extends OutputStream {

        protected int numBytesWritten = 0;

        public int getNumBytesWritten() {
            return this.numBytesWritten;
        }

        @Override
        public void write(int b) throws IOException {
            this.numBytesWritten++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.numBytesWritten += len;
        }

        @Override
        public void write(byte[] b) throws IOException {
            this.numBytesWritten += b.length;
        }
    }

    public static void writeToFile(File file, Serializable obj)
            throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(
                new BufferedOutputStream(new FileOutputStream(file))));
        out.writeObject(obj);
        out.flush();
        out.close();
    }

    public static Object readFromFile(File file) throws IOException,
            ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(
                new BufferedInputStream(new FileInputStream(file))));
        Object obj = in.readObject();
        in.close();
        return obj;
    }

    public static Object copyObject(Serializable obj) throws Exception {
        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(baoStream));
        out.writeObject(obj);
        out.flush();
        out.close();
        byte[] byteArray = baoStream.toByteArray();
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(
                new ByteArrayInputStream(byteArray)));
        Object copy = in.readObject();
        in.close();
        return copy;
    }

    public static int measureObjectByteSize(Serializable obj) throws Exception {
        ByteCountingOutputStream bcoStream = new ByteCountingOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(bcoStream));
        out.writeObject(obj);
        out.flush();
        out.close();
        return bcoStream.getNumBytesWritten();
    }
}
