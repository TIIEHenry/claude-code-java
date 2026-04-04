/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.mcp.transports;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Tests for InProcessTransport.
 */
@DisplayName("InProcessTransport Tests")
class InProcessTransportTest {

    private InProcessTransport transport;

    @BeforeEach
    void setUp() {
        transport = new InProcessTransport();
    }

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.disconnect();
        }
    }

    @Test
    @DisplayName("InProcessTransport starts disconnected")
    void startsDisconnected() {
        assertFalse(transport.isConnected());
    }

    @Test
    @DisplayName("InProcessTransport connect sets connected")
    void connectSetsConnected() {
        transport.connect();

        assertTrue(transport.isConnected());
    }

    @Test
    @DisplayName("InProcessTransport disconnect sets disconnected")
    void disconnectSetsDisconnected() {
        transport.connect();
        transport.disconnect();

        assertFalse(transport.isConnected());
    }

    @Test
    @DisplayName("InProcessTransport send throws when not connected")
    void sendThrowsWhenNotConnected() {
        assertThrows(IllegalStateException.class, () ->
            transport.send("test message")
        );
    }

    @Test
    @DisplayName("InProcessTransport send adds to outgoing queue")
    void sendAddsToOutgoingQueue() {
        transport.connect();
        transport.send("test message");

        List<String> outgoing = transport.getOutgoingMessages();
        assertEquals(1, outgoing.size());
        assertEquals("test message", outgoing.get(0));
    }

    @Test
    @DisplayName("InProcessTransport injectMessage adds to incoming queue")
    void injectMessageAddsToIncomingQueue() {
        transport.injectMessage("incoming message");

        try {
            String received = transport.receive(100, TimeUnit.MILLISECONDS);
            assertEquals("incoming message", received);
        } catch (InterruptedException e) {
            fail("Interrupted");
        }
    }

    @Test
    @DisplayName("InProcessTransport receive blocks for message")
    void receiveBlocksForMessage() throws InterruptedException {
        // Inject message after a delay
        new Thread(() -> {
            try {
                Thread.sleep(50);
                transport.injectMessage("delayed message");
            } catch (InterruptedException e) {
                // ignore
            }
        }).start();

        String received = transport.receive(200, TimeUnit.MILLISECONDS);
        assertEquals("delayed message", received);
    }

    @Test
    @DisplayName("InProcessTransport receive with timeout returns null if no message")
    void receiveWithTimeoutReturnsNullIfNoMessage() throws InterruptedException {
        String received = transport.receive(50, TimeUnit.MILLISECONDS);
        assertNull(received);
    }

    @Test
    @DisplayName("InProcessTransport getOutgoingMessages drains queue")
    void getOutgoingMessagesDrainsQueue() {
        transport.connect();
        transport.send("msg1");
        transport.send("msg2");
        transport.send("msg3");

        List<String> messages = transport.getOutgoingMessages();
        assertEquals(3, messages.size());

        // Queue should be empty now
        List<String> secondDrain = transport.getOutgoingMessages();
        assertTrue(secondDrain.isEmpty());
    }

    @Test
    @DisplayName("InProcessTransport disconnect clears queues")
    void disconnectClearsQueues() {
        transport.connect();
        transport.send("outgoing");
        transport.injectMessage("incoming");

        transport.disconnect();

        // Queues should be cleared
        List<String> outgoing = transport.getOutgoingMessages();
        assertTrue(outgoing.isEmpty());

        // Can't receive incoming anymore
        // After disconnect, we can connect again to verify
    }

    @Test
    @DisplayName("InProcessTransport addHandler adds handler")
    void addHandlerAddsHandler() {
        TestHandler handler = new TestHandler();
        transport.addHandler(handler);

        transport.injectMessage("test");
        transport.processMessages();

        assertEquals(1, handler.messages.size());
        assertEquals("test", handler.messages.get(0));
    }

    @Test
    @DisplayName("InProcessTransport removeHandler removes handler")
    void removeHandlerRemovesHandler() {
        TestHandler handler = new TestHandler();
        transport.addHandler(handler);
        transport.removeHandler(handler);

        transport.injectMessage("test");
        transport.processMessages();

        assertTrue(handler.messages.isEmpty());
    }

    @Test
    @DisplayName("InProcessTransport multiple handlers all receive messages")
    void multipleHandlersAllReceiveMessages() {
        TestHandler handler1 = new TestHandler();
        TestHandler handler2 = new TestHandler();
        transport.addHandler(handler1);
        transport.addHandler(handler2);

        transport.injectMessage("test");
        transport.processMessages();

        assertEquals(1, handler1.messages.size());
        assertEquals(1, handler2.messages.size());
        assertEquals("test", handler1.messages.get(0));
        assertEquals("test", handler2.messages.get(0));
    }

    @Test
    @DisplayName("InProcessTransport processMessages handles all queued messages")
    void processMessagesHandlesAllQueuedMessages() {
        TestHandler handler = new TestHandler();
        transport.addHandler(handler);

        transport.injectMessage("msg1");
        transport.injectMessage("msg2");
        transport.injectMessage("msg3");
        transport.processMessages();

        assertEquals(3, handler.messages.size());
    }

    @Test
    @DisplayName("InProcessTransport getInfo returns correct info")
    void getInfoReturnsCorrectInfo() {
        transport.connect();
        transport.send("outgoing");
        transport.injectMessage("incoming");

        InProcessTransport.TransportInfo info = transport.getInfo();

        assertEquals("in-process", info.type());
        assertTrue(info.connected());
        assertEquals(1, info.pendingIncoming());
        assertEquals(1, info.pendingOutgoing());
    }

    @Test
    @DisplayName("InProcessTransport TransportInfo format works correctly")
    void transportInfoFormatWorksCorrectly() {
        InProcessTransport.TransportInfo info = new InProcessTransport.TransportInfo(
            "in-process", true, 5, 3
        );

        String formatted = info.format();
        assertEquals("InProcess[connected=true, in=5, out=3]", formatted);
    }

    @Test
    @DisplayName("InProcessTransport can reconnect after disconnect")
    void canReconnectAfterDisconnect() {
        transport.connect();
        transport.disconnect();
        transport.connect();

        assertTrue(transport.isConnected());
        transport.send("test after reconnect");

        List<String> messages = transport.getOutgoingMessages();
        assertEquals(1, messages.size());
    }

    /**
     * Test handler implementation.
     */
    private static class TestHandler implements McpTransport.MessageHandler {
        final List<String> messages = new ArrayList<>();

        @Override
        public void handleMessage(String message) {
            messages.add(message);
        }
    }
}