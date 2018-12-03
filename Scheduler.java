import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.Set;
import java.lang.Thread;
import java.lang.Thread.State;

public class Scheduler{

    class CPUThread extends Thread {
        
        Process processOnThread;
        Semaphore sem;

        CPUThread(Process p) {
            processOnThread = p;
            sem = new Semaphore(1); 
        }

        public void setProcess(Process p){
            processOnThread = p;
        }
        public Process getProcess(){
            return processOnThread;
        }

        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    sem.acquire();
                    onCpu.executeCycle();
                    sem.release();
                }
                catch(InterruptedException e) { System.err.println(e); }
            }
        }
    }

    private final int NUM_THREADS = 5; // take in x processes in batches, one for each thread
    private CPU onCpu;
    private LinkedList<Process> readyQueue;
    private HashMap<String, IODevice> ioDevices; //key: device name, value: device object
    private CPUThread[] threads;
    
    //Processes ready to add to CPU but use conflicting resouces
    private HashMap<String, LinkedList<Process>> resourceQueues; //key: name of resource used, value: processes waiting to accesss it
    private LinkedList<Resource> resources;
    
    public Scheduler(){
        this.readyQueue = new LinkedList<Process>();
        this.ioDevices = new HashMap<String, IODevice>();
        this.threads = new CPUThread[NUM_THREADS];
        this.resourceQueues = new HashMap<String, LinkedList<Process>>();
        this.resources = new LinkedList<Resource>();
    }
    public Scheduler(CPU onCpu){
        this.readyQueue = new LinkedList<Process>();
        this.ioDevices = new HashMap<String, IODevice>();
        this.onCpu = onCpu;
        this.resourceQueues = new HashMap<String, LinkedList<Process>>();
        this.threads = new CPUThread[NUM_THREADS];
        this.resources = new LinkedList<Resource>();
    }
    public Scheduler(LinkedList<Process> processQueue, HashMap<String, IODevice> ioDevices, CPU onCpu){
        this.readyQueue = processQueue;
        this.ioDevices = ioDevices;
        this.onCpu = onCpu;
        this.resourceQueues = new HashMap<String, LinkedList<Process>>();
        this.threads = new CPUThread[NUM_THREADS];
        this.resources = new LinkedList<Resource>();
    }
    public Scheduler(LinkedList<Process> processQueue, HashMap<String, IODevice> ioDevices, CPU onCpu, HashMap<String, LinkedList<Process>> resourceQueues, LinkedList<Resource> resources){
        this.readyQueue = processQueue;
        this.ioDevices = ioDevices;
        this.onCpu = onCpu;
        this.resourceQueues = resourceQueues;
        this.threads = new CPUThread[NUM_THREADS];
        this.resources = resources;
    }

    /**
     * @return the ioDevices
     */
    public HashMap<String, IODevice> getIoDevices() {
        return ioDevices;
    }
    /**
     * @return the readyQueue
     */
    public LinkedList<Process> getReadyQueue() {
        return readyQueue;
    }

    public String getReadyString() {
        return readyQueue.toString();
    }

    public String getWaitString(){
        String waitStr = "";
        for(String ioName : ioDevices.keySet()){
            IODevice io = ioDevices.get(ioName);
            waitStr += io.toString() + "\n";
        }
        return waitStr;
    }

    public String getRunString(){
        String run = "null";
        if(onCpu.getRunningProcess() != null){
            run = onCpu.getRunningProcess().toString();
        }
        return run;
    }

    /**
     * @return the onCpu
     */
    public CPU getOnCpu() {
        return onCpu;
    }
    /**
     * @return the resourceQueue
     */
    public HashMap<String, LinkedList<Process>> getResourceQueues() {
        return resourceQueues;
    }
    /**
     * @return the threads
     */
    public CPUThread[] getThreads() {
        return threads;
    }
    
    public void addResource(Resource r){
        resources.add(r);
        resourceQueues.put(r.getName(), new LinkedList<Process>());
    }

    public void addToQueue(Process p){
        p.setState(Process.ProcessState.READY);
        readyQueue.add(p);
        System.out.println("Core: " + onCpu.getCoreNum() + " Process " + p.getPid() + " was added to the ready queue");
        if(onCpu.getRunningProcess() == null && !isEmpty()){ 
            setRunningProcess(readyQueue.poll());
        }
    }

    public void addIODevice(IODevice io){
        this.ioDevices.put(io.getDeviceName(), io);
    }

    public void setRunningProcess(){
        setRunningProcess(readyQueue.poll());
    }

    public void setRunningProcess(Process processToRun){
        if(processToRun != null){
            processToRun.setState(Process.ProcessState.RUNNING);
    
            //Use resource
            String resUsed = processToRun.getResourceUsed();
            for(Resource r : resources){
                if(r.getName().equals(resUsed)){
                    int numNeeded = processToRun.getResources().get(r.getName());
                    if(r.getNumAvailable() >= numNeeded){
                        r.use(numNeeded);
                    }
                    else { 
                        System.out.println("Core: " + onCpu.getCoreNum() + " Not enough of resource " + r.getName() + " available for process " 
                                            + processToRun.getPid() + ", setting on resource queue");
                        resourceQueues.get(r.getName()).add(processToRun);
                        setRunningProcess();
                        return;
                    }
                }
            }
        }

        onCpu.setRunningProcess(processToRun);

        int newThreadNum = getOpenThread();
        while(newThreadNum == -1){
            newThreadNum = getOpenThread();
        }
        threads[newThreadNum] = new CPUThread(processToRun);
        threads[newThreadNum].start();
    }

    public int getOpenThread(){
        for(int i=0; i<threads.length; i++){
            if(threads[i] == null || threads[i].getState() == State.TERMINATED) return i;
        }
        return -1;
    }

    /**
     * @param onCpu the CPU to set
     */
    public void setOnCpu(CPU onCpu) {
        this.onCpu = onCpu;
    }

    public void assess(){
        if(onCpu.getRunningProcess().getState() != Process.ProcessState.RUNNING){
            interrupt();
        }
    }

    /**
    *  Causes an interrupt of the process currently running in the CPU
    */
    public void interrupt(){
        Process interruptedProcess = onCpu.getRunningProcess();

        //Handling case where process is waiting
        if(interruptedProcess != null && interruptedProcess.getState() == Process.ProcessState.WAITING){
            handleWait();
        }

        //Handling case where process is terminated
        if(interruptedProcess != null && interruptedProcess.getState() == Process.ProcessState.TERMINATED){
            System.out.println("Core: " + onCpu.getCoreNum() + " Process " + interruptedProcess.getPid() + " terminated and memory was deallocated ");
            onCpu.getMemory().deallocate(interruptedProcess.getPageTable().values());
        }

        //Release resource
        String resourceUsed = interruptedProcess.getResourceUsed();
        for(Resource res : resources){
            if(res.getName().equals(resourceUsed)){
                res.release(interruptedProcess.getResources().get(resourceUsed));
                System.out.println("Core: " + onCpu.getCoreNum() + " Process " + interruptedProcess.getPid() + " releasing resource " + resourceUsed);
                if(!resourceQueues.get(resourceUsed).isEmpty()){
                    Process fromQueue = resourceQueues.get(resourceUsed).poll();
                    setRunningProcess(fromQueue);
                    System.out.println("Core: " + onCpu.getCoreNum() + " Process " + fromQueue.getPid() + " removed from wait queue for resource " + resourceUsed);
                    return;
                }
            }
        }

        Thread.currentThread().interrupt();

        //Setting new running process
        setRunningProcess();
        if(isEmpty()){
            System.out.println("Core: " + onCpu.getCoreNum() + " Ready queue is empty, so no process running");
        }
        else {
            System.out.println("Core: " + onCpu.getCoreNum() + " Process " + onCpu.getRunningProcess().getPid() + " set to running state");
        }
    }

    public void handleWait(){
        Process interruptedProcess = onCpu.getRunningProcess();

        System.out.println("Core: " + onCpu.getCoreNum() + " Process " + interruptedProcess.getPid() + 
                            " waiting for I/O device " + interruptedProcess.getWaitingDevice());
        // could also be waiting for child...
        addProcessToWait(interruptedProcess);
    }

    // Create child from this process
    public Process forkChild(Process p){
        int[] childText = { p.getText()[p.getProcessCounter()]/2 }; //Giving half of this line to the child
        p.getText()[p.getProcessCounter()] = p.getText()[p.getProcessCounter()]/2;
        Process child = new Process(p.getPid()*100, childText, p.getPriority(), 
                                    p.getPagesAccessedThisLine(), p.getResources(), p);
        p.setChild(child);
        p.setState(Process.ProcessState.WAITING);
        return child;
    }

    public boolean isEmpty(){
        return readyQueue.isEmpty();
    }

    public void addProcessToWait(Process p){
        if(ioDevices.containsKey(p.getWaitingDevice())){
            IODevice io = ioDevices.get(p.getWaitingDevice());
            io.addProcess(p);
        }
    }

    public void interruptThreads(){
        for(CPUThread th : threads){
            if(th != null)
                th.interrupt();
        }
    }

    public void decrementWaits(){
        Set<String> ioNames = ioDevices.keySet();
        for(String ioName : ioNames){
            IODevice io = ioDevices.get(ioName);
            if(io.getActiveProcess() != null){
                Process p = io.getActiveProcess();
                io.decrementWait();
                if(io.getActiveProcess() == null || io.getActiveProcess().getPid() != p.getPid()){
                    System.out.println("Core: " + onCpu.getCoreNum() + " Process " + p.getPid() + " finished waiting in device " 
                                        + io.getDeviceName() + ", returning to ready queue");
                    addToQueue(p);
                }
            }
        }
    }

}