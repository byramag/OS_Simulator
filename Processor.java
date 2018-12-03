public class Processor{

    private CPU cpu;

    public Processor(){
        this(new CPU());
    }

    public Processor(CPU cpu){
        this.cpu = cpu;
    }
}