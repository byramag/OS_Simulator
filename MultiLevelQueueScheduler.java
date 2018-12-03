import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Map.Entry;

public class MultiLevelQueueScheduler extends Scheduler{

    private RoundRobinScheduler queue0; // RR with time quantum 5
    private RoundRobinScheduler queue1; // RR with time quantum 20
    private Scheduler queue2; // FCFS
    private int runningProcessIsFromQ;
    private int turnForQ;

    public MultiLevelQueueScheduler(){
        queue0 = new RoundRobinScheduler();
        queue1 = new RoundRobinScheduler();
        queue2 = new Scheduler();
        turnForQ = 0;
    }
    public MultiLevelQueueScheduler(CPU onCpu){
        queue0 = new RoundRobinScheduler(onCpu, 8);
        queue1 = new RoundRobinScheduler(onCpu, 25);
        queue2 = new Scheduler(onCpu);
        turnForQ = 0;
    }

    @Override
    public String getReadyString() {
        return "Queue 0: " + queue0.getReadyString() + "\nQueue 1: " + queue1.getReadyString() + "\nQueue 2: " + queue2.getReadyString();
    }

    @Override
    public void addToQueue(Process p) {
        p.setState(Process.ProcessState.READY);
        queue0.addToQueue(p);
        System.out.println("Process " + p.getPid() + " was added to the first queue");
        if(getOnCpu().getRunningProcess() == null && !isEmpty()){ 
            queue0.setRunningProcess();
        }
    }

    @Override
    public void assess() {
        queue0.setCyclesUntilTimeout(queue0.getCyclesUntilTimeout()-1);
        queue1.setCyclesUntilTimeout(queue1.getCyclesUntilTimeout()-1);
        if(queue0.getCyclesUntilTimeout() <= 0){ //Queue 0 takes priority when timeout occurs on both
            queue0.setCyclesUntilTimeout(queue0.getTimeQuantum());
            this.getOnCpu().getRunningProcess().setState(Process.ProcessState.READY);
        }
        else if(queue0.getCyclesUntilTimeout() <= 0){
            queue0.setCyclesUntilTimeout(queue0.getTimeQuantum());
            this.getOnCpu().getRunningProcess().setState(Process.ProcessState.READY);
        }
        super.assess();
    }

    @Override
    public void interrupt() {
        Process interruptedProcess = this.getOnCpu().getRunningProcess();
        if(interruptedProcess.getState() == Process.ProcessState.READY) {
            System.out.println("Round robin timeout interrupt on process " + interruptedProcess.getPid()
                    + ", demoting to queue " + (runningProcessIsFromQ+1));
            if(runningProcessIsFromQ == 0){ 
                System.out.println("Adding to q 1");
                queue1.addToQueue(interruptedProcess);
            }
            else {
                System.out.println("Adding to q 1");
                queue2.addToQueue(interruptedProcess);
            }
        }
        super.interrupt();
    }

    @Override
    public void setRunningProcess() {
        System.out.print("From Queue: ");
        switch(selectQueue()){
            case 0: 
                System.out.println(0);
                queue0.setRunningProcess();
                runningProcessIsFromQ = 0;
                break;
            case 1:
                System.out.println(1);
                queue1.setRunningProcess();
                runningProcessIsFromQ = 1;
                break;
            case 2:
                System.out.println(2);
                queue2.setRunningProcess();
                runningProcessIsFromQ = 2;
        }
        turnForQ = (turnForQ+1)%3;
    }

    public int selectQueue(){
        if(turnForQ == 0 && !queue0.isEmpty()) return 0;
        if(turnForQ == 1 && !queue1.isEmpty()) return 1;
        if(turnForQ == 2 && !queue2.isEmpty()) return 2;

        if(queue0.isEmpty()){
            if(!queue1.isEmpty()) return 1;
            if(!queue2.isEmpty()) return 2;
        }
        return 0;
    }

    @Override
    public void setOnCpu(CPU onCpu) {
        super.setOnCpu(onCpu);
        queue0.setOnCpu(onCpu);
        queue1.setOnCpu(onCpu);
        queue2.setOnCpu(onCpu);
    }

    @Override
    public boolean isEmpty() {
        return queue0.isEmpty() && queue1.isEmpty() && queue2.isEmpty();
    }

}