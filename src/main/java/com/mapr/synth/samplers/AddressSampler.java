/*
 * Licensed to the Ted Dunning under one or more contributor license
 * agreements.  See the NOTICE file that may be
 * distributed with this work for additional information
 * regarding copyright ownership.  Ted Dunning licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.mapr.synth.samplers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Sample kind of plausible addresses
 *
 * Thread safe
 */
class AddressSampler extends FieldSampler {

    private final StreetNameSampler street;
    private final ForeignKeySampler number;

    public AddressSampler() {
        street = new StreetNameSampler();
        number = new ForeignKeySampler(100000, 0.5);
    }

    @Override
    public JsonNode doSample() {
        return new TextNode(number.doSample().asInt() + " " + street.doSample().asText());
    }
}
