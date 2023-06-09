
package org.test;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.test.pojo.Message;
import org.test.Deserialization.JsonDeserialization;
import org.test.TopicFilter.WinLogFilter;

import java.io.IOException;


public class TransformationJob {

	////////////////////////////////static inputs for personal lab //////////////////////////////////
	static final String inputTopic = "zach";
	static final String outputTopic = "Failed-first-filter";
	static final String wineventTopic = "windowslogs-topic";
	static final String jobTitle = "WinLogging";
	static final String deadLetterTopic = "failing-serialization";

	public TransformationJob() {
	}

	public static void main(String[] args) throws Exception {

		final String bootstrapServers = "broker:29092";

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		KafkaSource<String> logSource = KafkaSource.<String>builder()
				.setBootstrapServers(bootstrapServers)
				.setTopics(inputTopic)
				.setGroupId("winlog-group")
				.setStartingOffsets(OffsetsInitializer.earliest())
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

        ///////////////////////dirty data sinks(not matching winEventLog service_id string)////////////////////////
		KafkaRecordSerializationSchema<String> serializer = KafkaRecordSerializationSchema.builder()
				.setValueSerializationSchema(new SimpleStringSchema())
				.setTopic(outputTopic)
				.build();

		KafkaSink<String> sink = KafkaSink.<String>builder()
				.setBootstrapServers(bootstrapServers)
				.setRecordSerializer(serializer)
				.build();

		///////////////////////clean data sink(events matching winEventLog service_id that are now objects)///////////////////////
		KafkaRecordSerializationSchema<Message> wineventserializer = KafkaRecordSerializationSchema.builder()
				.setValueSerializationSchema(new JsonSerializationSchema<Message>())
				.setTopic(wineventTopic)
				.build();

		KafkaSink<Message> winEventSink = KafkaSink.<Message>builder()
				.setBootstrapServers(bootstrapServers)
				.setRecordSerializer(wineventserializer)
				.build();
////////////////////////////////dead letter sink (events Failining string -> Object conversion) /////////////////////
		KafkaRecordSerializationSchema<String> serializerDeadLetter = KafkaRecordSerializationSchema.builder()
				.setValueSerializationSchema(new SimpleStringSchema())
				.setTopic(deadLetterTopic)
				.build();

		KafkaSink<String> deadLetterTopic = KafkaSink.<String>builder()
				.setBootstrapServers(bootstrapServers)
				.setRecordSerializer(serializerDeadLetter)
				.build();
		////////////// log source data from above kafka source////////////////////
		DataStream<String> cleanLogSource = env.fromSource(logSource, WatermarkStrategy.noWatermarks(), "Kafka Source");

		try {

			final OutputTag<String> cleanSideOutput = new OutputTag<String>("cleanOutput") {
			};
			final OutputTag<String> dirtySideOutput = new OutputTag<String>("nonMatchedOutput") {
			};
			//////////// This first SideOutput is to filter strings in the logs source that are win event logs//////////
			SingleOutputStreamOperator<String> processStream =
					cleanLogSource.process(
							new ProcessFunction<String, String>() {
								@Override
								public void processElement(String message, ProcessFunction<String, String>.Context ctx, Collector<String> out) throws Exception {

									if (WinLogFilter.filter(message)) {
										ctx.output(cleanSideOutput, message);
									} else {
										ctx.output(dirtySideOutput, message);
									}

								}
							}
					).setParallelism(2);
			///// we use the cleanDataStream below and convert it to Objects/////
			DataStream<String> cleanDataStream = processStream.getSideOutput(cleanSideOutput);
			DataStream<String> nonMatchedDataStream = processStream.getSideOutput(dirtySideOutput);
			/// we used the string filtering sideOutput beccause everything I read on StackOverflow said it was faster
			// is this the fastest way or if it is better to use NodeObject for filtering and just map after that ////

			final OutputTag<String> deserializationErrors = new OutputTag<>("errors") {};


					SingleOutputStreamOperator<Message> winObjectStream =
							cleanDataStream.process(
									new ProcessFunction<>() {
										public void processElement(String value, ProcessFunction<String, Message>.Context ctx, Collector<Message> out) {
											final Message deserialized;
											try {
												deserialized = JsonDeserialization.deserialize(value); /// is there a way to use JsonKeyValuedeseraliser here to get a nodeobject?
											} catch (IOException e) {
												ctx.output(deserializationErrors, value);
												return;
											}
											out.collect(deserialized);
										}
									});
			////////////////////////////////logs that are failing serialization from strings to Objects/////////////////////
			final DataStream<String> elementsFailingDeserialization =
					winObjectStream.getSideOutput(deserializationErrors);

			elementsFailingDeserialization.sinkTo(deadLetterTopic);

			////////////////////////////////Dirty Data that did not match the first string filter////////////////////////////////
			nonMatchedDataStream.sinkTo(sink);

			//////////////////////////////// Datastream that has sucessfully been transformed into Objects ////////////////////////////////
			winObjectStream.sinkTo(winEventSink);

			/////////////////////////////////Placeholder for logically mapping the new Object Stream //////////////////////////////


		} catch (Exception sinkError) {
			throw new RuntimeException("Error connecting to producers: ", sinkError);
		}

		env.execute(jobTitle);
	}
}
