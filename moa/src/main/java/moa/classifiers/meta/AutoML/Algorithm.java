package moa.classifiers.meta.AutoML;

import com.github.javacliparser.Option;
import com.github.javacliparser.Options;
import com.yahoo.labs.samoa.instances.Attribute;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.options.ClassOption;

import java.util.Arrays;


class ParameterConfiguration {
    public String parameter;
    public Object value;
    public Object[] range;
    public String type;
    public boolean optimise = true;
}

// This class contains the settings of an algorithm (such as name) as well as an
// array of Parameter Settings
class AlgorithmConfiguration {
    public String algorithm;
    public ParameterConfiguration[] parameters;
}

// This contains the general settings (such as the max ensemble size) as well as
// an array of Algorithm Settings
class GeneralConfiguration {
    public int windowSize = 1000;
    public int ensembleSize = 10;
    public int newConfigurations = 4;
    public AlgorithmConfiguration[] algorithms;
    public boolean keepCurrentModel = true;
    public double lambda = 0.05;
    public boolean preventAlgorithmDeath = true;
    public boolean keepGlobalIncumbent = true;
    public boolean keepAlgorithmIncumbents = true;
    public boolean keepInitialConfigurations = true;
    public boolean useTestEnsemble = true;
    public double resetProbability = 0.01;
    public int numberOfCores = 1;
    public boolean performanceMeasureMaximisation = true;
}

public class Algorithm {
    public String algorithm;
    public IParameter[] parameters;
    public AbstractClassifier classifier;
    public Attribute[] attributes;
    public double prediction;
    public double performanceMeasure;
    public boolean preventRemoval;
    public boolean isDefault;

    // copy constructor
    public Algorithm(Algorithm x, double lambda, double reset, boolean keepCurrentModel, int verbose) {

        // make a (mostly) deep copy of the algorithm
        this.algorithm = x.algorithm;
        this.attributes = x.attributes; // this is a reference since we dont manipulate the attributes
        this.parameters = new IParameter[x.parameters.length];
        this.preventRemoval = x.preventRemoval;
        this.isDefault = false;


        for (int i = 0; i < x.parameters.length; i++) {
            this.parameters[i] = x.parameters[i].copy();
            this.parameters[i].sampleNewConfig(lambda, reset, verbose);
        }

        if (keepCurrentModel) {
            try{
                this.classifier = (AbstractClassifier) x.classifier.copy();
            } catch (RuntimeException e){
                if(verbose >= 2){
                    System.out.println("Copy failed for " + x.classifier.getCLICreationString(Classifier.class) + "! Reinitialise instead.");
                }
                this.classifier = x.classifier; // keep the old algorithm for now
                keepCurrentModel = false;
            }
        } else{
            this.classifier = x.classifier;  // keep the old algorithm for now
        }

        adjustAlgorithm(keepCurrentModel, verbose);
    }

    // init constructor
    public Algorithm(AlgorithmConfiguration x) {

        this.algorithm = x.algorithm;
        this.parameters = new IParameter[x.parameters.length];
        this.preventRemoval = false;
        this.isDefault = true;

        this.attributes = new Attribute[x.parameters.length];
        for (int i = 0; i < x.parameters.length; i++) {


            ParameterConfiguration paramConfig = x.parameters[i];
            if (paramConfig.type.equals("numeric") || paramConfig.type.equals("float") || paramConfig.type.equals("real")) {
                NumericalParameter param = new NumericalParameter(paramConfig);
                this.parameters[i] = param;
                this.attributes[i] = new Attribute(param.getParameter());
            } else if (paramConfig.type.equals("integer")) {
                IntegerParameter param = new IntegerParameter(paramConfig);
                this.parameters[i] = param;
                this.attributes[i] = new Attribute(param.getParameter());
            } else if (paramConfig.type.equals("nominal") || paramConfig.type.equals("categorical") || paramConfig.type.equals("factor")) {
                CategoricalParameter param = new CategoricalParameter(paramConfig);
                this.parameters[i] = param;
                this.attributes[i] = new Attribute(param.getParameter(), Arrays.asList(param.getRange()));
            } else if (paramConfig.type.equals("boolean") || paramConfig.type.equals("flag")) {
                BooleanParameter param = new BooleanParameter(paramConfig);
                this.parameters[i] = param;
                this.attributes[i] = new Attribute(param.getParameter(), Arrays.asList(param.getRange()));
            } else if (paramConfig.type.equals("ordinal")) {
                OrdinalParameter param = new OrdinalParameter(paramConfig);
                this.parameters[i] = param;
                this.attributes[i] = new Attribute(param.getParameter());
            } else {
                throw new RuntimeException("Unknown parameter type: '" + paramConfig.type
                        + "'. Available options are 'numeric', 'integer', 'nominal', 'boolean' or 'ordinal'");
            }
        }
        init();
    }

    // initialise a new algorithm using the Command Line Interface (CLI)
    public void init() {
        // construct CLI string from settings, e.g. denstream.WithDBSCAN -e 0.08 -b 0.3
        StringBuilder commandLine = new StringBuilder();
        commandLine.append(this.algorithm); // first the algorithm class
        for (IParameter param : this.parameters) {
            commandLine.append(" ");
            commandLine.append(param.getCLIString());
        }

        // create new classifier from CLI string
        ClassOption opt = new ClassOption("", ' ', "", Classifier.class, commandLine.toString());
        this.classifier = (AbstractClassifier) opt.materializeObject(null, null);
        this.classifier.prepareForUse();
    }

    // sample a new configuration based on the current one
    public void adjustAlgorithm(boolean keepCurrentModel, int verbose) {

        if (keepCurrentModel) {
            // Option 1: keep the old state and just change parameter
            StringBuilder commandLine = new StringBuilder();
            for (IParameter param : this.parameters) {
                commandLine.append(param.getCLIString());
            }

            Options opts = this.classifier.getOptions();
            for (IParameter param : this.parameters) {
                Option opt = opts.getOption(param.getParameter().charAt(0));
                opt.setValueViaCLIString(param.getCLIValueString());
            }

            // these changes do not transfer over directly since all algorithms cache the
            // option values. Therefore we try to adjust the cached values if possible
            try {
//                ((AbstractClassifier) this.classifier).adjustParameters();
                if (verbose >= 2) {
                    System.out.println("Changed: " + this.classifier.getCLICreationString(Classifier.class));
                }
            } catch (UnsupportedOperationException e) {
                if (verbose >= 2) {
                    System.out.println("Cannot change parameters of " + this.algorithm + " on the fly, reset instead.");
                }
                adjustAlgorithm(false, verbose);
            }
        } else{
            // Option 2: reinitialise the entire state
            this.init();
            if (verbose >= 2) {
                System.out.println("Initialise: " + this.classifier.getCLICreationString(Classifier.class));
            }

            if (verbose >= 2) {
                System.out.println("Train with existing classifiers.");
            }

        }
    }

    // returns the parameter values as an array
    public double[] getParamVector(int padding) {
        double[] params = new double[this.parameters.length + padding];
        int pos = 0;
        for (IParameter param : this.parameters) {
            params[pos++] = param.getValue();
        }
        return params;
    }
}