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

import org.eclipse.jface.dialogs.MessageDialog;

import javax.swing.*;
import java.util.function.Predicate;

/**
 * Provides some method of showing message dialogs.
 *
 * @author Andy Cheung
 */
public final class CommonDialogs {

    private CommonDialogs() {
        throw new UnsupportedOperationException("No instance of 'CommonDialogs' for you! ");
    }

    /**
     * Show input dialog.
     *
     * @param askMessage   The message of the dialog.
     * @param errorMessage Message to tell user if the input is not correct.
     * @return The user input.
     * @implNote The message dialog is produced by JOptionPane.
     */
    public static String inputDialog(String askMessage, String errorMessage) {
        return inputDialog(null, askMessage, errorMessage);
    }

    /**
     * Show input dialog.
     *
     * @param defaultString The default value of the dialog text area.
     * @param askMessage    The message of the dialog.
     * @param errorMessage  Message to tell user if the input is empty.
     * @return The user input.
     * @implNote The message dialog is produced by JOptionPane.
     */
    public static String inputDialog(String defaultString, String askMessage, String errorMessage) {
        return inputDialog(defaultString, askMessage, errorMessage, (a) -> true);
    }

    /**
     * Show input dialog.
     *
     * @param defaultString The default value of the dialog text area.
     * @param askMessage    The message of the dialog.
     * @param errorMessage  Message to tell user if the input is not correct.
     * @param filter        The filter to check the input.
     * @return The user input.
     * @implNote The message dialog is produced by JOptionPane.
     */
    public static String inputDialog(String defaultString, String askMessage, String errorMessage,
                                     Predicate<String> filter) {
        String response;
        int counter = 0;

        do {
            response = (String) JOptionPane.showInputDialog(null, askMessage, "Input", JOptionPane.QUESTION_MESSAGE,
                    null, null, defaultString);
            counter++;
        } while ((response == null || response.trim().isEmpty() || !filter.test(response)) && counter <= 5);

        if (counter > 5) {
            swingErrorDialog(errorMessage);
            System.exit(-1);
        }

        return response;
    }

    /**
     * Show error dialog.
     *
     * @param message The message to display.
     * @implNote The message dialog is produced by JFace.
     */
    public static void errorDialog(String message) {
        MessageDialog.openError(null, "错误", message);
    }

    /**
     * Show error dialog.
     *
     * @param message The message to display.
     * @implNote The message dialog is produced by Swing.
     */
    private static void swingErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }
}
