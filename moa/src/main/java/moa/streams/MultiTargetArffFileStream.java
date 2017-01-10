/*
 *    ArffFileStream.java
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
package moa.streams;

/**
 * Stream reader of ARFF files.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class MultiTargetArffFileStream extends ArffFileStream implements
        MultiTargetInstanceStream {

	// deprecated
	
//    @Override
//    public String getPurposeString() {
//        return "A stream read from an ARFF file.";
//    }
//
//    private static final long serialVersionUID = 1L;
//
//    public FileOption arffFileOption = new FileOption("arffFile", 'f',
//            "ARFF file to load.", null, "arff", false);
//
//    public RangeOption outputAttributesOption = new RangeOption(
//            "outputAttributes",
//            'c',
//            "Output attributes:  n for first n attributes,  -n for last n attributes  For example, 2 for the two first attributes, -2 for the two last attributes.",
//			"-1");
//
//    protected Instances instances;
//
//    protected Reader fileReader;
//
//    protected boolean hitEndOfFile;
//
//    protected InstanceExample lastInstanceRead;
//
//    protected int numInstancesRead;
//
//    protected InputStreamProgressMonitor fileProgressMonitor;
//
//    public MultiTargetArffFileStream() {
//    }
//
//	// jesse
//    public MultiTargetArffFileStream(String arffFileName, String classIndices) {
//		this.arffFileOption.setValue(arffFileName);
//		this.outputAttributesOption.setValue(classIndices);
//		restart();
//    }
//
//	/*
//    public ArffFileStream(String arffFileName, int classIndex) {
//        this.arffFileOption.setValue(arffFileName);
//        this.outputAttributesOption.setValue(classIndex);
//        restart();
//    }
//	*/
//
//    @Override
//    public void prepareForUseImpl(TaskMonitor monitor,
//            ObjectRepository repository) {
//        restart();
//    }
//
//    @Override
//    public InstancesHeader getHeader() {
//        return new InstancesHeader(this.instances);
//    }
//
//    @Override
//    public long estimatedRemainingInstances() {
//        double progressFraction = this.fileProgressMonitor.getProgressFraction();
//        if ((progressFraction > 0.0) && (this.numInstancesRead > 0)) {
//            return (long) ((this.numInstancesRead / progressFraction) - this.numInstancesRead);
//        }
//        return -1;
//    }
//
//    @Override
//    public boolean hasMoreInstances() {
//        return !this.hitEndOfFile;
//    }
//
//    @Override
//    public InstanceExample nextInstance() {
//        InstanceExample prevInstance = this.lastInstanceRead;
//        this.hitEndOfFile = !readNextInstanceFromFile();
//        return prevInstance;
//    }
//
//    @Override
//    public boolean isRestartable() {
//        return true;
//    }
//
//    @Override
//    public void restart() {
//        try {
//            if (this.fileReader != null) {
//                this.fileReader.close();
//            }
//            InputStream fileStream = new FileInputStream(this.arffFileOption.getFile());
//            this.fileProgressMonitor = new InputStreamProgressMonitor(
//                    fileStream);
//            this.fileReader = new BufferedReader(new InputStreamReader(
//                    this.fileProgressMonitor));
//			// jesse -----
//			/*int cs[] = this.outputAttributesOption.getRange(); 
//			if (cs.length == 1) {
//				// single label
//				int c = cs[0]; 
//				this.instances = new Instances(this.fileReader, 1, c);
//				if (c < 0) {
//					this.instances.setClassIndex(this.instances.numAttributes() - 1);
//				} else if (c > 0) {
//					this.instances.setClassIndex(c - 1);
//				}
//			}
//			else {*/
//				// multi-label since cs[] contains _multipe_ class indices, e.g., cs[] = {9,10,11} 
//				// what to do here?
//			this.instances = new Instances(this.fileReader, this.outputAttributesOption.getRange());
//			//}
//            this.numInstancesRead = 0;
//            this.lastInstanceRead = null;
//            this.hitEndOfFile = !readNextInstanceFromFile();
//        } catch (IOException ioe) {
//            throw new RuntimeException("ArffFileStream restart failed.", ioe);
//        }
//    }
//
//    protected boolean readNextInstanceFromFile() {
//        try {
//            if (this.instances.readInstance(this.fileReader)) {
//                this.lastInstanceRead = new InstanceExample(this.instances.instance(0));
//                this.instances.delete(); // keep instances clean
//                this.numInstancesRead++;
//                return true;
//            }
//            if (this.fileReader != null) {
//                this.fileReader.close();
//                this.fileReader = null;
//            }
//            return false;
//        } catch (IOException ioe) {
//            throw new RuntimeException(
//                    "ArffFileStream failed to read instance from stream.", ioe);
//        }
//    }
//
//    @Override
//    public void getDescription(StringBuilder sb, int indent) {
//        // TODO Auto-generated method stub
//    }
}
