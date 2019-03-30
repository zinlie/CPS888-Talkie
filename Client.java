package cps888.finalproject;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class Client extends JFrame implements Runnable, ActionListener {

    private static Socket cSocket = null;
    private static PrintStream wr = null;
    private static DataInputStream rd = null;

    private static InputStream buf = null;
    private static BufferedReader inputLine = null;
    private static boolean closed = false;

    private Client c;
    
    private JButton exitBtn, sendBtn;
    private JPanel mainPnl, consolePnl, exitPnl, lblPnl, msgPnl;
    private JLabel topLabel;
    private JScrollPane scroll;
    private JTextArea console;
    private static JTextField field;

    public Client(String title) {
        super(title);
        setSize(800, 600);

        mainPnl = new JPanel();
        mainPnl.setLayout(new BoxLayout(mainPnl, BoxLayout.Y_AXIS));
        mainPnl.add(Box.createVerticalStrut(5));
        lblPnl = new JPanel();
        topLabel = new JLabel("Secure Communications Console:\n");
        lblPnl.add(topLabel, SwingConstants.CENTER);
        mainPnl.add(lblPnl);
        console = new JTextArea("COMMUNICATION LINK OPENED - CLIENT SIDE CONSOLE:" + "\n----------------------------------------------\n\n", 25, 60);
        console.setLineWrap(true);
        console.setEditable(false);
        scroll = new JScrollPane(console);
        scroll.createVerticalScrollBar();

        consolePnl = new JPanel();
        consolePnl.add(scroll);
        mainPnl.add(Box.createVerticalStrut(5));
        mainPnl.add(consolePnl);

        msgPnl = new JPanel();
        field = new JTextField("", 30);
        msgPnl.add(field);
        sendBtn = new JButton("Send");
        sendBtn.addActionListener(this);
        msgPnl.add(sendBtn);
        mainPnl.add(msgPnl);

        exitPnl = new JPanel();
        exitBtn = new JButton("Exit");
        exitBtn.addActionListener(this);
        exitPnl.add(exitBtn);
        mainPnl.add(exitPnl);

        getContentPane().add(mainPnl);

        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent btnPress) {

        String label = ((JButton) btnPress.getSource()).getLabel();

        if (label.equals("Send")) {
            try {
                buf = new ByteArrayInputStream(field.getText().getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            field.setText("");
        }

        if (label.equals("Exit")) {
            wr.println("/quit");
            System.exit(0);
        }
    }

    public JTextArea getConsole() {
        return this.console;
    }

    public static void main(String[] args) {
        
        try {
            cSocket = new Socket("localhost", 4567);
            wr = new PrintStream(cSocket.getOutputStream());
            rd = new DataInputStream(cSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (cSocket != null && wr != null && rd != null) {
            try {
                /* Create a thread to read from the server. */
                Client c = new Client("Talkie - Client");
                new Thread(c).start();

                while (!closed) {
                    
                        while (buf == null) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        inputLine = new BufferedReader(new InputStreamReader(buf));
                        wr.println(inputLine.readLine().trim());

                    buf = null;
                }
                
          //Close the input/output streams and close the socket.
                
                wr.close();
                rd.close();
                cSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Create a thread to read from the server.
    public void run() {
    //Break loop when code 101 is received from the server (TERMINATE).
        String responseLine;
        try {
            while ((responseLine = rd.readLine()) != null) {
                getConsole().append(responseLine + "\n");
                if (responseLine.indexOf("CODE101") != -1) {
                    break;
                }
            }
            closed = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
