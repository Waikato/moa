import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.Classifier;
import moa.core.Example;
import moa.core.SizeOf;
import moa.options.ClassOption;
import moa.streams.ArffFileStream;

import java.util.Locale;

/**
 * Traces the resources of a learner along a stream: every {@code every} instances, prints the number of instances, the
 * cumulative wall time spent in the learner (prediction and training) and the size of the learner (SizeOf agent).
 *
 * <p>Usage: {@code java -javaagent:sizeofag-1.1.0.jar --add-opens ... TraceBenchmark <arff> <dataset> <config>
 * <learner CLI> <seed> <every>}. Prints CSV lines {@code dataset,config,seed,instances,learner_wall_s,mem_bytes}.</p>
 */
public class TraceBenchmark {

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--header")) {
            System.out.println("dataset,config,seed,instances,learner_wall_s,mem_bytes");
            return;
        }
        String arff = args[0], dataset = args[1], config = args[2], learnerCli = args[3];
        int seed = Integer.parseInt(args[4]);
        long every = Long.parseLong(args[5]);

        ArffFileStream stream = new ArffFileStream(arff, -1);
        stream.prepareForUse();
        Classifier learner = (Classifier) ClassOption.cliStringToObject(learnerCli, Classifier.class, null);
        learner.setModelContext(stream.getHeader());
        learner.prepareForUse();
        learner.setRandomSeed(seed);
        learner.resetLearning();

        long learnerNanos = 0, i = 0;
        while (stream.hasMoreInstances()) {
            Example<Instance> example = stream.nextInstance();
            Instance instance = example.getData();
            long t0 = System.nanoTime();
            learner.getVotesForInstance(instance);
            learner.trainOnInstance(instance);
            learnerNanos += System.nanoTime() - t0;
            i++;
            if (i % every == 0 || !stream.hasMoreInstances())
                System.out.println(String.format(Locale.ROOT, "%s,%s,%d,%d,%.6f,%d",
                        dataset, config, seed, i, learnerNanos / 1e9, SizeOf.fullSizeOf(learner)));
        }
    }
}
