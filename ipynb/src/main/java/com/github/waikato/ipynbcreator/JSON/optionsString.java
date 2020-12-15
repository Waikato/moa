package moa.tasks.JSON;

import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;

/**
 * This class get input string of learner, stream and evaluator then process them
 * the output will be name of learner, stream, or evaluator besides their options
 **/
public class optionsString {

    private String classShortName = "";

    private String classFullName = "";

    private String classOptionsString = "";

    private ArrayList<Pair<String, String>> outputClassObjectOptions = new ArrayList();

    private ArrayList<Pair<String, String>> outputClassOptions = new ArrayList();

    private StringBuilder inputString;

    public optionsString(String str) {
        inputString = new StringBuilder(str);
        int i = this.inputString.indexOf(" ");
        if (i > 0) {
            this.classOptionsString = this.inputString.substring(i + 1);
            this.classFullName = this.inputString.substring(0, i);
        } else {
            this.classFullName = this.inputString.toString();
        }
    }

    public String getInputString() {
        return this.inputString.toString();
    }

    public String getClassShortName() {
        return this.classShortName;
    }

    public String getClassFullName() {
        return this.classFullName;
    }

    public String generateOptionsString() {
        StringBuilder str = new StringBuilder(this.getClassShortName())
                .append(" ")
                .append(this.getClassShortName().substring(0, 4).toLowerCase())
                .append(" = new " + this.getClassShortName() + "();");
        if (!this.classOptionsString.equals(""))
            str.append(this.getClassShortName().substring(0, 4).toLowerCase())
                    .append(".getOptions().setViaCLIString(\"")
                    .append(this.classOptionsString + "\");");
        return str.toString();
    }

    /**
     * Separates out options from command strings
     */
    public void createOptionsList() {
        int j;
        int i;
        String tempClassOptionsString = this.classOptionsString;
        while (tempClassOptionsString.length() > 0) {
            char cliChar = ' ';
            String optionValue = "";
            String str = "";
            tempClassOptionsString = tempClassOptionsString.trim();

            i = tempClassOptionsString.indexOf("-");
            if (i >= 0) {
                cliChar = tempClassOptionsString.charAt(i + 1);
                tempClassOptionsString = tempClassOptionsString.substring(i + 2).trim();
                if (tempClassOptionsString.length() == 0) {
                    optionValue = "true";
                    Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
                    this.outputClassOptions.add(optionPair);
                } else {
                    if (tempClassOptionsString.charAt(0) == '-') {
                        optionValue = "true";
                        Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
                        this.outputClassOptions.add(optionPair);
                    } else if (tempClassOptionsString.charAt(0) == '(') {
                        int openBracket = 0;
                        int closeBracket = 0;
                        StringBuffer temp = new StringBuffer("");
                        for (int k = 0; k < tempClassOptionsString.length(); k++) {
                            char cTemp = tempClassOptionsString.charAt(k);
                            temp.append(cTemp);
                            switch (cTemp) {
                                case '(': {
                                    openBracket += 1;
                                    break;
                                }
                                case ')': {
                                    closeBracket += 1;
                                    if (closeBracket == openBracket) {
                                        tempClassOptionsString = tempClassOptionsString.substring(k + 1).trim();
                                        optionValue = temp.toString().trim();
                                        optionValue = optionValue.substring(1, optionValue.length() - 1);
                                        Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
                                        this.outputClassObjectOptions.add(optionPair);
                                        optionsString subObject = new optionsString(optionValue);
                                    }
                                    break;
                                }
                            }
                        }


                    } else {
                        j = tempClassOptionsString.indexOf(" ");
                        if (j > 0) {
                            optionValue = tempClassOptionsString.substring(0, j);
                            tempClassOptionsString = tempClassOptionsString.substring(j + 1).trim();
                            Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
                            this.outputClassOptions.add(optionPair);
                        } else {
                            optionValue = tempClassOptionsString;
                            tempClassOptionsString = "";
                            Pair<String, String> optionPair = new Pair<String, String>(String.valueOf(cliChar), optionValue);
                            this.outputClassOptions.add(optionPair);
                        }
                    }
                }

            }
        }

        i = this.classFullName.lastIndexOf('.');
        if (i > 0) {
            this.classShortName = this.classFullName.substring(i + 1);
        } else
            this.classShortName = this.classFullName;
    }
}