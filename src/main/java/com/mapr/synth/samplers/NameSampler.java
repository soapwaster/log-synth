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
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import org.apache.mahout.math.random.Multinomial;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sample from US names.
 * <p>
 * See http://www.census.gov/genealogy/www/data/1990surnames/names_files.html for data.
 *
 * Thread safe
 */
public class NameSampler extends FieldSampler {
    public enum Type {FIRST, LAST, FIRST_LAST, LAST_FIRST}

    private static AtomicReference<Multinomial<String>> first = new AtomicReference<>(null);
    private static AtomicReference<Multinomial<String>> last = new AtomicReference<>(null);

    private Type type = Type.FIRST_LAST;

    @SuppressWarnings("WeakerAccess")
    public NameSampler() {
        try {
            if (first.compareAndSet(null, new Multinomial<>())) {
                Preconditions.checkState(last.getAndSet(new Multinomial<>()) == null);

                Splitter onTab = Splitter.on(CharMatcher.whitespace())
                        .omitEmptyStrings().trimResults(CharMatcher.anyOf(" \""));
                for (String resourceName : ImmutableList.of("dist.male.first", "dist.female.first")) {
                    //noinspection UnstableApiUsage
                    for (String line : Resources.readLines(Resources.getResource(resourceName), Charsets.UTF_8)) {
                        if (!line.startsWith("#")) {
                            Iterator<String> parts = onTab.split(line).iterator();
                            String name = initialCap(parts.next());
                            double weight = Double.parseDouble(parts.next());
                            if (first.get().getWeight(name) == 0) {
                                first.get().add(name, weight);
                            } else {
                                // do this instead of add because some first names may appear more than once
                                first.get().set(name, first.get().getWeight(name) + weight);
                            }
                        }
                    }
                }

                //noinspection UnstableApiUsage
                for (String line : Resources.readLines(Resources.getResource("dist.all.last"), Charsets.UTF_8)) {
                    if (!line.startsWith("#")) {
                        Iterator<String> parts = onTab.split(line).iterator();
                        String name = initialCap(parts.next());
                        double weight = Double.parseDouble(parts.next());
                        last.get().add(name, weight);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read built-in resource file", e);
        }
    }

    public NameSampler(Type type) {
        this();
        setTypeRaw(type);
    }

    private String initialCap(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    @Override
    public JsonNode doSample() {
      synchronized (this) {
        switch (type) {
            case FIRST:
                return new TextNode(first.get().sample());
            case LAST:
                return new TextNode(last.get().sample());
            case FIRST_LAST:
                return new TextNode(first.get().sample() + " " + last.get().sample());
            case LAST_FIRST:
                return new TextNode(last.get().sample() + ", " + first.get().sample());
        }
      }
      // can't happen
        return null;
    }

    @SuppressWarnings("WeakerAccess")
    public void setTypeRaw(Type type) {
        this.type = type;
    }

    public void setType(String type) {
        setTypeRaw(Type.valueOf(type.toUpperCase()));
    }
}
