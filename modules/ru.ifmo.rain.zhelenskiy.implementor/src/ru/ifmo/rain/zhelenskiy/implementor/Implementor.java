package ru.ifmo.rain.zhelenskiy.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
//import org.jetbrains.annotations.Contract;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.CodeSource;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;

/**
 * This class implements interfaces and classes given by user.
 * It has:
 * <ul>
 *     <li>Method {@link Implementor#implement(Class, Path)}
 *     that implements the given class and saves {@code .java}-file</li>
 *     <li>Method {@link Implementor#implementJar(Class, Path)}
 *     that implements the given class and creates runnable {@code .jar}-file.</li>
 *     <li>Static method {@link Implementor#main(String[])}
 *     that gives the command line interface for using
 *     {@link Implementor#implement(Class, Path)} and {@link Implementor#implementJar(Class, Path)}.</li>
 * </ul>
 * @see info.kgeorgiy.java.advanced.implementor.Impler
 * @see info.kgeorgiy.java.advanced.implementor.JarImpler
 * @version 1.0
 * @author zhelenskiy
 */
public class Implementor implements Impler, JarImpler {

    /**
     * This class is buffered writer, that replaces non-ASCII characters with {@code \\u****}.
     * @see java.io.BufferedWriter
     * @author zhelenskiy
     * @version 1.0
     */
    private static class EscapeWriter extends BufferedWriter {

        /**
         * Constructs {@link EscapeWriter} with given internal {@link BufferedWriter}.
         * @param writer An internal common {@link BufferedWriter} to be called when {@code write} called.
         */
        protected EscapeWriter(BufferedWriter writer) {
            super(writer);
        }

        /**
         * Converts given string to unicode escaping
         *
         * @param in {@link String} to convert
         * @return converted string
         */
        private static String escape(String in) {
            StringBuilder b = new StringBuilder();
            for (char c : in.toCharArray()) {
                if (c >= 128) {
                    b.append(String.format("\\u%04X", (int) c));
                } else {
                    b.append(c);
                }
            }
            return b.toString();
        }

        /**
         * Writes the {@link String} with underlying {@link BufferedWriter}.
         * @param str The string to write.
         * @throws IOException when underlying writer can not write the string.
         */
        @Override
        public void write(String str) throws IOException {
            super.write(escape(str));
        }

        /**
         * Flushes the underlying {@link BufferedWriter}
         * @throws IOException when underlying writer can not flush.
         */
        @Override
        public void flush() throws IOException {
            super.flush();
        }

        /**
         * Closes the underlying {@link BufferedWriter}
         * @throws IOException when underlying writer can not close.
         */
        @Override
        public void close() throws IOException {
            super.close();
        }
    }
    /**
     * Produces {@code .jar} file implementing class or interface specified by provided {@link Class}.
     * Generated class classes name should be same as classes name of the type token with {@code Impl} suffix added.
     * @param token   type token to create implementation for.
     * @param jarFile target {@code .jar} file.
     * @throws ImplerException when implementation can not be generated.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        checkLackOfNulls(token, jarFile);
        try {
            Path tempDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tmp");
            try {
//                System.out.println("lol1");
                implement(token, tempDir);
//                System.out.println("lol2");
                var classFile = compile(token, tempDir);
//                System.out.println("lol3");
                generateArtifact(token, jarFile, classFile);
//                System.out.println("lol4");
            } catch (IOException | SecurityException e) {
                throw new ImplerException("Can not create jar-file: " + e.getMessage());
            } finally {
                cleanTempDirectory(tempDir);
            }
        } catch (IOException | SecurityException e) {
            throw new ImplerException("Can not create temporary directory: " + e.getMessage());
        }
    }

    /**
     * Creates an artifact for created {@code .class}-file.
     * @param token type token for which {@code .jar}-file is generated
     * @param jarFile target for the {@code .jar}-file
     * @param classFile path to the compiled implementation class
     * @throws IOException when can not write to the {@code .jar}-file.
     */
    private void generateArtifact(/*@NotNull */Class<?> token, Path jarFile, Path classFile) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            var className = Paths.get("").resolve(token.getPackageName()
                    .replace('.', File.separatorChar))
                    .resolve(token.getSimpleName() + "Impl.class").toString();
            out.putNextEntry(new ZipEntry(className));
            Files.copy(classFile, out);
            out.closeEntry();
        }
    }

    /**
     * Provides an interface for command line access to {@link #implement(Class, Path)} and {@link #implementJar(Class, Path)}.
     * Expected combinations of arguments:
     * <ul>
     *     <li>2 arguments to call {@link #implement(Class, Path)} method (Class name and directory to save).</li>
     *     <li>3 arguments to call {@link #implementJar(Class, Path)} method (Class name and target file to save).
     *     The first one must be {@code --jar} or {@code -jar}</li>
     * </ul>
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (invalidArgs(args)) return;
        try {
            if (args.length == 2) {
                new Implementor().implement(Class.forName(args[0]), Path.of(args[1]));
            } else if (args[0].equals("-jar") || args[0].equals("--jar")) {
                new Implementor().implementJar(Class.forName(args[1]), Path.of(args[2]));
            } else {
//                new Implementor().implementJar(gg.class, Paths.get("kek.jar"));
                System.err.println("expected -jar or --jar");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Invalid class name given: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Invalid path given: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("Error while creating " +
                    ((args.length == 2) ? "java" : "jar") + " file " + e.getMessage());
        }
    }

    /**
     * Checks if command line arguments are valid strings.
     * @param args command line arguments to check
     * @return if the arguments are <u>IN</u>valid.
     */
    private static boolean invalidArgs(String[] args) {
        if (args == null || args.length < 2 || args.length > 3) {
            System.err.println("Invalid arguments number, expected [-jar] <class.name> <output.path>");
            return true;
        }
        for (String arg : args) {
            if (arg == null) {
                System.err.println("All arguments should be not null");
                return true;
            }
        }
        return false;
    }

    /**
     * Compiles type token with given token and source code path.
     * @param token the token to compile
     * @param tempDir the source code path
     * @return path to generated {@code .class}-file.
     * @throws ImplerException when<ul>
     *     <li>Can not generate class-path;</li>
     *     <li>No java compiler found in the system;</li>
     *     <li>Return code of the compilation is not 0.</li>
     * </ul>
     */
