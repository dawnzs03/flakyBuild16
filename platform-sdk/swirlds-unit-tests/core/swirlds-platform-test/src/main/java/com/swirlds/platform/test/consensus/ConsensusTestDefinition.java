/*
 * Copyright (C) 2021-2023 Hedera Hashgraph, LLC
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

package com.swirlds.platform.test.consensus;

import static com.swirlds.platform.test.event.source.EventSourceFactory.newStandardEventSources;

import com.swirlds.common.system.address.AddressBook;
import com.swirlds.common.test.fixtures.WeightGenerator;
import com.swirlds.platform.test.event.TestSequence;
import com.swirlds.platform.test.event.emitter.EventEmitter;
import com.swirlds.platform.test.event.emitter.EventEmitterGenerator;
import com.swirlds.platform.test.event.emitter.ShuffledEventEmitter;
import com.swirlds.platform.test.event.source.WeightedGraphGenerator;
import com.swirlds.platform.test.fixtures.event.generator.GraphGenerator;
import com.swirlds.platform.test.fixtures.event.generator.StandardGraphGenerator;
import com.swirlds.platform.test.fixtures.event.source.EventSource;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A type that has the necessary information for executing a single consensus test based on a seed used for
 * randomization.
 */
public class ConsensusTestDefinition {

    /** The name of this test. */
    private final String testName;

    /** The number of nodes in the network. */
    private final int numberOfNodes;

    /** The number of events to generate in each test sequence */
    private final int eventsPerPhase;

    /** The weight generator used to generate and assign weight to each node in the network. */
    private final WeightGenerator weightGenerator;

    /**
     * A function that creates an event emitter based on a graph generator and a seed. They should produce emitters
     * that will emit events in different orders. For example, nothing would be tested if both returned a {@link
     * com.swirlds.platform.test.event.emitter.StandardEventEmitter}. It is for both to return {@link
     * ShuffledEventEmitter} because they will be seeded with different values and therefore emit events in different
     * orders. Each instance of consensus should receive the same events, but in a different order.
     */
    private EventEmitterGenerator node1EventEmitterGenerator;

    private EventEmitterGenerator node2EventEmitterGenerator;

    /** A function that creates a graph generator with an address book with the provided node weight values */
    private WeightedGraphGenerator graphGeneratorProvider;

    /** The generator used to create test sequences based on this configuration. */
    private Function<ConsensusTestDefinition, List<TestSequence>> testSequenceGenerator;

    /**
     * The node weights generated for each node using the current seed. {@code null} until {@link #setSeed(long)} is
     * called.
     */
    private List<Long> nodeWeights;

    /**
     * The event emitters used to emit events. They wrap a graph generator that creates a graph using sources with the
     * calculated node weights. {@code null} until {@link #setSeed(long)} is called. There are two, one for each
     * consensus instance.
     */
    private EventEmitter<?> node1EventEmitter;

    private EventEmitter<?> node2EventEmitter;

    /** The address book generated from the graph */
    private AddressBook addressBook;

    /** The test sequences generated using the current seed. {@code null} until {@link #setSeed(long)} is called. */
    private List<TestSequence> testSequences;

    /** if true, print debug information that is helpful for debugging failing tests. */
    private boolean debug = false;

    /**
     * Creates a new configuration using the default event source generator.
     *
     * @param testName
     * 		the name of this test
     * @param numberOfNodes
     * 		the number of nodes in the network
     * @param weightGenerator
     * 		used to generate a weight value for each node in the network
     * @param eventsPerPhase
     * 		the number of events per phase/test sequence
     */
    public ConsensusTestDefinition(
            final String testName,
            final int numberOfNodes,
            final WeightGenerator weightGenerator,
            final int eventsPerPhase) {
        Objects.requireNonNull(weightGenerator);
        this.testName = testName;
        this.numberOfNodes = numberOfNodes;
        this.weightGenerator = weightGenerator;
        this.eventsPerPhase = eventsPerPhase;

        // Default event source generator.
        // There is overlap between this code and EventSourceFactory and EventGeneratorFactory. It should be
        // consolidated at some point.
        graphGeneratorProvider = weights -> {
            final List<EventSource<?>> eventSources = newStandardEventSources(weights);
            // Maybe abstract this so that this code block is reusable by all consensus tests
            return new StandardGraphGenerator(0, eventSources);
        };

        // Defaults to a shuffled emitter
        node1EventEmitterGenerator = ShuffledEventEmitter::new;
        node2EventEmitterGenerator = ShuffledEventEmitter::new;

        // Default test sequence generator
        testSequenceGenerator = c -> List.of(new TestSequence(eventsPerPhase));
    }

    public void setGraphGeneratorProvider(final WeightedGraphGenerator graphGeneratorProvider) {
        Objects.requireNonNull(graphGeneratorProvider);
        this.graphGeneratorProvider = graphGeneratorProvider;
    }

    public void setTestSequenceGenerator(
            final Function<ConsensusTestDefinition, List<TestSequence>> testSequenceGenerator) {
        Objects.requireNonNull(testSequenceGenerator);
        this.testSequenceGenerator = testSequenceGenerator;
    }

    /**
     * Creates and returns a new set of {@link TestSequence} objects using the provided {@code seed}. {@link
     * #setTestSequenceGenerator(Function)} must be called prior to calling this method.
     */
    public List<TestSequence> getTestSequences() {
        return testSequences;
    }

    /**
     * Sets the function that provides the {@link EventEmitter} for the first instance of consensus in this test.
     */
    public void setNode1EventEmitterGenerator(final EventEmitterGenerator eventEmitterGenerator) {
        this.node1EventEmitterGenerator = eventEmitterGenerator;
    }

    /**
     * Sets the function that provides the {@link EventEmitter} for the second instance of consensus in this test.
     */
    public void setNode2EventEmitterGenerator(final EventEmitterGenerator eventEmitterGenerator) {
        this.node2EventEmitterGenerator = eventEmitterGenerator;
    }

    /**
     * Generates new values for {@link #nodeWeights}, {@link #node1EventEmitter}, {@link #node2EventEmitter}, and {@link
     * #testSequences} using the provided {@code seed}.
     *
     * @param seed
     * 		the seed to use
     */
    public void setSeed(final long seed) {
        nodeWeights = weightGenerator.getWeights(seed, numberOfNodes);
        if (debug) {
            System.out.println("Node Weights: " + nodeWeights);
        }
        final GraphGenerator<?> graphGenerator = graphGeneratorProvider.getGraphGenerator(nodeWeights);
        addressBook = graphGenerator.getAddressBook();
        node1EventEmitter = node1EventEmitterGenerator.getEventEmitter(graphGenerator.cleanCopy(), seed);
        node2EventEmitter = node2EventEmitterGenerator.getEventEmitter(graphGenerator.cleanCopy(), seed);
        testSequences = testSequenceGenerator.apply(this);
    }

    /**
     * Returns the address book generated from the graph.  Must be called after {@link #setSeed(long)}.
     * @return the address book generated from the graph
     */
    @NonNull
    public AddressBook getAddressBook() {
        return addressBook;
    }

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public int getEventsPerPhase() {
        return eventsPerPhase;
    }

    public EventEmitter<?> getNode1EventEmitter() {
        return node1EventEmitter;
    }

    public EventEmitter<?> getNode2EventEmitter() {
        return node2EventEmitter;
    }

    public List<Long> getNodeWeights() {
        return nodeWeights;
    }

    public String getTestName() {
        return testName;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }
}
