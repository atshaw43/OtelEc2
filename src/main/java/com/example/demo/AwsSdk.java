package com.example.demo;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Collection;

public class AwsSdk extends TestBase {
    public static void main(String[] args) throws Exception {
        testNoMessages();
        testSampled();
        testUnsampled();
        testTwoSampled();
        testTwoUnsampled();
        testTwoMixedSampled();
        testRepeatedUeOfHandler();
        testExceptionInHandler();
        testSampledByAttribute();
        testSampledAutoinstrumentation();

        Thread.sleep(5000);
    }

    public static void testSampled() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream span");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessage("Single sampled");
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(1);

        sqsMessageHandlerImpl.handle(response);

        deleteMessages(response);
    }

    public static void testUnsampled() throws Exception {
        Span upstreamSpan = startSpanUnsampled("Single unsampled");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessage("Single unsampled");
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(1);

        sqsMessageHandlerImpl.handle(response);

        deleteMessages(response);
    }

    public static void testNoMessages() throws Exception {
        Collection<Message> response = receiveMessage(0);
        sqsMessageHandlerImpl.handle(response);
    }

    public static void testTwoSampled() throws Exception {
        Span upstreamSpan1 = startSpanSampled("Multi 2 sampled 1");
        try (Scope scope = upstreamSpan1.makeCurrent()) {
            sendMessage("Multi 2 sampled 1");
        }
        upstreamSpan1.end();

        Span upstreamSpan2 = startSpanSampled("Multi 2 sampled 2");
        try (Scope scope = upstreamSpan2.makeCurrent()) {
            sendMessage("Multi 2 sampled 2");
        }
        upstreamSpan2.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(2);

        sqsMessageHandlerImpl.handle(response);

        deleteMessages(response);
    }

    public static void testTwoUnsampled() throws Exception {
        Span upstreamSpan1 = startSpanUnsampled("Multi 2 unsampled 1");
        try (Scope scope = upstreamSpan1.makeCurrent()) {
            sendMessage("Multi 2 unsampled 1");
        }
        upstreamSpan1.end();

        Span upstreamSpan2 = startSpanUnsampled("Multi 2 unsampled 2");
        try (Scope scope = upstreamSpan2.makeCurrent()) {
            sendMessage("Multi 2 unsampled 2");
        }
        upstreamSpan2.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(2);

        sqsMessageHandlerImpl.handle(response);

        deleteMessages(response);
    }

    public static void testTwoMixedSampled() throws Exception {
        Span upstreamSpan1 = startSpanSampled("Multi 2 mix 1");
        try (Scope scope = upstreamSpan1.makeCurrent()) {
            sendMessage("Multi 2 mix 1");
        }
        upstreamSpan1.end();

        Span upstreamSpan2 = startSpanUnsampled("Multi 2 mix 2");
        try (Scope scope = upstreamSpan2.makeCurrent()) {
            sendMessage("Multi 2 mix 2");
        }
        upstreamSpan2.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(2);

        sqsMessageHandlerImpl.handle(response);

        deleteMessages(response);
    }

    public static void testRepeatedUeOfHandler() throws Exception {
        // Use 1
        Span upstreamSpan1 = startSpanSampled("Repeat 1");
        try (Scope scope = upstreamSpan1.makeCurrent()) {
            sendMessage("Repeat 1");
        }
        upstreamSpan1.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(1);

        sqsMessageHandlerImpl.handle(response);

        deleteMessages(response);

        // Use 2
        Span upstreamSpan2 = startSpanUnsampled("Repeat 2");
        try (Scope scope = upstreamSpan2.makeCurrent()) {
            sendMessage("Repeat 2");
        }
        upstreamSpan2.end();

        Thread.sleep(1000);

        Collection<Message> response2 = receiveMessage(1);

        sqsMessageHandlerImpl.handle(response2);

        deleteMessages(response2);
    }

    public static void testExceptionInHandler() throws Exception {
        Span upstreamSpan = startSpanSampled("Exception");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessage("Exception");
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(1);

        try {
            sqsMessageHandlerImplException.handle(response);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            deleteMessages(response);
        }
    }

    public static void testSampledByAttribute() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream span by attribute");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessageByAttribute("Single sampled by attribute");
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(1);

        sqsMessageHandlerImpl.handle(response);

        deleteMessages(response);
    }

    public static void testSampledAutoinstrumentation() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream span");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            sendMessageByAuto("Upstream span");
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Span serviceSpan = startSpanSampled("Service Span");
        try (Scope scope = serviceSpan.makeCurrent()) {
            Collection<Message> response = receiveMessage(1);
            sqsMessageHandlerImpl.handle(response);
            deleteMessages(response);
        }
        serviceSpan.end();
    }
}