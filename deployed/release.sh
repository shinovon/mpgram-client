#!/bin/bash

if [[ -z "$MPGRAM_DEPLOY_DIR" ]]; then
MPGRAM_DEPLOY_DIR=$(pwd)
fi

# regular jar
cd Regular || exit 1
cp mpgram.jar "$MPGRAM_DEPLOY_DIR/mpgram.jar"

# regular jad
sed -i "s/MIDlet-Jar-URL: mpgram.jar/MIDlet-Jar-URL: http:\/\/nnproject.cc\/dl\/mpgram.jar/g" mpgram.jad && \
unix2dos mpgram.jad && \
cp mpgram.jad "$MPGRAM_DEPLOY_DIR/mpgram.jad"

# lite jar
cd ../Lite || exit 1
rm mpgram_lite.jar
mkdir META-INF
unzip -p mpgram.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
sed -i "/Nokia-Scalable-Icon: \/m.svg/d" META-INF/MANIFEST.MF && \
sed -i "/Nokia-Scalable-Icon-MIDlet-1: \/m.svg/d" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
7z a mpgram.jar META-INF/ && \
mv mpgram.jar mpgram_lite.jar && \
cp mpgram_lite.jar "$MPGRAM_DEPLOY_DIR/mpgram_lite.jar"

# lite jad
sed -i "/Nokia-Scalable-Icon: \/m.svg/d" mpgram.jad && \
sed -i "/Nokia-Scalable-Icon-MIDlet-1: \/m.svg/d" mpgram.jad && \
sed -i "/MIDlet-Jar-Size: /d" mpgram.jad && \
sed -i "s/MIDlet-Jar-URL: mpgram.jar/MIDlet-Jar-URL: http:\/\/nnproject.cc\/dl\/mpgram_lite.jar/g" mpgram.jad && \
size=$(stat -c %s "mpgram_lite.jar") && \
echo -e "MIDlet-Jar-Size: $size\n" >> mpgram.jad && \
unix2dos mpgram.jad && \
mv mpgram.jad mpgram_lite.jad && \
cp mpgram_lite.jad "$MPGRAM_DEPLOY_DIR/mpgram_lite.jad"

# bb jar
cd ../BlackBerry || exit 1
rm mpgram_bb.jar
mkdir META-INF
unzip -p mpgram.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
sed -i "/Nokia-Scalable-Icon: \/m.svg/d" META-INF/MANIFEST.MF && \
sed -i "/Nokia-Scalable-Icon-MIDlet-1: \/m.svg/d" META-INF/MANIFEST.MF && \
sed -i "s/Nokia-MIDlet-Splash-Screen-Image: suppress/mpgram-blackberry-build: true/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
7z a mpgram.jar META-INF/ && \
mv mpgram.jar mpgram_bb.jar && \
cp mpgram_bb.jar "$MPGRAM_DEPLOY_DIR/mpgram_bb.jar"

# samsung jar
cd ../Regular || exit 1
rm mpgram_samsung.jar
mkdir META-INF
cp mpgram.jar mpgram_samsung.jar && \
unzip -p mpgram.jar META-INF/MANIFEST.MF > META-INF/MANIFEST.MF && \
sed -i "s/Nokia-Scalable-Icon: \/m.svg/MIDlet-Touch-Support: True/g" META-INF/MANIFEST.MF && \
sed -i "s/Nokia-Scalable-Icon-MIDlet-1: \/m.svg/MIDlet-Landscape-Support: True/g" META-INF/MANIFEST.MF && \
sed -i "s/Nokia-MIDlet-Splash-Screen-Image: suppress/mpgram-samsung-build: true/g" META-INF/MANIFEST.MF && \
unix2dos META-INF/MANIFEST.MF && \
7z a mpgram_samsung.jar META-INF/ && \
cp mpgram_samsung.jar "$MPGRAM_DEPLOY_DIR/mpgram_samsung.jar"
