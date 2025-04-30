import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BeautifulScreenShot extends JFrame {

    private JButton captureButton, cancelButton;
    private JLabel countdownLabel, messageLabel;
    private JTextField delayTextField;
    private JProgressBar progressBar;
    private JPanel panel;
    private JLabel screenshotPreview;
    private BufferedImage lastScreenshot;
    private volatile boolean isCancelled = false;
    public static String currentUser = "guest";

    public BeautifulScreenShot(String currentUser1) {
        initComponents();
    }

    private void initComponents() {
        setTitle("JCapture");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 400);
        setLocationRelativeTo(null);
        setResizable(false);

        panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        countdownLabel = new JLabel("Enter Delay (seconds):");
        countdownLabel.setForeground(Color.WHITE);

        delayTextField = new JTextField(5);

        messageLabel = new JLabel(" ");
        messageLabel.setForeground(Color.YELLOW);

        captureButton = new JButton("Capture Screenshot");
        captureButton.addActionListener(e -> startCountdown());

        cancelButton = new JButton("Cancel Screenshot");
        cancelButton.addActionListener(e -> cancelScreenshot());

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        screenshotPreview = new JLabel();
        screenshotPreview.setPreferredSize(new Dimension(200, 100));

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(countdownLabel, gbc);

        gbc.gridx++;
        panel.add(delayTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(captureButton, gbc);

        gbc.gridy++;
        panel.add(cancelButton, gbc);

        gbc.gridy++;
        panel.add(progressBar, gbc);

        gbc.gridy++;
        panel.add(messageLabel, gbc);

        gbc.gridy++;
        panel.add(screenshotPreview, gbc);

        add(panel);
        setVisible(true);
    }

    private void startCountdown() {
        try {
            int delay = Integer.parseInt(delayTextField.getText().trim());

            if (delay <= 0) {
                messageLabel.setText("<html><font color='red'>Please enter a positive number.</font></html>");
                return;
            }

            String[] options = {"Full Screen", "Snipping Tool"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "Choose capture mode:",
                    "Capture Mode",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            isCancelled = false;
            new TimerThread(delay * 1000, choice == 1).start();

        } catch (NumberFormatException e) {
            messageLabel.setText("<html><font color='red'>Invalid input! Enter a positive number.</font></html>");
        }
    }

    private void cancelScreenshot() {
        isCancelled = true;
        messageLabel.setText("<html><font color='red'>Screenshot cancelled.</font></html>");
        progressBar.setValue(0);
    }

    private class TimerThread extends Thread {
        private int delay;
        private boolean useSnipTool;

        TimerThread(int delay, boolean useSnipTool) {
            this.delay = delay;
            this.useSnipTool = useSnipTool;
        }

        @Override
        public void run() {
            int seconds = delay / 1000;
            for (int i = seconds; i >= 0; i--) {
                if (isCancelled) return;
                messageLabel.setText("Capturing in " + i + " seconds...");
                progressBar.setValue((seconds - i) * 100 / seconds);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }

            if (!isCancelled) {
                if (useSnipTool) {
                    SwingUtilities.invokeLater(() -> {
                        setVisible(false);
                        new SnippingTool(currentUser, screenshotPreview, messageLabel, BeautifulScreenShot.this).setVisible(true);
                    });
                } else {
                    takeCapture();
                }
            }
        }
    }

    private void takeCapture() {
        try {
            if (isCancelled) return;

            setState(JFrame.ICONIFIED);
            Thread.sleep(1000);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Robot rt = new Robot();
            lastScreenshot = rt.createScreenCapture(new Rectangle(screenSize.width, screenSize.height));

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String directoryPath = "C:\\Users\\yosit\\OneDrive\\Desktop\\New folder\\project\\SAVEDSCREENCAPTURES\\" + currentUser;
            new File(directoryPath).mkdirs();
            String filePath = directoryPath + File.separator + "screenshot_" + timestamp + ".jpg";
            File outputfile = new File(filePath);
            ImageIO.write(lastScreenshot, "jpg", outputfile);

            SwingUtilities.invokeLater(() -> {
                setState(JFrame.NORMAL);
                messageLabel.setText("<html><font color='yellow'>Screenshot saved at:<br>" + filePath + "</font></html>");
                screenshotPreview.setIcon(new ImageIcon(lastScreenshot.getScaledInstance(200, 100, Image.SCALE_SMOOTH)));
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Inner class for SnippingTool
    class SnippingTool extends JWindow {
        private Point start, end;
        private BufferedImage screenCapture;
        private JLabel previewLabel;
        private JLabel msgLabel;
        private JFrame parent;

        SnippingTool(String user, JLabel previewLabel, JLabel msgLabel, JFrame parent) {
            this.previewLabel = previewLabel;
            this.msgLabel = msgLabel;
            this.parent = parent;

            try {
                Robot robot = new Robot();
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                screenCapture = robot.createScreenCapture(new Rectangle(screen));
            } catch (AWTException ex) {
                ex.printStackTrace();
            }

            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
            setBackground(new Color(0, 0, 0, 100));
            setAlwaysOnTop(true);

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    start = e.getPoint();
                    end = start;
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    end = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    end = e.getPoint();
                    captureAndSave(user);
                    dispose();
                    if (parent != null) {
                        parent.setVisible(true);
                    }
                }
            };

            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        private void captureAndSave(String user) {
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int w = Math.abs(end.x - start.x);
            int h = Math.abs(end.y - start.y);

            if (w == 0 || h == 0) return;

            BufferedImage cropped = screenCapture.getSubimage(x, y, w, h);

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String dir = "C:\\Users\\yosit\\OneDrive\\Desktop\\New folder\\project\\SAVEDSCREENCAPTURES\\" + user;
            new File(dir).mkdirs();

            File output = new File(dir + File.separator + "snip_" + timestamp + ".jpg");
            try {
                ImageIO.write(cropped, "jpg", output);
                msgLabel.setText("<html><font color='yellow'>Snip saved:<br>" + output.getAbsolutePath() + "</font></html>");
                previewLabel.setIcon(new ImageIcon(cropped.getScaledInstance(200, 100, Image.SCALE_SMOOTH)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (start != null && end != null) {
                int x = Math.min(start.x, end.x);
                int y = Math.min(start.y, end.y);
                int w = Math.abs(end.x - start.x);
                int h = Math.abs(end.y - start.y);

                g.setColor(Color.RED);
                g.drawRect(x, y, w, h);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BeautifulScreenShot("guest"));
    }
}