//    @NotNull
    private Path compile(Class<?> token, Path tempDir) throws ImplerException {
        Path superPath;
        try {
            CodeSource superCodeSource = token.getProtectionDomain().getCodeSource();
            superPath = Path.of((superCodeSource == null) ? "" : superCodeSource.getLocation().getPath());
        } catch (InvalidPathException e) {
            throw new ImplerException("Failed to generate valid classpath", e);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("No java compiler found in the system.");
        }
        var className = token.getSimpleName() + Constants.IMPLEMENTATION_POSTFIX;
        final Path pathToPackage = getPathToPackage(token, tempDir);
        String[] args = {
                "-classpath",
                String.join(File.pathSeparator, superPath.toString(), tempDir.toString(), System.getProperty("java.class.path")),
                pathToPackage.resolve(className + ".java").toString()
        };
        int resCode = compiler.run(null, null, null, args);
        if (resCode != 0) {
            throw new ImplerException("Can not compile the class, compiler's return code is " + resCode + ".");
        }
        return pathToPackage.resolve(className + ".class");
    }

    /**
     * Cleans temporary directory with source code and compiled class generated by {@link #implementJar(Class, Path)}.
     * The files are generated because creating of {@code .jar}-file is multi-step.
     * If an error occurs, a message to System.err is written.
     * @param tempDir the temporary directory
     */
    private void cleanTempDirectory(Path tempDir) {
        try {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Can not clean temporary directory: " + tempDir);
        }
    }

    /**
     * Constant strings for source code generation
     */
    private static final class Constants {
        public final static String NEW_LINE = System.lineSeparator();
        public final static String INDENT = "\t";
        public final static String SEPARATOR = NEW_LINE + NEW_LINE;
        public final static String BEGINNING_OF_EXECUTABLE = " {" + Constants.NEW_LINE + Constants.INDENT + Constants.INDENT;
        public final static String ENDING_OF_EXECUTABLE = Constants.NEW_LINE + Constants.INDENT + "}" + Constants.SEPARATOR;
        public final static String IMPLEMENTATION_POSTFIX = "Impl";
    }

    /**
     * Generates path to the package by type token and root directory.
     * @param token the type token to find package for
     * @param root the root directory
     * @return the generated path
     */
    private Path getPathToPackage(/*@NotNull */Class<?> token, Path root) {
        return token.getPackage() == null ? root : root.resolve(token.getPackage().getName().replace(".", File.separator));
    }

    /**
     * Produces code implementing class or interface specified by provided {@code token}.
     * Generated class classes name should be same as classes name of the type token with {@code Impl} suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * {@code root} directory and have correct file name. For example, the implementation of the
     * interface {@link List} should go to {@code $root/java/util/ListImpl.java}
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException when implementation can not be generated.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkLackOfNulls(token, root);
        checkSuperclass(token);
        Path outputDirectory = getPathToPackage(token, root);
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException | SecurityException e) {
            // no handling because if the exception happens because of parallel operation there is now reason to handle,
            // otherwise exception will be thrown later
        }
        var className = token.getSimpleName() + Constants.IMPLEMENTATION_POSTFIX;
        Path pathToFile = outputDirectory.resolve(className + ".java");
        try (BufferedWriter writer = new EscapeWriter(Files.newBufferedWriter(pathToFile))) {
            generateClass(token, className, writer);
        } catch (IOException e) {
            throw new ImplerException("Can not write to output file: " + e.getMessage() + "!");
        }
    }

    /**
     * Checking that given token and path are not nulls.
     * @param token the token to check
     * @param root the path to check
     * @throws ImplerException when some of given arguments are nulls.
     */
    private void checkLackOfNulls(Class<?> token, Path root) throws ImplerException {
        nonAssert(token == null, "Token must be not null!");
        nonAssert(root == null, "Root must be not null!");
    }

    /**
     * Generates the class implementation and writes it with given BufferedWriter.
     * @param token a type token to implement
     * @param className the name of the implementation class
     * @param writer buffered writer to save result
     * @throws IOException when can not write to the target file
     * @throws ImplerException when can not implement the class or the interface
     */
    private void generateClass(/*@NotNull*/ Class<?> token,
                                            String className,
            /*@NotNull*/ BufferedWriter writer) throws IOException, ImplerException {
        var constructor = getConstructor(token);
        writer.write(!token.getPackageName().isEmpty() ? "package " + token.getPackage().getName() + ";" + Constants.NEW_LINE : "");
        writer.write("@SuppressWarnings({\"unchecked\", \"deprecation\"})" + Constants.NEW_LINE);
        writer.write("public class " + className + " ");
        writer.write(token.isInterface() ? "implements " : "extends ");
        writer.write(token.getCanonicalName() + " {" + Constants.NEW_LINE);
        if (constructor != null) {
            writer.write(getConstructorString(constructor, className));
        } else {
            nonAssert(!token.isInterface(), "Classes must have at least one constructor!");
        }
        writer.write(getAllMethodsImplementations(token));
        writer.write("}");
    }

    /**
     * Generates formatted executable unit (method or constructor) with given header and expression
     * @param header the header of the executable
     * @param expression the expression of the executable
     * @return the generated unit source code
     */
