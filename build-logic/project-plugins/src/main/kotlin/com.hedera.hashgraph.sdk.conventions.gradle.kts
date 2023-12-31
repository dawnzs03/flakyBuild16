/*
 * Copyright (C) 2022-2023 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    id("java-library")
    id("com.hedera.hashgraph.java")
    id("com.adarshr.test-logger")
    id("com.gorylenko.gradle-git-properties")
}

group = "com.swirlds"

javaModuleDependencies { versionsFromConsistentResolution(":swirlds-platform-core") }

configurations.getByName("mainRuntimeClasspath") {
    extendsFrom(configurations.getByName("internal"))
}

gitProperties { keys = listOf("git.build.version", "git.commit.id", "git.commit.id.abbrev") }

testlogger {
    theme = ThemeType.MOCHA
    slowThreshold = 10000
    showStandardStreams = true
    showPassedStandardStreams = false
    showSkippedStandardStreams = false
    showFailedStandardStreams = true
}
