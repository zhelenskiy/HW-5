package ru.ifmo.rain.zhelenskiy.implementor;

//import org.jetbrains.annotations.Contract;
//import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * The method wrapper that specifies equality ({@link MethodDataClass#equals(Object)}) and hash code ({@link MethodDataClass#hashCode()}) methods.
 * The necessary parameters to be equal for equality of wrappers:
 * <ul>
 *     <li>Method name;</li>
 *     <li>Parameters types.</li>
 * </ul>
 * The hash code implementation provides equality of hashes of wrappers if the wrappers are equal.
 * @author zhelenskiy
 * @version 1.0
 * @see Method
 */
public class MethodDataClass {
    private final Method method;

    /**
     * Combines hashes
     * @param hashes the hashes to combine
     * @return the result hash
     */
//    @Contract(pure = true)
    private int hash(/*@NotNull*/ int... hashes) {
        final int POLYNOMIAL_CONSTANT = 997;
        int newHash = 1;
        for (int givenHash : hashes) {
            newHash = newHash * POLYNOMIAL_CONSTANT + givenHash;
        }
        return newHash;
    }

    /**
     * Constructs method wrapper with the given wrapper
     * @param method the given wrapper
     */
    public MethodDataClass(Method method) {
        this.method = method;
    }

    /**
     * Checks if the method wrappers are equal as it is defined in documentation of {@link MethodDataClass}.
     * @param obj the wrapper to compare with
     * @return if this and obj are equal method wrappers
     */
//    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MethodDataClass) {
            MethodDataClass otherWrapper = (MethodDataClass) obj;
            return method.getName().equals(otherWrapper.method.getName()) &&
                    Arrays.equals(method.getParameterTypes(), otherWrapper.method.getParameterTypes());
        }
        return false;
    }

    /**
     * Generates the hash code that satisfies the conditions in documentation of {@link MethodDataClass}
     * @return the hash code of the method wrapper
     */
    @Override
    public int hashCode() {
        return hash(Arrays.hashCode(method.getParameterTypes()),
                method.getName().hashCode()/*,
                method.getModifiers(),
                method.getReturnType().hashCode()*/);
    }

    /**
     * Gives access to the underlying method
     * @return the underlying method
     */
    public Method getMethod() {
        return method;
    }
}