# CITlabModule
This project makes it possible to use the Handwriting Text Recognition (HRT) and the Layout Analysis (LA) from URO/CITlab and Planet i.s. in Transkribus fulfilling the interfaces of TranskribusInterfaces

## Building
```
git clone https://github.com/Transkribus/CITlabModule.git
cd CITlabModule
mvn install
```

## Requirements
### general
- java 1.8 or higher
- planet_jar.jar of planet.de (not open available) containing basic libraries
### for LA
- OpenCV 3.10
### for advanced LA
- OpenCV 3.10
- GCOC (see below)
- tensorflow (see below)
### for advanced HTR
- tensorflow (see below)

## Building GCOC dependencies for advanced LA

* Ask for the GCOC.tar.gz
```
tar -xf GCOC.tar.gz
cd GCOC
cmake .
make install
``` 
The installation will create HOME/lib and HOME/include (if not existing), we are just interested in the lib file from HOME/lib in case of Linux, it is called libgcoj.so
Put this lib somewhere java can find it (LD_LIBRARY_PATH).
To add the JNI-Wrapper to your local Maven-Repo, follow:
```
cd JAVA
sh jar2mvnrepo.sh
```
## Build tensorflow dependencies for advanced LA and HTR:
* the dependency to tensorflow needs a c-lib (an *.so-file)
* the library can be found at https://ci.tensorflow.org/view/Release/job/release-libtensorflow/TYPE=cpu-slave/
* this library should match the version of the java dependency (1.3.0)
* download the library and put the lib somewhere java can find it (LD_LIBRARY_PATH)
* Note that compiling tensorflow on your device can make tensorflow running faster - especially if it can use gpu devices.
