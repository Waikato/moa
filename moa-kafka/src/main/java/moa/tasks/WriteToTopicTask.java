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
 * WriteToTopicTask.java
 * Copyright (C) 2021 University of Waikato, Hamilton, NZ
 */

package moa.tasks;

import com.github.javacliparser.IntOption;
import com.github.javacliparser.StringOption;
import com.yahoo.labs.samoa.instances.Instance;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.core.Example;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.streams.InstanceStream;
import moa.util.KafkaUtils;
import moa.util.ObjectSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Task to write instances from a stream to a Kafka topic.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
public class WriteToTopicTask extends AuxiliarMainTask implements CapabilitiesHandler {

  // The source of instances to write to the Kafka topic
  public ClassOption streamOption = new ClassOption(
        "stream",
        's',
        "Stream to write to the topic",
        InstanceStream.class,
        "generators.RandomTreeGenerator"
  );

  // The maximum number of instances to write
  public IntOption maxInstancesOption = new IntOption(
        "maxInstances",
        'm',
        "Maximum number of instances to write",
        100_000_000,
        0,
        Integer.MAX_VALUE
  );

  // The topic to write to
  public StringOption topicOption = new StringOption(
        "topic",
        't',
        "Kafka topic to write to",
        ""
  );

  // The broker host to connect to
  public StringOption hostOption = new StringOption(
        "host",
        'h',
        "The Kafka broker host",
        "localhost"
  );

  // The broker port to connect to
  public StringOption portOption = new StringOption(
        "port",
        'p',
        "The Kafka broker port",
        ""
  );

  /**
   * Creates the configuration for the Kakfa producer.
   *
   * @param host The Kafka host to connect to.
   * @param port The Kafka port to connect to.
   * @return The producer's configuration.
   */
  protected Map<String, Object> getProducerConfig(String host, String port) {
    Map<String, Object> config = new HashMap<>();

    config.put("key.serializer", LongSerializer.class);
    config.put("value.serializer", ObjectSerializer.class);
    config.put("bootstrap.servers", KafkaUtils.broker(host, port));
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

  @Override
  protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
    // Prepare all option values
    InstanceStream stream = (InstanceStream) getPreparedClassOption(streamOption);
    int maxInstances = maxInstancesOption.getValue();
    String topic = topicOption.getValue();
    String host = hostOption.getValue();
    String port = portOption.getValue();

    // Create the Kakfa producer
    KafkaProducer<Long, Instance> producer = new KafkaProducer<>(
          getProducerConfig(host, port)
    );

    int i = 0;
    while (i < maxInstances) {
      // If the stream is depleted, finalise the topic
      if (!stream.hasMoreInstances()) break;

      // Get the next instance from the stream
      Example<Instance> inst = stream.nextInstance();

      // Create a record of the instance for the topic
      ProducerRecord<Long, Instance> record = new ProducerRecord<>(
            topic, (long) i++, inst.getData()
      );

      // Send the record to the Kafka instance
      producer.send(record);

      // Abort if the task is cancelled (leaves the topic unfinished)
      if (monitor.isCancelled()) return null;

      // Estimate the number of instances left in the source stream
      long remainingInstances = stream.estimatedRemainingInstances();

      // Estimate the total number of instances that will be written
      long totalInstances = remainingInstances >= 0
            ? i + remainingInstances
            : maxInstances;

      // Update the task monitor on our progress
      monitor.setCurrentActivityFractionComplete(((double) i) / totalInstances);
    }

    // Send the null-terminator instance to the topic
    producer.send(
          new ProducerRecord<>(
                topic, (long) i, null
          )
    );

    return null;
  }

  @Override
  public Class<?> getTaskResultType() {
    return null;
  }

  @Override
  public ImmutableCapabilities defineImmutableCapabilities() {
    return new ImmutableCapabilities(Capability.VIEW_STANDARD);
  }
}
