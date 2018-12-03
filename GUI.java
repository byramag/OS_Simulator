
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.util.Hashtable;

// import org.knowm.xchart.QuickChart;
// import org.knowm.xchart.SwingWrapper;
// import org.knowm.xchart.XYChart;

public class GUI implements ActionListener {

        int speed;

        BorderLayout layout;

        JLabel commandLabel;
        JTextField command;
        JButton runCommandButton;

        JButton endButton;
        JSlider cpuSpeedSlider;
        JTextArea log;

        public GUI(){
                // Schedule a job for the event-dispatching thread:
                // creating and showing this application's GUI.
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                                createAndShowGUI();
                        }
                });
        }

        private void createAndShowGUI() {
                // Create and set up the window.
                JFrame f = new JFrame("OS Simulator");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                setLayout(f);

                commandLabel = new JLabel("Command:");
                command = new JTextField(30);
                command.setSize(5, 5);

                runCommandButton = new JButton("Run");
                runCommandButton.addActionListener(this);

                cpuSpeedSlider = new JSlider();
                Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
                table.put(0, new JLabel("Paused"));
                table.put(33, new JLabel("Slow"));
                table.put(67, new JLabel("Fast"));
                table.put(100, new JLabel("Full Speed"));
                cpuSpeedSlider.setLabelTable(table);
                cpuSpeedSlider.setPaintLabels(true);
                cpuSpeedSlider.setSnapToTicks(true);

                log = new JTextArea(5, 25);
                log.setLineWrap(true);
                log.setEditable(false);
                log.setVisible(true);
                JScrollPane scroll = new JScrollPane(log);
                scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                f.add(scroll);

                log.append("THis is log text");
                log.setEditable(false);

                endButton = new JButton("End Simulation");

                // JTable runningProcess = new JTable();
                // JTable readyProcesses = new JTable();
                // JTable waitQueues = new JTable();
                // // J___ cacheGraph = new J___();
                // // J___ mmGraph = new J___();
                // // J___ vmGraph = new J___();

                // Display the window.
                f.pack();
                f.setVisible(true);

                while(true){
                        String[] words = { "a", "b", "c" };
                        updateGUI(words);
                        try { Thread.sleep(1000); }
                        catch(InterruptedException e) {}
                }

        }

        public void setLayout(JFrame f){
                f.setLayout(new BorderLayout());
                f.setMinimumSize(new Dimension(800, 600));
        }
        
        public void updateGUI(String[] toLog){
                for(String str : toLog) log.append(str);
        }

        public void actionPerformed(ActionEvent e) {
                if(e.getSource().equals(runCommandButton)){
                        // runCommand(command.getText());
                        command.setText("");
                }
        }
}