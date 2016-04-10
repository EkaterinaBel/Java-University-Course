package javahibernate;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

/**
 * This is the class that shows the progress of the synchronization process on the client and on the server.
 */
public class SynchronizationProcessUI extends JFrame{

    public JFrame synchForm = this;
    public JProgressBar progressClient;
    public JProgressBar progressServer;
    private JLabel processOnClient = new JLabel();
    private JLabel processOnServer = new JLabel();
    private JLabel endSynchronized = new JLabel();
    private boolean bolClient;
    private boolean bolServer;

    /**
     * This constructor which are set parameters of components on the form.
     */
    public SynchronizationProcessUI () {

        super("Synchronization process");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        Image im = Toolkit.getDefaultToolkit().getImage("icon.jpg");
        setIconImage(im);
        java.awt.Dimension dim = getToolkit().getScreenSize();
        setSize(700, 450);
        setBounds(dim.width / 2 - getWidth() / 2, dim.height / 2 - getHeight() / 2,
                getWidth(), getHeight());
        setResizable(false);
        setContentPane(new BgPanel());
        JLabel client = new JLabel("Client");
        JLabel server = new JLabel("Server");
        progressClient = new JProgressBar();
        progressClient.setBounds(100, 30, 400, 30);
        progressServer = new JProgressBar();
        progressServer.setBounds(100, 220, 400, 30);

        Container container = this.getContentPane();
        container.setLayout(null);
        Color colorBackground = new Color(255,255,255);
        Color colorFont = new Color(54,54,54);
        Font font = new Font("Georgia", Font.BOLD, 14);
        Font entryNameFile = new Font("Georgia", Font.BOLD, 12);

        client.setFont(font);
        client.setForeground(colorFont);
        client.setBounds(30, 30, 60, 30);
        container.add(client);

        processOnClient.setFont(entryNameFile);
        processOnClient.setForeground(colorFont);
        processOnClient.setBounds(30, 90, 650, 30);
        container.add(processOnClient);

        server.setFont(font);
        server.setForeground(colorFont);
        server.setBounds(30, 200, 60, 30);
        container.add(server);

        processOnServer.setFont(entryNameFile);
        processOnServer.setForeground(colorFont);
        processOnServer.setBounds(30, 260, 650, 30);
        container.add(processOnServer);

        progressClient.setBackground(colorBackground);
        progressServer.setBackground(colorBackground);
        progressClient.setForeground(new Color(139,105,105));
        progressServer.setForeground(new Color(139,105,105));
        container.add(progressClient);
        container.add(progressServer);

        endSynchronized.setFont(font);
        endSynchronized.setForeground(colorFont);
        endSynchronized.setBounds(220, 350, 400, 30);
        container.add(endSynchronized);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                Object[] options = {"Да", "Нет"};
                int n = JOptionPane
                        .showOptionDialog(e.getWindow(), "Вы уверены, что хотите закрыть программу?",
                                "Подтверждение", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, options,
                                options[0]);
                if (n == 0) {
                    synchForm.dispose();
                    System.exit(0);
                }
            }
        });
    }

    /**
     * This method shows the progress of the synchronization process on the client.
     * @param o - object of class Client
     * @param sourceFolder - index beginning of the string name of the source folder on client
     * in the string the full path to the folder
     */
    public void barClientSynchronization(Client o, int sourceFolder) {

        progressClient.setStringPainted(true);
        progressClient.setValue(0);
        progressClient.setMaximum((int) o.sizeMapContainsChangesUI);
        int timerDelay = 0;
        new Timer(timerDelay , new ActionListener() {

            String str = "";
            public void actionPerformed(ActionEvent e) {

                progressClient.setValue((int) o.sizeClientUI);
                if (o.nameFileClientUI.length() > sourceFolder) {
                    processOnClient.setText(o.nameFileClientUI.substring(sourceFolder));
                }
                if (o.endClientUI) {
                    if (o.nameFileClientUI.length() > sourceFolder) {
                        processOnClient.setText(o.nameFileClientUI.substring(sourceFolder));
                    }
                    progressClient.setValue(progressClient.getMaximum());
                    bolClient = true;
                    endSynch();
                    ((Timer) e.getSource()).stop();
                }
            }
        }).start();
    }

    /**
     * This method shows the progress of the synchronization process on the server.
     * @param o - object of class Client
     * @param sourceFolder - index beginning of the string name of the source folder on server
     * in the string the full path to the folder
     */
    public void barServerSynchronization (Client o, int sourceFolder) {

        progressServer.setStringPainted(true);
        progressServer.setValue(0);
        progressServer.setMaximum((int) o.sizeMapContainsChangesUI);
        int timerDelay = 0;
        new Timer(timerDelay , new ActionListener() {

            String str = "";
            public void actionPerformed(ActionEvent e) {

                progressServer.setValue((int) o.sizeServerUI);
                if (o.nameFileServerUI.length() > sourceFolder) {
                    processOnServer.setText(o.nameFileServerUI.substring(sourceFolder));
                }
                if (o.endServerUI) {
                    if (o.nameFileServerUI.length() > sourceFolder) {
                        processOnServer.setText(o.nameFileServerUI.substring(sourceFolder));
                    }
                    progressServer.setValue(progressServer.getMaximum());
                    bolServer = true;
                    endSynch();
                    ((Timer) e.getSource()).stop();
                }
            }
        }).start();
    }

    /**
     * This method determines that the synchronization is complete.
     */
    private synchronized void endSynch() {
        if (bolClient && bolServer) {
            endSynchronized.setText("Synchronization is successful");
        }
    }

    /**
     * This is the class that initializes the image to form the background.
     */
    class BgPanel extends JPanel{
        public void paintComponent(Graphics g){
            Image im = null;
            try {
                im = ImageIO.read(new File("fantasy.jpg"));
            } catch (IOException e) {}
            g.drawImage(im, 0, 0, 700, 450, null);
        }
    }
}
