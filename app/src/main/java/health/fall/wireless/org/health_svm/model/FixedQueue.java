package health.fall.wireless.org.health_svm.model;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Amogh on 11/17/2017.
 */

public class FixedQueue extends ArrayBlockingQueue<Features> {

    /**
     * generated serial number
     */
    private static final long serialVersionUID = -7772085623838075506L;

    // Size of the queue
    private int size;

    // Constructor
    public FixedQueue(int crunchifySize) {

        // Creates an ArrayBlockingQueue with the given (fixed) capacity and default access policy
        super(crunchifySize);
        this.size = crunchifySize;
    }

    // If queue is full, it will remove oldest/first element from queue like FIFO
    // Do we need this add() method synchronize? What do you think?
    @Override
    synchronized public boolean add(Features e) {

        // Check if queue full already?
        if (super.size() == this.size) {
            // remove element from queue if queue is full
            this.remove();
        }
        return super.add(e);
    }
}
