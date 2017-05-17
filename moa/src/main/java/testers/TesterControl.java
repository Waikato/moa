package testers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TesterControl
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
	int testMode = 0;

	System.out.println("== Tester Controller ==");
	System.out.println("Test Modes: ");
	System.out.println("0: PHT TP Tester");
	System.out.println("1: PHT FP Tester");
	System.out.println("2: Options Tester");
	System.out.println("3: Hyperplane Tester");
	System.out.println("4: RBFGeneartor Tester");
	System.out.println("5: VDriftRealWorld Tester");
	System.out.println("6: BernoulliGenerator Tester");
	System.out.println("7: FalsePositive Tester");
	System.out.println("8: TruePositive Tester");
	System.out.println("9: Kappa Tester");
	System.out.println("10: Volatility Stream Generator");
	System.out.println("11: Volatility Tester");
	System.out.println("12: VolatilityTruePositive Tester");
	System.out.println("13: VolatilityFalsePositive Tester");
	System.out.println("============================");
	System.out.println("14: DriftCategorizer Tester");
	System.out.println("15: Drift Experiment Controller");
	System.out.println("16: Categorization Analyzer");
	System.out.println("17: Bernoulli Slope Tester");
	System.out.println("============================");
	System.out.println("19: DriftIntegrationTP Tester");
	System.out.println("20: DriftIntegrationFP Tester");
	System.out.println("============================");
	System.out.println("21: VolatilityPredictingDriftTP Tester");
	System.out.println("22: VolatilityPredictingDriftSummary Tester");
	System.out.println("23: VolatilityPredictingDriftFN Tester");
	System.out.println("24: VolatilityPredictingDriftSummary_Rand Tester");
	System.out.println("25: VolatilityPredictingDriftFP Tester");
	System.out.println("26: VolatilityPredictingDriftRealWorld Tester");
	System.out.println("============================");
	System.out.println("27: CUSUM TP Tester");
	System.out.println("28: CUSUM FP Tester");
	System.out.println("============================");
	System.out.println("29: VolatilityPredictionFeatureExtractor Tester");
	System.out.println("31: VolatilityPredictingDriftFP_Online Tester");
	System.out.println("32: VolatilityPredictingDriftTP_Online Tester");
	System.out.println("33: VolatilityPredictingDrift Classifier Tester");
	System.out.println("============================");
	System.out.println("100: OrangeFactor Tester");

	try
	{
	    BufferedReader br = new BufferedReader(new InputStreamReader(
		    System.in));
	    System.out.print("Please input test mode: ");
	    testMode = Integer.parseInt(br.readLine());
	    System.out.println();
	} catch (IOException e)
	{
	    e.printStackTrace();
	}

	switch (testMode)
	{
	case 0:
	    PHTTruePositiveTester pht = new PHTTruePositiveTester();
	    pht.doTest();
	    break;
	case 1:
	    PHTFalsePositiveTester phtfp = new PHTFalsePositiveTester();
	    phtfp.doTest();
	    break;
	case 2:
	    OptionsTester optionsTester = new OptionsTester();
	    optionsTester.doTest();
	    break;
	case 3:
	    HyperplaneGeneratorTester hpgenTester = new HyperplaneGeneratorTester();
	    hpgenTester.doTest();
	    break;
	case 4:
	    RBFGeneratorTester RBFTester = new RBFGeneratorTester();
	    RBFTester.doTest();
	    break;
	case 5:
	    VDriftRealWorld adwintester = new VDriftRealWorld();
	    adwintester.doTest();
	    break;
	case 6:
	    BernoulliGeneratorTester bernoullitester = new BernoulliGeneratorTester();
	    bernoullitester.doTest();
	    break;
	case 7:
	    FalsePositiveTester FPtester = new FalsePositiveTester();
	    FPtester.doTest();
	    break;
	case 8:
	    TruePositiveTester TPtester = new TruePositiveTester();
	    TPtester.doTest();
	    break;
	case 9:
	    KappaTester Kappatester = new KappaTester();
	    Kappatester.doTest();
	    break;
	case 10:
	    VolatilityStreamGenerator vGen = new VolatilityStreamGenerator();
	    vGen.doTest();
	    break;
	case 11:
	    RelativeVolatilityTester vtest = new RelativeVolatilityTester();
	    vtest.doTest();
	    break;
	case 12:
	    VolatilityTruePositiveTester rvtest = new VolatilityTruePositiveTester();
	    rvtest.doTest();
	    break;
	case 13:
	    VolatilityFalsePositiveTester rvfptest = new VolatilityFalsePositiveTester();
	    rvfptest.doTest();
	    break;
	case 14:
	    DriftCategorizerTester drifttest = new DriftCategorizerTester();
	    drifttest.doTest();
	    break;
	case 15:
	    DriftCategorizationAccuracyTester driftgen = new DriftCategorizationAccuracyTester();
	    driftgen.doTest();
	    break;
	case 16:
	    CategorizationAnalyzer analyze = new CategorizationAnalyzer();
	    analyze.doTest();
	    break;
	case 17:
	    BernoulliSlopeTester slopetesst = new BernoulliSlopeTester();
	    slopetesst.doTest();
	    break;
	case 18:
	    VolatilityCutPointTester cptest = new VolatilityCutPointTester();
	    cptest.doTest();
	    break;
	case 19:
	    DriftIntegrationTruePositiveTester ditest = new DriftIntegrationTruePositiveTester();
	    ditest.doTest();
	    break;
	case 20:
	    DriftIntegrationFalsePositiveTester difptest = new DriftIntegrationFalsePositiveTester();
	    difptest.doTest();
	    break;
	case 21:
	    VDriftDetectionOnlineVersion_TruePositiveTester vdtptest = new VDriftDetectionOnlineVersion_TruePositiveTester();
	    vdtptest.doTest();
	    break;
	case 22:
	    VolatilityPredictingDriftSummary summarytester = new VolatilityPredictingDriftSummary();
	    summarytester.doTest();
	    break;
	case 23:
	    VDriftDetectionFalseNegativeTester vdfntest = new VDriftDetectionFalseNegativeTester();
	    vdfntest.doTest();
	    break;
	case 24:
	    VolatilityPredictingDriftSummary_Random summaryrandtester = new VolatilityPredictingDriftSummary_Random();
	    summaryrandtester.doTest();
	    break;
	case 25:
	    VDriftDetectionFalsePositiveTester vdfptest = new VDriftDetectionFalsePositiveTester();
	    vdfptest.doTest();
	    break;
	case 26:
	    VDriftRealWorld ctest = new VDriftRealWorld();
	    ctest.doTest();
	    break;
	case 27:
	    CUSUMTester cusumtest = new CUSUMTester();
	    cusumtest.doTest();
	    break;
	case 28:
	    CUSUMFP cusumfptest = new CUSUMFP();
	    cusumfptest.doTest();
	    break;
	case 29:
	    VPredFeatureExtractorTester vpfet = new VPredFeatureExtractorTester();
	    vpfet.doTest();
	    break;
	case 30:
	    ADWINTester adwinTest = new ADWINTester();
	    adwinTest.doTest();
	    break;
	case 31:
	    VDriftDetectionOnlineVersion_FalsePositiveTester vdonline = new VDriftDetectionOnlineVersion_FalsePositiveTester();
	    vdonline.doTest();
	    break;
	case 32:
	    VDriftDetectionTruePositiveTester vdtponline = new VDriftDetectionTruePositiveTester();
	    vdtponline.doTest();
	    break;
	case 33:
	    ClassifierTester classtest = new ClassifierTester();
	    classtest.doTest();
	    break;
	case 100:
	    OrangeFactorTester oftest = new OrangeFactorTester();
	    oftest.doTest();
	    break;
	}

    }

}
