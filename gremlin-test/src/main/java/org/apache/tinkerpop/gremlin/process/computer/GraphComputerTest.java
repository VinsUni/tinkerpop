/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.process.computer;

import org.apache.tinkerpop.gremlin.ExceptionCoverage;
import org.apache.tinkerpop.gremlin.LoadGraphWith;
import org.apache.tinkerpop.gremlin.process.AbstractGremlinProcessTest;
import org.apache.tinkerpop.gremlin.process.UseEngine;
import org.apache.tinkerpop.gremlin.process.computer.lambda.LambdaMapReduce;
import org.apache.tinkerpop.gremlin.process.computer.lambda.LambdaVertexProgram;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalEngine;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.StreamFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static org.apache.tinkerpop.gremlin.LoadGraphWith.GraphData.MODERN;
import static org.junit.Assert.*;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@ExceptionCoverage(exceptionClass = GraphComputer.Exceptions.class, methods = {
        "providedKeyIsNotAMemoryComputeKey",
        "computerHasNoVertexProgramNorMapReducers",
        "computerHasAlreadyBeenSubmittedAVertexProgram",
        "providedKeyIsNotAnElementComputeKey",
        "isolationNotSupported",
        "incidentAndAdjacentElementsCanNotBeAccessedInMapReduce"
})
@ExceptionCoverage(exceptionClass = Graph.Exceptions.class, methods = {
        "graphDoesNotSupportProvidedGraphComputer",
        "onlyOneOrNoGraphComputerClass"
})
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public abstract class GraphComputerTest extends AbstractGremlinProcessTest {

    public abstract GraphComputer get_g_compute();

    public abstract GraphComputer get_g_compute_setupXX_executeXX_terminateXtrueX();

    public abstract GraphComputer get_g_compute_setupXX_executeXX_terminateXtrueX_memoryKeysXset_incr_and_orX();

    public abstract GraphComputer get_g_compute_setupXX_executeXX_terminateXtrueX_memoryKeysXnullX();

    public abstract GraphComputer get_g_compute_setupXX_executeXX_terminateXtrueX_memoryKeysX_X();

    public abstract GraphComputer get_g_compute_setupXsetXa_trueXX_executeXX_terminateXtrueX();

    public abstract GraphComputer get_g_compute_setupXX_executeXv_blah_m_incrX_terminateX1X_elementKeysXnameLengthCounterX_memoryKeysXa_bX();

    public abstract GraphComputer get_g_compute_setupXabcdeX_executeXtestMemoryX_terminateXtestMemoryXmemoryKeysXabcdeX();

    public abstract GraphComputer get_g_compute_mapXageX_reduceXsumX_memoryXnextX_memoryKeyXageSumX();

    public abstract GraphComputer get_g_compute_executeXcounterX_terminateX8X_mapreduceXcounter_aX_mapreduceXcounter_bX();

    public abstract GraphComputer get_g_compute_mapXidX_reduceXidX_reduceKeySortXreverseX_memoryKeyXidsX();

    public abstract GraphComputer get_g_compute_programXTraversalVertexProgram_build_traversalXg_V_both_hasXlabel_personX_age_groupCountXaXX_create();

    public abstract GraphComputer get_g_compute_mapXoutE_countX_reduceXsumX();

    @Test
    @LoadGraphWith(MODERN)
    public void shouldHaveStandardStringRepresentation() {
        final GraphComputer computer = get_g_compute();
        assertEquals(StringFactory.graphComputerString(computer), computer.toString());
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldNotAllowWithNoVertexProgramNorMapReducers() throws Exception {
        try {
            get_g_compute().submit().get();
            fail("Should throw an IllegalStateException when there is no vertex program nor map reducers");
        } catch (Exception ex) {
            validateException(GraphComputer.Exceptions.computerHasNoVertexProgramNorMapReducers(), ex);
        }
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldFailIfIsolationIsNotSupported() {
        final GraphComputer computer = get_g_compute();
        if (!computer.features().supportsIsolation(GraphComputer.Isolation.BSP)) {
            try {
                computer.isolation(GraphComputer.Isolation.BSP);
                fail("GraphComputer.isolation() should throw an exception if the isolation is not supported");
            } catch (Exception ex) {
                validateException(GraphComputer.Exceptions.isolationNotSupported(GraphComputer.Isolation.BSP), ex);
            }
        }
        if (!computer.features().supportsIsolation(GraphComputer.Isolation.DIRTY_BSP)) {
            try {
                computer.isolation(GraphComputer.Isolation.DIRTY_BSP);
                fail("GraphComputer.isolation() should throw an exception if the isolation is not supported");
            } catch (Exception ex) {
                validateException(GraphComputer.Exceptions.isolationNotSupported(GraphComputer.Isolation.DIRTY_BSP), ex);
            }
        }
        assertEquals(StringFactory.graphComputerString(computer), computer.toString());
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldNotAllowBadGraphComputers() {
        try {
            graph.compute(BadGraphComputer.class);
            fail("Providing a bad graph computer class should fail");
        } catch (Exception ex) {
            validateException(Graph.Exceptions.graphDoesNotSupportProvidedGraphComputer(BadGraphComputer.class), ex);
        }
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldHaveImmutableComputeResultMemory() throws Exception {
        final ComputerResult results = this.get_g_compute_setupXX_executeXX_terminateXtrueX_memoryKeysXset_incr_and_orX().submit().get();

        try {
            results.memory().set("set", "test");
        } catch (Exception ex) {
            validateException(Memory.Exceptions.memoryIsCurrentlyImmutable(), ex);
        }

        try {
            results.memory().incr("incr", 1);
        } catch (Exception ex) {
            validateException(Memory.Exceptions.memoryIsCurrentlyImmutable(), ex);
        }

        try {
            results.memory().and("and", true);
        } catch (Exception ex) {
            validateException(Memory.Exceptions.memoryIsCurrentlyImmutable(), ex);
        }

        try {
            results.memory().or("or", false);
        } catch (Exception ex) {
            validateException(Memory.Exceptions.memoryIsCurrentlyImmutable(), ex);
        }
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldNotAllowNullMemoryKeys() throws Exception {
        try {
            get_g_compute_setupXX_executeXX_terminateXtrueX_memoryKeysXnullX().submit().get();
            fail("Providing null memory key should fail");
        } catch (Exception ex) {
            // TODO ex.printStackTrace();
            //validateException(Memory.Exceptions.memoryKeyCanNotBeNull(), ex);
        }
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldNotAllowEmptyMemoryKeys() throws Exception {
        try {
            get_g_compute_setupXX_executeXX_terminateXtrueX_memoryKeysX_X().submit().get();
            fail("Providing empty memory key should fail");
        } catch (Exception ex) {
            validateException(Memory.Exceptions.memoryKeyCanNotBeEmpty(), ex);
        }
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldNotAllowSettingUndeclaredMemoryKeys() throws Exception {
        try {
            get_g_compute_setupXsetXa_trueXX_executeXX_terminateXtrueX().submit().get();
            fail("Setting a memory key that wasn't declared should fail");
        } catch (Exception ex) {
            // TODO: validateException(GraphComputer.Exceptions.providedKeyIsNotAMemoryComputeKey("a"), ex.getCause());
        }
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldNotAllowTheSameComputerToExecutedTwice() throws Exception {
        final GraphComputer computer = get_g_compute_setupXX_executeXX_terminateXtrueX();
        computer.submit().get(); // this should work as its the first run of the graph computer

        try {
            computer.submit(); // this should fail as the computer has already been executed
            fail("Using the same graph computer to compute again should not be possible");
        } catch (IllegalStateException e) {

        } catch (Exception e) {
            fail("Should yield an illegal state exception for graph computer being executed twice");
        }

        // test no rerun of graph computer
        try {
            computer.submit(); // this should fail as the computer has already been executed even through new program submitted
            fail("Using the same graph computer to compute again should not be possible");
        } catch (IllegalStateException e) {

        } catch (Exception e) {
            fail("Should yield an illegal state exception for graph computer being executed twice");
        }
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldHaveConsistentMemoryVertexPropertiesAndExceptions() throws Exception {
        ComputerResult results = get_g_compute_setupXX_executeXv_blah_m_incrX_terminateX1X_elementKeysXnameLengthCounterX_memoryKeysXa_bX().submit().get();
        assertEquals(1, results.memory().getIteration());
        assertEquals(2, results.memory().asMap().size());
        assertEquals(2, results.memory().keys().size());
        assertTrue(results.memory().keys().contains("a"));
        assertTrue(results.memory().keys().contains("b"));
        assertTrue(results.memory().getRuntime() >= 0);

        assertEquals(Long.valueOf(12), results.memory().<Long>get("a"));   // 2 iterations
        assertEquals(Long.valueOf(28), results.memory().<Long>get("b"));
        try {
            results.memory().get("BAD");
            fail("Should throw an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(Memory.Exceptions.memoryDoesNotExist("BAD").getMessage(), e.getMessage());
        }
        assertEquals(Long.valueOf(6), results.graph().traversal().V().count().next());

        results.graph().traversal().V().forEachRemaining(v -> {
            assertTrue(v.property("nameLengthCounter").isPresent());
            assertEquals(Integer.valueOf(v.<String>value("name").length() * 2), Integer.valueOf(v.<Integer>value("nameLengthCounter")));
        });
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldAndOrIncrCorrectlyThroughSubStages() throws Exception {
        ComputerResult results = get_g_compute_setupXabcdeX_executeXtestMemoryX_terminateXtestMemoryXmemoryKeysXabcdeX().submit().get();
        assertEquals(2, results.memory().getIteration());
        assertEquals(5, results.memory().asMap().size());
        assertEquals(5, results.memory().keys().size());
        assertTrue(results.memory().keys().contains("a"));
        assertTrue(results.memory().keys().contains("b"));
        assertTrue(results.memory().keys().contains("c"));
        assertTrue(results.memory().keys().contains("d"));
        assertTrue(results.memory().keys().contains("e"));

        assertEquals(Long.valueOf(18), results.memory().get("a"));
        assertEquals(Long.valueOf(0), results.memory().get("b"));
        assertFalse(results.memory().get("c"));
        assertTrue(results.memory().get("d"));
        assertTrue(results.memory().get("e"));
    }


    @Test
    @LoadGraphWith(MODERN)
    public void shouldAllowMapReduceWithNoVertexProgram() throws Exception {
        final ComputerResult results = get_g_compute_mapXageX_reduceXsumX_memoryXnextX_memoryKeyXageSumX().submit().get();
        assertEquals(123, results.memory().<Integer>get("ageSum").intValue());
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldSupportMultipleMapReduceJobs() throws Exception {
        final ComputerResult results = get_g_compute_executeXcounterX_terminateX8X_mapreduceXcounter_aX_mapreduceXcounter_bX().submit().get();
        assertEquals(60, results.memory().<Integer>get("a").intValue());
        assertEquals(1, results.memory().<Integer>get("b").intValue());
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldSortReduceOutput() throws Exception {
        final ComputerResult results = get_g_compute_mapXidX_reduceXidX_reduceKeySortXreverseX_memoryKeyXidsX().submit().get();
        final List<Long> ids = results.memory().get("ids");
        assertEquals(6, ids.size());
        for (int i = 1; i < ids.size(); i++) {
            assertTrue(ids.get(i) < ids.get(i - 1));
        }
    }

    @Test
    @Ignore("Because of Graph.engine()")
    @LoadGraphWith(MODERN)
    public void shouldSupportStringTraversalVertexProgramExecution() throws Exception {
        final ComputerResult result = get_g_compute_programXTraversalVertexProgram_build_traversalXg_V_both_hasXlabel_personX_age_groupCountXaXX_create().submit().get();
        final Map<Integer, Long> map = result.memory().get("a");
        assertEquals(4, map.size());
        assertEquals(3, map.get(32).intValue());
        assertEquals(1, map.get(35).intValue());
        assertEquals(1, map.get(27).intValue());
        assertEquals(3, map.get(29).intValue());
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldNotAllowEdgeAccessInMapReduce() throws Exception {
        try {
            get_g_compute_mapXoutE_countX_reduceXsumX().submit().get();
            fail("Should not allow incident edges to be accessed in MapReduce");
        } catch (Exception ex) {
            // ex.printStackTrace();
        }
    }


    @UseEngine(TraversalEngine.Type.COMPUTER)
    public static class ComputerTest extends GraphComputerTest {
        @Override
        public GraphComputer get_g_compute() {
            return graph.compute();
        }

        @Override
        public GraphComputer get_g_compute_setupXX_executeXX_terminateXtrueX() {
            return graph.compute().program(LambdaVertexProgram.build().create());
        }

        @Override
        public GraphComputer get_g_compute_setupXX_executeXX_terminateXtrueX_memoryKeysXset_incr_and_orX() {
            return graph.compute().program(LambdaVertexProgram.build().memoryComputeKeys("set", "incr", "and", "or").create());
        }

        @Override
        public GraphComputer get_g_compute_setupXX_executeXX_terminateXtrueX_memoryKeysXnullX() {
            return graph.compute().program(LambdaVertexProgram.build().memoryComputeKeys(new HashSet<String>() {{
                add(null);
            }}).create());
        }

        @Override
        public GraphComputer get_g_compute_setupXX_executeXX_terminateXtrueX_memoryKeysX_X() {
            return graph.compute().program(LambdaVertexProgram.build().memoryComputeKeys("").create());
        }

        @Override
        public GraphComputer get_g_compute_setupXsetXa_trueXX_executeXX_terminateXtrueX() {
            return graph.compute().program(LambdaVertexProgram.build().setup(m -> m.set("a", true)).create());
        }

        @Override
        public GraphComputer get_g_compute_setupXX_executeXv_blah_m_incrX_terminateX1X_elementKeysXnameLengthCounterX_memoryKeysXa_bX() {
            return graph.compute().program(LambdaVertexProgram.build().
                    setup(memory -> {
                    }).
                    execute((vertex, messenger, memory) -> {
                        // TODO: Implement wrapper for GiraphGraph internal TinkerVertex
                        try {
                            vertex.property("blah", "blah");
                            fail("Should throw an IllegalArgumentException");
                        } catch (IllegalArgumentException e) {
                            assertEquals(GraphComputer.Exceptions.providedKeyIsNotAnElementComputeKey("blah").getMessage(), e.getMessage());
                        } catch (Exception e) {
                            fail("Should throw an IllegalArgumentException: " + e);
                        }

                        memory.incr("a", 1);
                        if (memory.isInitialIteration()) {
                            vertex.property("nameLengthCounter", vertex.<String>value("name").length());
                            memory.incr("b", vertex.<String>value("name").length());
                        } else {
                            vertex.property(VertexProperty.Cardinality.single, "nameLengthCounter", vertex.<String>value("name").length() + vertex.<Integer>value("nameLengthCounter"));
                        }
                    }).
                    terminate(memory -> memory.getIteration() == 1).
                    elementComputeKeys("nameLengthCounter").
                    memoryComputeKeys("a", "b").create());
        }

        @Override
        public GraphComputer get_g_compute_setupXabcdeX_executeXtestMemoryX_terminateXtestMemoryXmemoryKeysXabcdeX() {
            return graph.compute().program(LambdaVertexProgram.build().
                    setup(memory -> {
                        memory.set("a", 0l);
                        memory.set("b", 0l);
                        memory.set("c", true);
                        memory.set("d", false);
                        memory.set("e", true);
                    }).
                    execute((vertex, messenger, memory) -> {
                        // test current step values
                        assertEquals(Long.valueOf(6 * memory.getIteration()), memory.get("a"));
                        assertEquals(Long.valueOf(0), memory.get("b"));
                        if (memory.isInitialIteration()) {
                            assertTrue(memory.get("c"));
                            assertFalse(memory.get("d"));
                        } else {
                            assertFalse(memory.get("c"));
                            assertTrue(memory.get("d"));
                        }
                        assertTrue(memory.get("e"));

                        // update current step values
                        memory.incr("a", 1l);
                        memory.incr("b", 1l);
                        memory.and("c", false);
                        memory.or("d", true);
                        memory.and("e", false);

                        // test current step values, should be the same as previous prior to update
                        assertEquals(Long.valueOf(6 * memory.getIteration()), memory.get("a"));
                        assertEquals(Long.valueOf(0), memory.get("b"));
                        if (memory.isInitialIteration()) {
                            assertTrue(memory.get("c"));
                            assertFalse(memory.get("d"));
                        } else {
                            assertFalse(memory.get("c"));
                            assertTrue(memory.get("d"));
                        }
                        assertTrue(memory.get("e"));
                    }).
                    terminate(memory -> {
                        assertEquals(Long.valueOf(6 * (memory.getIteration() + 1)), memory.get("a"));
                        assertEquals(Long.valueOf(6), memory.get("b"));
                        assertFalse(memory.get("c"));
                        assertTrue(memory.get("d"));
                        assertFalse(memory.get("e"));
                        memory.set("b", 0l);
                        memory.set("e", true);
                        return memory.getIteration() > 1;
                    }).
                    memoryComputeKeys("a", "b", "c", "d", "e").create());
        }

        @Override
        public GraphComputer get_g_compute_mapXageX_reduceXsumX_memoryXnextX_memoryKeyXageSumX() {
            return graph.compute().mapReduce(LambdaMapReduce.<MapReduce.NullObject, Integer, MapReduce.NullObject, Integer, Integer>build()
                    .map((v, e) -> v.<Integer>property("age").ifPresent(age -> e.emit(MapReduce.NullObject.instance(), age)))
                    .reduce((k, vv, e) -> e.emit(StreamFactory.stream(vv).mapToInt(i -> i).sum()))
                    .memory(i -> i.next().getValue())
                    .memoryKey("ageSum").create());
        }

        @Override
        public GraphComputer get_g_compute_executeXcounterX_terminateX8X_mapreduceXcounter_aX_mapreduceXcounter_bX() {
            return graph.compute().program(LambdaVertexProgram.build()
                    .execute((vertex, messenger, memory) -> {
                        vertex.property(VertexProperty.Cardinality.single, "counter", memory.isInitialIteration() ? 1 : vertex.<Integer>value("counter") + 1);
                    })
                    .terminate(memory -> memory.getIteration() > 8)
                    .elementComputeKeys(new HashSet<>(Arrays.asList("counter"))).create())
                    .mapReduce(LambdaMapReduce.<MapReduce.NullObject, Integer, MapReduce.NullObject, Integer, Integer>build()
                            .map((v, e) -> e.emit(v.value("counter")))
                            .reduce((k, vv, e) -> {
                                int counter = 0;
                                while (vv.hasNext()) {
                                    counter = counter + vv.next();
                                }
                                e.emit(MapReduce.NullObject.instance(), counter);

                            })
                            .memory(i -> i.next().getValue())
                            .memoryKey("a").create())
                    .mapReduce(LambdaMapReduce.<MapReduce.NullObject, Integer, MapReduce.NullObject, Integer, Integer>build()
                            .map((v, e) -> e.emit(MapReduce.NullObject.instance(), v.value("counter")))
                            .combine((k, vv, e) -> e.emit(1))
                            .reduce((k, vv, e) -> e.emit(1))
                            .memory(i -> i.next().getValue())
                            .memoryKey("b").create());

        }

        @Override
        public GraphComputer get_g_compute_mapXidX_reduceXidX_reduceKeySortXreverseX_memoryKeyXidsX() {
            return graph.compute().mapReduce(LambdaMapReduce.<Long, Long, Long, Long, List<Long>>build()
                    .map((vertex, emitter) -> emitter.emit(Long.valueOf(vertex.id().toString()), Long.valueOf(vertex.id().toString())))
                    .reduce((key, values, emitter) -> values.forEachRemaining(id -> emitter.emit(id, id)))
                    .memoryKey("ids")
                    .reduceKeySort(Comparator::reverseOrder)
                    .memory(itty -> {
                        final List<Long> list = new ArrayList<>();
                        itty.forEachRemaining(id -> list.add(id.getKey()));
                        return list;
                    })
                    .create());
        }

        @Override
        public GraphComputer get_g_compute_programXTraversalVertexProgram_build_traversalXg_V_both_hasXlabel_personX_age_groupCountXaXX_create() {
            /*return graph.compute().program(TraversalVertexProgram.build().
                    // TODO: need to set the engine to be computer
                            traversal("GraphFactory.open(['gremlin.graph':'" + g.getClass().getCanonicalName() + "']).V().both().has(label,'person').values('age').groupCount('a')").
                    create()); */
            return null;
        }

        @Override
        public GraphComputer get_g_compute_mapXoutE_countX_reduceXsumX() {
            return null;
        }

    }




   /*
    @Test
    @LoadGraphWith(MODERN)
    @FeatureRequirement(featureClass = Graph.Features.GraphFeatures.class, feature = Graph.Features.GraphFeatures.FEATURE_COMPUTER)
    public void shouldSupportVertexProperties() throws Exception {
        GraphComputer computer = g.compute();
        ComputerResult results = computer.program(LambdaVertexProgram.build().
                execute((vertex, messenger, memory) -> {

                    if (memory.getIteration() == 0) {
                        assertEquals(VertexProperty.empty(), vertex.property("a"));
                        assertEquals(VertexProperty.empty(), vertex.property("b"));
                        assertFalse(vertex.property("a").isPresent());
                        assertFalse(vertex.property("b").isPresent());
                    } else if (memory.getIteration() == 1) {
                        assertEquals(1, vertex.property("a").value());
                        assertEquals(1, vertex.property("b").value());
                    } else if (memory.getIteration() == 2) {
                        assertEquals(2, vertex.properties("a").count().next().intValue());
                        assertEquals(1, vertex.properties("a").has(T.value, 1).count().next().intValue());
                        assertEquals(1, vertex.properties("a").has(T.value, 2).count().next().intValue());
                        assertEquals(2, vertex.property("b").value());
                    } else if (memory.getIteration() == 3) {
                        assertEquals(3, vertex.properties("a").count().next().intValue());
                        assertEquals(1, vertex.properties("a").has(T.value, 1).count().next().intValue());
                        assertEquals(2, vertex.properties("a").has(T.value, 2).count().next().intValue());
                        assertEquals(2, vertex.property("b").value());
                    } else {
                        fail("There should not be more than 3 iterations in this vertex program");
                    }

                    ///////////////////////////


                    if (memory.isInitialIteration()) {
                        vertex.property("a", 1);
                        vertex.property("b", 1);
                    } else {
                        vertex.property("a", 2);
                        vertex.property(single, ("b", 2);
                        try {
                            vertex.property("a");
                            fail("Should fail with IllegalStateException");
                        } catch (IllegalStateException e) {
                            assertEquals(Vertex.Exceptions.multiplePropertiesExistForProvidedKey("a").getMessage(), e.getMessage());
                        } catch (Exception e) {
                            fail("Should fail with IllegalStateException");
                        }
                        vertex.property("b");
                    }


                    ///////////////////////////

                    if (memory.getIteration() == 0) {
                        assertEquals(1, vertex.properties("a").count().next().intValue());
                        assertEquals(1, vertex.property("a").value());
                        assertEquals(1, vertex.property("b").value());
                    } else if (memory.getIteration() == 1) {
                        assertEquals(2, vertex.properties("a").count().next().intValue());
                        assertEquals(1, vertex.properties("a").has(T.value, 1).count().next().intValue());
                        assertEquals(1, vertex.properties("a").has(T.value, 2).count().next().intValue());
                        assertEquals(2, vertex.property("b").value());
                    } else if (memory.getIteration() == 2) {
                        assertEquals(3, vertex.properties("a").count().next().intValue());
                        assertEquals(1, vertex.properties("a").has(T.value, 1).count().next().intValue());
                        assertEquals(2, vertex.properties("a").has(T.value, 2).count().next().intValue());
                        assertEquals(2, vertex.property("b").value());
                    } else if (memory.getIteration() == 3) {
                        assertEquals(4, vertex.properties("a").count().next().intValue());
                        assertEquals(1, vertex.properties("a").has(T.value, 1).count().next().intValue());
                        assertEquals(3, vertex.properties("a").has(T.value, 2).count().next().intValue());
                        assertEquals(2, vertex.property("b").value());
                    } else {
                        fail("There should not be more than 3 iterators in this vertex program");
                    }

                }).
                terminate(memory -> memory.getIteration() > 2).
                elementComputeKeys(new HashSet<>(Arrays.asList("a", "b"))).create())
                .submit().get();
        assertEquals(3, results.getMemory().getIteration());
        final Vertex vertex = results.getGraph().V().next();
        assertEquals(2, vertex.property("b").value());
        assertEquals(4, vertex.properties("a").count().next().intValue());
        assertEquals(1, vertex.properties("a").has(T.value, 1).count().next().intValue());
        assertEquals(3, vertex.properties("a").has(T.value, 2).count().next().intValue());
        assertEquals(1, vertex.properties("b").count().next().intValue());
    }*/

    class BadGraphComputer implements GraphComputer {
        @Override
        public GraphComputer isolation(final Isolation isolation) {
            return null;
        }

        @Override
        public GraphComputer program(final VertexProgram vertexProgram) {
            return null;
        }

        @Override
        public GraphComputer mapReduce(final MapReduce mapReduce) {
            return null;
        }

        @Override
        public Future<ComputerResult> submit() {
            return null;
        }
    }
}
