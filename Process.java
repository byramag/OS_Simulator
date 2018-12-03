import java.util.ArrayList;
import java.util.HashMap;

public class Process implements Comparable<Process>{

    public enum ProcessState{
        NEW, READY, RUNNING, WAITING, TERMINATED
    }

    /*****************PROCESS CONTROL BLOCK****************** */
    private int pid;
    private ProcessState state;
    private int processCounter;
    private int priority;
    private int cpuTimeLeft;
    private int readyQueueAge;
    private Process child;
    private Process parent;
    
    private int waitingTime;
    private String waitingDevice;
    private HashMap<Integer,String> ioNeeds; // key: at processCounter value, value: time to wait in I/O queue 
    private HashMap<String,Integer> resources; //key: res name, value: number of instances needed

    private int memorySize;
    private int[] text;
    private HashMap<Integer, Integer> pageTable; // key: page in process sequence, value: page location in MM
    /********************************************************* */

    public Process(int pid, int[] text, HashMap<Integer,String> ioNeeds, int priority, int memory, HashMap<String,Integer> resources){
        this.state = ProcessState.NEW;
        setText(text);
        setIoNeeds(ioNeeds);
        processCounter = 0;
        setCpuTimeLeft(text[0]);
        setPid(pid);
        setPriority(priority);
        setMemorySize(memory);
        setReadyQueueAge(0);
        setResources(resources);
        this.pageTable = new HashMap<Integer, Integer>();
    }
    
    //For constructing child processes
    public Process(int pid, int[] text, int priority, HashMap<Integer, Integer> pageTable, HashMap<String,Integer> resources, Process parent){
        this.state = ProcessState.NEW;
        setText(text);
        processCounter = 0;
        setCpuTimeLeft(text[0]);
        setPid(pid);
        setPriority(priority);
        setMemorySize(pageTable.size());
        setReadyQueueAge(0);
        setResources(resources);
        this.pageTable = pageTable;
        this.parent = parent;
    }

    /////////////////////////////////////////////////
    // Accessors
    /////////////////////////////////////////////////
    public int getPid(){
        return pid;
    }
    public int getPriority(){
        return priority;
    }
    public int getCpuTimeLeft(){
        return cpuTimeLeft;
    }
    public ProcessState getState(){
        return state;
    }
    /**
     * @return the text
     */
    public int[] getText() {
        return text;
    }
    /**
     * @return the ioNeeds
     */
    public HashMap<Integer, String> getIoNeeds() {
        return ioNeeds;
    }
    /**
     * @return the processCounter
     */
    public int getProcessCounter() {
        return processCounter;
    }
    /**
     * @return the waitingTime
     */
    public int getWaitingTime() {
        return waitingTime;
    }
    /**
     * @return the waitingDevice
     */
    public String getWaitingDevice() {
        return waitingDevice;
    }
    /**
     * @return the memorySize
     */
    public int getMemorySize() {
        return memorySize;
    }
    /**
     * @return the pageTable
     */
    public HashMap<Integer, Integer> getPageTable() {
        return pageTable;
    }
    /**
     * @return the readyQueueAge
     */
    public int getReadyQueueAge() {
        return readyQueueAge;
    }
    /**
     * @return the resources
     */
    public HashMap<String,Integer> getResources() {
        return resources;
    }
    public String getResourceUsed(){
        String resource = "none";
        if(resources.keySet().toArray().length > 0) resource = (String)resources.keySet().toArray()[0];
        return resource;
    }
    /**
     * @return the child
     */
    public Process getChild() {
        return child;
    }
    /////////////////////////////////////////////////

