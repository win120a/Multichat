/*
    Copyright (C) 2011-2024 Andy Cheung

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package ac.adproj.mchat.handler;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static ac.adproj.mchat.model.Protocol.DISCONNECT;
import static ac.adproj.mchat.model.Protocol.MESSAGE_HEADER_LEFT_HALF;
import static ac.adproj.mchat.model.Protocol.MESSAGE_HEADER_MIDDLE_HALF;
import static ac.adproj.mchat.model.Protocol.MESSAGE_HEADER_RIGHT_HALF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientMessageHandlerTest {

    private ClientMessageHandler clientMessageHandler;

    private CountDownLatch latch;

    private final AtomicBoolean invoked = new AtomicBoolean(false);

    @BeforeEach
    void init() {
        latch = new CountDownLatch(1);

        clientMessageHandler = new ClientMessageHandler(unused -> {
            try {
                invoked.set(true);
            } finally {
                latch.countDown();
            }
        });
    }

    @Test
    @SneakyThrows
    void testIncomingMessage() {
        var uuidValue = UUID.randomUUID().toString();
        var message = UUID.randomUUID().toString();

        String protocolMessage = MESSAGE_HEADER_LEFT_HALF + uuidValue + MESSAGE_HEADER_MIDDLE_HALF + MESSAGE_HEADER_RIGHT_HALF + message;

        assertEquals(
                uuidValue + ": " + message,
                clientMessageHandler.handleMessage(protocolMessage, null)
        );
    }

    @Test
    @SneakyThrows
    void testDisconnectMessageFromServer() {
        String message = DISCONNECT + "SERVER";

        clientMessageHandler.handleMessage(message, null);
        latch.await();

        assertTrue(invoked.get());
    }

    @Test
    @SneakyThrows
    void testGibberishFromServer() {
        String message = UUID.randomUUID().toString();
        assertEquals("", clientMessageHandler.handleMessage(message, null));
    }
}