package image_processing.gui;

import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

/**
 * A JComponent with an help icon showing an HTML description
 * 
 * @author Terence
 *
 */
public class JComponentWithHelp extends JPanel {

    /**
     * The help icon image
     */
    private static final ImageIcon HELP_ICON = new ImageIcon("data/icons/help.png");

    /**
     * The JComponent for which to add the help icon
     */
    private final JComponent component;

    /**
     * Instantiate a new JComponentWithHelp
     * 
     * @param component           the JComponent to which to add the help icon
     * @param htmlHelpDescription the HTML help description when the help icon is hovered
     */
    public JComponentWithHelp(JComponent component, String htmlHelpDescription) {
        this.component = component;
        setLayout(new BorderLayout());
        JLabel helpLabel = new JLabel(HELP_ICON);
        helpLabel.setToolTipText(htmlHelpDescription);
        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(30 * 1000);
        add(component, BorderLayout.CENTER);
        add(helpLabel, BorderLayout.WEST);
    }

    /**
     * Get the JComponent to which the add icon was added
     * 
     * @return the JComponent to which the add icon was added
     */
    public JComponent getComponent() {
        return component;
    }

}
