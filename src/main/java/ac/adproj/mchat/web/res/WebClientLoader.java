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

package ac.adproj.mchat.web.res;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Loader of WebSocket (client) web application package.
 * 
 * @author Andy Cheung
 * @since 2020/5/22
 */
public final class WebClientLoader {
    public static final String WAR_NAME_IN_RESOURCE_PATH = "webClient.war";

    /**
     * Load web application package in resource, and copy it to temporary location.
     * (Delete when application stops.)
     * 
     * @return The absolute path of copied web application.
     */
    public static String getWebappWarPath() {
        FileOutputStream fos = null;

        try (BufferedInputStream is = new BufferedInputStream(
                WebClientLoader.class.getResourceAsStream(WAR_NAME_IN_RESOURCE_PATH))) {

            File f = File.createTempFile("acmc-webC-", ".war");
            f.deleteOnExit();
            fos = new FileOutputStream(f);

            int dataBit;
            while ((dataBit = is.read()) != -1) {
                fos.write(dataBit);
            }

            return f.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
