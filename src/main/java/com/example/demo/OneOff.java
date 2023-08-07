package com.example.demo;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeNameForSends;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;

public class OneOff extends TestBase {
    public static void main(String[] args) throws Exception {
        testInvalidTraceContextFormat();
        testSpanLinksAttributes();
        testTwoDownstreamSpanLinksDifferentSpan();
        testTwoDownstreamSpanLinksSameSpan();

        testUpstreamSubsegment();
        testDownstreamSubsegment();
        testUpstreamAndDownstreamSubsegmnt();

        Thread.sleep(5000);
    }

    public static void testInvalidTraceContextFormat() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream span");
        try (Scope scope = upstreamSpan.makeCurrent()) {
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(url)
                    .messageBody("Invalid")
                    .messageAttributes(
                            Collections.singletonMap(
                                    "X-Amzn-Trace-Id",
                                    MessageAttributeValue.builder().dataType("String").stringValue(getSpanTraceHeader()+"P").build())).build();

            sqs.sendMessage(sendMessageRequest);
        }
        upstreamSpan.end();

        Thread.sleep(1000);

        Collection<Message> response = receiveMessage(1);

        sqsMessageHandlerImpl.handle(response);

        deleteMessages(response);
    }

    public static void testSpanLinksAttributes() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream span");
        upstreamSpan.end();

        Thread.sleep(1000);

        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
        SpanBuilder upstreamSpanBuilder = tracer.spanBuilder("Has attributes");
        upstreamSpanBuilder.addLink(upstreamSpan.getSpanContext(), Attributes.of(AttributeKey.stringKey("KeyName"), "MyValue"));
        Span span = upstreamSpanBuilder.startSpan();

        span.end();
    }

    public static void testTwoDownstreamSpanLinksDifferentSpan() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream span");
        upstreamSpan.end();

        Thread.sleep(1000);

        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
        SpanBuilder upstreamSpanBuilder1 = tracer.spanBuilder("Downstream 1");
        upstreamSpanBuilder1.addLink(upstreamSpan.getSpanContext());
        Span span1 = upstreamSpanBuilder1.startSpan();
        span1.end();

        Span downStreamSpan1 = startSpanSampled("Downstream 2");
        try (Scope scope = downStreamSpan1.makeCurrent()) {
            SpanBuilder upstreamSpanBuilder2 = tracer.spanBuilder("Downstream 2");
            upstreamSpanBuilder2.setSpanKind(SpanKind.SERVER);
            upstreamSpanBuilder2.addLink(upstreamSpan.getSpanContext());
            Span span2 = upstreamSpanBuilder2.startSpan();
            span2.end();
        }
        downStreamSpan1.end();
    }

    public static void testTwoDownstreamSpanLinksSameSpan() throws Exception {
        Span upstreamSpan = startSpanSampled("Upstream span");
        upstreamSpan.end();

        Thread.sleep(1000);

        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");

        SpanBuilder sb1 = tracer.spanBuilder("Downstream 1");
        sb1.setSpanKind(SpanKind.SERVER);
        sb1.addLink(upstreamSpan.getSpanContext());
        Span ds1 = sb1.startSpan();

        Context c = ds1.storeInContext(Context.root());
        SpanBuilder sb2 = tracer.spanBuilder("Downstream 2");
        sb2.setAttribute("aws.local.service", "Downstream 2");
        sb2.setParent(c);
        sb2.setSpanKind(SpanKind.SERVER);
        sb2.addLink(upstreamSpan.getSpanContext());
        Span ds2 = sb2.startSpan();
        ds2.end();

        ds1.end();
    }

    public static void testUpstreamSubsegment() throws Exception {
        Span upstreamSegment = startSpanSampled("Upstream segment");
        Span upstreamSubsegment = null;
        try (Scope scope = upstreamSegment.makeCurrent()) {
            upstreamSubsegment = startSpanSampled("Upstream subsegment");
            upstreamSubsegment.end();
        }
        upstreamSegment.end();

        Thread.sleep(1000);

        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");

        SpanBuilder sb1 = tracer.spanBuilder("Downstream 1");
        sb1.setSpanKind(SpanKind.SERVER);
        sb1.addLink(upstreamSubsegment.getSpanContext());
        Span ds1 = sb1.startSpan();

        ds1.end();
    }

    public static void testDownstreamSubsegment() throws Exception {
        Span upstreamSegment = startSpanSampled("Upstream segment");
        upstreamSegment.end();

        Thread.sleep(1000);

        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");

        Span downstreamSegment = startSpanSampled("Downstream segment");

        try (Scope scope = downstreamSegment.makeCurrent()) {
            SpanBuilder sb1 = tracer.spanBuilder("Downstream subsegment");
            sb1.setSpanKind(SpanKind.CONSUMER);
            sb1.addLink(upstreamSegment.getSpanContext());
            Span ds1 = sb1.startSpan();
            ds1.end();
        }

        downstreamSegment.end();
    }

    public static void testUpstreamAndDownstreamSubsegmnt() throws Exception {
        Span upstreamSegment = startSpanSampled("Upstream segment");
        Span upstreamSubsegment = null;
        try (Scope scope = upstreamSegment.makeCurrent()) {
            upstreamSubsegment = startSpanSampled("Upstream subsegment");
            upstreamSubsegment.end();
        }
        upstreamSegment.end();

        Thread.sleep(1000);

        Tracer tracer =
                openTelemetry.getTracer("instrumentation-library-name", "1.0.0");

        Span downstreamSegment = startSpanSampled("Downstream segment");

        try (Scope scope = downstreamSegment.makeCurrent()) {
            SpanBuilder sb1 = tracer.spanBuilder("Downstream subsegment");
            sb1.setSpanKind(SpanKind.CONSUMER);
            sb1.addLink(upstreamSubsegment.getSpanContext());
            Span ds1 = sb1.startSpan();
            ds1.end();
        }

        downstreamSegment.end();
    }
}