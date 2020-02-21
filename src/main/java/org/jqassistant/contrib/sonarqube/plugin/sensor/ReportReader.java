package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.sun.xml.bind.api.JAXBRIContext;
import lombok.extern.slf4j.Slf4j;
import org.jqassistant.schema.report.v1.JqassistantReport;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Reads jQA report files using JAXB.
 */
@Slf4j
public final class ReportReader {

    public static ReportReader getInstance() {
        return INSTANCE;
    }

    private static final ReportReader INSTANCE = new ReportReader();

    private String targetNamespace;

    private XMLInputFactory inputFactory;

    private JAXBContext jaxbContext;

    private ReportReader() {
        inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        jaxbContext = withPluginClassLoader(() -> {
            try {
                return JAXBContext.newInstance(JqassistantReport.class);
            } catch (JAXBException e) {
                throw new IllegalStateException("Cannot create JAXB context for " + JqassistantReport.class.getName(), e);
            }
        });
        this.targetNamespace = getTargetNamespace(jaxbContext);
    }

    /**
     * Read the report file.
     *
     * @param reportFile The report file.
     * @return The {@link JqassistantReport}.
     */
    public JqassistantReport read(File reportFile) {
        return withPluginClassLoader(() -> {
            try (InputStream inputStream = new FileInputStream(reportFile)) {
                return unmarshal(inputStream);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read jQAssistant report from file " + reportFile, e);
            }
        });
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

    /**
     * Determines the target namespace from the root element registered in the {@link JAXBContext}.
     *
     * @param jaxbContext The {@link JAXBContext}.
     * @return The target namespace.
     */
    private String getTargetNamespace(JAXBContext jaxbContext) {
        if (jaxbContext instanceof JAXBRIContext) {
            try {
                return ((JAXBRIContext) this.jaxbContext).getElementName(JqassistantReport.class).getNamespaceURI();
            } catch (JAXBException e) {
                throw new IllegalStateException("Cannot determine XML element name for " + JqassistantReport.class.getName());
            }
        }
        throw new IllegalStateException("Expecting JAXBContext to be of type " + JAXBRIContext.class.getName() + " but got " + jaxbContext.getClass().getName());
    }

    private JqassistantReport unmarshal(InputStream stream) {
        try {
            XMLStreamReader xmlStreamReader = new NamespaceMappingStreamReader(inputFactory.createXMLStreamReader(stream), targetNamespace);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return unmarshaller.unmarshal(xmlStreamReader, JqassistantReport.class).getValue();
        } catch (XMLStreamException | JAXBException e) {
            throw new IllegalStateException("Cannot read XML document.", e);
        }
    }

    /**
     * A {@link StreamReaderDelegate} which maps all namespaces from a document to the
     * specified target namespace.
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
