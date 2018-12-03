/* Orchestrates running of simple process/schduler/CPU/memory simulation */

import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.*;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.*;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.util.Hashtable;

/**
 * TODOs
 * 
 * Multi-threading - debug
 * Critical section resolving scheme - make better
 * multi-core via simulation - debug
 *   
 * parent-child - fix, show use
 * interprocess communication methods - show use
 * 
 */

public class Simulator implements ActionListener{

    // Create global objects
    static CPU cpu1;
    static CPU cpu2;
    static Scheduler scheduler1;
    static Scheduler scheduler2;
    static Memory memory;

    static boolean run;
    static int numProcesses;
    static int speed;
    static Random rand = new Random();
    static Scanner userInput = new Scanner(System.in);

    static BorderLayout layout;

    static JLabel commandLabel;
    static JTextField command;
    static JButton runCommandButton;

    static JTextField numInitialProcesses;
    static JButton startButton;
    static JButton endButton;
    static JSlider cpuSpeedSlider;
    static JTextArea log;

    static JTextArea runningProcess;
    static JTextArea readyProcesses;
    static JTextArea waitQueues;
    
    public Simulator(){
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                    createAndShowGUI();
            }
        });
    }
    public static void main(String[] args){

        scheduler1 = new Scheduler(); //FCFS
        // scheduler1 = new RoundRobinScheduler();
        // scheduler1 = new PriorityQueueScheduler(); //with preemption and aging
        // scheduler1 = new MultiLevelQueueScheduler();
        
        //IO devices available to the system
        scheduler1.addIODevice(new IODevice("a"));
        scheduler1.addIODevice(new IODevice("b")); 
        scheduler1.addIODevice(new IODevice("c"));
        //Other resources available to the system
        scheduler1.addResource(new Resource("a", 8));
        scheduler1.addResource(new Resource("b", 6));
        scheduler1.addResource(new Resource("c", 5));
        
        scheduler2 = new Scheduler(); //FCFS
        // scheduler2 = new RoundRobinScheduler();
        // scheduler2 = new PriorityQueueScheduler(); //with preemption and aging
        // scheduler2 = new MultiLevelQueueScheduler();
        
        //IO devices available to the system
        scheduler2.addIODevice(new IODevice("a"));
        scheduler2.addIODevice(new IODevice("b")); 
        scheduler2.addIODevice(new IODevice("c"));
        //Other resources available to the system
        scheduler2.addResource(new Resource("a", 8));
        scheduler2.addResource(new Resource("b", 6));
        scheduler2.addResource(new Resource("c", 5));

        memory = new Memory();
        cpu1 = new CPU(scheduler1, memory, 1000, 1);
        cpu2 = new CPU(scheduler2, memory, 1000, 2);

        // run = true;
        // run();

        // Simulator gui = new Simulator();
        // while(true){
        //     if(gui != null) run(gui);
        // }
    }

    public static void run(){
        // Create processes
        for(int i=0; i<numProcesses; i++){
            createRandomizedProcess(i%5+1, i);
        }

        int cycleCount = 0;
        while(run){
            cycleCount++;

            //Chance to add a new process each cycle
            if(rand.nextInt(250) == 1) 
                createRandomizedProcess(cycleCount%5+1, numProcesses++);

            //Periodically causes an external I/O interrupt on running process
            if(rand.nextInt(300) == 1)
                ioInterrupt("a", 20, rand.nextInt(2));

            //Check if command entered
            try { if(System.in.available() > 0) runCommand(userInput.next()); }
            catch(IOException e) { System.err.println(e); }
        }
    }

    public static void run(Simulator gui){

        // Create processes
        for(int i=0; i<numProcesses; i++){
            createRandomizedProcess(i%5+1, i);
        }

        // ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // PrintStream ps = new PrintStream(baos);
        // PrintStream old = System.out;
        // System.setOut(ps);

        int cycleCount = 0;
        while(run){
            cycleCount++;

            //Chance to add a new process each cycle
            if(rand.nextInt(250) == 1) 
                createRandomizedProcess(cycleCount%5+1, numProcesses++);

            //Periodically causes an external I/O interrupt on running process
            if(rand.nextInt(300) == 1)
                ioInterrupt("a", 20, rand.nextInt(2));

            //Check if command entered
            try { if(System.in.available() > 0) runCommand(userInput.next()); }
            catch(IOException e) { e.printStackTrace(); }

            // gui.updateGUI(baos.toString());
            gui.updateGUI("hi");
        }
    }

    public static void setSpeed(int speed){
        cpu1.setSpeed(5001-speed);
        cpu2.setSpeed(5001-speed);
    }

    public static void runCommand(String command){
        String[] parts = command.split(" ");
        if (parts.length > 0 && parts[0].equalsIgnoreCase("add")){
            int newProcessType = 1;
            if(parts.length > 1) newProcessType = Integer.parseInt(parts[1]);
            createRandomizedProcess(newProcessType, numProcesses++);
        }
        else if (parts.length > 0 && parts[0].equalsIgnoreCase("io")){
            String deviceName = "a";
            int time = 10;
            if(parts.length > 1) deviceName = parts[1];
            if(parts.length > 2) time = Integer.parseInt(parts[2]);
            ioInterrupt(deviceName, time, rand.nextInt(2));
        }
        else if(command.equals("stop")) System.exit(0);
        else if (parts.length > 0 && parts[0].equalsIgnoreCase("slow")){
            int speed = 100;
            if(parts.length > 1) speed = Integer.parseInt(parts[1]);
            cpu1.setSpeed(speed);
            cpu2.setSpeed(speed);
        }
        else if (command.equalsIgnoreCase("pause")){
            while(!command.equalsIgnoreCase("resume")){
                if(command.equalsIgnoreCase("print")){
                    command = userInput.next();
                    if(command.equalsIgnoreCase("wait")){
                        System.out.println(cpu1.getScheduler().getWaitString());
                        System.out.println(cpu2.getScheduler().getWaitString());
                    }
                    else if(command.equalsIgnoreCase("ready")){ 
                        System.out.println(cpu1.getScheduler().getReadyQueue());
                        System.out.println(cpu2.getScheduler().getReadyQueue());
                    }
                    else if(command.equalsIgnoreCase("running")){ 
                        System.out.println(cpu1.getRunningProcess().toString());
                        System.out.println(cpu2.getRunningProcess().toString());
                    }
                    else if(command.equalsIgnoreCase("memory")){
                        System.out.println(memory.toString());
                    }
                }
                else if(command.equalsIgnoreCase("stop"))  System.exit(0);
                command = userInput.next();
            }
        }
    }

    public static void ioInterrupt(String deviceName, int time, int cpuNum){
        CPU cpu;
        if(cpuNum == 1) cpu = cpu1;
        else cpu = cpu2;

        Process runningProcess = cpu.getRunningProcess();
        if(runningProcess != null){
            runningProcess.waitForIO(deviceName + " " + time);
            System.out.println("External interrupt for I/O device " + deviceName +
                                " on running process " + runningProcess.getPid());
            cpu.getScheduler().interrupt();
        }
    }

    public static String[][] getTableData(String type){
        if(type.equals("run")){
            String[][] running = new String[2][3];
            if(cpu1.getRunningProcess() != null && cpu2.getRunningProcess() != null){
                running[0] = cpu1.getRunningProcess().getInfo(); 
                running[1] = cpu2.getRunningProcess().getInfo();
            }
            return running;
        }
        else if(type.equals("ready")){
            LinkedList<Process> q1 = scheduler1.getReadyQueue();
            LinkedList<Process> q2 = scheduler2.getReadyQueue();
            String[][] ready = new String[q1.size()+q2.size()][3];
            for(int i=0; i<q1.size()+q2.size(); i++){
                if(q1.size() > 0)
                    ready[i] = q1.poll().getInfo();
                else if(q2.size() > 0)
                    ready[i] = q2.poll().getInfo();
            }
            return ready;
        }
        else if(type.equals("wait")){
            return new String[3][3];
        }
        return null;
    }

    public static void loadProcessIntoMemory(Process p){
        boolean loaded = memory.loadWholeProcess(p);
        if(loaded) { // Randomly select to add to CPU 1 or 2
            if(rand.nextInt(2) == 1)
                scheduler1.addToQueue(p);
            else
                scheduler2.addToQueue(p);
        }
    }

    public static int parseRange(String range){
        String[] vals = range.split("-");
        int min = Integer.parseInt(vals[0]);
        int max = Integer.parseInt(vals[1]);
        int result = rand.nextInt(max-min+1)+min;
        return result;
    }

    public static void createRandomizedProcess(int type, int pid){ 
        // read correct file for type
        String fileName = "process_templates/proc";
        fileName += type%5+1;
        fileName += ".json";

        String processString = "";
        try{
            Scanner sc = new Scanner(new File(fileName));
            while(sc.hasNextLine()){
                processString += sc.nextLine();
            }
            sc.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        System.out.println(processString);

        //Default process values
        int[] computations = {10, 10};
        HashMap<Integer, String> io = new HashMap<Integer, String>();

        //Parse file contents
        Pattern calcPattern = Pattern.compile("\"calculate\"\\s*:\\s*\\[\\s*([^\\]]+)");
        Pattern ioPattern = Pattern.compile("\"i/o\"\\s*:\\s*\\{\\s*([^\\}]*)");
        Matcher matchCalc = calcPattern.matcher(processString);
        Matcher matchIO = ioPattern.matcher(processString);

        //creating random calcualtion values in ranges
        if (matchCalc.find()) {
            String[] calcStrings = matchCalc.group(1).replaceAll("\"|\\s+", "").split(",");
            computations = new int[calcStrings.length];
            for (int i=0; i<computations.length; i++){
                computations[i] = parseRange(calcStrings[i]);
            }
        } else System.out.println("Error parsing file, creating default process");

        //creating randomized io wait times
        if (matchIO.find()) {
            String[] ioStrings = matchIO.group(1).replaceAll("\"|\\s\\s+", "").split(",");
            for( String str : ioStrings){
                if(str==null || str.equals("")) continue;
                String[] vals = str.split("\\s*:\\s*");
                int key = Integer.parseInt(vals[0]);
                vals = vals[1].split(" ");
                String device = vals[0];
                io.put(key, device + " " + parseRange(vals[1]));
            }
        }

        int priority = 0;
        Pattern priorityPattern = Pattern.compile("\"priority\"\\s*:\\s*(\\d+)");
        Matcher matchPriority = priorityPattern.matcher(processString);
        if(matchPriority.find())
            priority = Integer.parseInt(matchPriority.group(1));

        int memory = 0;
        Pattern memPattern = Pattern.compile("\"memory\"\\s*:\\s*\"([\\-\\d]+)\"");
        Matcher matchMem = memPattern.matcher(processString);
        if(matchMem.find())
            memory = parseRange(matchMem.group(1));

        HashMap<String, Integer> resources = new HashMap<String, Integer>();
        Pattern resPattern = Pattern.compile("\"resource\"\\s*:\\s*\"(.+)\"");
        Matcher matchRes = resPattern.matcher(processString);
        if(matchRes.find()){
            String[] resString = matchRes.group(1).split(" ");
            resources.put(resString[0], Integer.parseInt(resString[1]));
        }

        System.out.print("Creating Process " + pid + " with values\n\tCalc: ");
        for(int val:computations) System.out.print(val + ", ");
        System.out.print("\n\tIO: " + io);
        System.out.print("\n\tPriority: " + priority);
        System.out.print("\n\tMemory size: " + memory);
        System.out.println("\n\tResource(s) used: Resource " + resources + " instances");
        
        // instantiate process objects
        Process process = new Process(pid, computations, io, priority, memory, resources);
        loadProcessIntoMemory(process);
    }

    private void createAndShowGUI() {
        // Create and set up the window.
        JFrame f = new JFrame("OS Simulator");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setMinimumSize(new Dimension(800, 600));

        addComponentsToPane(f.getContentPane());

        // Display the window.
        f.pack();
        f.setVisible(true);
    }

    public void addComponentsToPane(Container pane) {

        JPanel controllerPanel = new JPanel();

        numInitialProcesses = new JTextField(5);
        startButton = new JButton("Begin Simulation");
        startButton.addActionListener(this);
        command = new JTextField(30);
        command.setSize(5, 5);
        runCommandButton = new JButton("Run");
        runCommandButton.addActionListener(this);
        endButton = new JButton("End Simulation");
        endButton.addActionListener(this);

        controllerPanel.add( numInitialProcesses );
        controllerPanel.add( startButton );
        controllerPanel.add( new JLabel("Command:") );
        controllerPanel.add( command );
        controllerPanel.add( runCommandButton );
        controllerPanel.add( endButton );

        pane.add(controllerPanel, BorderLayout.PAGE_START);

        cpuSpeedSlider = new JSlider();
        Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
        table.put(0, new JLabel("Paused"));
        table.put(33, new JLabel("Slow"));
        table.put(67, new JLabel("Fast"));
        table.put(100, new JLabel("Full Speed"));
        cpuSpeedSlider.setLabelTable(table);
        cpuSpeedSlider.setPaintLabels(true);
        pane.add(cpuSpeedSlider, BorderLayout.PAGE_END);

        log = new JTextArea();
        log.setPreferredSize(new Dimension(200, 100));
        log.setLineWrap(true);
        log.setEditable(false);
        pane.add(new JScrollPane(log), BorderLayout.CENTER);


        JPanel processesPanel = new JPanel();

        runningProcess = new JTextArea(scheduler1.getRunString()+"\n"+scheduler2.getRunString());
        runningProcess.setLineWrap(true);
        readyProcesses = new JTextArea(scheduler1.getReadyString()+"\n"+scheduler2.getReadyString());
        readyProcesses.setLineWrap(true);
        waitQueues = new JTextArea(scheduler1.getWaitString()+"\n"+scheduler2.getWaitString());
        waitQueues.setLineWrap(true);
        processesPanel.add( runningProcess );
        processesPanel.add( readyProcesses );
        processesPanel.add( waitQueues );

        pane.add(processesPanel, BorderLayout.LINE_START);

        // // J___ cacheGraph = new J___();
        // // J___ mmGraph = new J___();
        // // J___ vmGraph = new J___();
    }

    public void updateGUI(String toLog){
        if(log != null) log.append(toLog);
        if(cpuSpeedSlider != null) {
            cpu1.setSpeed(cpuSpeedSlider.getValue()*50+1);
            cpu2.setSpeed(cpuSpeedSlider.getValue()*50+1);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if(e.getSource().equals(runCommandButton)){
            runCommand(command.getText());
            command.setText("");
        }
        if(e.getSource().equals(startButton)){
            run = true;
            run(this);
            if(numInitialProcesses.getText() != null && !numInitialProcesses.getText().equals(""))
            numProcesses = Integer.parseInt(numInitialProcesses.getText());
            numInitialProcesses.setText("");
            endButton.setEnabled(true);
            startButton.setEnabled(false);
        }
        if(e.getSource().equals(endButton)){
            run = false;
            scheduler1.interruptThreads();
            scheduler2.interruptThreads();
            endButton.setEnabled(false);
            startButton.setEnabled(true);
        }
    }

}