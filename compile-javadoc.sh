#modulep="modules"
javadoc \
    -link https://docs.oracle.com/en/java/javase/11/docs/api/ \
    -d _javadoc \
    -cp artifacts/JarImplementorTest.jar:"lib/hamcrest-core-1.3.jar:lib/junit-4.11.jar:lib/jsoup-1.8.1.jar:"lib/quickcheck-0.6.jar:\
     modules/ru.ifmo.rain.zhelenskiy.implementor/src/ru/ifmo/rain/zhelenskiy/implementor/MethodDataClass.java \
     modules/ru.ifmo.rain.zhelenskiy.implementor/src/ru/ifmo/rain/zhelenskiy/implementor/Implementor.java \
     modules/ru.ifmo.rain.zhelenskiy.implementor/src/package-info.java \
     modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/Impler.java \
     modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/JarImpler.java \
     modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/ImplerException.java\
     modules/info.kgeorgiy.java.advanced.implementor/package-info.java