import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class Memory{

    private int MM_SIZE_MB = 2048;
    private int PAGE_SIZE_MB = 4;

    private boolean[] isAvailableMM;
    private boolean[] isAvailableVM;
    private int numAvailableMM;
    private int numAvailableVM;
    private int[] agesMM;
    private LinkedList<Process> waitingToLoad;
    private ArrayList<CPU> onCpus;

    private String[] mainMemory;        //values in memory for this simulator is the pid and logical address
    private String[] virtualMemory;     //concatenated in a string e.g. "3 14"
    private String[] cache;

    public Memory(){
        mainMemory = new String[ MM_SIZE_MB / PAGE_SIZE_MB ];
        virtualMemory = new String[ 4*(MM_SIZE_MB/PAGE_SIZE_MB) ];
        cache = new String[4];
        Arrays.fill(mainMemory, "");
        Arrays.fill(virtualMemory, "");
        Arrays.fill(cache, "");

        numAvailableMM = MM_SIZE_MB / PAGE_SIZE_MB;
        numAvailableVM = 4*(MM_SIZE_MB/PAGE_SIZE_MB);
        isAvailableMM = new boolean[ MM_SIZE_MB / PAGE_SIZE_MB ];
        isAvailableVM = new boolean[ 4*(MM_SIZE_MB / PAGE_SIZE_MB) ];
        Arrays.fill(isAvailableMM, true);
        Arrays.fill(isAvailableVM, true);
        agesMM = new int[ MM_SIZE_MB / PAGE_SIZE_MB ];
        Arrays.fill(agesMM, 0);
        waitingToLoad = new LinkedList<Process>();

        onCpus = new ArrayList<CPU>();
    }

    // **Functions on memory are the functionalities of the MMU** //

    public String getPageMM(int physAddress){
        return mainMemory[physAddress];
    }

    public int copyPageFromVM(int processId, int logicalAddress){ // todo sometimes doesn't match
        String dataToFind = String.valueOf(processId + " " + logicalAddress);
        int addr = 0;
        for(String page : virtualMemory){
            if(page.equals(dataToFind)){
                addr = findEmptyMMPage();
                if(addr == -1){
                    addr = selectVictim();
                }
                setPageMM(dataToFind, addr);
            }
        }
        return addr;
    }

    public boolean hasSpaceVM(int memSize){
        return numAvailableVM >= (memSize/PAGE_SIZE_MB) + 1;
    }

    public boolean hasSpaceMM(int memSize){
        return numAvailableMM >= (memSize/PAGE_SIZE_MB) + 1;
    }

    public void setPageMM(String data, int address){
        if(address == -1 || findEmptyMMPage() == -1){
            address = selectVictim();
        }
        else numAvailableMM--;
        mainMemory[address] = data;
        isAvailableMM[address] = false;
    }

    public void setPageMM(String data){
        setPageMM(data, findEmptyMMPage());
    }

    public void setPageVM(String data, int address){
        if(address == -1 || findEmptyMMPage() == -1){
            address = selectVictim();
        }
        virtualMemory[address] = data;
        isAvailableVM[address] = false;
        numAvailableVM--;
    }

    public void setPageVM(String data){
        setPageVM(data, findEmptyVMPage());
    }

    public void setOnCpus(ArrayList<CPU> onCpus) {
        this.onCpus = onCpus;
    }

    public void addCpu(CPU cpu){
        onCpus.add(cpu);
    }

    public boolean loadWholeProcess(Process p){
        if(!hasSpaceVM(p.getMemorySize())){
            addToLoadQueue(p);
            return false;
        }
        for(int i=0; i<((p.getMemorySize()/PAGE_SIZE_MB)+1); i++){
            int addrVM = findEmptyVMPage();
            setPageVM(String.valueOf(p.getPid() + " " + i), addrVM);
        }
        
        int[] physAddresses = new int[ p.getMemorySize()/PAGE_SIZE_MB+1 ];
        Arrays.fill(physAddresses, -1);

        for(int i=0; i<((p.getMemorySize()/PAGE_SIZE_MB)+1); i++){
            int addr = findEmptyMMPage();
            if(addr != -1){
                setPageMM(String.valueOf(p.getPid() + " " + i), addr);
                physAddresses[i] = addr;
            }
        }
        p.setPageTable(physAddresses);
        System.out.println("Process " + p.getPid() + " just loaded into memory spaces\n" + p.getPageTable().values());
        return true;
    }

    public int findEmptyMMPage(){
        for(int addr=0; addr<isAvailableMM.length; addr++){
            if(isAvailableMM[addr] == true) return addr;
        }
        return -1;
    }

    public int findEmptyVMPage(){
        for(int addr=0; addr<isAvailableVM.length; addr++){
            if(isAvailableVM[addr] == true) return addr;
        }
        return -1;
    }

    public void addToLoadQueue(Process p){
        if(p.getState() == Process.ProcessState.NEW) 
            waitingToLoad.add(p);
    }

    public void loadFromQueue(){
        if(!waitingToLoad.isEmpty() && hasSpaceVM(waitingToLoad.peek().getMemorySize())){
            Process p = waitingToLoad.poll();
            loadWholeProcess(p);
            p.setState(Process.ProcessState.READY);
            CPU cpu = chooseCpu();
            cpu.getScheduler().addToQueue(p);
            System.out.println("Core: " + cpu.getCoreNum() + " Process " + p.getPid() 
                                + " just loaded into memory spaces\n" + p.getPageTable().values());
        }
    }

    public CPU chooseCpu(){
        CPU mostIdle = onCpus.get(0);
        for(CPU cpu : onCpus){
            if(cpu.getScheduler().getReadyQueue().size() < mostIdle.getScheduler().getReadyQueue().size())
                mostIdle = cpu;
        }
        return mostIdle;
    }

    public void incrementAges(){
        for(int i=0; i<agesMM.length; i++) agesMM[i]++;
    }

    public int getMaxAged(){
        int maxAddr = 0;
        for(int i=0; i<agesMM.length; i++){
            if(agesMM[i] > agesMM[maxAddr]) maxAddr = i;
        }
        return maxAddr;
    }

    // Page with longest time since last access
    public int selectVictim(){
        return getMaxAged();
    }

    public void accessPages(HashMap<Integer, Integer> addrs, int fromProcess){
        int count = 0;
        for(int logAddr : addrs.keySet()){
            int addr = addrs.get(logAddr);
            if(inCache(fromProcess + " " + logAddr)){
                //use data
                continue;
            }
            if(addr == -1){ //Page fault
                addr = copyPageFromVM(fromProcess, logAddr);
                //use data
            }
            else { //Data is in MM
                //use data
            }
            agesMM[addr] = 0;
            copyToCache(addr, count%4);
            count++;
        }
    }

    public void copyToCache(int addr, int destination){
        cache[destination] = mainMemory[addr];
    }

    public boolean inCache(String dataToFind){
        for(String data : cache){
            if(data.equals(dataToFind)) return true;
        }
        return false;
    }

    public void deallocate(Collection<Integer> addresses){
        String pid = "";
        for(int addr : addresses){
            if(addr == -1) continue;
            pid = mainMemory[addr].split(" ")[0];
            mainMemory[addr] = "";
            isAvailableMM[addr] = true;
            numAvailableMM++;
        }
        for(int i=0; i<virtualMemory.length; i++){
            if(!virtualMemory[i].equals("") && virtualMemory[i].split(" ")[0].equals(pid)){
                virtualMemory[i] = "";
                isAvailableVM[i] = true;
                numAvailableVM++;
            }
        }
        loadFromQueue();
    }

    public String toString(){
        return "\nCurrent memory state: " + numAvailableMM + " available in MM out of " + mainMemory.length
                + "\nwith values: " + getMMString() + "\n\n" + numAvailableVM + "/" + 4*(MM_SIZE_MB/PAGE_SIZE_MB) 
                + " pages available in virtual memory" + "\nwith values:" + getVMString() 
                + "\n Cache values: " + getCacheString()
                + "\nProcesses waiting to load: " + waitingToLoad;
    }

    public String getMMString(){
        return Arrays.toString(mainMemory);
    }

    public String getVMString(){
        return Arrays.toString(virtualMemory);
    }

    public String getCacheString(){
        return Arrays.toString(cache);
    }

}