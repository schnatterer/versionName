/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 TRIOLOGY GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.triology.versionname;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static de.triology.versionname.VersionNames.DEFAULT_MANIFEST_ATTRIBUTE;
import static de.triology.versionname.VersionNames.DEFAULT_MANIFEST_PATH;
import static de.triology.versionname.VersionNames.DEFAULT_PROPERTIES_FILE_PATH;
import static de.triology.versionname.VersionNames.DEFAULT_PROPERTY;
import static de.triology.versionname.VersionNames.VERSION_STRING_ON_ERROR;
import static de.triology.versionname.VersionNames.VersionName.LOG_EXCEPTION_ON_CLOSE;
import static de.triology.versionname.VersionNames.VersionName.LOG_EXCEPTION_READING_FROM_RESOURCE;
import static de.triology.versionname.VersionNames.VersionName.LOG_KEY_NULL;
import static de.triology.versionname.VersionNames.VersionName.LOG_NOT_FOUND_IN_RESOURCE;
import static de.triology.versionname.VersionNames.VersionName.LOG_RESOURCE_NOT_FOUND;
import static de.triology.versionname.VersionNames.VersionName.LOG_RESOURCE_PATH_NULL;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link VersionNames}.
 */
public class VersionNamesTest {

    /**
     * Allows for mocking behavior of the static methods of {@link VersionNames}.
     */
    private ClassLoader classLoader = mock(ClassLoader.class);

    /**
     * Logger of class under test.
     */
    private static final TestLogger LOG = TestLoggerFactory.getTestLogger(VersionNames.class);

    /**
     * Rest logger before each test.
     **/
    @Rule public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

    @Before
    public void setUp() throws Exception {
        // Inject mocked class into static field
        setVersionNamesClassLoader(classLoader);
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromProperties(String, String)}, for specific properties file and property.
     */
    @Test
    public void testGetVersionNameFromPropertiesNonDefault() throws Exception {
        String expectedPath = "path";
        String expectedProperty = "someOtherProperty";
        String expectedVersionName = "42";
        ByteArrayInputStream resourceStream =
            new ByteArrayInputStream((expectedProperty + "=" + expectedVersionName).getBytes());
        when(classLoader.getResourceAsStream(expectedPath)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromProperties(expectedPath, expectedProperty);

        // Assertions
        assertEquals("Unexpected version name", expectedVersionName, actualVersionName);
        verify(classLoader).getResourceAsStream(expectedPath);
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromProperties(String, String)}, where the property parameter is
     * <code>null</code>
     */
    @Test
    public void testGetVersionNameFromPropertiesPropertyNull() throws Exception {
        String expectedPath = "path";
        ByteArrayInputStream resourceStream = new ByteArrayInputStream(("someProperty=42").getBytes());
        when(classLoader.getResourceAsStream(expectedPath)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromProperties(expectedPath, null);

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_KEY_NULL, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.ERROR, logEvent.getLevel());
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromProperties(String, String)}, where the path parameter is
     * <code>null</code>
     */
    @Test
    public void testGetVersionNameFromPropertiesPathNull() throws Exception {
        // Use real class loader
        setVersionNamesClassLoader(VersionNames.class.getClassLoader());

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromProperties(null, "someProperty");

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_RESOURCE_PATH_NULL, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.ERROR, logEvent.getLevel());
    }