//    @NotNull
//    @Contract(pure = true)
    private static String generateExecutable(String header, String expression) {
        return Constants.INDENT
                + header +
                Constants.BEGINNING_OF_EXECUTABLE
                + expression +
                Constants.ENDING_OF_EXECUTABLE;
    }

    /**
     * Generates constructor
     * @param constructor the constructor to implement
     * @param className the name of the current class
     * @return the source code for the constructor implementation
     */
//    @NotNull
    private String getConstructorString(Constructor<?> constructor, String className) {
        String header = "public " + className + getDeclarationSinceArguments(constructor);
        String superArguments = getArgumentString(constructor.getParameterCount(), i -> "arg" + i);
        String body = "super(" + superArguments + ");";
        return generateExecutable(header, body);
    }

    /**
     * Generates arguments by index and name generator
     * @param parameterCount the number of parameters
     * @param f the name generator
     * @return the generated string of parameters
     */
    private String getArgumentString(int parameterCount, IntFunction<String> f) {
        return IntStream
                .range(0, parameterCount)
                .mapToObj(f)
                .collect(Collectors.joining(", "));
    }

    /**
     * Generates declaration for executables (such as constructors or methods) since arguments list
     * @param callable the executable to generate the declaration part
     * @return the generated declaration part
     */
//    @NotNull
    private String getDeclarationSinceArguments(/*@NotNull*/ Executable callable) {
        return "(" + getParametersString(callable.getParameters()) + ")" +
                getExceptionsString(callable.getExceptionTypes());
    }

    /**
     * Generates source code for list of parameters
     * @param parameters the parameters to be included in in source code
     * @return the source code for the parameters list
     */
    private String getParametersString(/*@NotNull*/ Parameter[] parameters) {
        return getArgumentString(parameters.length, i -> parameters[i].getType().getCanonicalName() + " arg" + i);
    }

    /**
     * Generates source code for list of exceptions
     * @param exceptions the exceptions to be included in in source code
     * @return the source code for the exceptions list
     */
//    @NotNull
    private String getExceptionsString(/*@NotNull*/ Class<?>[] exceptions) {
        return exceptions.length == 0
                ? ""
                : " throws " + Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", "));
    }


    /**
     * Checks that condition is false.
     * If it is true throws {@link ImplerException} with given message.
     * @param condition the condition to check
     * @param message the message to show if condition is true
     * @throws ImplerException when condition is true
     */
