/*
 *    Plot.java
 *    Copyright (C) 2010 Poznan University of Technology, Poznan, Poland
 *    @author Dariusz Brzezinski (dariusz.brzezinski@cs.put.poznan.pl)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import moa.core.ObjectRepository;
import com.github.javacliparser.FileOption;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.ListOption;
import com.github.javacliparser.MultiChoiceOption;
import com.github.javacliparser.StringOption;

/**
 * A task allowing to create and plot gnuplot scripts.
 * 
 * @author Dariusz Brzezinski
 * 
 */
public class Plot extends MainTask {
    @Override
    public String getPurposeString() {
	return "Creates a Gnuplot script and plots a chart from a set of given csv files.";
    }

    private static final long serialVersionUID = 1L;

    /**
     * Path to gunplot's binary directory, for example C:\Tools\Gnuplot\binary.
     */
    public StringOption gnuplotPathOption = new StringOption("gnuplotPath", 'e',
	    "Directory of the gnuplot executable. For example \"C:\\Tools\\Gnuplot\\binary\".", "");

    
    /**
     * FileOption for selecting the plot output file.
     */
    public FileOption plotOutputOption = new FileOption("plotOutputFile", 'r',
	    "File with the result plot (image).", null, "eps", true);

    /**
     * Comma separated list of input *csv files. The file paths can be absolute
     * or relative to the executing directory (moa.jar directory).
     */
    public ListOption inputFilesOption = new ListOption(
	    "inputFiles",
	    'i',
	    "File names or file paths of csv inputs. Values should be seperated by commas.",
	    new StringOption("inputFile", ' ', "Input file.", "algorithm.csv"),
	    new StringOption[] {
		    new StringOption("", ' ', "", "algorithm1.csv"),
		    new StringOption("", ' ', "", "algorithm2.csv"),
		    new StringOption("", ' ', "", "algorithm3.csv") }, ',');

    /**
     * Comma separated list of aliases for the input *csv files. If a legend is
     * added to the plot, aliases will be presented in the legend.
     */
    public ListOption fileAliasesOption = new ListOption(
	    "aliases",
	    'a',
	    "Aliases for files stated in the inputFiles parameter. Aliases will be presented in the plot's legend.",
	    new StringOption("alias", ' ', "File alias.", "MyAlg"),
	    new StringOption[] { new StringOption("", ' ', "", "OZABag"),
		    new StringOption("", ' ', "", "HOT"),
		    new StringOption("", ' ', "", "AWE") }, ',');

    /**
     * Gnuplot terminal - postscript, png, pdf etc.
     */
    public MultiChoiceOption outputTypeOption = new MultiChoiceOption(
	    "outputType", 't', "Gnuplot output terminal.", Terminal
		    .getStringValues(), Terminal.getDescriptions(), 8);

    /**
     * Type of plot - dots, points, lines ets.
     */
    public MultiChoiceOption plotStyleOption = new MultiChoiceOption(
	    "plotStyle", 'p', "Plot style.", PlotStyle.getStringValues(),
	    PlotStyle.getDescriptions(), 2);

    /**
     * Index of the csv column from which values for the x-axis should be taken.
     */
    public IntOption xColumnOption = new IntOption(
	    "xColumn",
	    'x',
	    "Index of the csv column from which values for the x-axis should be taken.",
	    1);

    /**
     * Title of the plots' x-axis.
     */
    public StringOption xTitleOption = new StringOption("xTitle", 'm',
	    "Title of the plots' x-axis.", "Processed instances");

    /**
     * Units displayed next to x-axis values.
     */
    public StringOption xUnitOption = new StringOption("xUnit", 'g',
	    "Units displayed next to x-axis values.", "");

    /**
     * Index of the csv column from which values for the y-axis should be taken.
     */
    public IntOption yColumnOption = new IntOption(
	    "yColumn",
	    'y',
	    "Index of the column from which values for the y-axis should be taken",
	    9);

    /**
     * Title of the plots' y-axis.
     */
    public StringOption yTitleOption = new StringOption("yTitle", 'n',
	    "Title of the plots' y-axis.", "Accuracy");

    /**
     * Units displayed next to y-axis values.
     */
    public StringOption yUnitOption = new StringOption("yUnit", 'u',
	    "Units displayed next to y-axis values.", "%");

    /**
     * Plotted line width.
     */
    public IntOption lineWidthOption = new IntOption("lineWidth", 'w',
	    "Determines the thickness of plotted lines", 2);

