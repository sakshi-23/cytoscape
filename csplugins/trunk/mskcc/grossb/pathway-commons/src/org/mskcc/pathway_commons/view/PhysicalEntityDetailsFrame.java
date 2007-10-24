package org.mskcc.pathway_commons.view;

import org.jdesktop.swingx.JXPanel;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Summary Panel.
 *
 * @author Ethan Cerami.
 */
public class PhysicalEntityDetailsFrame extends JXPanel {
    private Document doc;
    private JTextPane textPane;
    private PopupDaemon daemon;

    /**
     * Constructor.
     */
    public PhysicalEntityDetailsFrame() {
        daemon = new PopupDaemon(this, 1000);
        this.setLayout(new BorderLayout());
//        this.setUndecorated(true);
//        this.setResizable(false);
        textPane = createTextPane();
        doc = textPane.getDocument();
        JScrollPane scrollPane = encloseInJScrollPane (textPane);
        scrollPane.setBorder(GuiUtils.createTitledBorder("Gene Summary"));
        add(textPane, BorderLayout.CENTER);
        attachMouseListener(this, daemon);
        setAlpha(0.0f);
        this.setOpaque(false);
    }

    /**
     * Attaches appropriate mouse listeners.
     * @param daemon PopupDaemon Object.
     */
    private void attachMouseListener(PhysicalEntityDetailsFrame detailsFrame,
            final PopupDaemon daemon) {
        MouseAdapter mouseAdapter = new MouseAdapter() {

            /**
             * When mouse enters frame, stop daemon.  Frame will persist.
             * @param mouseEvent MouseEvent Object.
             */
            public void mouseEntered(MouseEvent mouseEvent) {
                System.out.println("Mouse entered");
                //daemon.stop();
            }


            public void mousePressed(MouseEvent mouseEvent) {
                System.out.println("Mouse pressed");
            }

            /**
             * When mouse exits frame, restart deamon.  Frame will disappear after XX milliseconds.
             * @param mouseEvent Mouse Event Object.
             */
            public void mouseExited(MouseEvent mouseEvent) {
                System.out.println("Mouse exited");
                //daemon.restart();
            }
        };
    }

    /**
     * Gets the summary document model.
     * @return Document object.
     */
    public Document getDocument() {
        return doc;
    }

    /**
     * Gets the summary text pane object.
     * @return JTextPane Object.
     */
    public JTextPane getTextPane() {
        return textPane;
    }

    /**
     * Encloses the specified JTextPane in a JScrollPane.
     *
     * @param textPane JTextPane Object.
     * @return JScrollPane Object.
     */
    private JScrollPane encloseInJScrollPane(JTextPane textPane) {
        JScrollPane scrollPane = new JScrollPane(textPane);
        return scrollPane;
    }

    /**
     * Creates a JTextPane with correct line wrap settings.
     *
     * @return JTextPane Object.
     */
    private JTextPane createTextPane() {
        JTextPane textPane = new JTextPane();
        textPane.setText("Gene Summary....");
        textPane.setEditable(false);
        textPane.setBorder(new EmptyBorder(7,7,7,7));
        return textPane;
    }
}

/**
 * Daemon Thread to automatically hide Pop-up Window after xxx milliseconds.
 *
 * @author Ethan Cerami
 */
class PopupDaemon implements ActionListener {
    private Timer timer;
    private PhysicalEntityDetailsFrame detailsFrame;

    /**
     * Constructor.
     *
     * @param detailsFrame PhysicalEntityDetailsFrame Object.
     * @param delay  Delay until pop-up window is hidden.
     */
    public PopupDaemon(PhysicalEntityDetailsFrame detailsFrame, int delay) {
        this.detailsFrame = detailsFrame;
        timer = new Timer(delay, this);
        timer.setRepeats(false);
    }

    /**
     * Restart timer.
     */
    public void restart() {
        timer.restart();
    }

    public void stop() {
        timer.stop();
    }

    /**
     * Timer Event:  Hide popup now.
     *
     * @param e ActionEvent Object.
     */
    public void actionPerformed(ActionEvent e) {
        System.out.println("HIDE");
        detailsFrame.setVisible(false);
    }
}
