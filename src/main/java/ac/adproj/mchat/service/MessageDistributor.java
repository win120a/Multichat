/*
    Copyright (C) 2011-2020 Andy Cheung

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

package ac.adproj.mchat.service;

import ac.adproj.mchat.handler.MessageType;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provide the functionality of broadcasting message to many subscribers. 
 * (Like broadcast mechanism in Android)
 * 
 * @author Andy Cheung
 * @since 2020.5.24
 */
@Slf4j
public class MessageDistributor {

    /**
     * Holder of instance.
     */
    private static class Holder {
        private static MessageDistributor INSTANCE = new MessageDistributor();
    }

    private MessageDistributor() {
        uiMessages = new LinkedBlockingQueue<>();
        callbacks = new LinkedList<>();
        CommonThreadPool.execute(new MessageDistributingService(), "Message Distributing Service");
    }

    /**
     * Obtain the only instance of this class.
     * 
     * @return The instance.
     */
    public static MessageDistributor getInstance() {
        return Holder.INSTANCE;
    }

    private BlockingQueue<String> uiMessages;

    private LinkedList<SubscriberCallback> callbacks;

    /**
     * Message distributing service.
     * 
     * @author Andy Cheung
     */
    private class MessageDistributingService implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    String message = uiMessages.take();

                    for (SubscriberCallback cb : callbacks) {
                        cb.onMessageReceived(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    // Stops when the thread is interrupted.
                    break;

                } catch (Exception e) {
                    // Other problems, print the exception information and continue.

                    log.error("Error occurred when distributing message.", e);
                }
            }
        }
    }

    /**
     * Defines the callback interface when subscriber receives message.
     * (Like Broadcast receiver in Android.)
     * 
     * @author Andy Cheung
     * @since 2020.5.24
     */
    public interface SubscriberCallback {
        /**
         * Callback method.
         * 
         * @param uiMessage Processed message that can be displayed directly in UI.
         */
        void onMessageReceived(String uiMessage);
    }

    /**
     * Send user-friendly message to subscribers.
     * 
     * @param message User-friendly message
     * @throws InterruptedException If the operation is being interrupted.
     */
    public void sendUiMessage(String message) throws InterruptedException {
        uiMessages.put(message);
    }

    /**
     * Convenience method of sending user-friendly message from raw protocol message (user message only).
     * 
     * @param message Raw "User message" protocol message.
     * @throws InterruptedException If the operation is being interrupted.
     */
    public void sendRawProtocolMessage(String message) throws InterruptedException {
        Map<String, String> tresult = MessageType.INCOMING_MESSAGE.tokenize(message);
        uiMessages.put(tresult.get("uuid") + ": " + tresult.get("messageText"));
    }

    /**
     * Register the subscriber to receive UI messages.
     * 
     * @param callback The subscriber callback.
     */
    public void registerSubscriber(SubscriberCallback callback) {
        callbacks.add(callback);
    }
    
    /**
     * Unregister the subscriber.
     * 
     * @param callback The subscriber callback.
     */
    public void unregisterSubscriber(SubscriberCallback callback) {
        callbacks.remove(callback);
    }
}