    /**
     * Interval between plotted data points.
     */
    public IntOption pointIntervalOption = new IntOption(
	    "pointInterval",
	    'v',
	    "Determines the inteval between plotted data points. Used for LINESPOINTS plots only.",
	    0, 0, Integer.MAX_VALUE);
    /**
     * Determines whether to smooth the plot with bezier curves.
     */
    public FlagOption smoothOption = new FlagOption("smooth", 's',
	    "Determines whether to smooth the plot with bezier curves.");
    /**
     * Determines whether to delete gnuplot scripts after plotting.
     */
    public FlagOption deleteScriptsOption = new FlagOption("deleteScripts",
	    'd', "Determines whether to delete gnuplot scripts after plotting.");

    /**
     * Legend (key) location on the plot.
     */
    public MultiChoiceOption legendLocationOption = new MultiChoiceOption(
	    "legendLocation", 'l', "Legend (key) location on the plot.",
	    LegendLocation.getStringValues(), LegendLocation.getDescriptions(),
	    8);

    /**
     * Legend elements' alignment.
     */
    public MultiChoiceOption legendTypeOption = new MultiChoiceOption(
	    "legendType", 'k', "Legend elements' alignment.", LegendType
		    .getStringValues(), LegendType.getDescriptions(), 1);

    /**
     * Addition pre-plot gunplot commands. For example "set tics out" will
     * change the default tic option and force outward facing tics. See the
     * gnuplot manual for more commands.
     */
    public StringOption additionalSetOption = new StringOption(
	    "additionalCommands",
	    'c',
	    "Additional commands that should be added to the gnuplot script before the plot command. For example \"set tics out\" will change the default tic option and force outward facing tics. See the gnuplot manual for more commands.",
	    " ");

    /**
     * Additional plot options. For example \"[] [0:]\" will force the y-axis to
     * start from 0. See the gnuplot manual for more options.
     */
    public StringOption additionalPlotOption = new StringOption(
	    "additionalPlotOptions",
	    'z',
	    "Additional options that should be added to the gnuplot script in the plot statement. For example \"[] [0:]\" will force the y-axis to start from 0. See the gnuplot manual for more options.",
	    " ");

    /**
     * Plot output terminal.
     * @author Dariusz Brzezi�ski
     *
     */
    public enum Terminal {
	CANVAS, EPSLATEX, GIF, JPEG, LATEX, PDFCAIRO, PNG, POSTSCRIPT, POSTSCRIPT_COLOR, PSLATEX, PSTEX, PSTRICKS, SVG;

	private static String[] descriptions = new String[] {
		"HTML Canvas object",
		"LaTeX picture environment using graphicx package",
		"GIF images using libgd and TrueType fonts",
		"JPEG images using libgd and TrueType fonts",
		"LaTeX picture environment",
		"pdf terminal based on cairo",
		"PNG images using libgd and TrueType fonts",
		"PostScript graphics, including EPSF embedded files (*.eps)",
		"Color PostScript graphics, including EPSF embedded files (*.eps)",
		"LaTeX picture environment with PostScript specials",
		"plain TeX with PostScript specials",
		"LaTeX picture environment with PSTricks macros",
		"W3C Scalable Vector Graphics driver" };

	/**
	 * Gets an array of string descriptions - one for each enum value.
	 * @return a description for each enum value.
	 */
	public static String[] getDescriptions() {
	    return descriptions;
	}

	/**
	 * Get string values for the enum values.
	 * @return a set of string values for the enum values.
	 */
	public static String[] getStringValues() {
	    int i = 0;
	    String[] result = new String[values().length];

	    for (Terminal value : values()) {
		result[i++] = value.name();
	    }

	    return result;
	}
    }

    /**
     * Location of the legend on the plot.
     * @author Dariusz Brzezi�ski
     *
     */
    public enum LegendLocation {
	TOP_LEFT_INSIDE, TOP_CENTER_INSIDE, TOP_RIGHT_INSIDE, LEFT_INSIDE, CENTER_INSIDE, RIGHT_INSIDE, BOTTOM_LEFT_INSIDE, BOTTOM_CENTER_INSIDE, BOTTOM_RIGHT_INSIDE, TOP_LEFT_OUTSIDE, TOP_CENTER_OUTSIDE, TOP_RIGHT_OUTSIDE, LEFT_OUTSIDE, CENTER_OUTSIDE, RIGHT_OUTSIDE, BOTTOM_LEFT_OUTSIDE, BOTTOM_CENTER_OUTSIDE, BOTTOM_RIGHT_OUTSIDE;

	/**
	 * Gets an array of string descriptions - one for each enum value.
	 * @return a description for each enum value.
	 */
	public static String[] getDescriptions() {
	    int i = 0;
	    String[] result = new String[values().length];

	    for (LegendLocation value : values()) {
		result[i++] = value.name().toLowerCase().replace('_', ' ');
	    }

	    return result;
	}

