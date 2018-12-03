import java.awt.*;
import javax.swing.*;
import javafx.scene.layout.Border;
import java.util.*;
import java.io.*;
import java.awt.event.*;

public class Test {
    JTextArea incoming = new JTextArea(32, 32);
    JTextArea processing = new JTextArea(32, 32);
    JTextArea finished = new JTextArea(32, 32);
    JTextArea user = new JTextArea(12, 50);
    JButton run = new JButton("Run");
    JButton pause = new JButton("Pause");
    JButton resume = new JButton("Resume");
    JButton add = new JButton("Add Process");
    JButton print = new JButton("Print all queues");
    JButton stop = new JButton("Stop");
    public static void main(String[] args) {
        new Test();
    }

    public Test() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Operating Systems Simulator");
                frame.setSize(1500, 1500);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new WholePane());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    public class UserPane extends JPanel {
        public UserPane() {
            setLayout(new FlowLayout());

            add(run);
            add(pause);
            add(resume);
            add(add);
            add(print);
            add(stop);
        }
    }

    public class WholePane extends JPanel {

        public WholePane() {
            setLayout(new BorderLayout());

            processing.setBackground(Color.lightGray);
            user.setBackground(Color.gray);

            incoming.setEditable(false);
            processing.setEditable(false);
            finished.setEditable(false);
            user.setEditable(false);

            // pause.setEnabled(false);
            resume.setEnabled(false);
            add.setEnabled(false);
            print.setEnabled(false);
            // stop.setEnabled(false);


            add(new UserPane(), BorderLayout.NORTH);
            add(new JScrollPane(incoming), BorderLayout.WEST);
            add(new JScrollPane(processing), BorderLayout.CENTER);
            add(new JScrollPane(finished), BorderLayout.EAST);
            add(new JScrollPane(user), BorderLayout.SOUTH);

            // Add behavior for run
            run.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                }
            });
        }
    }

    }