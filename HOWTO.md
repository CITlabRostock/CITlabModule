# HOWTO

## Goal
The aim is to use the transcriptions made by UCL to create traindata for an HTR model

UCL transcribed more than 80.000 pages of Jeremy Bentham.
So far the transcription is only available tei-xml-files.
To make them usable to train an HTR,
text lines have to be extracted from the image and have to be transcribed
or aligned with the available transcription.
This produces a huge amount of manual work. The key idea is to construct a workflow
which uses technologies of URO to automatically produce a huge amount of training data for the HTR.

#### Setting up Text Alignment Task
Therefore we first have to extract the transcripts from the xml-fils.
the method
```
java -cp <this-jar> de.uros.citlab.module.workflow.SetUpBenthamData \
<folder_images> \
<folder_tei-xmls> \
<folder_merged>
```
will extract the text of the xml-files and put them to the corresponding image.
It assumes that the xml-files are named ``JB_<ID>.xml``,
whereas the corresponding image is named ``<ID>.jpg``.
The text of the xml-file contains a lot of markups,
for example, if the written text is striked through or underlined,
if it is a superscript or an addition e.t.c.
The algorithm trys to normalize the transcipt so that it fits to the script on the image.

#### Find Lines
To get possible training data a Layout Analysis (LA) have to be run on the pages.
This produces a baseline with its corresponding surrounding polygon. This LA is important:
lines which are not found or are erroneously found by the LA,
cannot be matched or have to be ignored by the text alignment tool.
The Method (LA Advanced) can be applied by running  
```
java -cp <this-jar> de.uros.citlab.module.workflow.Apply2Folder_ML \
-xml_in <folder_merged> \
-xml_out <folder_merged> \
-b2p de.uros.citlab.module.baseline2polygon.B2PSeamMultiOriented
```
Here it is important to choose the same folder,
because the algorithm does not copy the *.txt files created by the method above.
#### Apply Text Alignment
The text alignment methods needs an HTR model to calculate the so-called ConfMat.
Then it tries to align the ConfMats/text lines with the given transcripts.
```
java -cp <this-jar> de.uros.citlab.module.workflow.Text2ImageNoLineBreak \
-in <folder_merged> \
-out <folder_aligned> \
-htr <folder_htr>
```
The text lines of the resulting xml-files are now enriched with corresponding transcripts
(if they were found) and a confidence of it alignment quality.

#### Create Training Data and CharMap
For the training the algorithm need training data (which are image-transcript-pairs)
and a CharMap which contains the characters the HTR should recognize.
In addition it is possible to provide a threshold for the confidence
(calculated by the Text Alignment tool) of the training data.
So only training data which are likely enough to be correct will be used in training.
```
java -cp <this-jar> de.uros.citlab.module.workflow.CreateTraindata \
-xml <folder_aligned> \
-out <folder_traindata> \
-cm <path_to_charmap> \
-minconf <float_value>
```
a value ``minconf = 0.1`` ensures that the resulting training nearly all are correct.
With ``minconf = 0.01`` also very challenging lines will apear in the training,
but also some wrong aligned lines. A good default is ``minconf = 0.05``.