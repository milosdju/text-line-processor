# Text lines detector (OCR preprocessing unit)

Java project that can be used as preprocessing unit for OCR application. This particular module detects lines of scanned texts in the image format. From external libraries, OpenCV is used for image processing.

## How it started
One of the exercises for Machine Learning summer school was to develop application that counts number of text lines in scanned document. That includes to build functionality for detecting text lines, which later should be used as part of advanced OCR application that converts scanned text files into digital format. Here I am going to work more on this module, make it more accurate in first place (currently, for some small test suite, it shoot in 4/10 cases with **exactly** number of lines, in 4/10 with 90% accuracy, in 2/10 missed it totally)

## How to use it?
* Run java project (either from IDE or as JAR file)
* Enter absolute path to the PNG, JPG text file in the console
* Number of lines detected will be printed to the console
* (DEBUG) In the containing folder 4 image files will be created

## How it works?
We will use this scanned text document (with chinese symbols) for example.
<br>
<img src="https://cloud.githubusercontent.com/assets/10442500/25198447/de9f7e6c-2547-11e7-9dc5-bd431dc076f4.jpg" width="450">
<br>
<br>

**1. step:** 
- Gray scale and binarize image. Text is white, background is black.
- Because of cases where scanned document contains shades in the background, document will be divided in couple parts and for each part threshold for dividing black-white pixels will be calculated.

<img src="https://cloud.githubusercontent.com/assets/10442500/25198451/e2a2d0fe-2547-11e7-8ec0-ea8a72def61a.jpg" width="450">
<br>

**2. step:**
- Find minimal rectangle in which whole text can be packed

<img src="https://cloud.githubusercontent.com/assets/10442500/25200573/11b688f2-254f-11e7-8a35-1e2a5ecd1c61.jpg" width="450">
<br>

**3. step:**
- Rotate the image according to the found angle of minimal rectangle

<img src="https://cloud.githubusercontent.com/assets/10442500/25198461/e6ff58e8-2547-11e7-89ad-13ab406d96c0.jpg" width="450">
<br>

**4. step:**
- Using horizontal projection decide which lines contains text and which not

<img src="https://cloud.githubusercontent.com/assets/10442500/25198463/e85aa3fa-2547-11e7-9a56-1201458bea18.jpg" width="450">
<br>

## *Debug files (images)

1. Source image in black-white format
2. Minimal rectangle that contains text is drawn
3. Text is rotated that is alligned to horizontal line
4. Every line of text is marked
