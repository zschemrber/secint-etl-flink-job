/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.atlassian.Deserialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.atlassian.pojo.Message;

import java.io.IOException;

/** Utility for deserializing JSON. */
public class JsonDeserialization {

    // JavaTimeModule is needed for Java 8 data time (Instant) support
    private static final ObjectMapper OBJECT_MAPPER =
            JsonMapper.builder().build().registerModule(new JavaTimeModule());

    public static Message deserialize(String message) throws IOException {
        return OBJECT_MAPPER.readValue(message, Message.class);
    }
}