    /////////////////////////////////////////////////
    // Mutators
    /////////////////////////////////////////////////
    public void setPid(int pid){
        this.pid = pid;
    }
    public void setPriority(int priority){
        this.priority = priority;
    }
    public void setCpuTimeLeft(int newTime){
        this.cpuTimeLeft = newTime;
    }
    public void setState(ProcessState state){
        this.state = state;
    }
    /**
     * @param text the text to set
     */
    public void setText(int[] text) {
        if(text.length >= 0) this.text = text;
        else this.state = ProcessState.TERMINATED;
    }
    /**
     * @param ioNeeds the ioNeeds to set
     */
    public void setIoNeeds(HashMap<Integer, String> ioNeeds) {
        this.ioNeeds = ioNeeds;
    }
    /**
     * @param waitingTime the waitingTime to set
     */
    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }
    /**
     * @param waitingDevice the waitingDevice to set
     */
    public void setWaitingDevice(String waitingDevice) {
        this.waitingDevice = waitingDevice;
    }
    /**
     * @param memorySize the memorySize to set
     */
    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }
    /**
     * @param pageTable the pageTable to set
     */
    public void setPageTable(int[] physPages) {
        for(int i=0; i<physPages.length; i++){
            this.pageTable.put(i, physPages[i]);
        }
    }
    /**
     * @param readyQueueAge the readyQueueAge to set
     */
    public void setReadyQueueAge(int readyQueueAge) {
        this.readyQueueAge = readyQueueAge;
    }
    /**
     * @param resources the resources to set
     */
    public void setResources(HashMap<String,Integer> resources) {
        this.resources = resources;
    }
    /**
     * @param child the child to set
     */
    public void setChild(Process child) {
        this.child = child;
    }
    /////////////////////////////////////////////////

    public void run(){
        if(cpuTimeLeft > 1){
            cpuTimeLeft--;
            return;   
        }
        else { //finished this compute line
            incrementProgram();
        }
    }

    public void incrementProgram(){
        processCounter++;
        if(ioNeeds.containsKey(processCounter)){
            waitForIO(ioNeeds.get(processCounter));
        }
        else if (processCounter >= text.length){
            System.out.println("\tProcess " + pid + " terminated");
        }
        else{
            System.out.println("\tProcess " + pid + " yielding, process counter moved to " + processCounter);
        }

        if(processCounter < text.length){
            this.cpuTimeLeft = text[processCounter];
        }
        else this.state = ProcessState.TERMINATED; // set state to terminated if at the end of the process
    }

    //TODO other action by process to wait?
    public void waitForIO(String waitString){
        setState(ProcessState.WAITING);
        String[] waitInfo = waitString.split(" ");
        this.waitingTime = Integer.parseInt(waitInfo[1]);
        this.waitingDevice = waitInfo[0];
    }

    public int getPagesNeededPerLine(){
        return (pageTable.size()/text.length)-1;
    }

    public HashMap<Integer, Integer> getPagesAccessedThisLine(){
        HashMap<Integer, Integer> pagesAccessed = new HashMap<Integer, Integer>();
        int startPage = getPagesNeededPerLine()*(processCounter);
        for(int i=1; i<getPagesNeededPerLine(); i++){
            if(pageTable.get(startPage+i) != null)
                pagesAccessed.put(startPage+i, pageTable.get(startPage+i));
        }
        return pagesAccessed;
    }

    //////////////////////////////////////////////////
    // Ordinary pipe between parent and child - interprocess communication #1
    public void communicateWithChild(){
        String data = "data sent from parent to child";
        if(child != null){
            child.receiveDataFromParent(data);
        }
    }
    public void receiveDataFromParent(String data){
        System.out.println("Data received from parent process " + parent.getPid() + ", data is " + data);
    }
    public void communicateWithParent(){
        String data = "data sent from child to parent";
        if(parent != null){
            parent.receiveDataFromChild(data);
        }
    }
    public void receiveDataFromChild(String data){
        System.out.println("Data received from child process " + parent.getPid() + ", data is " + data);
    }
    /////////////////////////////////////////////////////
    // Named pipe between unrelated processes - interprocess communication #2
    public void receiveThroughNamedPipe(String data, Process p){
        System.out.println("Data received through named pipe from process " + parent.getPid() + ", data is " + data);
    }
    public void sendThroughNamedPipe(Process p){
        String data = "data sent from through named pipe from process " + pid;
        if(p != null){
            p.receiveThroughNamedPipe(data, p);
        }
    }
    /////////////////////////////////////////////////////



    // Cascading termination
    public void killCascadingTermination(){
        child.killCascadingTermination();
        setState(ProcessState.TERMINATED);
    }

    @Override
    public String toString(){
        String str = "\nProcess " + pid + ", state: " + getState() + ", priority: " + getPriority() 
        + ", memory size: " + memorySize + ", line " + processCounter + " out of " + text.length
        + " with " + cpuTimeLeft + " CPU cycles left";
        
        return str;
    }

    public String[] getInfo(){
        String[] info = {String.valueOf(pid), String.valueOf(state), 
                        String.valueOf(processCounter + "/" + text.length)};
        return info;
    }

    @Override
    public int compareTo(Process p){
        return ((Integer) priority).compareTo( (Integer) p.getPriority() );
    }
}