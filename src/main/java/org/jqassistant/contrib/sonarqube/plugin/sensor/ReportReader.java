package org.jqassistant.contrib.sonarqube.plugin.sensor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import com.sun.xml.bind.api.JAXBRIContext;
import lombok.extern.slf4j.Slf4j;
import org.jqassistant.schema.report.v1.JqassistantReport;
import org.sonar.api.scanner.ScannerSide;

/**
 * Reads jQA report files using JAXB.
 */
@Slf4j
@ScannerSide
public final class ReportReader {

    public static ReportReader getInstance() {
        return INSTANCE;
    }

    private static final ReportReader INSTANCE = new ReportReader();

    private final XMLInputFactory inputFactory;

    private final JAXBContext jaxbContext;

    private final Map<File, JqassistantReport> reports = new HashMap<>();

    public ReportReader() {
        inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        jaxbContext = withPluginClassLoader(() -> {
            try {
                return JAXBContext.newInstance(JqassistantReport.class);
            } catch (JAXBException e) {
                throw new IllegalStateException("Cannot create JAXB context for " + JqassistantReport.class.getName(), e);
            }
        });
    }

    /**
     * Read the report file.
     *
     * @param reportFile
     *            The report file.
     * @return The {@link JqassistantReport}.
     */
    public JqassistantReport read(File reportFile) {
        return reports.computeIfAbsent(reportFile, file -> withPluginClassLoader(() -> {
            log.info("Reading report '{}'.", file);
            try (InputStream inputStream = new FileInputStream(file)) {
                return unmarshal(inputStream);
            } catch (IOException | XMLStreamException | JAXBException e) {
                throw new IllegalStateException("Cannot read jQAssistant report from file " + file, e);
            }
        }));
    }

    private <T> T withPluginClassLoader(Supplier<T> supplier) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader pluginClassLoader = this.getClass().getClassLoader();
        try {
            // TCCL must be set for JAXB and Java 11
            Thread.currentThread().setContextClassLoader(pluginClassLoader);
            return supplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private JqassistantReport unmarshal(InputStream stream) throws JAXBException, XMLStreamException {
        XMLStreamReader xmlStreamReader = new NamespaceMappingStreamReader(inputFactory.createXMLStreamReader(stream), getTargetNamespace());
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(xmlStreamReader, JqassistantReport.class).getValue();
    }

    /**
     * Determines the target namespace from the root element registered in the
     * {@link JAXBContext}.
     *
     * @return The target namespace.
     * @throws JAXBException
     *             If the root element cannot be determined.
     */
    private String getTargetNamespace() throws JAXBException {
        return ((JAXBRIContext) this.jaxbContext).getElementName(JqassistantReport.class).getNamespaceURI();
    }

    /**
     * A {@link StreamReaderDelegate} which maps all namespaces from a document to
     * the specified target namespace.
     */
    private static class NamespaceMappingStreamReader extends StreamReaderDelegate {

        private String targetNamespace;

        NamespaceMappingStreamReader(XMLStreamReader reader, String targetNamespace) {
            super(reader);
            this.targetNamespace = targetNamespace;
        }

        @Override
        public String getNamespaceURI() {
            return targetNamespace;
        }
    }
}
