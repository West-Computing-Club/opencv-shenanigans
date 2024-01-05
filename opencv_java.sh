#!/bin/bash

# Get source from sourceforge.
wget 'https://versaweb.dl.sourceforge.net/project/opencvlibrary/4.9.0/OpenCV%204.9.0%20source%20code.tar.gz'
tar -xzf 'OpenCV 4.9.0 source code.tar.gz'
rm 'OpenCV 4.9.0 source code.tar.gz'

# Get CMake.
sudo apt update
sudo apt install cmake

# Build from source. There are no pre-built Java bindings for Linux.
cd opencv-opencv-d7d8670
mkdir build
cd build
cmake -DBUILD_SHARED_LIBS=OFF ..
make -j8

# Retrieve binaries.
cd ../..
mkdir bin
cp opencv-opencv-d7d8670/build/bin/opencv-490.jar ./bin
cp opencv-opencv-d7d8670/build/lib/* ./bin

# Clean up.
rm -rf opencv-opencv-d7d8670
