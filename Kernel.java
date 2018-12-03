public class Kernel{

    private String multiThreadingModel;

    public Kernel(){
        multiThreadingModel = "1-1";
    }

    public Kernel(String multiThreadingModel){
        this.multiThreadingModel = multiThreadingModel;
    }

    public void setKernelThreads(Process p) {
        if(multiThreadingModel.equals("1-1"))
        p.setKernelThreads(userThreads);
        else if(multiThreadingModel.equals("1-M"))
        p.setKernelThreads(1);
        else if(multiThreadingModel.equals("M-M")){ //M-M
            //TODO: Better logic here to decide how many kernel threads to give
            p.setKernelThreads(userThreads/2 + 1);
        }
        else { // assuming hybrid model
            if(p.getPriority() <= 1 || p.getText().length < ioNeeds.getSize()*2) 
                p.setKernelThreads(userThreads); //High priority or I/O bound process
            else{
                p.setKernelThreads(userThreads/2 + 1);
            }
        }
    }
}