	/**
	 * Get string values for the enum values.
	 * @return a set of string values for the enum values.
	 */
	public static String[] getStringValues() {
	    int i = 0;
	    String[] result = new String[values().length];

	    for (LegendLocation value : values()) {
		result[i++] = value.name();
	    }

	    return result;
	}
    }

    /**
     * Type of legend.
     * @author Dariusz Brzezi�ski
     *
     */
    public enum LegendType {
	NONE, BOX_VERTICAL, BOX_HORIZONTAL, NOBOX_VERTICAL, NOBOX_HORIZONTAL;

	/**
	 * Gets an array of string descriptions - one for each enum value.
	 * @return a description for each enum value.
	 */
	public static String[] getDescriptions() {
	    int i = 0;
	    String[] result = new String[values().length];

	    for (LegendType value : values()) {
		result[i++] = value.name().toLowerCase().replace('_', ' ');
	    }

	    return result;
	}

	/**
	 * Get string values for the enum values.
	 * @return a set of string values for the enum values.
	 */
	public static String[] getStringValues() {
	    int i = 0;
	    String[] result = new String[values().length];

	    for (LegendType value : values()) {
		result[i++] = value.name();
	    }

	    return result;
	}
    }

    public enum PlotStyle {
	LINES, POINTS, LINESPOINTS, IMPULSES, STEPS, FSTEPS, HISTEPS, DOTS;

	private static String[] descriptions = new String[] {
		"It connects each data point with lines. Suitable to smoothly varying data.",
		"Symbols are shown at the data point location, can be used to plot experimental data.",
		"Draws lines and symbols at the same time.",
		"Draw vertical lines from each data point to X-axis. This is a bar-graph without width.",
		"Histogram type 1",
		"Histogram type 2",
		"Histogram type 3",
		"It displays dots, can be used when there many data points, but hard to see though.", };

	/**
	 * Gets an array of string descriptions = one for each enum value.
	 * @return a description for each enum value.
	 */
	public static String[] getDescriptions() {
	    return descriptions;
	}

	/**
	 * Get string values for the enum values.
	 * @return a set of string values for the enum values.
	 */
	public static String[] getStringValues() {
	    int i = 0;
	    String[] result = new String[values().length];

	    for (PlotStyle value : values()) {
		result[i++] = value.name();
	    }

	    return result;
	}
    }

    /**
     * Defines the task's result type.
     */
    public Class<?> getTaskResultType() {
	return String.class;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
	File resultFile = this.plotOutputOption.getFile();
	if (this.plotOutputOption.getFile() == null) {
	    throw new RuntimeException("Plot output file option not set!");
	}

	String resultDirectory = (new File(resultFile.getAbsolutePath()))
		.getParent();
	String gnuPlotPath = gnuplotPathOption.getValue();
	File gnuplotDir = new File(gnuPlotPath);
	if(!gnuplotDir.exists()){
	    throw new RuntimeException("Gnuplot directory not found: " + gnuPlotPath);
	}

	monitor.setCurrentActivity("Verifying input files...", 0.0);

	if (inputFilesOption.getList().length > fileAliasesOption.getList().length) {
	    throw new RuntimeException("Too little aliases for input files!");
	} else if (inputFilesOption.getList().length < fileAliasesOption
		.getList().length) {
	    throw new RuntimeException("Too many aliases for input files!");
	} else {
	    for (int i = 0; i < inputFilesOption.getList().length; i++) {
		File inputFile = new File(((StringOption) inputFilesOption
			.getList()[i]).getValue());

		if (!inputFile.exists()) {
		    throw new RuntimeException("File not found: "
			    + inputFile.getAbsolutePath());
		}
	    }
	}

	if (monitor.taskShouldAbort()) {
	    return null;
	}
	monitor.setCurrentActivity("Creating script file...", 1.0 / 4.0);

	String gnuplotScriptPath = resultDirectory + File.separator
		+ resultFile.getName() + ".plt";
	String script = createScript(resultFile);
	File scriptFile = writeScriptToFile(gnuplotScriptPath, script);

	if (monitor.taskShouldAbort()) {
	    return null;
	}
	monitor.setCurrentActivity("Plotting data...", 2.0 / 4.0);

	String gnuplotCommand = gnuPlotPath + File.separator + "gnuplot \""
		+ gnuplotScriptPath + "\"";

	String line, gnuplotOutput = "";
	try {
	    Process p = Runtime.getRuntime().exec(gnuplotCommand);

	    BufferedReader err = new BufferedReader(new InputStreamReader(p
		    .getErrorStream()));
	    while ((line = err.readLine()) != null) {
		gnuplotOutput += line + System.getProperty("line.separator");
	    }
	    err.close();
	} catch (IOException ex) {
	    throw new RuntimeException("Error while executing gnuplot script:"
		    + scriptFile, ex);
	}

	if (monitor.taskShouldAbort()) {
	    return null;
	}
	if (deleteScriptsOption.isSet()) {
	    monitor.setCurrentActivity("Deleting script...", 3.0 / 4.0);
	    scriptFile.delete();
	}
	if (monitor.taskShouldAbort()) {
	    return null;
	}
	monitor.setCurrentActivity("Done", 1.0);

	return resultFile.getAbsolutePath()
		+ System.getProperty("line.separator") + gnuplotOutput;
    }

