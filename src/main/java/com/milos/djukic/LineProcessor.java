package com.milos.djukic;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import nu.pattern.OpenCV;

/**
 * @author milosdju
 *
 */
public class LineProcessor {

	public static void main(String[] args) throws IOException {
		OpenCV.loadLocally();
		
		Scanner scanner = new Scanner(System.in);
		System.out.print("Absolute path to the text image: ");
		String imgPath = scanner.nextLine();
		int actualNumOfLines = determineNumOfLines(imgPath, false);
		if (actualNumOfLines < 5) {
			actualNumOfLines = determineNumOfLines(imgPath, true);
		}

		System.out.println("Number of text lines detected in text: " + actualNumOfLines);
		System.out.println("\r\nProcessing steps on image while detecting lines are stored "
				+ "as 4 separate images in folder 'Processing Steps' relative to the source image");
	}

	/**
	 * @param imagePath Absolute path of scanned document
	 * @param rotate Additional option if rotating of text fail
	 * @return Number of text lines in scanned document
	 * @throws IOException
	 */
	public static int determineNumOfLines(String imagePath, boolean rotate) throws IOException {
		// Read the image
		Mat img = Imgcodecs.imread(imagePath);

		/*
		 * 1st phase
		 * 
		 * First gray scale the image.
		 * Then, binarize image. Text is white, background is black.
		 * 
		 * Because of cases where scanned document contains shades 
		 * in the background, document will be divided in couple parts
		 * and for each part threshold for dividing black-white pixels
		 * will be calculated
		 */
		Mat gray = new Mat();
		Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
		List<Mat> quarters = new ArrayList<Mat>();
		// Binarize image quarter by quarter
		for (int i = 0; i < 4; i++) {
			int startingRow = i * (gray.rows() / 4);
			int endingRow = (i + 1) * gray.rows() / 4 > gray.rows() ? gray.rows() : (i + 1) * gray.rows() / 4;
			Mat quarter = gray.submat(startingRow, endingRow, 0, gray.cols());
			Imgproc.threshold(quarter, quarter, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
			quarters.add(quarter);
		}
		Core.vconcat(quarters, gray);
		Core.bitwise_not(gray, gray);
		printImage(gray, imagePath.replace(imagePath.substring(imagePath.length() - 4), "") + "/grayed");
		
		/*
		 * 2nd phase
		 * 
		 * Find minimal rectangle in which
		 * whole text can be packed
		 */
		// Find all white pixels
		Mat whitePoints = new Mat();
		Core.findNonZero(gray, whitePoints);

		// Get rotated rectangle of white pixels
		MatOfPoint2f points = new MatOfPoint2f();
		whitePoints.convertTo(points, CvType.CV_32FC2);

		RotatedRect rotatedRect = Imgproc.minAreaRect(points);
		if (rotate && rotatedRect.size.width > rotatedRect.size.height) {
			double tmp = rotatedRect.size.width;
			rotatedRect.size.width = rotatedRect.size.height;
			rotatedRect.size.height = tmp;
			rotatedRect.angle += 90;
		}

		Point[] rectPoints = new Point[4];
		rotatedRect.points(rectPoints);
		printImage(img, imagePath.replace(imagePath.substring(imagePath.length() - 4), "") + "/minArea");
		
		/*
		 * 3rd phase
		 * 
		 * Rotate the image according to the found angle 
		 * of minimal rectangle
		 */
		Mat rotatedImg = new Mat();
		Mat m = Imgproc.getRotationMatrix2D(rotatedRect.center, rotatedRect.angle, 1.0);
		Imgproc.warpAffine(gray, rotatedImg, m, gray.size());
		printImage(rotatedImg, imagePath.replace(imagePath.substring(imagePath.length() - 4), "")+  "/rotated");

		/*
		 * 4th phase 
		 * 
		 * Using horizontal projection decide
		 * which lines contains text and which not
		 */
		// Compute horizontal projections (number of white pixels per row)
		Mat horProj = new Mat();
		Core.reduce(rotatedImg, horProj, 1, Core.REDUCE_AVG);

		// Remove noise in histogram:
		// Calculating threshold (i.e. number of pixels per row) which
		// should differentiate empty from text lines
		MatOfInt hist = new MatOfInt();

		int count = 0;
		double[] medianList = new double[20];
		// This block of code will compute
		// MEDIAN height of text lines in 20 cases (i.e. with 20 different thresholds)
		
		// Median that is closest to the mean of all medians
		// will be chosen as best projected line height
		// and threshold related to that median will be used as threshold value
		for (int t = 0; t < 20; t++) {
			List<Integer> numOfEl = new ArrayList<Integer>();
			Core.compare(horProj, new Scalar(t), hist, Core.CMP_LE);
			for (int i = 0; i < rotatedImg.rows(); i++) {
				if (hist.get(i, 0)[0] == 0) {
					if (hist.get(i - 1, 0)[0] == 0) {
						count++;
					} else {
						numOfEl.add(count);
						count = 0;
					}
				}
			}
			Integer[] numOfElArray = new Integer[numOfEl.size()];
			Arrays.sort(numOfEl.toArray(numOfElArray));
			int numOfElSize = numOfElArray.length;
			int sum = 0;
			for (int i = 0; i < numOfElSize; i++) {
				sum += numOfElArray[i];
			}
			double medianAvg = (double) sum / numOfElSize;
			medianList[t] = medianAvg;
		}
		double medianSum = 0;
		for (double median : medianList) {
			medianSum += median;
		}
		double medianOfAllThresholds = medianSum / medianList.length;

		// Remove noise (continued)
		double threshold = findClosest(medianList, medianOfAllThresholds);
		Scalar th = new Scalar(threshold);
		Core.compare(horProj, th, hist, Core.CMP_LE);

		// Find lines
		// Every row with none of the white pixels will determine 
		// end of one and beginning of another text line
		List<Integer> ycoords = new ArrayList<Integer>();
		boolean isSpace = true;
		int y = 0;
		for (int i = 0; i < rotatedImg.rows(); i++) {
			if (isSpace) {
				if (hist.get(i, 0)[0] == 0) {
					isSpace = false;
					count = 1;
					y = i;
				}
			} else {
				if (hist.get(i, 0)[0] != 0) {
					isSpace = true;
					if (count < (int) (medianOfAllThresholds * 0.5))
						continue;
					ycoords.add(y / count);
				} else {
					y += i;
					count++;
				}
			}
		}
		// Draw lines
		Mat result = new Mat();
		Imgproc.cvtColor(rotatedImg, result, Imgproc.COLOR_GRAY2BGR);
		for (int i = 0; i < ycoords.size(); i++) {
			Imgproc.line(result, new Point(0, ycoords.get(i)), new Point(result.cols(), ycoords.get(i)), new Scalar(0, 255, 0));
		}
		printImage(result, imagePath.replace(imagePath.substring(imagePath.length() - 4), "") + "/with lines");
		
		return ycoords.size();
	}

	/**
	 * @param array
	 * @param num
	 * @return element of {@code array} that is closest to {@code num}
	 */
	private static int findClosest(double[] array, double num) {
		double diff = Math.abs(array[0] - num);
		int position = 0;
		for (int i = 0; i < array.length; i++) {
			if (diff > Math.abs(array[i] - num)) {
				diff = Math.abs(array[i] - num);
				position = i;
			}
		}

		return position;
	}

	/**
	 * Method will make file and store image (from Matrix of pixels) in it
	 * 
	 * @param image Matrix of pixels
	 * @param outputName Absolute path to the file where image will be stored
	 * @throws IOException
	 */
	private static void printImage(Mat image, String outputName) throws IOException {
		BufferedImage bufferedImage = matToBufferedImage(image, null);
		File outputfile = new File(outputName + ".jpg");
		outputfile.getParentFile().mkdirs();
		ImageIO.write(bufferedImage, "jpg", outputfile);
	}
	
	/**
	 * Method will use matrix of pixels to make BufferedImage
	 * 
	 * @param image Matrix of pixels
	 * @param bimg null
	 * @return BufferedImage of the matrix
	 */
	private static BufferedImage matToBufferedImage(Mat image, BufferedImage bimg) {
	    if (image != null) { 
	        int cols = image.cols();  
	        int rows = image.rows();  
	        int elemSize = (int)image.elemSize();  
	        byte[] data = new byte[cols * rows * elemSize];  
	        int type;  
	        image.get(0, 0, data);  
	        switch (image.channels()) {  
	        case 1:  
	            type = BufferedImage.TYPE_BYTE_GRAY;  
	            break;  
	        case 3:  
	            type = BufferedImage.TYPE_3BYTE_BGR;  
	            // bgr to rgb  
	            byte b;  
	            for(int i=0; i<data.length; i=i+3) {  
	                b = data[i];  
	                data[i] = data[i+2];  
	                data[i+2] = b;  
	            }  
	            break;  
	        default:  
	            return null;  
	        }  

	        if (bimg == null || bimg.getWidth() != cols || bimg.getHeight() != rows || bimg.getType() != type) {
	            bimg = new BufferedImage(cols, rows, type);
	        }        
	        bimg.getRaster().setDataElements(0, 0, cols, rows, data);
	    } else { // mat was null
	        bimg = null;
	    }
	    return bimg;  
	} 
	
}
