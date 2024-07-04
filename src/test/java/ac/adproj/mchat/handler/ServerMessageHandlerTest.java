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

import ac.adproj.mchat.model.Protocol;
import ac.adproj.mchat.protocol.ServerListener;
import ac.adproj.mchat.service.UserManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerMessageHandlerTest {

    private static UserManager userManager;

    private ServerListener listener;

    private ServerMessageHandler handler;

    private static String testUserName;

    private String uuid;

    private AsynchronousSocketChannel mockChannel;

    @BeforeAll
    static void init() {
        userManager = UserManager.getInstance();
    }

    @BeforeEach
    void setUp() {
        listener = Mockito.mock(ServerListener.class);
        doCallRealMethod().when(listener).sendCommunicationData(any(), any());
        doCallRealMethod().when(listener).sendMessage(any(), any());

        handler = new ServerMessageHandler(listener);

        uuid = UUID.randomUUID().toString();
        testUserName = "TEST" + UUID.randomUUID();

        mockChannel = generateMockChannel();

        userManager.register(uuid, testUserName, mockChannel);
    }

    private AsynchronousSocketChannel generateMockChannel() {
        var tempMockChannel = mock(AsynchronousSocketChannel.class);

        when(tempMockChannel.write(any())).thenAnswer((Answer<Future<Integer>>) invocation -> {

            ByteBuffer byteBuffer = (ByteBuffer) invocation.getRawArguments()[0];

            return new Future<>() {
                boolean cancelled = false;

                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    cancelled = true;

                    return true;
                }

                @Override
                public boolean isCancelled() {
                    return cancelled;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public Integer get() {
                    return byteBuffer.limit();
                }

                @Override
                public Integer get(long timeout, TimeUnit unit) {
                    return 0;
                }
            };
        });

        return tempMockChannel;
    }

    @AfterEach
    void autoCleanup() {
        userManager.clearAllProfiles();
    }

    @Test
    @SneakyThrows
    void handleLogoffMessage() {
        var message = Protocol.DISCONNECT + uuid;

        handler.handleMessage(message, null);
        verify(listener).disconnect(uuid);
    }

    @Test
    @SneakyThrows
    void handleIncomingBroadcastMessage() {
        var secondUserUUID = UUID.randomUUID().toString();
        userManager.register(secondUserUUID, "TEST2", mock(AsynchronousSocketChannel.class));

        // Simulates the user 1 sends a message to public.
        var mockMessageText = UUID.randomUUID().toString();
        var message = Protocol.MESSAGE_HEADER_LEFT_HALF + secondUserUUID +
                Protocol.MESSAGE_HEADER_MIDDLE_HALF + Protocol.MESSAGE_HEADER_RIGHT_HALF + mockMessageText;

        handler.handleMessage(message, userManager.lookup(secondUserUUID).getChannel());

        verify(mockChannel).write(any());
    }

    @Test
    @SneakyThrows
    void handleIncomingPrivateMessage() {
        var secondUserUUID = UUID.randomUUID().toString();
        userManager.register(secondUserUUID, "TEST2", generateMockChannel());

        // Simulates the user 1 privately sends a message to the user 2.
        var mockMessageText = "@TEST2#" + UUID.randomUUID();
        var message = Protocol.MESSAGE_HEADER_LEFT_HALF + uuid +
                Protocol.MESSAGE_HEADER_MIDDLE_HALF + Protocol.MESSAGE_HEADER_RIGHT_HALF + mockMessageText;

        handler.handleMessage(message, mockChannel);

        var secondChannel = userManager.lookup(secondUserUUID).getChannel();
        verify(secondChannel).write(any());
        verify(mockChannel, times(0)).write(any());
    }

    @Test
    @SneakyThrows
    void handleIncomingPrivateMessageGibberishUsername() {
        var secondUserUUID = UUID.randomUUID().toString();
        userManager.register(secondUserUUID, "TEST2", generateMockChannel());

        // Simulates the user 1 privately sends a message to the user doesn't exist.
        var mockMessageText = "@" + UUID.randomUUID() + "#" + UUID.randomUUID();
        var message = Protocol.MESSAGE_HEADER_LEFT_HALF + uuid +
                Protocol.MESSAGE_HEADER_MIDDLE_HALF + Protocol.MESSAGE_HEADER_RIGHT_HALF + mockMessageText;

        handler.handleMessage(message, mockChannel);

        var secondChannel = userManager.lookup(secondUserUUID).getChannel();
        verify(secondChannel, times(0)).write(any());
        verify(mockChannel).write(any());
    }
}