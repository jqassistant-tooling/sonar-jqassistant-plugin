package com.buschmais.jqassistant.scm.neo4jserver.test.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.support.PageFactory;

import com.buschmais.jqassistant.scm.neo4jserver.test.AbstractServerTest;
import com.buschmais.jqassistant.scm.neo4jserver.test.ui.pageobjects.MetricsPage;

/**
 * Test the metrics html page.
 */
public class MetricsViewIT extends AbstractUITest {

    /** This metric group is currently delivered by default. */
    private static final String METRIC_GROUP_ID_artifactDependencies = "metric:ArtifactDependencies";

    /** The metrics page. */
    private MetricsPage metricsPage;

    @Override
    protected String getWebPage() {
        return "metrics.html";
    }

    @Before
    public void setup() {

        metricsPage = PageFactory.initElements(driver, MetricsPage.class);
    }

    /**
     * This test proves if the metrics service successfully returns the metric groups.
     */
    @Test
    public void testGetMetricIds() {

        Set<String> ruleSetMetricGroupIds = ruleSet.getMetricGroups().keySet();
        assertFalse(ruleSetMetricGroupIds.isEmpty());

        Set<String> metricGroupIds = metricsPage.getMetricGroupIds();

        assertFalse(metricGroupIds.isEmpty());

        // check if the metric group ids of the rule set are available in the metrics page
        Set<String> missingGroupIds = new HashSet<>();
        for (String ruleSetMetricGroupId : ruleSetMetricGroupIds) {
            if (!metricGroupIds.contains(ruleSetMetricGroupId)) {
                missingGroupIds.add(ruleSetMetricGroupId);
            }
        }
        assertTrue("Missing groups from rule set: " + missingGroupIds, missingGroupIds.isEmpty());

        assertTrue(metricGroupIds.contains(METRIC_GROUP_ID_artifactDependencies));
    }

    /**
     * This test lets the metric service run a metric.
     *
     * @throws IOException
     */
    @Ignore("Will fail as the database is empty.")
    @Test
    public void testMetricGroupSelection() throws IOException {

        // let's scan something, we need some data
        scanClassPathDirectory(getClassesDirectory(AbstractServerTest.class));

        // this will run the first metric of the group
        metricsPage.selectMetricGroup(METRIC_GROUP_ID_artifactDependencies);

        // if running the metric succeeded, the metric ID field in the metric page is filled with the metric ID
        assertEquals("metric:TypesAndDependenciesPerArtifact", metricsPage.getCurrentMetricId());
    }
}