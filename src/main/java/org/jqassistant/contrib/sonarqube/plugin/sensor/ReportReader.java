package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.shared.xml.JAXBUnmarshaller;
import org.jqassistant.schema.report.v1.JqassistantReport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ReportReader {

    public static final String REPORT_NAMESPACE_1_8 = "http://schema.jqassistant.org/report/v1.8";

    public static ReportReader getInstance() {
        return INSTANCE;
    }

    private static final ReportReader INSTANCE = new ReportReader();

    private JAXBUnmarshaller<JqassistantReport> jaxbUnmarshaller = new JAXBUnmarshaller<>(JqassistantReport.class, REPORT_NAMESPACE_1_8);

    public JqassistantReport read(File reportFile) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = new FileInputStream(reportFile)) {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            return jaxbUnmarshaller.unmarshal(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read jQAssistant report from file " + reportFile, e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

}
