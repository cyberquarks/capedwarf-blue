/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.test.capedwarf.testsuite.mapreduce.test;

import java.util.Arrays;
import java.util.List;

import com.google.appengine.tools.mapreduce.Counter;
import com.google.appengine.tools.mapreduce.Counters;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.MapReduceJob;
import com.google.appengine.tools.mapreduce.MapReduceResult;
import com.google.appengine.tools.mapreduce.MapReduceSpecification;
import com.google.appengine.tools.mapreduce.Marshallers;
import com.google.appengine.tools.mapreduce.inputs.ConsecutiveLongInput;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.InMemoryOutput;
import com.google.appengine.tools.mapreduce.outputs.NoOutput;
import com.google.appengine.tools.mapreduce.reducers.NoReducer;
import com.google.appengine.tools.pipeline.JobInfo;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.capedwarf.common.support.All;
import org.jboss.test.capedwarf.testsuite.mapreduce.support.CountMapper;
import org.jboss.test.capedwarf.testsuite.mapreduce.support.CountReducer;
import org.jboss.test.capedwarf.testsuite.mapreduce.support.EntityCreator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@Category(All.class)
public class MapReduceTest extends MapReduceTestBase {
    @Deployment
    public static WebArchive getDeployment() {
        WebArchive war = getDefaultDeployment();
        war.addPackage(EntityCreator.class.getPackage());
        return war;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCountJob() throws Exception {
        List<String> payloads = Arrays.asList("capedwarf", "jboss", "redhat");
        int shardCount = 1;

        MapReduceSpecification.Builder builder = new MapReduceSpecification.Builder();
        builder.setJobName("Create MapReduce entities");
        builder.setInput(new ConsecutiveLongInput(0, payloads.size() * (long) shardCount, shardCount));
        builder.setMapper(new EntityCreator("MapReduceTest", payloads));
        builder.setKeyMarshaller(Marshallers.getVoidMarshaller());
        builder.setValueMarshaller(Marshallers.getVoidMarshaller());
        builder.setReducer(NoReducer.create());
        builder.setOutput(new NoOutput());

        final String createHandle = MapReduceJob.start(builder.build(), getSettings());

        JobInfo createJI = waitToFinish("CREATE", createHandle);
        Object create = createJI.getOutput();
        log.warning("----- Create: " + create);

        int mapShardCount = 1;

        builder = new MapReduceSpecification.Builder();
        builder.setJobName("MapReduceTest stats");
        builder.setInput(new DatastoreInput("MapReduceTest", mapShardCount));
        builder.setMapper(new CountMapper());
        builder.setKeyMarshaller(Marshallers.getStringMarshaller());
        builder.setValueMarshaller(Marshallers.getLongMarshaller());
        builder.setReducer(new CountReducer());
        builder.setOutput(new InMemoryOutput<KeyValue<String, Long>>());

        final String countHandle = MapReduceJob.start(builder.build(), getSettings());

        JobInfo countJI = waitToFinish("COUNT", countHandle);
        Object count = countJI.getOutput();
        log.warning("----- Count: " + count);

        Assert.assertTrue(count instanceof MapReduceResult);
        MapReduceResult result = MapReduceResult.class.cast(count);
        int[] chars = toChars(payloads);
        Counters counters = result.getCounters();
        for (int i = 0; i < chars.length; i++) {
            char ch = (char) ('a' + i);
            Counter c = counters.getCounter(CountMapper.toKey(ch));
            Assert.assertEquals(String.format("Invalid count for '%s'.", ch), chars[i], c.getValue());
        }
    }

    protected static int[] toChars(List<String> payloads) {
        int[] chars = new int['z' - 'a' + 1];
        for (String payload : payloads) {
            for (int i = 0; i < payload.length(); i++) {
                chars[payload.charAt(i) - 'a']++;
            }
        }
        return chars;
    }
}
