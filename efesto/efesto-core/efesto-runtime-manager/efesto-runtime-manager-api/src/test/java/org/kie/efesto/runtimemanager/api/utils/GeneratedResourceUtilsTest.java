/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.efesto.runtimemanager.api.utils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.kie.efesto.common.api.io.IndexFile;
import org.kie.efesto.common.api.model.FRI;
import org.kie.efesto.common.api.model.GeneratedExecutableResource;
import org.kie.efesto.common.api.model.GeneratedRedirectResource;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedResourceUtilsTest {

    @Test
    void getGeneratedExecutableResource() {
        FRI fri = new FRI("testmod", "test");
        Optional<GeneratedExecutableResource> retrieved = GeneratedResourceUtils.getGeneratedExecutableResource(fri, "test");
        assertThat(retrieved).isNotNull().isPresent();
        fri = new FRI("notestmod", "test");
        retrieved = GeneratedResourceUtils.getGeneratedExecutableResource(fri, "test");
        assertThat(retrieved).isNotNull().isNotPresent();
    }

    @Test
    void getGeneratedRedirectResourceFromFile() {
        FRI fri = new FRI("redirecttestmod", "test");
        Optional<GeneratedRedirectResource> retrieved = GeneratedResourceUtils.getGeneratedRedirectResource(fri,
                                                                                                            "test");
        assertThat(retrieved).isNotNull().isPresent();
        fri = new FRI("redirectnotestmod", "test");
        retrieved = GeneratedResourceUtils.getGeneratedRedirectResource(fri, "test");
        assertThat(retrieved).isNotNull().isNotPresent();
    }

    @Test
    void getGeneratedRedirectResourceFromJar() {
        ClassLoader originalClassLoader = addJarToClassLoader();
        FRI fri = new FRI("redirecttestmod", "testb");
        Optional<GeneratedRedirectResource> retrieved = GeneratedResourceUtils.getGeneratedRedirectResource(fri,
                                                                                                            "testb");
        assertThat(retrieved).isNotNull().isPresent();
        fri = new FRI("redirectnotestmod", "testb");
        retrieved = GeneratedResourceUtils.getGeneratedRedirectResource(fri, "testb");
        assertThat(retrieved).isNotNull().isNotPresent();
        restoreClassLoader(originalClassLoader);
    }

    @Test
    void getIndexFileFromFile() {
        Optional<IndexFile> retrieved = GeneratedResourceUtils.getIndexFile("test");
        assertThat(retrieved).isNotNull().isPresent();
    }

    @Test
    void getIndexFileFromJar() {
        ClassLoader originalClassLoader = addJarToClassLoader();
        Optional<IndexFile> retrieved = GeneratedResourceUtils.getIndexFile("testb");
        assertThat(retrieved).isNotNull().isPresent();
        IndexFile indexFile = retrieved.get();
        assertThat(indexFile.length()).isGreaterThan(0);
        restoreClassLoader(originalClassLoader);
    }

    private ClassLoader addJarToClassLoader() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        URL jarUrl = Thread.currentThread().getContextClassLoader().getResource("TestJar.jar");
        assertThat(jarUrl).isNotNull();
        URL fileUrl = Thread.currentThread().getContextClassLoader().getResource("IndexFile.testb_json");
        assertThat(fileUrl).isNull();
        URL[] urls = {jarUrl};
        URLClassLoader testClassLoader = URLClassLoader.newInstance(urls, originalClassLoader);
        Thread.currentThread().setContextClassLoader(testClassLoader);
        return originalClassLoader;
    }

    private void restoreClassLoader(ClassLoader toRestore) {
        Thread.currentThread().setContextClassLoader(toRestore);
    }
}