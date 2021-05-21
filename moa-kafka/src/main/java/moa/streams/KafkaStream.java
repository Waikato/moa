/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * KafkaStream.java
 * Copyright (C) 2019 University of Waikato, Hamilton, NZ
 */

package moa.streams;

import com.github.javacliparser.StringOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;
import moa.util.KafkaUtils;
import moa.util.ObjectDeserializer;
import moa.util.ObjectSerializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.LongDeserializer;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

/**
 * Instance stream which consumes instances from a Kafka topic.
 *
 * Assumptions:
 *   - The ordering of the instances in the topic is unimportant,
 *     or if it is important, it is ensured by a topic with only one
 *     partition.
 *   - The stream is considered ended when a record with a null
 *     value is found.
 *   - The serialised form of the instances is using Java's own
 *     serialisation tools (i.e. {@link ObjectSerializer}).
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
public class KafkaStream extends AbstractOptionHandler implements
  InstanceStream, CapabilitiesHandler, Closeable {

  // Serialisation UID#
  private static final long serialVersionUID = 671271388371039247L;

  // -- OPTIONS -- //

  // The topic to consume
  public StringOption topicOption = new StringOption("topic", 't',
    "Kafka topic to consume", "");

  // The broker host to connect to
  public StringOption hostOption = new StringOption("host", 'h',
    "The Kafka broker host", "localhost");

  // The broker port to connect to
  public StringOption portOption = new StringOption("port", 'p',
    "The Kafka broker port", "9092");

  // -- TRANSIENTS -- //

  // The consumer which will retrieve records from the Kafka stream
  protected transient KafkaConsumer<Long, Instance> m_Consumer = null;

  // A buffer of instances retrieved from the Kafka stream
  protected transient Queue<Instance> m_InstanceBuffer = null;

  // Whether we have reached the end of the stream
  protected transient boolean m_EndOfStreamReached = false;

  // The header for the instances
  protected transient InstancesHeader m_Header = null;

  @Override
  public String getPurposeString() {
    return "A stream consumed from a Kafka topic.";
  }

  @Override
  protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
    restart();
  }

  @Override
  public InstancesHeader getHeader() {
    fillBufferIfNecessary();

    return m_Header;
  }

  @Override
  public long estimatedRemainingInstances() {
    fillBufferIfNecessary();

    // If we've reached the end of the stream, what's in the buffer is all
    // that remain
    if (m_EndOfStreamReached)
      return m_InstanceBuffer.size();

    // Other than that we can't know this
    return -1;
  }

  @Override
  public boolean hasMoreInstances() {
    fillBufferIfNecessary();

    return !m_EndOfStreamReached || !bufferIsEmpty();
  }

  @Override
  public Example<Instance> nextInstance() {
    // Retrieve more instances from Kafka if the buffer is empty
    fillBufferIfNecessary();

    // If the buffer is empty, return null
    if (bufferIsEmpty())
      return null;

    // Return the next instance from the buffer
    return new InstanceExample(m_InstanceBuffer.remove());
  }

  @Override
  public boolean isRestartable() {
    return true;
  }

  @Override
  public void restart() {
    // Get the consumer in a usable state and restart it
    restartConsumer();

    // Throw away any buffered instances
    m_InstanceBuffer = null;

    // Mark the stream as not complete
    m_EndOfStreamReached = false;
  }

  @Override
  public ImmutableCapabilities defineImmutableCapabilities() {
    return new ImmutableCapabilities(Capability.VIEW_EXPERIMENTAL, Capability.VIEW_STANDARD);
  }

  @Override
  public void getDescription(StringBuilder sb, int indent) {
    // Indent
    while (indent > 0) {
      sb.append(" ");
      indent--;
    }

    // Add the actual description
    sb.append("Kafka instance stream consuming topic '");
    sb.append(topicOption.getValue());
    sb.append("' from broker at ");
    sb.append(broker());
  }

  @Override
  public void close() {
    if (m_Consumer != null) {
      m_Consumer.unsubscribe();
      m_Consumer.close();
    }

    m_Consumer = null;
  }

  /**
   * Makes sure the Kafka consumer is available and ready to
   * retrieve instances.
   */
  protected void establishConsumer() {
    // If the consumer is already resident, abort
    if (m_Consumer != null)
      return;

    // Create the consumer
    m_Consumer = new KafkaConsumer<>(createConsumerConfiguration());

    // Subscribe to the given topic
    m_Consumer.subscribe(Collections.singletonList(topicOption.getValue()));

    // Make sure the consumer starts from the beginning of the topic
    restartConsumer();
  }

  /**
   * Creates the configuration for the Kafka consumer.
   */
  protected Map<String, Object> createConsumerConfiguration() {
    Map<String, Object> config = new HashMap<>();

    config.put("key.deserializer", LongDeserializer.class);
    config.put("value.deserializer", ObjectDeserializer.class);
    config.put("bootstrap.servers", broker());
    config.put("fetch.min.bytes", 1);
    config.put("group.id", KafkaUtils.uniqueGroupIDString(this));
    config.put("max.partition.fetch.bytes", 1 << 20); // 1MB
    config.put("allow.auto.create.topics", false);
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", true);
    config.put("fetch.max.bytes", 1 << 24); // 16MB
    config.put("isolation.level", "read_committed");
    config.put("client.id", this.getClass().getName());

    return config;
  }

  /**
   * Gets the Kafka broker to connect to.
   */
  protected String broker() {
    return KafkaUtils.broker(hostOption.getValue(), portOption.getValue());
  }

  /**
   * Directs the consumer to the beginning of the Kafka stream.
   */
  protected void restartConsumer() {
    // Can't restart a non-resident consumer
    if (m_Consumer == null)
      return;

    // Seek all partitions back to the zero-record
    m_Consumer.seekToBeginning(Collections.emptyList());
  }

  /**
   * Gets a list of all topic partitions the consumer is consuming.
   */
  protected List<TopicPartition> getPartitions() {
    // Create a buffer for the results
    List<TopicPartition> result = new LinkedList<>();

    // If the consumer isn't established, return the empty list
    if (m_Consumer == null)
      return result;

    // Iterate through the topics and partitions, adding them to the result
    for (Entry<String, List<PartitionInfo>> topicPartitions : m_Consumer.listTopics().entrySet()) {
      String topic = topicPartitions.getKey();
      for (PartitionInfo info : topicPartitions.getValue()) {
        int partition = info.partition();

        result.add(new TopicPartition(topic, partition));
      }
    }

    return result;
  }

  /**
   * Retrieves more instances from Kafka and places them in the buffer.
   */
  protected void fillBufferIfNecessary() {
    // If we've reached the end of stream, we can't fill the buffer
    if (m_EndOfStreamReached)
      return;

    // If the buffer isn't empty, no need to fill it yet
    if (!bufferIsEmpty())
      return;

    // Make sure we have a consumer instance to use
    establishConsumer();

    // If the buffer isn't there, create it
    if (m_InstanceBuffer == null)
      m_InstanceBuffer = new LinkedList<>();

    // Get some records from Kafka
    ConsumerRecords<Long, Instance> records = m_Consumer.poll(KafkaUtils.WAIT_AS_LONG_AS_POSSIBLE);

    // Add each instance to the buffer
    for (ConsumerRecord<Long, Instance> record : records) {
      // Extract the instance from the record
      Instance instance = record.value();

      // If it's null, this is the sentinel that the end of stream has been reached
      if (instance == null) {
        m_EndOfStreamReached = true;
        close();
        break;
      }

      // Add the instance to the buffer
      m_InstanceBuffer.add(record.value());
    }

    // Save the header if we can and need to
    cacheHeaderIfNecessary();
  }

  /**
   * Caches the header for these instances if it hasn't already.
   */
  protected void cacheHeaderIfNecessary() {
    // Skip if we've already cached a header
    if (m_Header != null)
      return;

    // Get one of the instances
    Instance instance = m_InstanceBuffer.peek();

    // If there isn't one (should always be at this point), abort
    if (instance == null)
      return;

    // Get it's dataset
    Instances dataset = instance.dataset();

    // Save it for future reference
    if (dataset instanceof InstancesHeader)
      m_Header = (InstancesHeader) dataset;
    else
      m_Header = new InstancesHeader(dataset);
  }

  /**
   * Whether the instance buffer is empty.
   */
  protected boolean bufferIsEmpty() {
    return m_InstanceBuffer == null || m_InstanceBuffer.peek() == null;
  }
}
