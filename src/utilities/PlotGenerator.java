package utilities;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.math.plot.Plot2DPanel;
import org.math.plot.plotObjects.BaseLabel;

public class PlotGenerator {
	public static int msBeforeClosingEachGraph = 2000;
	public static void plotTrainingAndValidationErrors(String plotFileName, String titleStr, ArrayList<Double> trainingErrorByIteration,  ArrayList<Double> validationErrorByIteration) {
		double[] training = new double[trainingErrorByIteration.size()]; 
		for (int i = 0; i < training.length; i++) {
			training[i] = trainingErrorByIteration.get(i);
		}
		double[] validation = new double[validationErrorByIteration.size()]; 
		for (int i = 0; i < validation.length; i++) {
			validation[i] = validationErrorByIteration.get(i);
		}
		plotTrainingAndValidationErrors(plotFileName, titleStr, training, validation);
	}
	
	public static void plotTrainingAndValidationErrors(String plotFileName, String titleStr, double[] trainingErrorByIteration, double[] validationErrorByIteration) {
		plotTrainingAndValidationErrors(plotFileName, titleStr, null, trainingErrorByIteration, validationErrorByIteration);
	}
	
	public static void plotTrainingAndValidationErrors(String plotFileName, String titleStr, double[] iterationNumber, double[] trainingErrorByIteration, double[] validationErrorByIteration) {
		if (iterationNumber == null) {
			iterationNumber = new double[trainingErrorByIteration.length];
			for (int i = 1; i <= trainingErrorByIteration.length; i++) {
				iterationNumber[i-1] = i;
			}
		}
		Plot2DPanel plot = new Plot2DPanel(new double[] {0 , 0 }, new double[] { 1, 1 },  new String[] { "lin", "lin" }, new String[] { "Iteration Number", "Error" });
		plot.addLegend("SOUTH");
		plot.addLinePlot("Training Error", iterationNumber, trainingErrorByIteration);
		plot.addLinePlot("Validation Error", iterationNumber, validationErrorByIteration);

		//plot.setFixedBounds(1, -0.1, 1);
		//plot.setFixedBounds(0, 0, 1000);
		//plot.setLinearSlicing(1, 11);
		//plot.setLinearSlicing(0, 10);
		
	    BaseLabel title = new BaseLabel(titleStr, Color.BLACK, 0.5, 1.1);
        title.setFont(new Font("Courier", Font.BOLD, 15));
        plot.addPlotable(title);
        
        double minTrainingError = Double.MAX_VALUE, minValidationError = Double.MAX_VALUE;
        int minTrainingErrorIteration = trainingErrorByIteration.length, minValidationErrorIteration = validationErrorByIteration.length;
        for (int i = 0; i < trainingErrorByIteration.length; i++) {
        	if ( trainingErrorByIteration[i] < minTrainingError) {
        		minTrainingError = trainingErrorByIteration[i];
        		minTrainingErrorIteration = i;
        	}
        	if ( validationErrorByIteration[i] < minValidationError) {
        		minValidationError = validationErrorByIteration[i];
        		minValidationErrorIteration = i;
        	}
        	
        }
        String minTrainingErr = "Min Training Err:   " + String.format("%.4f", minTrainingError) + "  Iteration: " + minTrainingErrorIteration;
        String minValidErr =    "Min Validation Err: " + String.format("%.4f", minValidationError) + "  Iteration: " + minValidationErrorIteration;
	    BaseLabel minErrors = new BaseLabel(minTrainingErr + "\n" + minValidErr, Color.BLACK, 0.78, .88);
	    minErrors.setFont(new Font("Courier", Font.PLAIN, 11));
        plot.addPlotable(minErrors);
        
        
		//plot.setLinearSlicing(1, 11);
		
		JFrame frame = new JFrame("a plot panel");
		frame.setContentPane(plot);
		frame.setVisible(true);
		frame.setBounds(0, 0, 1000, 500);
		
		  
		BufferedImage bufferedImage = new BufferedImage(1000, 500, BufferedImage.TYPE_INT_RGB);
		Graphics g = bufferedImage.createGraphics();
		try {
			Thread.sleep(msBeforeClosingEachGraph);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		frame.paint(g);
	
		g.dispose();
		frame.dispose();
		try {
			ImageIO.write((RenderedImage) bufferedImage, "PNG", new File("PREFIX" + plotFileName + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
