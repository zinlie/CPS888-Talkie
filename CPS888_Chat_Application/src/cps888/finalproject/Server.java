package cps888.finalproject;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

public class Server extends JFrame implements ActionListener {

    private static Socket cSocket = null;
    private static ServerSocket sSocket = null;
    private FileWriter fw = null;

    private JButton exitBtn;
    private JPanel mainPnl, consolePnl, exitPnl, lblPnl;
    private JLabel topLabel;
    private JScrollPane scroll;
    private JTextArea console;

    private static final ArrayList<ClientThread> threads = new ArrayList<ClientThread>();

    public Server(String title) {
        super(title);
        setSize(800, 600);

        mainPnl = new JPanel();
        mainPnl.setLayout(new BoxLayout(mainPnl, BoxLayout.Y_AXIS));
        mainPnl.add(Box.createVerticalStrut(5));
        lblPnl = new JPanel();
        topLabel = new JLabel("Secure Communications Console:\n");
        lblPnl.add(topLabel, SwingConstants.CENTER);
        mainPnl.add(lblPnl);
        console = new JTextArea("COMMUNICATION LINK OPENED - SERVER SIDE CONSOLE:" + "\n----------------------------------------------\n\n", 25, 60);
        console.setLineWrap(true);
        console.setEditable(false);
        scroll = new JScrollPane(console);
        scroll.createVerticalScrollBar();

        consolePnl = new JPanel();
        consolePnl.add(scroll);
        mainPnl.add(Box.createVerticalStrut(5));
        mainPnl.add(consolePnl);

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
        System.exit(0);
    }

    public JTextArea getConsole() {
        return this.console;
    }

    public FileWriter getWriter() {
        return fw;
    }

    public static void main(String args[]) {

        Server server = new Server("Talkie - Server");

        try {
            sSocket = new ServerSocket(4567);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Create a client socket for each connection and create a corresponding client thread.
        while (true) {
            try {
                cSocket = sSocket.accept();

                threads.add(new ClientThread(server, cSocket, threads));
                threads.get(threads.size() - 1).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class ClientThread extends Thread {

    private DataInputStream is = null;
    private PrintStream os = null;
    private Socket clientSocket = null;
    private final ArrayList<ClientThread> threads;
    private Server server;
    private FileWriter fw, chatWriter;
    private BufferedReader br;
    private String usr_contents = "";

    public ClientThread(Server server, Socket clientSocket, ArrayList<ClientThread> threads) {
        this.server = server;
        this.clientSocket = clientSocket;
        this.threads = threads;
    }

    public void createUser(String name, String passwd) {
        try {
            fw = new FileWriter("users.txt");
            usr_contents += (name + " " + passwd + "\n");
            usr_contents += "usr_end";
            fw.write(usr_contents);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        ArrayList<ClientThread> threads = this.threads;

        //Check credentials and create input and output streams for this client.
        try {
            boolean passCheck = false, user_found = false;
            String name = "", passwd = "", line = "";

            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());

            while (!passCheck) {

                os.println("//***// Enter your username to login. //***//");
                name = is.readLine().trim();

                os.println("//***// Enter your password. //***//");
                passwd = is.readLine().trim();

                br = new BufferedReader(new FileReader("users.txt"));

                do {
                    line = br.readLine().trim();

                    if (line.equals("usr_end")) {
                        break;
                    }
                    usr_contents += line + "\n";
                    String[] split = line.split(" ");

                    if (name.equals(split[0])) {
                        user_found = true;
                    }

                    if (name.equals(split[0]) && passwd.equals(split[1])) {
                        passCheck = true;
                        user_found = true;
                        break;
                    }
                } while (!(line.equals("usr_end")));

                if (!passCheck && user_found) {
                    os.println("//***// Password incorrect. Please try again. //***//");
                    usr_contents = "";
                }
                if (!user_found) {
                    break;
                }

            }
            
            if (!user_found) {
                    os.println("//***// New user " + name + " has created an account. //***//");
                    createUser(name, passwd);
                }

            server.getConsole().append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ": User " + name + " has connected.\n");

            os.println("//***// Welcome to the group chat, " + name + "! //***//");

            BufferedReader history = new BufferedReader(new FileReader("chat_history.txt"));
            String history_line;
            while ((history_line = history.readLine()) != null) {
                os.println(history_line);
            }

            for (int i = 0; i < threads.size(); i++) {
                if (threads.get(i) != null && threads.get(i) != this) {
                    threads.get(i).os.println("//***// User " + name + " has joined the group chat. //***//");
                }
            }
            while (true) {
                String txtline = is.readLine();
                if (txtline.startsWith("/quit")) {
                    server.getConsole().append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ": User " + name + " has disconnected.\n");
                    break;
                }

                for (int i = 0; i < threads.size(); i++) {
                    if (threads.get(i) != null) {
                        threads.get(i).os.println(new SimpleDateFormat("yyyy-MM-dd hh:mm a:").format(Calendar.getInstance().getTime()) + " @" + name + ": " + txtline);
                        chatWriter = new FileWriter("chat_history.txt", true);
                        chatWriter.write(new SimpleDateFormat("yyyy-MM-dd hh:mm a:").format(Calendar.getInstance().getTime()) + " @" + name + ": " + txtline + "\n");
                        chatWriter.close();
                    }
                }
            }
            for (int i = 0; i < threads.size(); i++) {
                if (threads.get(i) != null && threads.get(i) != this) {
                    threads.get(i).os.println("//***// User " + name + " has left the group chat. //***//");
                }
            }
            os.println("CODE101");

            //Remove deprecated threads from the ArrayList
            for (int i = 0; i < threads.size(); i++) {
                if (threads.get(i) == this) {
                    threads.remove(i);
                }
            }

            /*
       * Close the output stream, close the input stream, close the socket.
             */
            is.close();
            os.close();
            clientSocket.close();
        } catch (IOException e) {
        }
    }
}
