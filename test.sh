#######################################
## CLASSPATH CONFIGURATION
#######################################

# Configure where to look for the MEKA and MOA jars.
MOA="$PWD/moa/target/moa-2012.09-SNAPSHOT.jar"
MEKA="$PWD/meka-1.9.1-SNAPSHOT.jar"

# Configure other libraries. NOTE, if you are using an IDE with Maven integration, these should be taken care of automatically
PENT="$HOME/.m2/repository/org/pentaho/pentaho-commons/pentaho-package-manager/1.0.11/pentaho-package-manager-1.0.11.jar"
WEKA="$PENT:$HOME/.m2/repository/nz/ac/waikato/cms/weka/weka-dev/3.7.13/weka-dev-3.7.13.jar"

#######################################
## DATASET CONFIGURATION
#######################################

DATA_DIR=$PWD
#DATA_SET="IMDB-F.arff"
#L=28
DATA_SET="A-TMC7-REDU-X2-500.arff"
L=22

# How many examples we want to consider
N=28596
N=5000

# How many windows we want
f=$[N/20]		


#######################################
## CLASSIFIER CONFIGURATION
#######################################

W="weka.classifiers.meta.MOA -- -B moa.classifiers.trees.HoeffdingTree"
W="weka.classifiers.trees.HoeffdingTree"
W_W=$W
W_NB="weka.classifiers.bayes.NaiveBayesUpdateable"



#######################################
## SINGLE LABEL 
## (just check that moa is working)
#######################################

#java -cp $MOA moa.DoTask "EvaluatePrequential -f 1000 -l moa.classifiers.bayes.NaiveBayes -s (ArffFileStream -f $DATA_DIR/$DATA_SET -c 5)"

## MULTI-TARGET
#java -cp $MOA moa.DoTask "EvaluatePrequentialMultiTarget -l multitarget.functions.MultiTargetNoChange -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L)"

#######################################
## MULTI-LABEL
## The command-line option. 
## Use 0 to try methods from the paper.
#######################################

echo "******** BENCHMARKS (EXACT MATCH) ***********************"
echo "           20NG      IMDB     MMILL      OSHU     TMC7"
echo "BRa                  0.050                        0.035  "
echo "PS(1,1)                                                  "
echo "EaBR                 0.050                        0.135  "
echo "EaPS                 0.096                        0.135  "
echo "EaHT/PS              0.026                        0.266  "
echo "*********************************************************"

# GET AN OPTION FROM THE COMMAND LINE
SEL=1
if [ ! -z $1 ]; then
	SEL=$1
fi

if [ $SEL -eq 0 ]; then
	echo "======================================================================="
	echo "0.a) E - BR - HT"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.meta.OzaBagML -l (multilabel.MEKAClassifier -l (meka.classifiers.multilabel.incremental.BRUpdateable -W $W_W))) -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15
	echo "======================================================================="
	echo "0.b) E_a - BR - HT"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.meta.OzaBagAdwinML -l (multilabel.MEKAClassifier -l (meka.classifiers.multilabel.incremental.BRUpdateable -W $W_W))) -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15
	echo "======================================================================="
	echo "0.c) E_a - PS - HT"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.meta.OzaBagAdwinML -l (multilabel.MEKAClassifier -l (meka.classifiers.multilabel.incremental.PSUpdateable -P 1 -I $[f/2] -S 10 -W $W_W))) -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15
	echo "======================================================================="
	echo "0.d) E - HT"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.meta.OzaBagML -l (multilabel.MultilabelHoeffdingTree -a (multilabel.MajorityLabelset))) -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N"   | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15
	echo "======================================================================="
	echo "0.e) E_a - HT - PS - NB"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.meta.OzaBagAdwinML -l (multilabel.MultilabelHoeffdingTree -a (multilabel.MEKAClassifier -l (meka.classifiers.multilabel.incremental.PSUpdateable -P 1 -I $[f/2] -S 10 -W $W_NB)))) -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15
elif [ $SEL -eq 1 ]; then
	echo "======================================================================="
	echo "1.) MajorityLabelset"
	java -cp $MOA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l multilabel.MajorityLabelset -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15
elif [ $SEL -eq 2 ]; then
	echo "======================================================================="
	echo "2.a) MEKAClassifier (BR, default DATA_DIR classifier: HoeffdingTree)"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.MEKAClassifier -l meka.classifiers.multilabel.incremental.BRUpdateable)  -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15

	echo "======================================================================="
	echo "2.b) MEKAClassifier (BR, default DATA_DIR classifier: IBk) -- same as 2.a?"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.MEKAClassifier -l (meka.classifiers.multilabel.incremental.BRUpdateable -W weka.classifiers.lazy.IBk))  -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15

	echo "======================================================================="
	echo "2.c) MEKAClassifier (PS, default DATA_DIR classifier: NaiveBayesUpdateable)"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.MEKAClassifier -l (meka.classifiers.multilabel.incremental.PSUpdateable -W weka.classifiers.bayes.NaiveBayesUpdateable))  -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15

	echo "======================================================================="
	echo "2.d) MEKAClassifier (PS, default base classifier: IBk) -- same as 2.c?"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.MEKAClassifier -l (meka.classifiers.multilabel.incremental.PSUpdateable -I 200 -S 5 -W weka.classifiers.lazy.IBk))  -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15
elif [ $SEL -eq 3 ]; then
	echo "======================================================================="
	echo "3.a) Multilabel Hoeffding Tree"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.MultilabelHoeffdingTree -a multilabel.MajorityLabelset) -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" 
	echo "======================================================================="
	echo "3.b) Multilabel Hoeffding Tree with PS At the leaves"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (multilabel.MultilabelHoeffdingTree -l (multilabel.MEKAClassifier -l meka.classifiers.multilabel.incremental.PSUpdateable)) -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" | cut -f4,5 -d',' | $DATA_DIR/align.sh , 15
elif [ $SEL -eq 4 ]; then
	echo "======================================================================="
	echo "4.a) Multilabel Hoeffding Tree in Bagging Scheme"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (OzaBagML -l (multilabel.MultilabelHoeffdingTree -a multilabel.MajorityLabelset)) -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" 
	echo "======================================================================="
	echo "4.b) Multilabel Hoeffding Tree (with PS) in Bagging Scheme"
	java -cp $MOA:$MEKA:$WEKA moa.DoTask "EvaluatePrequentialMultiTarget -e BasicMultiLabelPerformanceEvaluator -f $f -l (OzaBagML -l (multilabel.MultilabelHoeffdingTree -l (multilabel.MEKAClassifier -l meka.classifiers.multilabel.incremental.PSUpdateable))) -s (MultiTargetArffFileStream -f $DATA_DIR/$DATA_SET -c $L) -i $N" 
fi


