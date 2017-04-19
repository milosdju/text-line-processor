# Text lines detector (OCR preprocessing unit)

Java project that can be used as preprocessing unit for OCR application. This particular module detects lines of scanned texts in the image format. From external libraries, OpenCV is used for image processing.

## How it started
One of the exercises for Machine Learning summer school was to develop application that counts number of text lines in scanned document. That includes to build functionality for detecting text lines, which later should be used as part of advanced OCR application that converts scanned text files into digital format. Here I am going to work more on this module, make it more accurate in first place (currently, for some small test suite, it shoot in 4/10 cases with **exactly** number of lines, in 4/10 with 90% accuracy, in 2/10 missed it totally)

## How to use it?
* Run java project (either from IDE or as JAR file)
* Enter absolute path to the PNG, JPG text file in the console
* Number of lines detected will be printed to the console
* (DEBUG) In the containing folder 4 image files will be created

## *Debug files (images)

Source image in black-white format
Minimal rectangle that contains text is drawn
Text is rotated that is alligned to horizontal line
Every line of text is marked
