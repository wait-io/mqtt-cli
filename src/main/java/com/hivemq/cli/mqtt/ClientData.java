/*
 * Copyright 2019 HiveMQ and the HiveMQ Community
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
 *
 */
package com.hivemq.cli.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttTopic;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class ClientData {

    private LocalDateTime creationTime;
    private Set<MqttTopic> subscribedTopics;

    public ClientData(final @NotNull LocalDateTime creationTime) {
        this.creationTime = creationTime;
        subscribedTopics = new HashSet<>();
    }

    public void addSubscription(final @NotNull MqttTopic topic) {
        subscribedTopics.add(topic);
    }

    public boolean removeSubscription(final @NotNull MqttTopic topic) {
        return subscribedTopics.remove(topic);
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(final LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public Set<MqttTopic> getSubscribedTopics() {
        return subscribedTopics;
    }

    public void setSubscribedTopics(final Set<MqttTopic> subscribedTopics) {
        this.subscribedTopics = subscribedTopics;
    }
}
