package com.ocft.gateway.openapi.admin;

import java.io.IOException;

/**
 * A service dedicated to retrieving source code of classes from the classpath.
 */
public interface SourceCodeService {

    /**
     * Finds and reads the source code of a given class name.
     *
     * @param className The fully qualified name of the class.
     * @return The source code as a String.
     * @throws IOException If the source file cannot be found or read.
     */
    String getSourceCode(String className) throws IOException;
}
