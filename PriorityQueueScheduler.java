import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Map.Entry;

public class PriorityQueueScheduler extends Scheduler{

    private final int AGE_THRESHOLD = 100;
    private PriorityQueue<Process> priorityReadyQueue;

    public PriorityQueueScheduler(){
        super();
        priorityReadyQueue = new PriorityQueue<Process>();
    }
    public PriorityQueueScheduler(CPU onCpu){
        super(onCpu);
        priorityReadyQueue = new PriorityQueue<Process>();
    }
    public PriorityQueueScheduler(PriorityQueue<Process> processQueue, HashMap<String, IODevice> ioDevices, CPU onCpu){
        super(new LinkedList<>(), ioDevices, onCpu);
        priorityReadyQueue = processQueue;
    }

    @Override
    public String getReadyString() {
        return priorityReadyQueue.toString();
    }

    @Override
    public void addToQueue(Process p) {
        p.setState(Process.ProcessState.READY);
        priorityReadyQueue.add(p);
        System.out.println("Process " + p.getPid() + " with priority " + p.getPriority() + " was added to the priority ready queue");
        if(getOnCpu().isIdle()){ 
            setRunningProcess(priorityReadyQueue.poll());
        }
        else if(getOnCpu().getRunningProcess().getPriority() > priorityReadyQueue.peek().getPriority()){
            getOnCpu().getRunningProcess().setState(Process.ProcessState.READY);
            System.out.println("Process " + priorityReadyQueue.peek().getPid() + " has a higher priority than the running process " 
                                + getOnCpu().getRunningProcess().getPid() + " so interrupting...");
            interrupt();
        }
    }

    public void addToQueue(){
        Process p = getOnCpu().getRunningProcess();
        p.setState(Process.ProcessState.READY);
        priorityReadyQueue.add(p);
        System.out.println("Process " + p.getPid() + " with priority " + p.getPriority() + " was added to the priority ready queue");
    }

    @Override
    public void assess() {
        incrementAges();
        Process oldestProcess = getOldest();
        System.out.println("oldest process is " + oldestProcess.getPid() + " with an age of " + oldestProcess.getReadyQueueAge());
        if(oldestProcess.getReadyQueueAge() >= AGE_THRESHOLD){
            System.out.println("Process " + oldestProcess.getPid() + " has passed the age threshold with an age of " + oldestProcess.getReadyQueueAge() 
            + ", so the running process " + getOnCpu().getRunningProcess().getPid() + " will be interrupted...");
            addToQueue();
            priorityReadyQueue.remove(oldestProcess);
            oldestProcess.setReadyQueueAge(0);
            setRunningProcess(oldestProcess);
        }
        super.assess();
    }

    @Override
    public void setRunningProcess() {
        setRunningProcess(priorityReadyQueue.poll());
    }

    /**
    *  Causes an interrupt of the process currently running in the CPU
    */
    @Override
    public void interrupt(){
        Process interruptedProcess = this.getOnCpu().getRunningProcess();
        if(interruptedProcess.getState() == Process.ProcessState.READY) {
            addToQueue();
        }
        super.interrupt();
    }

    @Override
    public boolean isEmpty() {
        return priorityReadyQueue.isEmpty();
    }

    public void incrementAges(){
        for(Process p : priorityReadyQueue)
            p.setReadyQueueAge(p.getReadyQueueAge()+1);
    }

    public Process getOldest(){
        Process oldest = priorityReadyQueue.peek();
        for(Process p : priorityReadyQueue){
            if(p.getReadyQueueAge() > oldest.getReadyQueueAge())
                oldest = p;
        }
        return oldest;
    }

}