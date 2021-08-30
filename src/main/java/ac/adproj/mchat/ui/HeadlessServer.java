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

package ac.adproj.mchat.ui;

import java.io.IOException;

import ac.adproj.mchat.model.Protocol;
import ac.adproj.mchat.protocol.ServerListener;
import ac.adproj.mchat.service.MessageDistributor;
import ac.adproj.mchat.web.WebServerStarter;

/**
 * Provides the simplest user interface that only uses console to produce output. 
 * 
 * @author Andy Cheung
 */
public class HeadlessServer {
    public static void main(String[] args) throws IOException {
        System.out.println("AC Multichat Server (TCP, Headless)");
        System.out.println(String.format(
                            "Accepting TCP Connection on port %d, WebSocket connection on %d."
                                , Protocol.SERVER_PORT, 8090));
        System.out.println("Press <Ctrl> + <C / D> to stop.");
        System.out.println();
        
        try (ServerListener listener = ServerListener.getInstance(); WebServerStarter starter = new WebServerStarter()) {
            MessageDistributor.getInstance().registerSubscriber((message) -> {
                System.out.println(message);
            });
            
            starter.start(8090);
            
            Thread.currentThread().join();
        } catch (Exception e1) {
            e1.printStackTrace();
            System.exit(-1);
        }
    }
}
