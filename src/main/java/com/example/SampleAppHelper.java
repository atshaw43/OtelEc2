package com.example;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.contrib.awsxray.AwsXrayIdGenerator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.awssdk.v2_2.SqsMessageHandler;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeNameForSends;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.utils.AttributeMap;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

public class SampleAppHelper {
    static final String url = "https://sqs.us-west-1.amazonaws.com/103216442713/MyQueue";

    static Resource resource = Resource.getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME, "MyServiceName")));

    static SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
            .setIdGenerator(AwsXrayIdGenerator.getInstance())
            .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build()).build())
            .setResource(resource)
            .build();

    static SdkTracerProvider sdkTracerProviderNeverSample = SdkTracerProvider.builder()
            .setIdGenerator(AwsXrayIdGenerator.getInstance())
            .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build()).build())
            .setResource(resource)
            .setSampler(Sampler.alwaysOff())
            .build();

    static SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
            .registerMetricReader(
                    PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().build()).build())
            .setResource(resource)
            .build();

    static SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(
                    BatchLogRecordProcessor.builder(OtlpGrpcLogRecordExporter.builder().build()).build())
            .setResource(resource)
            .build();

    static OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setMeterProvider(sdkMeterProvider)
            .setLoggerProvider(sdkLoggerProvider)
            .setPropagators(ContextPropagators.create(AwsXrayPropagator.getInstance()))
            .buildAndRegisterGlobal();

    static OpenTelemetry openTelemetryNeverSample = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProviderNeverSample)
            .setMeterProvider(sdkMeterProvider)
            .setLoggerProvider(sdkLoggerProvider)
            .setPropagators(ContextPropagators.create(AwsXrayPropagator.getInstance()))
            .build();

    static SqsClient sqs;

    static {
        SdkHttpClient httpClient = ApacheHttpClient.builder()
                .buildWithDefaults(AttributeMap.builder()
                        .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                        .build());

        sqs = SqsClient.builder()
                //.overrideConfiguration(c -> c.addExecutionInterceptor(AwsSdkTelemetry.builder(openTelemetry).build().newExecutionInterceptor())) // Uncomment this line for auto-instrumentation
                .region(Region.US_EAST_1)
                .httpClient(httpClient)
                .build();
    }

    public static String getSpanTraceHeader() {
        Map<String, String> map = new TreeMap<>();

        AwsXrayPropagator.getInstance()
                .inject(
                        Context.current(), // We don't want the ambient context.
                        map,
                        AwsSdk.MapSetter.INSTANCE);

        return map.get("X-Amzn-Trace-Id");
    }

    enum MapSetter implements TextMapSetter<Map<String, String>> {
        INSTANCE;

        @Override
        public void set(Map<String, String> stringStringMap, String s, String s1) {
            stringStringMap.put(s, s1);
        }
    }

    static SqsMessageHandlerImpl sqsMessageHandlerImpl = new SqsMessageHandlerImpl();
    static SqsMessageHandlerImplException sqsMessageHandlerImplException = new SqsMessageHandlerImplException();

    static class SqsMessageHandlerImpl extends SqsMessageHandler<Message> {
        public final AtomicInteger handleCalls = new AtomicInteger();
        public SqsMessageHandlerImpl() {
            super(openTelemetry, "destination");
        }

        public void doHandle(Collection<Message> messages) {
            handleCalls.getAndIncrement();
            for (Message message: messages) {
                System.out.println("Processing: " + message.body());
            }
        }
    }

    static class SqsMessageHandlerImplException extends SqsMessageHandler<Message> {
        public final AtomicInteger handleCalls = new AtomicInteger();
        public SqsMessageHandlerImplException() {
            super(openTelemetry, "destination");
        }

        public void doHandle(Collection<Message> messages) {
            for (Message message: messages) {
                System.out.println("Processing: " + message.body());
            }
            throw new IllegalArgumentException("Test");
        }
    }

    public static Span startSpanSampled(String name) {
        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
        SpanBuilder upstreamSpanBuilder = tracer.spanBuilder(name);
        return upstreamSpanBuilder.startSpan();
    }

    public static Span startSpanUnsampled(String name) {
        Tracer tracer =
                openTelemetryNeverSample.getTracer("instrumentation-library-name", "1.0.0");

        SpanBuilder upstreamSpanBuilder = tracer.spanBuilder(name);
        return upstreamSpanBuilder.startSpan();
    }

    public static void sendMessage(String body) {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(body)
                .messageSystemAttributes(
                        Collections.singletonMap(
                                MessageSystemAttributeNameForSends.AWS_TRACE_HEADER,
                                MessageSystemAttributeValue.builder().dataType("String").stringValue(getSpanTraceHeader()).build())).build();

        System.out.println("Sending: " + sendMessageRequest.messageBody());

        sqs.sendMessage(sendMessageRequest);
    }

    public static void sendMessageByAttribute(String body) {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(body)
                .messageAttributes(
                        Collections.singletonMap(
                                "X-Amzn-Trace-Id",
                                MessageAttributeValue.builder().dataType("String").stringValue(getSpanTraceHeader()).build())).build();

        System.out.println("Sending: " + sendMessageRequest.messageBody());

        sqs.sendMessage(sendMessageRequest);
    }



    public static void sendMessageByAuto(String body) {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(body).build();

        System.out.println("Sending: " + sendMessageRequest.messageBody());

        sqs.sendMessage(sendMessageRequest);
    }

    public static Collection<Message> receiveMessage(int expected) {
        Collection<Message> messages = new LinkedList<>();

        while(messages.size() < expected) {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .maxNumberOfMessages(10)
                    .attributeNamesWithStrings(MessageSystemAttributeNameForSends.AWS_TRACE_HEADER.toString())
                    .messageAttributeNames("X-Amzn-Trace-Id")
                    .queueUrl(url).build();

            ReceiveMessageResponse response = sqs.receiveMessage(request);

            messages.addAll(response.messages());
        }

        return messages;
    }

    public static void deleteMessages(Collection<Message> messages) {
        for (Message message: messages) {
            System.out.println("Deleting: " + message.body());

            DeleteMessageRequest deleteMessageRequest =
                    DeleteMessageRequest.builder()
                            .queueUrl(url)
                            .receiptHandle(message.receiptHandle())
                            .build();

            sqs.deleteMessage(deleteMessageRequest);
        }
    }
}