    /**
     * Positive test for {@link VersionNames#getVersionNameFromProperties(String, String)}.
     */
    @Test
    public void testGetVersionNameFromPropertiesResource() {
        String expectedVersionName = "42L";
        ByteArrayInputStream resourceStream =
            new ByteArrayInputStream((DEFAULT_PROPERTY + "=" + expectedVersionName).getBytes());
        when(classLoader.getResourceAsStream(DEFAULT_PROPERTIES_FILE_PATH)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromProperties();

        // Assertions
        assertEquals("Unexpected version name", expectedVersionName, actualVersionName);
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromProperties(String, String)}, where the properties file is
     * <code>null</code>.
     */
    @Test
    public void testGetVersionNameFromPropertiesResourceNull() throws Exception {
        when(classLoader.getResourceAsStream(DEFAULT_PROPERTIES_FILE_PATH)).thenReturn(null);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromProperties();

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_RESOURCE_NOT_FOUND, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.ERROR, logEvent.getLevel());
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromProperties(String, String)}, when an IOException occurs when
     * loading the properties.
     */
    @Test
    public void testGetVersionNameFromPropertiesResourceIOExceptionProperties() throws Exception {
        InputStream resourceStream = mock(InputStream.class, new ThrowIOExceptionOnEachMethodCall());
        when(classLoader.getResourceAsStream(DEFAULT_PROPERTIES_FILE_PATH)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromProperties();

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_EXCEPTION_READING_FROM_RESOURCE, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.ERROR, logEvent.getLevel());
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromProperties(String, String)}, where the property is
     * <code>null</code>.
     */
    @Test
    public void testGetVersionNameFromPropertiesResourcePropertyNull() throws Exception {
        ByteArrayInputStream resourceStream = new ByteArrayInputStream("someOtherProperty=42L".getBytes());
        when(classLoader.getResourceAsStream(DEFAULT_PROPERTIES_FILE_PATH)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromProperties();

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_NOT_FOUND_IN_RESOURCE, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.ERROR, logEvent.getLevel());
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromProperties(String, String)}, when an IOException occurs when
     * closing the resource stream.
     */
    @Test
    public void testGetVersionNameFromPropertiesResourceIOExceptionOnClose() throws Exception {
        String expectedVersionName = "42L";
        ByteArrayInputStream resourceStream =
            new ByteArrayInputStream((DEFAULT_PROPERTY + "=" + expectedVersionName).getBytes());
        when(classLoader.getResourceAsStream(DEFAULT_PROPERTIES_FILE_PATH)).thenReturn(resourceStream);

        InputStream resourceStreamSpy = spy(resourceStream);
        doThrow(new IOException("Mocked exception")).when(resourceStreamSpy).close();
        when(classLoader.getResourceAsStream(DEFAULT_PROPERTIES_FILE_PATH)).thenReturn(resourceStreamSpy);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromProperties();

        // Assertions
        assertEquals("Unexpected version name", expectedVersionName, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_EXCEPTION_ON_CLOSE, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.WARN, logEvent.getLevel());
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromManifest(String, String)}.
     */
    @Test
    public void testGetVersionNameFromManifestNonDefault() throws Exception {
        String expectedPath = "path";
        String expectedAttribute = "someOtherAttribute";
        String expectedVersionName = "42";
        ByteArrayInputStream resourceStream = createManifest(expectedAttribute, expectedVersionName);
        when(classLoader.getResourceAsStream(expectedPath)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromManifest(expectedPath, expectedAttribute);

        // Assertions
        assertEquals("Unexpected version name", expectedVersionName, actualVersionName);
        verify(classLoader).getResourceAsStream(expectedPath);
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromManifest(String, String)}, where the attribute parameter is
     * <code>null</code>
     */
    @Test
    public void testGetVersionNameFromManifestParameterAttributeNull() throws Exception {
        String expectedPath = "path";
        ByteArrayInputStream resourceStream = createManifest("someAttribute", "42");
        when(classLoader.getResourceAsStream(expectedPath)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromManifest(expectedPath, null);

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_KEY_NULL, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.ERROR, logEvent.getLevel());
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromManifest(String, String)}, where the path parameter is
     * <code>null</code>
     */
    @Test
    public void testGetVersionNameFromManifestParameterPathNull() throws Exception {
        // Use real class loader
        setVersionNamesClassLoader(VersionNames.class.getClassLoader());

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromManifest(null, "someProperty");

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_RESOURCE_PATH_NULL, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.ERROR, logEvent.getLevel());
    }

