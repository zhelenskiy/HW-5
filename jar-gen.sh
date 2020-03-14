#!/bin/bash

cp -rp info out
cp Manifest.txt out/

mkdir out 2> /dev/null

cd out || exit

jar xf ../artifacts/info.kgeorgiy.java.advanced.implementor.jar \
        info/kgeorgiy/java/advanced/implementor/Impler.class \
        info/kgeorgiy/java/advanced/implementor/JarImpler.class \
        info/kgeorgiy/java/advanced/implementor/ImplerException.class

mkdir ../jar-files 2> /dev/null
jar cfm ../jar-files/Implementor.jar Manifest.txt \
     production/ru.ifmo.rain.zhelenskiy.implementor/ru/ifmo/rain/zhelenskiy/implementor/*.class \
     info/kgeorgiy/java/advanced/implementor/*.class