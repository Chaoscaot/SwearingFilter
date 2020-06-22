package swearingfilter;

public class SwearingWorker extends Thread {

    private final WorkerType type;

    public SwearingWorker(Runnable runnable, WorkerType type) {
        super(runnable, "SwearingWorker-" + type.toString());
        this.type = type;
        start();
    }

    public WorkerType getType() {
        return type;
    }

    public enum WorkerType {

        FILTER(0),
        CACHER(1),
        GITHUBLOADER(2);

        private final int value1;

        private WorkerType(int value1) {
            this.value1 = value1;
        }
    }
}
