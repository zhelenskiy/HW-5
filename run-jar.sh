#! /usr/bin/env bash
ROOT=$PWD

MODULE_NAME=ru.ifmo.rain.zhelenskiy.implementor

LIB_PATH=$ROOT/lib:$ROOT/artifacts:$ROOT/jar
#echo $@
java --module-path "$LIB_PATH" -m $MODULE_NAME $@
#running like: run-tests.sh java.util.ArrayList test