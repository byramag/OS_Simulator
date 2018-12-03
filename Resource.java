public class Resource{
    private int numInstances;
    private String name;
    private int numAvailable;

    public Resource(String name, int numInstances){
        this.name = name;
        this.numInstances = numInstances;
        this.numAvailable = numInstances;
    }
    public String getName() {
        return name;
    }
    public int getNumInstances() {
        return numInstances;
    }
    public int getNumAvailable() {
        return numAvailable;
    }

    public void use(int num){
        if(num <= numInstances) numAvailable -= num;
    }
    public void release(int num){
        if(num <= numInstances-numAvailable) numAvailable += num;
    }
}