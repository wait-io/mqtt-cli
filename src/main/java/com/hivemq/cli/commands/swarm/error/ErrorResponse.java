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
package com.hivemq.cli.commands.swarm.error;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Yannick Weber
 */
public class ErrorResponse {

    private final @NotNull List<Error> errors;

    public ErrorResponse(final @NotNull List<Error> errors) {
        this.errors = errors;
    }

    public @NotNull List<Error> getErrors() {
        return errors;
    }

}