package server;

import java.io.PrintWriter;

/**
 * Created by ADMIN on 10/6/2017.
 */
public class ClientData {

    protected PrintWriter out;
    protected long lastActive;

    public ClientData(PrintWriter out) {
        this.out = out;
        this.markAsActive();
    }

    protected void markAsActive() {
        this.lastActive = System.currentTimeMillis();
    }
}
