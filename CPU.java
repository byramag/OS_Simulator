

public class CPU{

    private int coreNum;
    private Process runningProcess;
    private Scheduler scheduler;
    private int cycleWait;
    private Memory memory;

    public CPU(int speed, int coreNum){
        this.coreNum = coreNum;
        setScheduler(new Scheduler(this)); //Default to FCFS scheduler
        setSpeed(speed);
    }
    public CPU(Scheduler scheduler, Memory mem, int speed, int coreNum){
        this.coreNum = coreNum;
        setScheduler(scheduler);
        this.scheduler.setOnCpu(this);
        setMemory(mem);
        this.memory.addCpu(this);
        setSpeed(speed);
    }

    /**
     * @param runningProcess the runningProcess to set
     */
    public void setRunningProcess(Process newProcess) {

        this.runningProcess = newProcess;
        if(newProcess != null) {
            memory.accessPages(newProcess.getPagesAccessedThisLine(), newProcess.getPid());
        }
    }
    /**
     * @param scheduler the scheduler to set
     */
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    /**
     * @param memory the memory to set
     */
    public void setMemory(Memory memory) {
        this.memory = memory;
    }
    /**
     * @param speed the speed to set
     */
    public void setSpeed(int sleepTime) {
        if(sleepTime >= 0)
            this.cycleWait = sleepTime;
            if(cycleWait > 5000) pause();
    }

    public void pause(){
        boolean resume = false;
        while(!resume){
            resume = cycleWait<=5000;
        }
    }

    /**
     * @return the runningProcess
     */
    public Process getRunningProcess() {
        return runningProcess;
    }
    /**
     * @return the coreNum
     */
    public int getCoreNum() {
        return coreNum;
    }
    /**
     * @return the scheduler
     */
    public Scheduler getScheduler() {
        return scheduler;
    }
    /**
     * @return the memory
     */
    public Memory getMemory() {
        return memory;
    }

    public boolean isIdle(){
        return runningProcess == null;
    }

    public int getThreadNum(){
        int threadNum = 0;
        Thread thisThread = Thread.currentThread();
        for(int i=0; i<scheduler.getThreads().length; i++){
            Thread t = scheduler.getThreads()[i];
            if(t != null && t.equals(thisThread)) 
                threadNum = i;
        }
        return threadNum;
    }

    /**
     * Executes one CPU burst
     */
    public void executeCycle(){
        try{ Thread.sleep(cycleWait); } //Slowing CPU speed for easier visualization
        catch(InterruptedException e){ System.err.println(e); }
        scheduler.decrementWaits();
        memory.incrementAges();
        if(!isIdle()) {
            System.out.println("Core: " + coreNum + " executing from thread " + getThreadNum() + runningProcess);
            runningProcess.run();
            scheduler.assess();
        }
        else {
            System.out.println("Core: " + coreNum + " idle cycle with nothing running in thread " + getThreadNum());
        }
    }
}