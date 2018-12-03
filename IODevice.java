import java.util.LinkedList;


public class IODevice{

    private String deviceName;
    private Process activeProcess;
    private int activeProcessWaitTime;
    private LinkedList<Process> queue;

    public IODevice(String name){
        this.deviceName = name;
        this.queue = new LinkedList<Process>();
    }
    
    public IODevice(String name, LinkedList<Process> queue){
        this.deviceName = name;
        this.queue = queue;
    }

    // Accessors
    /**
     * @return the deviceName
     */
    public String getDeviceName() {
        return deviceName;
    }
    /**
     * @return the activeProcess
     */
    public Process getActiveProcess() {
        return activeProcess;
    }
    /**
     * @return the activeProcessWaitTime
     */
    public int getActiveProcessWaitTime() {
        return activeProcessWaitTime;
    }
    /**
     * @return the queue
     */
    public LinkedList<Process> getQueue() {
        return queue;
    }

    /**
     * Add a process to the back of the queue
     */
    public void addProcess(Process p){
        if(activeProcess == null){
            activeProcess = p;
            activeProcessWaitTime = p.getWaitingTime();
        }
        else queue.add(p);
    }

    /**
     * Run one clock cycle on this waiting process
     */
    public void decrementWait(){
        if(activeProcess != null){
            activeProcessWaitTime--;
            System.out.println("\tDecrementing wait for process " + activeProcess.getPid()
                                + " in I/O device " + deviceName
                                + " wait time is now " + activeProcessWaitTime);
            if(activeProcessWaitTime <= 0 && !queue.isEmpty()){
                activeProcess = queue.poll();
                activeProcessWaitTime = activeProcess.getWaitingTime();
            }
            else if(activeProcessWaitTime <= 0){
                activeProcess = null;
                activeProcessWaitTime = 0;
            }
        }
    }

    /**
     * Replaces active process with first in queue
     */
    public void updateActiveProcess(){
        if(queue.peek() != null) activeProcess = queue.poll();
        else activeProcess = null;
    }

    @Override
    public String toString(){
        String pid = (activeProcess != null) ? String.valueOf(activeProcess.getPid()) : "nothing";
        return "I/O Device " + deviceName + ", currently processing: " + pid + ", processes in queue: " + queue + "\n";
    }
}