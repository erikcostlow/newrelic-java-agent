package com.newrelic;

import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.trace.v1.V1;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ResponseObserverTest {
    @Test
    public void shouldIncrementCounterOnNext() {
        MetricAggregator metricAggregator = mock(MetricAggregator.class);

        ResponseObserver target = new ResponseObserver(
                metricAggregator,
                mock(Logger.class),
                mock(DisconnectionHandler.class), shouldRecreateCall);

        target.onNext(V1.RecordStatus.newBuilder().setMessagesSeen(3000).build());

        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/Response");
    }

    @Test
    public void shouldDisconnectOnNormalException() {
        DisconnectionHandler disconnectionHandler = mock(DisconnectionHandler.class);
        MetricAggregator metricAggregator = mock(MetricAggregator.class);

        ResponseObserver target = new ResponseObserver(
                metricAggregator,
                mock(Logger.class),
                disconnectionHandler, shouldRecreateCall);

        target.onError(new Throwable());

        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/Response/Error");
        verify(disconnectionHandler).handle(null);
    }

    @Test
    public void shouldReportStatusOnError() {
        DisconnectionHandler disconnectionHandler = mock(DisconnectionHandler.class);
        MetricAggregator metricAggregator = mock(MetricAggregator.class);

        ResponseObserver target = new ResponseObserver(
                metricAggregator,
                mock(Logger.class),
                disconnectionHandler, shouldRecreateCall);

        StatusRuntimeException exception = new StatusRuntimeException(Status.CANCELLED);

        target.onError(exception);

        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/Span/gRPC/CANCELLED");
        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/Response/Error");
        verify(disconnectionHandler).handle(Status.CANCELLED);
    }

    @Test
    public void shouldNotDisconnectWhenChannelClosing() {
        DisconnectionHandler disconnectionHandler = mock(DisconnectionHandler.class);
        MetricAggregator metricAggregator = mock(MetricAggregator.class);

        ResponseObserver target = new ResponseObserver(
                metricAggregator,
                mock(Logger.class),
                disconnectionHandler, shouldRecreateCall);

        StatusRuntimeException exception = Status.CANCELLED.withCause(new ChannelClosingException()).asRuntimeException();
        target.onError(exception);

        verifyNoInteractions(disconnectionHandler, metricAggregator);
    }

    @Test
    public void shouldDisconnectOnCompleted() {
        DisconnectionHandler mockHandler = mock(DisconnectionHandler.class);
        MetricAggregator metricAggregator = mock(MetricAggregator.class);

        ResponseObserver target = new ResponseObserver(
                metricAggregator,
                mock(Logger.class),
                mockHandler, shouldRecreateCall);

        target.onCompleted();

        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/Response/Completed");
        assertTrue(shouldRecreateCall.get());
    }

    @Test
    public void shouldTerminateOnALPNError() {
        DisconnectionHandler disconnectionHandler = mock(DisconnectionHandler.class);
        MetricAggregator metricAggregator = mock(MetricAggregator.class);

        ResponseObserver target = new ResponseObserver(
                metricAggregator,
                mock(Logger.class),
                disconnectionHandler, shouldRecreateCall);

        RuntimeException cause = new RuntimeException("TLS ALPN negotiation failed with protocols: [h2]");
        StatusRuntimeException exception = Status.UNAVAILABLE.withCause(cause).asRuntimeException();

        target.onError(exception);

        verify(metricAggregator).incrementCounter("Supportability/InfiniteTracing/NoALPNSupport");
        verify(disconnectionHandler).terminate();
    }

    AtomicBoolean shouldRecreateCall = new AtomicBoolean();
}