//    @Contract("true, _ -> fail")
    private void nonAssert(boolean condition, String message) throws ImplerException {
        if (condition) {
            throw new ImplerException(message);
        }
    }

    /**
     * Checks if we can extend (implement) the given token.
     * @param token the token to check
     * @throws ImplerException when can not extend (implement) the given token.
     */
    private void checkSuperclass(/*@NotNull*/ Class<?> token) throws ImplerException {
        nonAssert(token.isPrimitive(), "Can not implement primitive type!");
        nonAssert(token.isArray(), "Can not implement array!");
        nonAssert(token.equals(Enum.class), "Can not implement enum!");
        nonAssert(Modifier.isFinal(token.getModifiers()), "Can not extend final class!");
        nonAssert(Modifier.isPrivate(token.getModifiers()), "Can not extend final class!");
    }


    /**
     * Finds constructor for the token.
     * @param token the token whose constructor to find.
     * @return the found constructor for the class token, {@code null} of an interface token.
     * @throws ImplerException when the token is a class, but no accessible constructors found.
     */
//    @Nullable
    private Constructor<?> getConstructor(/*@NotNull*/ Class<?> token) throws ImplerException {
        if (token.isInterface()) {
            return null;
        }
        Optional<Constructor<?>> constructor = hasAccessibleConstructors(token);
        nonAssert(constructor.isEmpty(), "Must be an interface or contain at least one not private constructor!");
        return constructor.get();
    }

    /**
     * Gets the predicate that checks if the executable has a specified modifier.
     * @param predicate predicate that checks if the executable has a specified modifier by given modifiers {@code int}.
     * @return the checking predicate.
     */
//    @NotNull
//    @Contract(pure = true)
    private static Predicate<Executable> checkModifier(/*@NotNull*/ IntPredicate predicate) {
        return executable -> predicate.test(executable.getModifiers());
    }

    /**
     * Checks if the token has any accessible (=non-private) constructors.
     * @param token the token whose constructor to find
     * @return any constructor if such one found
     */
//    @NotNull
    private Optional<Constructor<?>> hasAccessibleConstructors(/*@NotNull*/ Class<?> token) {
        return Arrays.stream(token.getDeclaredConstructors())
                .filter(checkModifier(Modifier::isPrivate).negate())
                .findAny();
    }


    /**
     * Generates the source code of abstract methods of the class token and all its subclasses.
     * @param token the source of abstract methods
     * @return the generated code
     */
    private String getAllMethodsImplementations(Class<?> token) {
        Set<MethodDataClass> methods = getAbstractMethodsFromSuperclasses(token);
        addMethodsToSet(token.getMethods(), methods);

        return methods.stream()
                .map(MethodDataClass::getMethod)
                .map(this::getMethodImplementation)
                .collect(Collectors.joining());
    }

    /**
     * Generates the implementation for the given method.
     * It returns the default value if the the method's return type is not {@code void}.
     * @param method the method to implement
     * @return the generated source code
     */
//    @NotNull
    private String getMethodImplementation(/*@NotNull */Method method) {
        Class<?> resultType = method.getReturnType();
        String modifiers = Modifier.toString(
                method.getModifiers()
                        & ~Modifier.ABSTRACT
                        & ~Modifier.TRANSIENT //NOT SERIALIZABLE
                        & ~Modifier.NATIVE
        );
        String header = modifiers + " " +
                resultType.getCanonicalName() + " " + method.getName()
                + getDeclarationSinceArguments(method);

        String returnValue = resultType.equals(void.class) ? ""
                : "return " +
                (resultType.equals(boolean.class) ? "false"
                        : resultType.isPrimitive() ? "0"
                        : "null") + ";";
        return generateExecutable(header, returnValue);
    }

    /**
     * Recursively adds the abstract methods from the class and all its superclasses to the set.
     * @param token the source of the methods
     * @return set of found methods
     */
//    @NotNull
    private Set<MethodDataClass> getAbstractMethodsFromSuperclasses(/*@NotNull */Class<?> token) {
//        assert !Modifier.isPrivate(token.getModifiers());
        Set<MethodDataClass> res = new HashSet<>();
        Class<?> cur = token;
        while (cur != null) {
            addMethodsToSet(cur.getDeclaredMethods(), res);
            cur = cur.getSuperclass();
        }
        return res;
    }

    /**
     * Adds methods from array to set of method wrappers.
     * @param declaredMethods the source array
     * @param res the destination set
     */
    private void addMethodsToSet(Method[] declaredMethods, /*@NotNull*/ Set<MethodDataClass> res) {
        Arrays.stream(declaredMethods)
                .filter(checkModifier(Modifier::isAbstract))
                .map(MethodDataClass::new)
                .forEach(res::add);
    }
}
/*
interface gg {
    int ggg();

}*/
