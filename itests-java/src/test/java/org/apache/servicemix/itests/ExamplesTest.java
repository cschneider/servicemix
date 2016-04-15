/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.itests;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;

import javax.inject.Inject;

import org.apache.karaf.features.FeaturesService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
public class ExamplesTest {
    @Inject
    FeaturesService featuresService;
    
    @Inject
    BundleContext context;

    @Test
    public void testExampleActiveMQ() throws Exception {
        LogCollector collector = new LogCollector(context);
        featuresService.installFeature("examples-activemq-camel-blueprint");
        collector.expectContains("ActiveMQ-Blueprint-Example set body");
    }
    
    @Test
    public void testActivity() throws Exception {
        LogCollector collector = new LogCollector(context);
        HashSet<String> features = new HashSet<>(Arrays.asList("transaction", "activiti", "examples-activiti-camel"));
        featuresService.installFeatures(features, EnumSet.noneOf(FeaturesService.Option.class));
        collector.expectContains("route4 started and consuming");
        Files.copy(stream("Some nice order message goes here"), 
                   new File("var/activiti-camel/order/001").toPath());
        
        collector.expectContains("Processing order");

        Files.copy(stream("Some nice delicery message goes here"), 
                   new File("var/activiti-camel/delivery/001").toPath());
        collector.expectContains("Processing delivery for order");

    }

    private InputStream stream(String content) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(content.getBytes("UTF-8"));
    }
    
    @Configuration
    public Option[] config() {
        String LOCAL_REPOSITORY = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.servicemix").artifactId("apache-servicemix").type("zip").versionAsInProject();

        return new Option[]{
            // KarafDistributionOption.debugConfiguration("8889", true),
            karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf")
                .unpackDirectory(new File("target/exam"))
                .useDeployFolder(false),
            // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
            configureSecurity().disableKarafMBeanServerBuilder(),
            keepRuntimeFolder(),
            logLevel(LogLevel.INFO),
            when(null != LOCAL_REPOSITORY && LOCAL_REPOSITORY.length() > 0)
            .useOptions(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.localRepository", LOCAL_REPOSITORY))
        };
    }
}
