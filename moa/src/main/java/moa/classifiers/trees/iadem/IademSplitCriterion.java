/*
 *    IademSplitMeasure.java
 *
 *    @author Isvani Frias-Blanco
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.classifiers.trees.iadem;

import java.io.Serializable;
import java.util.ArrayList;

public final class IademSplitCriterion implements Serializable {

    private static final long serialVersionUID = 1L;
    private final static int ENTROPY = 0;
    private final static int ENTROPY_LOG_VAR = 1;
    private final static int WEIGHTED_ENTROPY_LOG_VAR = 2;
    private final static int WEIGHTED_ENTROPY = 9;
    private final static int BETA_1 = 3;
    private final static int GAMMA_1 = 4;
    private final static int BETA_2 = 5;
    private final static int GAMMA_2 = 6;
    private final static int BETA_4 = 7;
    private final static int GAMMA_4 = 8;
    private final static int ERROR_GAIN = 10;
    private final static String ENTROPY_TEXT = "entropy";
    private final static String ENTROPY_LOG_VAR_TEXT = "entropy_logVar";
    private final static String WEIGHTED_ENTROPY_LOG_VAR_TEXT = "entropy_logVar+Weight";
    private final static String WEIGHTED_ENTROPY_TEXT = "entropy+Weight";
    private final static String BETA_1_TEXT = "beta1";
    private final static String BETA_2_TEXT = "beta2";
    private final static String BETA_4_TEXT = "beta4";
    private final static String GAMMA_1_TEXT = "gamma1";
    private final static String GAMMA_2_TEXT = "gamma2";
    private final static String GAMMA_4_TEXT = "gamma4";
    private final static String ERROR_GAIN_TEXT = "error_gain";
    private int splitMeasure;

    public IademSplitCriterion() {
        this.splitMeasure = IademSplitCriterion.ENTROPY;
    }

    public IademSplitCriterion(String splitMeasure) throws IademException {
        try {
            setSplitMeasure(splitMeasure);
        } catch (IademException e) {
            throw new IademException("MySplitMeasure", "constructor",
                    "Measure could not be created" + '\n'
                    + e.getMessage());
        }
    }

    public String getSplitMeasureText() {
        String splitMeasureText = "";
        switch (this.splitMeasure) {
            case IademSplitCriterion.ENTROPY:
                splitMeasureText = IademSplitCriterion.ENTROPY_TEXT;
                break;
            case IademSplitCriterion.ENTROPY_LOG_VAR:
                splitMeasureText = IademSplitCriterion.ENTROPY_LOG_VAR_TEXT;
                break;
            case IademSplitCriterion.WEIGHTED_ENTROPY_LOG_VAR:
                splitMeasureText = IademSplitCriterion.WEIGHTED_ENTROPY_LOG_VAR_TEXT;
                break;
            case IademSplitCriterion.WEIGHTED_ENTROPY:
                splitMeasureText = IademSplitCriterion.WEIGHTED_ENTROPY_TEXT;
                break;
            case IademSplitCriterion.BETA_1:
                splitMeasureText = IademSplitCriterion.BETA_1_TEXT;
                break;
            case IademSplitCriterion.GAMMA_1:
                splitMeasureText = IademSplitCriterion.GAMMA_1_TEXT;
                break;
            case IademSplitCriterion.BETA_2:
                splitMeasureText = IademSplitCriterion.BETA_2_TEXT;
                break;
            case IademSplitCriterion.GAMMA_2:
                splitMeasureText = IademSplitCriterion.GAMMA_2_TEXT;
                break;
            case IademSplitCriterion.BETA_4:
                splitMeasureText = IademSplitCriterion.BETA_4_TEXT;
                break;
            case IademSplitCriterion.GAMMA_4:
                splitMeasureText = IademSplitCriterion.GAMMA_4_TEXT;
                break;
            case IademSplitCriterion.ERROR_GAIN:
                splitMeasureText = IademSplitCriterion.ERROR_GAIN_TEXT;
                break;
        }
        return splitMeasureText;
    }

    public void setSplitMeasure(String splitMeasureType) throws IademException {
        if (splitMeasureType.equals(IademSplitCriterion.ENTROPY_TEXT)) {
            this.splitMeasure = IademSplitCriterion.ENTROPY;
        } else if (splitMeasureType.equals(IademSplitCriterion.ENTROPY_LOG_VAR_TEXT)) {
            splitMeasure = IademSplitCriterion.ENTROPY_LOG_VAR;
        } else if (splitMeasureType.equals(WEIGHTED_ENTROPY_LOG_VAR_TEXT)) {
            splitMeasure = IademSplitCriterion.WEIGHTED_ENTROPY_LOG_VAR;
        } else if (splitMeasureType.equals(IademSplitCriterion.WEIGHTED_ENTROPY_TEXT)) {
            splitMeasure = IademSplitCriterion.WEIGHTED_ENTROPY;
        } else if (splitMeasureType.equals(IademSplitCriterion.BETA_1_TEXT)) {
            splitMeasure = IademSplitCriterion.BETA_1;
        } else if (splitMeasureType.equals(IademSplitCriterion.GAMMA_1_TEXT)) {
            splitMeasure = IademSplitCriterion.GAMMA_1;
        } else if (splitMeasureType.equals(IademSplitCriterion.BETA_2_TEXT)) {
            splitMeasure = IademSplitCriterion.BETA_2;
        } else if (splitMeasureType.equals(IademSplitCriterion.GAMMA_2_TEXT)) {
            splitMeasure = IademSplitCriterion.GAMMA_2;
        } else if (splitMeasureType.equals(IademSplitCriterion.BETA_4_TEXT)) {
            splitMeasure = IademSplitCriterion.BETA_4;
        } else if (splitMeasureType.equals(IademSplitCriterion.GAMMA_4_TEXT)) {
            splitMeasure = IademSplitCriterion.GAMMA_4;
        } else if (splitMeasureType.equals(IademSplitCriterion.ERROR_GAIN_TEXT)) {
            splitMeasure = IademSplitCriterion.ERROR_GAIN;
        } else {
            throw new IademException("MySplitMeasure", "setSplitMeasure", "Measure does not exist");
        }
    }

    public static ArrayList<String> getSplitMeasureOptions() {
        ArrayList<String> splitMeasureList = new ArrayList<String>();
        splitMeasureList.add(IademSplitCriterion.ENTROPY_TEXT);
        splitMeasureList.add(IademSplitCriterion.ENTROPY_LOG_VAR_TEXT);
        splitMeasureList.add(IademSplitCriterion.WEIGHTED_ENTROPY_LOG_VAR_TEXT);
        splitMeasureList.add(IademSplitCriterion.WEIGHTED_ENTROPY_TEXT);
        splitMeasureList.add(IademSplitCriterion.BETA_1_TEXT);
        splitMeasureList.add(IademSplitCriterion.GAMMA_1_TEXT);
        splitMeasureList.add(IademSplitCriterion.BETA_2_TEXT);
        splitMeasureList.add(IademSplitCriterion.GAMMA_2_TEXT);
        splitMeasureList.add(IademSplitCriterion.BETA_4_TEXT);
        splitMeasureList.add(IademSplitCriterion.GAMMA_4_TEXT);

        return splitMeasureList;
    }

    public static String getDefaultSplitMeasure() {
        return IademSplitCriterion.ENTROPY_TEXT;
    }

    public double doMeasure(ArrayList<Double> vector) throws IademException {
        double logBase; // logarithm base for entropy
        logBase = (double) vector.size();

        double measure = 0.0;
        switch (this.splitMeasure) {
            case IademSplitCriterion.ENTROPY: {
                logBase = vector.size();
                if (logBase > 1.0) {
                    double n = 0.0;
                    for (Double elem : vector) {
                        double tmpValue = elem;
                        if (tmpValue > 0.0) {
                            n += tmpValue;
                        } else if (tmpValue < 0.0) {
                            throw new IademException("MySplitMeasure", "doMeasure",
                                    "All values must be positive");
                        }
                    }

                    if (n == 0.0) {
                        throw new IademException("MySplitMeasure", "doMeasure",
                                "Vector must be different from 0.0");
                    } else {
                        double a_i = 0.0;
                        double sum = 0.0;

                        for (int i = 0; i < vector.size(); i++) {
                            a_i = vector.get(i);
                            if (a_i > 0.0) {
                                sum += (a_i * IademCommonProcedures.log(logBase, a_i));
                            }
                        }
                        measure = -((1 / n) * (sum - (n * IademCommonProcedures.log(logBase, n))));
                    }
                } else if (logBase == 1.0) {
                    measure = 0.0;
                }
            }
            break;
            case ENTROPY_LOG_VAR: {
                if (logBase > 1.0) {
                    double n = 0.0;
                    for (Double elem : vector) {
                        double tmpValue = elem;
                        if (tmpValue > 0.0) {
                            n += tmpValue;
                        } else if (tmpValue < 0.0) {
                            throw new IademException("MySplitMeasure", "doMeasure",
                                    "All values must be positive");
                        }
                    }

                    if (n == 0.0) {
                        throw new IademException("MySplitMeasure", "doMeasure",
                                "Vector must be different from 0.0");
                    } else {
                        double a_i = 0.0;
                        double sum = 0.0;

                        for (Double elem : vector) {
                            a_i = elem;
                            if (a_i > 0.0) {
                                sum += a_i * IademCommonProcedures.log(logBase, a_i);
                            }
                        }
                        measure = -((1 / n) * (sum - (n * IademCommonProcedures.log(logBase, n))));
                    }
                } else if (logBase == 1.0) {
                    measure = 0.0;
                }
            }
            break;

            case WEIGHTED_ENTROPY: {
                if (logBase > 1.0) {
                    double n = 0.0;
                    for (int i = 0; i < vector.size(); i++) {
                        double tmpValue = vector.get(i);
                        if (tmpValue > 0.0) {
                            n += tmpValue;
                        } else if (tmpValue < 0.0) {
                            throw new IademException("MySplitMeasure", "doMeasure",
                                    "All values must be positive");
                        }
                    }

                    if (n == 0.0) {
                        measure = 0.0;
                    } else {
                        double a_i = 0.0;
                        double sum = 0.0;

                        for (int i = 0; i < vector.size(); i++) {
                            a_i = vector.get(i);
                            if (a_i > 0.0) {
                                sum += (a_i * IademCommonProcedures.log(logBase, a_i));
                            }
                        }
                        measure = -((1 / n) * (sum - (n * IademCommonProcedures.log(logBase, n))));
                    }
                } else if (logBase == 1.0) {
                    measure = 0.0;
                }
            }
            break;

            case WEIGHTED_ENTROPY_LOG_VAR: {
                logBase = vector.size();
                if (logBase > 1.0) {
                    double n = 0.0;
                    for (int i = 0; i < vector.size(); i++) {
                        double tmpValue = vector.get(i);
                        if (tmpValue > 0.0) {
                            n += tmpValue;
                        } else if (tmpValue < 0.0) {
                            throw new IademException("MySplitMeasure", "doMeasure",
                                    "All values must be positive");
                        }
                    }

                    if (n == 0.0) {
                        measure = 0.0;
                    } else {
                        double a_i = 0.0;
                        double sum = 0.0;

                        for (Double elem : vector) {
                            a_i = elem;
                            if (a_i > 0.0) {
                                sum += (a_i * IademCommonProcedures.log(logBase, a_i));
                            }
                        }
                        measure = -((1 / n) * (sum - (n * IademCommonProcedures.log(logBase, n))));
                    }
                } else if (logBase == 1.0) {
                    measure = 0.0;
                }
            }
            break;

            case BETA_1: {
                double mayor = 0.0;
                for (Double elem : vector) {
                    mayor = Math.max(mayor, elem);
                }
                measure = 1.0 - mayor;
            }
            break;

            case GAMMA_1: {
                double max = 0.0;
                for (Double elem : vector) {
                    max = Math.max(max, elem);
                }
                measure = 1.0 - max;
                measure = Math.sqrt(2.0 * measure);
            }
            break;

            case BETA_2: {
                double mayor = 0.0;
                double sum = 0.0;
                for (Double elem : vector) {
                    double num = elem;
                    mayor = Math.max(mayor, num);
                    sum += (num * num);
                }
                measure = 1.0 - mayor + (mayor * mayor) - sum;
            }
            break;

            case GAMMA_2: {
                double max = 0.0;
                double sum = 0.0;
                for (Double elem : vector) {
                    double num = elem;
                    max = Math.max(max, num);
                    sum += (num * num);
                }
                measure = 1.0 - max + (max * max) - sum;
                measure = Math.sqrt(4.0 * measure);
            }
            break;

            case BETA_4: {
                double sum = 0.0;
                for (Double elem : vector) {
                    double num = elem;
                    sum += (num * num);
                }
                measure = 1.0 - sum;
            }
            break;

            case GAMMA_4: {
                double sum = 0.0;
                for (Double elem : vector) {
                    double num = elem;
                    sum += (num * num);
                }
                measure = 1.0 - sum;
                measure = Math.sqrt(2.0 * measure);
            }
            break;
        }
        if (measure < 0.0) {
            throw new IademException("MySplitMeasure", "doMeasure",
                    "Measure could not be calculated");
        }
        return measure;
    }
}
