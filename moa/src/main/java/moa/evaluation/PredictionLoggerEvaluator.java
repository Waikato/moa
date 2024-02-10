package moa.evaluation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Prediction;

import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.core.Example;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.Utils;
import moa.options.AbstractOptionHandler;
import moa.options.ClassOption;
import moa.tasks.TaskMonitor;

public class PredictionLoggerEvaluator extends AbstractOptionHandler
        implements ClassificationPerformanceEvaluator {

    private static final long serialVersionUID = 1L;

    private OutputStreamWriter writer;
    private int index = 0;

    public FileOption outputPredictionFileOption = new FileOption("output", 'o',
            "A file to write comma separated predictions to.", null, "csv.gzip", true);

    public FlagOption overwrite = new FlagOption("overwrite", 'f', "Overwrite existing file.");

    public ClassOption wrappedEvaluatorOption = new ClassOption("evaluator", 'e',
            "Classification performance evaluation method.", ClassificationPerformanceEvaluator.class,
            "BasicClassificationPerformanceEvaluator");

    public FlagOption probabilities = new FlagOption("probabilities", 'p',
            "Log probabilities instead of raw predictions.");

    public FlagOption uncompressed = new FlagOption("uncompressed", 'u',
            "The output file should be saved uncompressed.");

    private ClassificationPerformanceEvaluator wrappedEvaluator;

    @Override
    public String getPurposeString() {
        return "Log raw predictions and probabilities to a CSV file, and evaluate using a wrapped evaluator.";
    }

    @Override
    public void addResult(Example<Instance> example, double[] classVotes) {
        Instance instance = example.getData();
        int predictedClass = Utils.maxIndex(classVotes);
        double normalizingFactor = Arrays.stream(classVotes).sum();
        int numClasses = instance.numClasses();

        if (normalizingFactor == 0) {
            normalizingFactor = 1;
        }
        try {
            // If this is the first result, write the header to the top of the file
            if (index == 0)
                writeHeader(numClasses);
            
            
            // Add row to CSV file
            if (instance.classIsMissing() == true)
            {
                writer.write(String.format("?,%d,", predictedClass));
            }
            else
            {
                int trueClass = (int) instance.classValue();
                writer.write(String.format("%d,%d,", trueClass, predictedClass));
            }
            
            if (probabilities.isSet()) {
                for (int i = 0; i < numClasses; i++) {
                    double probability = 0.0;
                    if (i < classVotes.length){
                        probability = classVotes[i] / normalizingFactor;
                    }
                    writer.write(String.format("%.2f,", probability));
                }
            }

            writer.write("\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Pass result to wrapped evaluator
        wrappedEvaluator.addResult(example, classVotes);
        index ++;
    }

    @Override
    public void addResult(Example<Instance> testInst, Prediction prediction) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        wrappedEvaluator = (ClassificationPerformanceEvaluator) getPreparedClassOption(wrappedEvaluatorOption);
        try {
            File file = outputPredictionFileOption.getFile();
            if (file.exists() && !overwrite.isSet()) {
                throw new RuntimeException(
                        "File already exists: " + file.getAbsolutePath()
                                + ". MOA doesn't want to overwrite it.");
            }
            if (uncompressed.isSet())
                writer = new OutputStreamWriter(new FileOutputStream(file));
            else
                writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file)));            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeHeader(int numClasses) throws IOException {
        writer.write("true_class,class_prediction,");
        if (probabilities.isSet()) {
            for (int i = 0; i < numClasses; i++) {
                writer.write(String.format("class_probability_%d,", i));
            }
        }
        writer.write("\n");
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }

    @Override
    public void reset() {
        wrappedEvaluator.reset();
    }

    @Override
    public Measurement[] getPerformanceMeasurements() {
        return wrappedEvaluator.getPerformanceMeasurements();
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        sb.append(getPurposeString());
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
