import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;;

public class RoundRobinScheduler extends Scheduler{

    private int timeQuantum;
    private int cyclesUntilTimeout;

    public RoundRobinScheduler(){
        super();
        timeQuantum = 10;
        cyclesUntilTimeout = timeQuantum;
    }
    public RoundRobinScheduler(CPU onCpu){
        super(onCpu);
        timeQuantum = 10;
        cyclesUntilTimeout = timeQuantum;
    }
    public RoundRobinScheduler(CPU onCpu, int timeQuantum){
        super(onCpu);
        this.timeQuantum = timeQuantum;
        cyclesUntilTimeout = timeQuantum;
    }
    public RoundRobinScheduler(LinkedList<Process> processQueue, HashMap<String, IODevice> ioDevices, CPU onCpu){
        super(processQueue, ioDevices, onCpu);
        cyclesUntilTimeout = timeQuantum;
    }

    /**
     * @return the cyclesUntilTimeout
     */
    public int getCyclesUntilTimeout() {
        return cyclesUntilTimeout;
    }
    /**
     * @param cyclesUntilTimeout the cyclesUntilTimeout to set
     */
    public void setCyclesUntilTimeout(int cyclesUntilTimeout) {
        this.cyclesUntilTimeout = cyclesUntilTimeout;
    }
    /**
     * @return the timeQuantum
     */
    public int getTimeQuantum() {
        return timeQuantum;
    }

    @Override
    public void assess() {
        cyclesUntilTimeout--;
        if(cyclesUntilTimeout <= 0){
            cyclesUntilTimeout = timeQuantum;
            this.getOnCpu().getRunningProcess().setState(Process.ProcessState.READY);
        }
        super.assess();
    }

    @Override
    public void interrupt(){
        Process interruptedProcess = this.getOnCpu().getRunningProcess();
        if(interruptedProcess.getState() == Process.ProcessState.READY) {
            System.out.println("Round robin timeout interrupt on process " + interruptedProcess.getPid());
            addToQueue(interruptedProcess);
        }
        super.interrupt();
    }

}