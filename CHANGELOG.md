# Change Log

# 2.4.3
* bugfix for HTR+ ignore very small line regions 

# 2.4.2
* bugfix for HTR+ delete old *.pb in export folder

# 2.4.1
* bugfix for HTR+ base model training
* needs tf_htsr version 3.0.5
* training snippets are greyscale to reduce disc storage

# 2.4.0
* make planet and private dependencies 'provided'
* make dependent on new tokenizer
* bugfix in Text2Image is used

# 2.3.2
* bugfix format characters in transcripts: ignore lines and characters, when they are from an unknown category and report to observer. 
* bugfix T2I: do not delete folder, if input and ouput is the same

# 2.3.1
* bugfix B2P: ignores text lines while B2P, if they have invalid baselines.

# 2.3.0
* bugfix training: charmap cannot be in htrOut
* bugfix language model: exception for empty transcript caught and reported via ErrorNotification
* feature: slightly better preproc

# 2.2.2
* bugfix training: shuffle training list so that order is "randomly"
* bugfix surrogates: if a line contains surrogates, it is ignored in training and validation
* bugfix empty validation: If a validation set is given, but no lines are in, throw exception
* feature notification: errors while creating training/validation data are reported via notification

# 2.2.1
* bugfix LA Advanced: when region is given and rotated text lines are found, lines will now not rotated out of image 

# 2.2.0
* update to newer versions for bugfix in planets sources

# 2.1.1
* bugfix training of HTR+ with base model

# 2.1.0
* faster TextAlignment and base technology moved to CITlabTextAlignment
* HTR+ can be trained further - also with changed CharMap
* bugfix load dictionary: now also possible for ";"-seperator with headlines
* feature-request: logging in multi-process-use possible.
* newest versions of all citlab and planet libs
* bugfix fail-save Baseline2Polygon - 2 fallbacks for fails

# 2.0.2
* MOVE REPOSITORY WITHOUT HISTORY FROM Transkribs to CITlabRostock AND MAKE OPENSOURCE
* bugfix: check System.getenv() instead of System.getProperty() for $PYTHONPATH
* bugfix: do not set train_size_per_epoch instad of "-1"
* bugfix: add process listener to TrainHTRPlus to make status/process observable
* feature: language resource can be created when trainingdata are created (set property "create_lr"==>"true" or "path_to/file"
# 2.0.1
* bugfix: master-confict

# 2.0.0
* add new HTR+ which requires CUDA and Tensorflow
* switch from file-structure to folder-structure fo HTR
* switch from 1 big planet jar to planet artifacts

# 1.1.9
* feature request: make Filename of ConfMatContainer configurable via properties

# 1.1.8
* bugfix erronious calculation of coords for very small baselines
* patch ignore bidi control characters
* image type: indexed_byte is supported 
* bugfix LA module (array out of bounds error)
* memory leak fixed?!
* fix concerning erroneous baselines
* reduced number of points for short baselines

# 1.1.6
* bugfix erroneous split of baseline in LA module 

# 1.1.5
* adapted the scaling prior to LA module
* BaseLine2Polygon uses angle of baseline, not angle of region for calculation
* character \n are ignored when getting TextLine->TextEquiv->Unicode
* minor bugfixes

# 1.1.4
* bugfix #27: switch from CenterOfMass to Average
* better logging and statistic for Text2Image

# 1.1.3
* bugfix #27: line are sorted correctly

# 1.1.2
* make kws-group (?<KW>ABC) optional

# 1.1.1
* bugfix charmap for advanced ATR can be set, if charmap is equal

# 1.1
* make dependent on TranskribusErrorRate 2.2.3
* enhance KWS-quality
* update advanced HTR
* make KWS useable for large sets

*
## 1.0.4
* improve kWS (calculate maximum of PRE_KW_POST, PRE and POST group)
* improve GT-creation for KWS

## 1.0.3
* KWS improvement:
* Property "kws_upper" can be set to true
* Property "kws_expert" can be used to directly set regular expressions
* Property "kws_part" can be set to find keyword in words
* additional properties "kws_min_conf", "kws_threads" and "kws_max_anz" can be set

## 1.0.2
* bugfix #26: only throw exception when
* bugfix T2I: do not throw exception if no baseline for matching is found
* bugfix LA: resources available in jar
* bugfix B2P: if region too small, nullpointer can be handled

## 1.0.1
* add tests for advanced Layout Analysis
* update dependencies to PageXmlExtractor-0.3
* delete unsolved dependencies to log4j

## 1.0.0
* bugfix KWS is threadsave
* bugfix HTR: missing baselines are added using BaselineGeneartionHist
* update GetTrainFile to seperated train- and test-file
* bugfixes on creating traindata (traindata will be written into subfolders)
* save jar-version into metadate of PageXml-file
* bugfix KWS: missing lineID does not result in empty result
* adding advanced Layout Analysis
* adding advanced HTR (without training)
* make module dependent on trensorflow and GCOC

## 0.3.3
* Bugfix number of threads in T2I-Train-Workflow
* delete depricated T2I-Workflow
* do not allow B2P in T2I-Workflow
* delete old T2I-methods
* make LayoutAnalysisParser::process with PcGtsType public

## 0.3.2
* switch to java-version 1.8

## 0.3.1
* KWS improvements
* save LineID in ConfmatContainer
* use TRANSKRIBUS_HTR-properties to generate name for xml metadata

## 0.3
* Bugfix Bidi in Text2Image
* switch to new Interface in KWS
* Bugfix maxAnz KWS

## 0.2.2
* T2I is available with BIDI
* Hyphenation more is configurable via T2I_Hyphenation-Properties
* property "train_status" can be set to train only on pages with a specific statuses (e.g."GT;DONE")
* Semi-Supvervised training adds channels to Network, if channels are not there
* Accept all images, that are accepted by transkribus 1.3.*

## 0.2.1
* bugfix LA: LA works also for binary images
* making Text2Image available via Interfaces

## 0.2
* continue Text2Image
* add Test-Method de.uro.citlab.module.util.DictionaryTest to test external dictionaries 
* make dependent on stable versions of TranskribusTokenizer and TranskribusErrorRate
* Integrate TranskribusErrorRate in WER-calculation while training
* Make HTR Training observable
* Cleanup of dictionary works

## 0.1.6
* cleanup (test-) resources - only corresponding planet_jar-x.x.x.jar is needed
* feature #22
* solve bugfix #23

## 0.1.5
* solve bugfix #20
* more accurate handlings of unicode characters

## 0.1.4
* starting of using changelog after this version
