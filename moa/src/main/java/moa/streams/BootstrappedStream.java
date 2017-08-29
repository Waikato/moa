/*
 *    AMRulesMultiLabel.java
 *    Copyright (C) 2014 INESC TEC, Portugal
 *    @author João Duarte
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

package moa.streams;

import java.util.Random;

import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.MiscUtils;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

/**
 * Bootstrapped Stream
 * 
 * @author  Joao Duarte
 * @version $Revision: 1$* 
 * 
 * Bootstraps a data stream using the poisson distribution
 **/

public class BootstrappedStream extends AbstractOptionHandler implements
		InstanceStream, MultiTargetInstanceStream {

	private static final long serialVersionUID = 1L;

    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to filter.", MultiTargetInstanceStream.class,
            MultiTargetArffFileStream.class.getName());
    public IntOption randomSeedOption = new IntOption("randomSeed",'r',"Seed for the random generator",1);
    
    protected MultiTargetInstanceStream originalStream;
    protected int waitingToSend;
    protected Example<Instance> queuedInstance;
    protected Random randomGenerator;
    
	public BootstrappedStream() {
		randomGenerator=new Random();
	}

	@Override
	public InstancesHeader getHeader() {
		InstancesHeader h=null;
		if (originalStream!=null)
			h=originalStream.getHeader();
		return h;
	}

	@Override
	public long estimatedRemainingInstances() {
		if (originalStream!=null)
			return originalStream.estimatedRemainingInstances();
		else
			return 0;
	}

	@Override
	public boolean hasMoreInstances() {
		boolean flag=false;
		if(originalStream!=null){
			if(waitingToSend>0 || originalStream.hasMoreInstances()){
				flag=true;
			}
		}		
		return flag;
	}

	@Override
	public Example<Instance> nextInstance() {

		if(waitingToSend==0){
			do {
				queuedInstance=originalStream.nextInstance();
				waitingToSend= MiscUtils.poisson(1.0, this.randomGenerator);;
			} while (waitingToSend==0);
			
		}
		Example<Instance> instance=new InstanceExample(queuedInstance.getData().copy());
		instance.setWeight(queuedInstance.weight());
		waitingToSend--;
		return instance;
	}

	@Override
	public boolean isRestartable() {
		return true;
	}

	@Override
	public void restart() {
		originalStream=((MultiTargetInstanceStream)getPreparedClassOption(streamOption));
		waitingToSend=0;
		randomGenerator.setSeed(randomSeedOption.getValue());
		queuedInstance=null;
		originalStream.restart();
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor,
			ObjectRepository repository) {
		restart();
	}

}
