/*
 * Copyright 2019-present HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.cli.commands.cli;

import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import com.hivemq.cli.MqttCLIMain;
import com.hivemq.testcontainer.junit5.HiveMQTestContainerExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

@Disabled("Tests are only used to check output")
class TestBrokerCommandDefaultTest {

    static final @NotNull HiveMQTestContainerExtension hivemq =
            new HiveMQTestContainerExtension(DockerImageName.parse("hivemq/hivemq4").withTag("4.4.0"));

    @BeforeAll
    static void beforeAll() {
        hivemq.start();
    }

    @AfterAll
    static void afterAll() {
        hivemq.stop();
    }

    @Test
    @ExpectSystemExitWithStatus(0)
    void mqtt3_failed_connect() {
        MqttCLIMain.main(new String[]{"test", "-V", "3"});
    }

    @Test
    @ExpectSystemExitWithStatus(0)
    void mqtt5_failed_connect() {
        MqttCLIMain.main(new String[]{"test", "-V", "5"});
    }

    @Test
    @ExpectSystemExitWithStatus(0)
    void mqtt3_features() {
        MqttCLIMain.main(new String[]{"test", "-V", "3", "-p", String.valueOf(hivemq.getMqttPort())});
    }

    @Test
    @ExpectSystemExitWithStatus(0)
    void mqtt5_features() {
        MqttCLIMain.main(new String[]{"test", "-V", "5", "-a", "-p", String.valueOf(hivemq.getMqttPort())});
    }
}