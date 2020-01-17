package org.jqassistant.contrib.sonarqube.plugin.sensor;

import com.buschmais.jqassistant.core.report.schema.v1.JqassistantReport;
import com.buschmais.jqassistant.core.shared.xml.JAXBUnmarshaller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class ReportReader {

    public static final String REPORT_NAMSESPACE_1_8 = "http://schema.jqassistant.org/report/v1.8";

    private static final ReportReader INSTANCE = new ReportReader();

    private final Map<String, String> namespaceMappings;

    private ReportReader() {
        namespaceMappings = new HashMap<>();
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.0", REPORT_NAMSESPACE_1_8);
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.2", REPORT_NAMSESPACE_1_8);
        namespaceMappings.put("http://www.buschmais.com/jqassistant/core/report/schema/v1.3", REPORT_NAMSESPACE_1_8);
    }

    public static ReportReader getInstance() {
        return INSTANCE;
    }

    public JqassistantReport read(File reportFile) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = new FileInputStream(reportFile)) {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            JAXBUnmarshaller<JqassistantReport> jaxbUnmarshaller = new JAXBUnmarshaller<>(JqassistantReport.class, namespaceMappings);
            return jaxbUnmarshaller.unmarshal(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read jQAssistant report from file " + reportFile, e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

}