    /**
     * Method responsible for saving a gnuplot script to a file.
     * @param gnuplotScriptPath Path of the file
     * @param script gnuplot script content
     * @return the object of the saved file
     */
    private File writeScriptToFile(String gnuplotScriptPath, String script) {
	File scriptFile = new File(gnuplotScriptPath);
	BufferedWriter writer;
	try {
	    writer = new BufferedWriter(new FileWriter(scriptFile));
	    writer.write(script);
	    writer.close();
	} catch (IOException ex) {
	    throw new RuntimeException(
		    "Unable to create or write to script file: " + scriptFile,
		    ex);
	}
	return scriptFile;
    }

    /**
     * Creates the content of the gnuplot script.
     * @param resultFile path of the plot output file
     * @return gnuplot script
     */
    private String createScript(File resultFile) {
	String newLine = System.getProperty("line.separator");
	int sourceFileIdx = 0;

	// terminal options;
	String script = "set term "
		+ terminalOptions(Terminal.valueOf(outputTypeOption
			.getChosenLabel())) + newLine;
	script += "set output '" + resultFile.getAbsolutePath() + "'" + newLine;
	script += "set datafile separator ','" + newLine;
	script += "set grid" + newLine;
	script += "set style line 1 pt 8" + newLine;
	script += "set style line 2 lt rgb '#00C000'" + newLine;
	script += "set style line 5 lt rgb '#FFD800'" + newLine;
	script += "set style line 6 lt rgb '#4E0000'" + newLine;
	script += "set format x '%.0s %c" + getAxisUnit(xUnitOption.getValue())
		+ "'" + newLine;
	script += "set format y '%.0s %c" + getAxisUnit(yUnitOption.getValue())
		+ "'" + newLine;
	script += "set ylabel '" + yTitleOption.getValue() + "'" + newLine;
	script += "set xlabel '" + xTitleOption.getValue() + "'" + newLine;
	if (!legendTypeOption.getChosenLabel().equals(LegendType.NONE)) {
	    script += "set key "
		    + legendTypeOption.getChosenLabel().toLowerCase().replace(
			    '_', ' ')
		    + " "
		    + legendLocationOption.getChosenLabel().toLowerCase()
			    .replace('_', ' ') + newLine;
	}

	// additional commands
	script += additionalSetOption.getValue();

	// plot command
	script += "plot " + additionalPlotOption.getValue() + " ";

	// plot for each input file
	for (int i = 0; i < inputFilesOption.getList().length; i++) {
		
	    if (sourceFileIdx > 0) {
		script += ", ";
	    }
	    sourceFileIdx++;
	    script += "'" + ((StringOption) inputFilesOption
			.getList()[i]).getValue() + "' using "
		    + xColumnOption.getValue() + ":" + yColumnOption.getValue();

	    if (smoothOption.isSet()) {
		script += ":(1.0) smooth bezier";
	    }

	    script += " with " + plotStyleOption.getChosenLabel().toLowerCase()
		    + " ls " + sourceFileIdx + " lw "
		    + lineWidthOption.getValue();
	    if (plotStyleOption.getChosenLabel().equals(
		    PlotStyle.LINESPOINTS.toString())
		    && pointIntervalOption.getValue() > 0) {
		script += " pointinterval " + pointIntervalOption.getValue();
	    }
	    script += " title '" + ((StringOption) fileAliasesOption
			.getList()[i]).getValue() + "'";
	}
	script += newLine;
	return script;
    }

    private String getAxisUnit(String unit) {
	if (unit.equals("%")) {
	    return "%%";
	} else {
	    return unit;
	}
    }

    private String terminalOptions(Terminal term) {
	String options;

	switch (term) {
	case POSTSCRIPT:
	    options = "postscript enhanced";
	    break;
	case POSTSCRIPT_COLOR:
	    options = "postscript color enhanced";
	    break;
	default:
	    options = term.toString().toLowerCase();
	    break;
	}
	return options;
    }
}
