#!/usr/bin/python2.7

import sys, getopt

def main(argv):
   cmd = ''
   learner = ''
   baseLearner = ''
   budget = ''
   try:
      opts, args = getopt.getopt(argv,"hlBb")
   except getopt.GetoptError:
      print 'cmd_script.py -l <learner> -B <baseLearner> -b <budget>'
      sys.exit(2)
   for opt, arg in opts:
      if opt == '-h':
         print 'cmd_script.py -l <learner> -B <baseLearner> -b <budget>'
         sys.exit()
      elif opt == '-l':
         # learner
         if arg == 'PALStream':
            learner = 'PALStream'
         elif arg == 'PAL@IDA':
            # TODO
         elif arg == 'ZliobaiteSplit':
            learner = 'ALZliobaite2011 -d SelSampling'
         elif arg == 'ZliobaiteAdaptThresh':
            learner = 'ALZliobaite2011 -d VarUncertainty'
         elif arg == 'DBALStream':
            # TODO
         elif arg == 'Random':
            learner = 'ALRandom'
         else:
            print 'Algorithm not found.'
            print 'Available algorithms: PALStream, PAL@IDA, ' + \
            'ZliobiateSplit, ZliobaiteAdaptThresh, DBALStream, Random'
            sys.exit()
      elif opt == '-B':
         # base learner
         if arg == 'NaiveBayes':
            baseLearner = 'drift.SingleClassifierDrift'
         elif arg == 'HoeffdingAdaptiveTree':
            baseLearner = 'trees.HoeffdingAdaptiveTree'
         elif arg == 'AccuracyUpdatedEnsemble':
            baseLearner = 'meta.AccuracyUpdatedEnsemble'
         else:
            print 'BaseLearner not found.'
            print 'Available base learners: NaiveBayes, HoeffdingAdaptiveTree,' + \
            'AccuracyUpdatedEnsemble'
            sys.exit()            
      elif opt == '-b':
         # budget
         budget = arg

         cmd = 'java -cp moa.jar moa.DoTask "' +  ???

if __name__ == "__main__":
   main(sys.argv[1:])
