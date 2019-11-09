########################################################
#                                                      #
# CLUSTER-AND-LABEL WITH DIFFERENT CATEGORICAL METRICS #
#                                                      # 
########################################################

rootfolder=/mnt/d/Study/M2_DK/Internship
folder="cl-categorical"
m=100
h=1000
evaluator="BasicClassificationPerformanceEvaluator -o -p -r -f"
learner_sp="trees.HoeffdingTree"
global_learner="trees.HoeffdingTree"
local_learner="bayes.NaiveBayes"

for d in randomTree elecNormNew airlines;
# for d in agrawal;
do
	for ratio in 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 0.91 0.92 0.93 0.94 0.95 0.96 0.97 0.98 0.99;
	do
		# compute the percentage of unlabeled data
		frac=`echo $ratio | cut -d. -f2`
		len=${#frac}
		if [[ $len == 1 ]]; then
			((percent=frac*10))
		else
			percent=$frac
		fi

		# define necessary variables
		data="$rootfolder/data/semi-masked/$d-semi-$percent.arff"
		stream="ArffFileStream -f $data"

		# define the task
		for method in 0 1 2 3 4 5;
		do
			echo
			echo "==============================================================================================="
			echo "CLUSTER-N-LABEL & MULTIVIEW & CATEGORICAL METRICS: dataset = $d, r = $percent, method = $method"
			echo "==============================================================================================="
			echo

			if [[ $method == 0 ]]; then
				method_name="nothing"
			elif [[ $method == 1 ]]; then
				method_name="euclidean"
			elif [[ $method == 2 ]]; then
				method_name="of"
			elif [[ $method == 3 ]]; then
				method_name="lin"
			elif [[ $method == 4 ]]; then
				method_name="goodall3"
			elif [[ $method == 5 ]]; then
				method_name="iof"
			fi

			clusterer="semisupervised.ClustreamSSL -h $h -l 0.0 -m $m -k 5 -a $method"
			learner_ssl="semisupervised.ClusterAndLabelSubLearnerClassifier -c ($clusterer) -p -l ($local_learner) -g ($global_learner) -t 1"
			result="$rootfolder/experiments/$folder/$d-cl-$percent-$method_name.csv"
			task="EvaluateInterleavedTestThenTrainSemi -b ($learner_sp) -l ($learner_ssl) -s ($stream) -d ($result) -i -1 -f 1000 -q 1000"
			java -classpath /mnt/d/Study/M2_DK/Internship/work/moa/moa/target/classes moa.DoTask $task
		done
	done
done