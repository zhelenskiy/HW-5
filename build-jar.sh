ROOT=$PWD

MODULE_NAME=ru.ifmo.rain.zhelenskiy.implementor
MODULE_PATH=ru/ifmo/rain/zhelenskiy/implementor

OUT_PATH=out/production/$MODULE_NAME

LIB_PATH=$ROOT/lib:$ROOT/artifacts
SRC_PATH=$ROOT/modules/$MODULE_NAME/src
JAR_PATH=$ROOT/jar

javac --module-path "$LIB_PATH"  "$SRC_PATH"/module-info.java  "$SRC_PATH"/$MODULE_PATH/*.java -d $OUT_PATH

cd $OUT_PATH || exit

mkdir "$JAR_PATH" 2> /dev/null
jar -c --file="$JAR_PATH"/Implementor.jar --main-class=$MODULE_NAME.Implementor --module-path="$LIB_PATH" \
    module-info.class $MODULE_PATH/*