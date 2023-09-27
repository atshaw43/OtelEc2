package com.example;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Collection;

public class AwsSdk extends SampleAppHelper {
    public static void main(String[] args) throws Exception {
        //testNoMessages();
        testSampled();
        //testUnsampled();
        //testTwoSampled();
        //testTwoUnsampled();
        //testTwoMixedSampled();
        //testRepeatedUeOfHandler();
        //testExceptionInHandler();
        //testSampledByAttribute();
        //testSampledAutoinstrumentation();
        //testSampledAutoinstrumentationUpsteamNotSampled();
        //testMultiSampledAutoinstrumentation();
        //testMultiDifferentUpstreamSampledAutoinstrumentation();
        //testNoMessageAutoinstrumentation();
        //unsampledDownstream();

        Thread.sleep(5000);
    }

    public static void testSampled() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream span");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessage("Single sampled", sqsAutoinstrumentation);
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(1, sqsAutoinstrumentation);

       //sqsMessageHandlerImpl.handle(response);

        deleteMessages(response, sqsAutoinstrumentation);
    }

    public static void testUnsampled() throws Exception {
        Span upstreamSpan = startSpanUnsampled("Single unsampled");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessage("Single unsampled", sqsAutoinstrumentation);
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(1, sqsAutoinstrumentation);

       //sqsMessageHandlerImpl.handle(response);

        deleteMessages(response, sqsAutoinstrumentation);
    }

    public static void testNoMessages() throws Exception {
        Collection<Message> response = receiveMessage(0, sqsAutoinstrumentation);
       //sqsMessageHandlerImpl.handle(response);
    }

    public static void testTwoSampled() throws Exception {
        Span upstreamSpan1 = startSpanSampled("Multi 2 sampled 1");
        try (Scope scope = upstreamSpan1.makeCurrent()) {
            sendMessage("Multi 2 sampled 1", sqsAutoinstrumentation);
        }
        upstreamSpan1.end();

        Span upstreamSpan2 = startSpanSampled("Multi 2 sampled 2");
        try (Scope scope = upstreamSpan2.makeCurrent()) {
            sendMessage("Multi 2 sampled 2", sqsAutoinstrumentation);
        }
        upstreamSpan2.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(2, sqsAutoinstrumentation);

       //sqsMessageHandlerImpl.handle(response);

        deleteMessages(response, sqsAutoinstrumentation);
    }

    public static void testTwoUnsampled() throws Exception {
        Span upstreamSpan1 = startSpanUnsampled("Multi 2 unsampled 1");
        try (Scope scope = upstreamSpan1.makeCurrent()) {
            sendMessage("Multi 2 unsampled 1", sqsAutoinstrumentation);
        }
        upstreamSpan1.end();

        Span upstreamSpan2 = startSpanUnsampled("Multi 2 unsampled 2");
        try (Scope scope = upstreamSpan2.makeCurrent()) {
            sendMessage("Multi 2 unsampled 2", sqsAutoinstrumentation);
        }
        upstreamSpan2.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(2, sqsAutoinstrumentation);;

       //sqsMessageHandlerImpl.handle(response);

        deleteMessages(response, sqsAutoinstrumentation);
    }

    public static void testTwoMixedSampled() throws Exception {
        Span upstreamSpan1 = startSpanSampled("Multi 2 mix 1");
        try (Scope scope = upstreamSpan1.makeCurrent()) {
            sendMessage("Multi 2 mix 1", sqsAutoinstrumentation);
        }
        upstreamSpan1.end();

        Span upstreamSpan2 = startSpanUnsampled("Multi 2 mix 2");
        try (Scope scope = upstreamSpan2.makeCurrent()) {
            sendMessage("Multi 2 mix 2", sqsAutoinstrumentation);
        }
        upstreamSpan2.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(2, sqsAutoinstrumentation);

       //sqsMessageHandlerImpl.handle(response);

        deleteMessages(response, sqsAutoinstrumentation);
    }

    public static void testRepeatedUeOfHandler() throws Exception {
        // Use 1
        Span upstreamSpan1 = startSpanSampled("Repeat 1");
        try (Scope scope = upstreamSpan1.makeCurrent()) {
            sendMessage("Repeat 1", sqsAutoinstrumentation);
        }
        upstreamSpan1.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(1, sqsAutoinstrumentation);

       //sqsMessageHandlerImpl.handle(response);

        deleteMessages(response, sqsAutoinstrumentation);

        // Use 2
        Span upstreamSpan2 = startSpanUnsampled("Repeat 2");
        try (Scope scope = upstreamSpan2.makeCurrent()) {
            sendMessage("Repeat 2", sqsAutoinstrumentation);
        }
        upstreamSpan2.end();

        Thread.sleep(1000);

        Collection<Message> response2 = receiveMessage(1, sqsAutoinstrumentation);

        //sqsMessageHandlerImpl.handle(response2);

        deleteMessages(response2, sqsAutoinstrumentation);
    }

    public static void testExceptionInHandler() throws Exception {
        Span upstreamSpan = startSpanSampled("Exception");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessage("Exception", sqsAutoinstrumentation);
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(1, sqsAutoinstrumentation);

        try {
            //sqsMessageHandlerImplException.handle(response);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            deleteMessages(response, sqsAutoinstrumentation);
        }
    }

    public static void testSampledAutoinstrumentation() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream Span");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
        SpanBuilder serviceSpanBuilder = tracer.spanBuilder("Service Span");
        serviceSpanBuilder.setSpanKind(SpanKind.SERVER);
        Span serviceSpan = serviceSpanBuilder.startSpan();

        try (Scope scope = serviceSpan.makeCurrent()) {
            Collection<Message> response = receiveMessage(1, sqsAutoinstrumentation);
            //sqsMessageHandlerImpl.handle(response);
            deleteMessages(response, sqsAutoinstrumentation);
        }
        serviceSpan.end();
    }

    public static void testMultiSampledAutoinstrumentation() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream Span");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
        SpanBuilder serviceSpanBuilder = tracer.spanBuilder("Service Span");
        serviceSpanBuilder.setSpanKind(SpanKind.SERVER);
        Span serviceSpan = serviceSpanBuilder.startSpan();

        try (Scope scope = serviceSpan.makeCurrent()) {
            Collection<Message> response = receiveMessage(5, sqsAutoinstrumentation);
            //sqsMessageHandlerImpl.handle(response);
            deleteMessages(response, sqsAutoinstrumentation);
        }
        serviceSpan.end();
    }

    public static void testMultiDifferentUpstreamSampledAutoinstrumentation() throws Exception {
        Span upstreamSpan1 = startSpanSampled("Upstream Span");
        try (Scope scope = upstreamSpan1.makeCurrent()) {
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
        }
        upstreamSpan1.end();

        Span upstreamSpan2 = startSpanSampled("Upstream Span");
        try (Scope scope = upstreamSpan2.makeCurrent()) {
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
        }
        upstreamSpan2.end();

        Span upstreamSpan3 = startSpanSampled("Upstream Span");
        try (Scope scope = upstreamSpan3.makeCurrent()) {
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
        }
        upstreamSpan3.end();

        Span upstreamSpan4 = startSpanSampled("Upstream Span");
        try (Scope scope = upstreamSpan4.makeCurrent()) {
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
        }
        upstreamSpan4.end();

        Span upstreamSpan5 = startSpanSampled("Upstream Span");
        try (Scope scope = upstreamSpan5.makeCurrent()) {
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
        }
        upstreamSpan5.end();

        Thread.sleep(1000);

        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
        SpanBuilder serviceSpanBuilder = tracer.spanBuilder("Service Span");
        serviceSpanBuilder.setSpanKind(SpanKind.SERVER);
        Span serviceSpan = serviceSpanBuilder.startSpan();

        try (Scope scope = serviceSpan.makeCurrent()) {
            Collection<Message> response = receiveMessage(5, sqsAutoinstrumentation);
            //sqsMessageHandlerImpl.handle(response);
            deleteMessages(response, sqsAutoinstrumentation);
        }
        serviceSpan.end();
    }

    public static void testNoMessageAutoinstrumentation() throws Exception {
        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
        SpanBuilder serviceSpanBuilder = tracer.spanBuilder("Service Span");
        serviceSpanBuilder.setSpanKind(SpanKind.SERVER);
        Span serviceSpan = serviceSpanBuilder.startSpan();

        try (Scope scope = serviceSpan.makeCurrent()) {
            Collection<Message> response = receiveMessage(0, sqsAutoinstrumentation);
            //sqsMessageHandlerImpl.handle(response);
            deleteMessages(response, sqsAutoinstrumentation);
        }
        serviceSpan.end();
    }

    public static void unsampledDownstream() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream Span");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessageAuto("Upstream Span", sqsAutoinstrumentation);
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Tracer tracer =
                openTelemetryNeverSample.getTracer("instrumentation-library-name", "1.0.0");
        SpanBuilder serviceSpanBuilder = tracer.spanBuilder("Service Span");
        serviceSpanBuilder.setSpanKind(SpanKind.SERVER);
        Span serviceSpan = serviceSpanBuilder.startSpan();

        try (Scope scope = serviceSpan.makeCurrent()) {
            Collection<Message> response = receiveMessage(1, sqsAutoinstrumentation);
            //sqsMessageHandlerImpl.handle(response);
            deleteMessages(response, sqsAutoinstrumentation);
        }
        serviceSpan.end();
    }
}