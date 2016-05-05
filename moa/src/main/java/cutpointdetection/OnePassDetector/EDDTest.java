package cutpointdetection.OnePassDetector;
import java.io.BufferedReader;
import java.io.FileReader;


class EDDTest
{
    static double j=0;

    //Parameters
    static double d_delta = 0.01;
    static int i_blockSize = 100;
    static int i_inequality = EDD.BERNSTEIN;
    static int i_errorCorrection = EDD.SERIES;




    public static void main(String args[])
    {
        GenerateData sData = new GenerateData();
        sData.run();
        
        EDD edd = new EDD(d_delta,i_blockSize,i_inequality,i_errorCorrection);
        //EDD.setReportingType(EDD.DRIFT_ONLY);
        int i_driftCount = 0;
        try
        {
            BufferedReader br = new BufferedReader(new FileReader("data.txt"));
            String sLine = null;
            int iLine= 0;
            while(( sLine = br.readLine()) != null)
            {
                if(edd.setInput(Double.parseDouble(sLine))) //Input data into Adwin
                {
                    System.out.println("Change Detected: "+iLine);
                    i_driftCount++;                            
                } 
                iLine++;
            }
            br.close();
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
        System.out.println("Drifts detected :"+	i_driftCount);
    }

    public static double f(int i)
    {
        //j = j+0.001;
        //return i;
        //return i;
        return 0.001*i;
        //return (i<500?500:1000);
    }
}
