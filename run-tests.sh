#! /usr/bin/env bash
ROOT=$PWD

MODULE_NAME=ru.ifmo.rain.zhelenskiy.implementor

#LIB_PATH=$ROOT/lib:$ROOT/artifacts:$ROOT/jar
#echo $@
cd out/production/"$MODULE_NAME" || exit
java -cp . -p . --module-path "$ROOT"/artifacts:"$ROOT"/lib \
    -m info.kgeorgiy.java.advanced.implementor jar-class "$MODULE_NAME".Implementor "$1"
#running like: run-tests.sh java.util.ArrayList test