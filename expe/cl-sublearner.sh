#####################################
#                                   #
# CLUSTER-AND-LABEL WITH SUBLEARNER #
#                                   # 
#####################################

rootfolder=/mnt/d/Study/M2_DK/Internship
folder="cl-sublearner"
m=100
h=1000

for d in agrawal led randomTree randomRBF hyperplane elecNormNew covtypeNorm airlines;
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
		evaluator="BasicClassificationPerformanceEvaluator -o -p -r -f"
		learner_sp="trees.HoeffdingTree"
		clusterer="semisupervised.ClustreamSSL -h $h -l 0.0 -m $m -k 5 -a 1"
		global_learner="trees.HoeffdingTree"
		local_learner="bayes.NaiveBayes"
		stream="ArffFileStream -f $data"

		# define the task
		echo
		echo "=================================================================================="
		echo "CLUSTER-N-LABEL & MULTIVIEW: dataset = $d, r = $percent"
		echo "=================================================================================="
		echo
		learner_ssl="semisupervised.ClusterAndLabelSubLearnerClassifier -c ($clusterer) -p -l ($local_learner) -g ($global_learner) -t 1"
		result="$rootfolder/experiments/$folder/$d-cl-$percent-multiview.csv"
		task="EvaluateInterleavedTestThenTrainSemi -b ($learner_sp) -l ($learner_ssl) -s ($stream) -d ($result) -i -1 -f 1000 -q 1000"
		java -classpath /mnt/d/Study/M2_DK/Internship/work/moa/moa/target/classes moa.DoTask $task

		echo
		echo "=================================================================================="
		echo "CLUSTER-N-LABEL STANDARD: dataset = $d, r = $percent"
		echo "=================================================================================="
		echo
		learner_ssl="semisupervised.ClusterAndLabelClassifier -c ($clusterer) -p -k 1"
		result="$rootfolder/experiments/$folder/$d-cl-$percent-standard.csv"
		task="EvaluateInterleavedTestThenTrainSemi -b ($learner_sp) -l ($learner_ssl) -s ($stream) -d ($result) -i -1 -f 1000 -q 1000"
		java -classpath /mnt/d/Study/M2_DK/Internship/work/moa/moa/target/classes moa.DoTask $task

		echo
		echo "=================================================================================="
		echo "FULLY SUPERVISED: dataset = $d, r = $percent"
		echo "=================================================================================="
		echo
		learner_ssl="semisupervised.ClusterAndLabelSubLearnerClassifier -c ($clusterer) -p -l ($local_learner) -g ($global_learner) -t 1"
		result="$rootfolder/experiments/$folder/$d-cl-$percent-supervised.csv"
		task="EvaluateInterleavedTestThenTrainSemi -a -b ($learner_sp) -l ($learner_ssl) -s ($stream) -d ($result) -i -1 -f 1000 -q 1000"
		java -classpath /mnt/d/Study/M2_DK/Internship/work/moa/moa/target/classes moa.DoTask $task
	done
done