    /**
     * Positive test for {@link VersionNames#getVersionNameFromManifest()}.
     */
    @Test
    public void testGetVersionNameFromManifest() throws Exception {
        String expectedVersionName = "42";
        ByteArrayInputStream resourceStream = createManifest(DEFAULT_MANIFEST_ATTRIBUTE, expectedVersionName);
        when(classLoader.getResourceAsStream(DEFAULT_MANIFEST_PATH)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromManifest();

        // Assertions
        assertEquals("Unexpected version name", expectedVersionName, actualVersionName);
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromManifest()}., where the manifest file is
     * <code>null</code>.
     */
    @Test
    public void testGetVersionNameFromManifestResourceNull() throws Exception {
        when(classLoader.getResourceAsStream(DEFAULT_MANIFEST_PATH)).thenReturn(null);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromManifest();

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_RESOURCE_NOT_FOUND, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.ERROR, logEvent.getLevel());
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromManifest()}., when an IOException occurs when
     * creating the manifest.
     */
    @Test
    public void testGetVersionNameFromManifestIOExceptionManifest() throws Exception {
        InputStream resourceStream = mock(InputStream.class, new ThrowIOExceptionOnEachMethodCall());
        when(classLoader.getResourceAsStream(DEFAULT_MANIFEST_PATH)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromManifest();

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_EXCEPTION_READING_FROM_RESOURCE, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.ERROR, logEvent.getLevel());
    }

    /**
     * Test for {@link VersionNames#getVersionNameFromManifest()}., the attribute is <code>null</code>
     */
    @Test
    public void testGetVersionNameFromManifestAttributeNull() throws Exception {
        ByteArrayInputStream resourceStream = createManifest("someOtherAttribute", "42");
        when(classLoader.getResourceAsStream(DEFAULT_MANIFEST_PATH)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromManifest();

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_NOT_FOUND_IN_RESOURCE, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.ERROR, logEvent.getLevel());
    }

    /**
     * Test for{@link VersionNames#getVersionNameFromManifest()}., when an IOException occurs when
     * closing the resource stream.
     */
    @Test
    public void testGetVersionNameFromManifestIOExceptionOnClose() throws Exception {
        InputStream resourceStream = mock(InputStream.class);
        doThrow(new IOException("Mocked exception")).when(resourceStream).close();
        when(classLoader.getResourceAsStream(DEFAULT_MANIFEST_PATH)).thenReturn(resourceStream);

        // Call method under test
        String actualVersionName = VersionNames.getVersionNameFromManifest();

        // Assertions
        assertEquals("Unexpected version name", VERSION_STRING_ON_ERROR, actualVersionName);
        LoggingEvent logEvent = getLogEvent(0);
        assertEquals("Unexpected log message", LOG_EXCEPTION_ON_CLOSE, logEvent.getMessage());
        assertEquals("Unexpected log level", Level.WARN, logEvent.getLevel());
    }

    /**
     * @return a {@link Manifest} as {@link ByteArrayInputStream}
     */
    private ByteArrayInputStream createManifest(String expectedAttribute, String expectedVersionName)
        throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name(expectedAttribute), expectedVersionName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        manifest.write(out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Allows for replacing {@link VersionNames}' clazz field by a mock.
     */
    private void setVersionNamesClassLoader(ClassLoader classLoader) throws Exception {
        Field field = VersionNames.class.getDeclaredField("classLoader");
        field.setAccessible(true);

        field.set(null, classLoader);
    }

    /**
     * @return the logging event at <code>index</code>
     */
    public LoggingEvent getLogEvent(int index) {
        assertThat("Unexpected number of Log messages", LOG.getLoggingEvents().size(), greaterThan(index));
        return LOG.getLoggingEvents().get(index);
    }

    /**
     * Answer that makes mock throw an {@link IOException} on each method call.
     */
    private static class ThrowIOExceptionOnEachMethodCall implements Answer {
        public Object answer(InvocationOnMock invocation) throws Throwable {
            throw new IOException("Mocked Exception");
        }
    }
}