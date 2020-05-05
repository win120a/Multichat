package ac.adproj.mchat.ui;

import java.util.function.Predicate;

import javax.swing.JOptionPane;

/**
 * Provide some method of showing input dialogs.
 * 
 * @author Andy Cheung
 */
public final class CommonDialogs {
    
    /**
     * Show input dialog.
     * @param askMessage The message of the dialog.
     * @param errorMessage Message to tell user if the input is not correct.
     * @return The user input.
     * @implNote The message dialog is produced by JOptionPane.
     */
    public static String inputDialog(String askMessage, String errorMessage) {
        return inputDialog(null, askMessage, errorMessage);
    }
    
    /**
     * Show input dialog.
     * @param defaultString The default value of the dialog text area.
     * @param askMessage The message of the dialog.
     * @param errorMessage Message to tell user if the input is empty.
     * @return The user input.
     * @implNote The message dialog is produced by JOptionPane.
     */
    public static String inputDialog(String defaultString, String askMessage, String errorMessage) {
        return inputDialog(defaultString, askMessage, errorMessage, (a) -> true);
    }
    
    /**
     * Show input dialog.
     * @param defaultString The default value of the dialog text area.
     * @param askMessage The message of the dialog.
     * @param errorMessage Message to tell user if the input is not correct.
     * @param filter The filter to check the input.
     * @return The user input.
     * @implNote The message dialog is produced by JOptionPane.
     */
    public static String inputDialog(String defaultString, String askMessage, String errorMessage, Predicate<String> filter) {
        String response;
        int counter = 0;
        
        do {
            response = (String) JOptionPane.showInputDialog(null, askMessage, "Input", JOptionPane.QUESTION_MESSAGE, null, null, defaultString);
            counter++;
        } while ((response == null || response.isBlank() || !filter.test(response)) && counter <= 5);
        
        if (counter > 5) {
            errorDialog(errorMessage);
            System.exit(-1);
        }
        
        return response;
    }
    
    /**
     * Show error dialog.
     * @param message The message to display.
     * @implNote The message dialog is produced by JOptionPane.
     */
    public static void errorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }
}
