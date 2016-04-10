package javahibernate;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * This is the class that draws the user interface (a form with a button and input fields for username, password
 * and select the source folder synchronization) to the client.
 */
public class UserInterface extends JFrame {

    private JButton synch = new JButton("Start synchronization");
    private JButton fileChoose = new JButton("Choose a folder");
    private JTextField log = new JTextField("");
    private JPasswordField pas = new JPasswordField("");
    private JLabel login = new JLabel("Login");
    private JLabel password = new JLabel("Password");
    private JFileChooser folderChoose = new JFileChooser();
    private JFrame jFrame = this;
    private File file = null;

    /**
     * This constructor which are set parameters of components on the form.
     */
    public UserInterface() {

        super("Synchronization");
        Image im = Toolkit.getDefaultToolkit().getImage("icon.jpg");
        setIconImage(im);
        java.awt.Dimension dim = getToolkit().getScreenSize();
        setSize(700, 450);
        setBounds(dim.width / 2 - this.getWidth() / 2, dim.height / 2 - this.getHeight() / 2,
                this.getWidth(), this.getHeight());
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new BgPanel());

        Container container = this.getContentPane();
        container.setLayout(null);

        Color colorBackground = new Color(255,255,255);
        Color colorFont = new Color(54,54,54);
        Font font = new Font("Georgia", Font.BOLD, 14);

        fileChoose.setFont(font);
        fileChoose.setForeground(colorFont);
        fileChoose.setBackground(colorBackground);
        fileChoose.setBounds(30, 70, 200, 30);
        container.add(fileChoose);

        login.setFont(font);
        login.setForeground(colorFont);
        login.setBounds(30, 120, 200, 30);
        container.add(login);

        log.setFont(font);
        log.setForeground(colorFont);
        log.setBounds(30, 160, 200, 30);
        container.add(log);

        password.setFont(font);
        password.setForeground(colorFont);
        password.setBounds(30, 200, 200, 30);
        container.add(password);

        pas.setFont(font);
        pas.setForeground(colorFont);
        pas.setBounds(30, 240, 200, 30);
        container.add(pas);

        synch.setFont(font);
        synch.setForeground(colorFont);
        synch.setBackground(colorBackground);

        synch.setBounds(240, 350, 200, 30);

        fileChoose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                folderChoose.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                folderChoose.showOpenDialog(jFrame);
                file = folderChoose.getSelectedFile();
                fileChoose.setText(file.getName());
            }
        });

        synch.addActionListener(new ButtonEventListener());
        container.add(synch);
    }

    /**
     * This is the class that responds to pressing the "Start synchronization" button.
     */
    class ButtonEventListener implements ActionListener {
        /**
         * This method processes the press of the button. If all parameters are entered correctly on the form,
         * it will open a new form that shows the synchronization process.
         * @param e - the press of the button
         */
        public void actionPerformed(ActionEvent e) {

            synch.setEnabled(false);
            if (file == null || log.getText().equals("") || pas.getPassword().length == 0) {
                String message = "";
                if (file == null) {
                    message = "The folder is not selected";
                } else {
                    message = "Login or password is not entered";
                }
                JOptionPane.showMessageDialog(null,
                        message,
                        "Error",
                        JOptionPane.PLAIN_MESSAGE);
                synch.setEnabled(true);
            } else {

                SynchronizationProcessUI synchProcess = new SynchronizationProcessUI();
                try {
                    Client c = new Client(file, 9191, log.getText(), pas.getText());
                    c.start();
                    int index = file.getPath().lastIndexOf(File.separator);
                    long time = System.currentTimeMillis();
                    new Timer(100 , new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (c.beginClientUI) {
                                synchProcess.synchForm.setVisible(true);
                                jFrame.setVisible(false);
                                synchProcess.barClientSynchronization(c, index);
                                synchProcess.barServerSynchronization(c,
                                        c.nameSourceFolderServerUI.lastIndexOf(File.separator));
                                ((Timer)e.getSource()).stop();
                            }
                            if (System.currentTimeMillis() - time > 6000) {

                                if (!c.startUI) {
                                    JOptionPane.showMessageDialog(null,
                                            "Login or password is incorrect",
                                            "Error",
                                            JOptionPane.PLAIN_MESSAGE);
                                } else if (!c.startUI2) {
                                    jFrame.setVisible(false);
                                    synchProcess.synchForm.setVisible(true);
                                    synchProcess.progressClient.setValue(100);
                                    synchProcess.progressServer.setValue(100);
                                    JOptionPane.showMessageDialog(null,
                                            "Folders already synchronized",
                                            "Error",
                                            JOptionPane.PLAIN_MESSAGE);
                                }
                                synch.setEnabled(true);
                                ((Timer)e.getSource()).stop();
                            }
                        }
                    }).start();

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
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

    public static void main(String[] args) {
        UserInterface app = new UserInterface();
        app.setVisible(true);
    }
}
