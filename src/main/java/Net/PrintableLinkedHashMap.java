package Net;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public class PrintableLinkedHashMap<T, U> extends LinkedHashMap<Pair<InetAddress, Integer>, Long> {
    public void print() {
        System.out.println();
        for (Map.Entry<Pair<InetAddress, Integer>, Long> entry : this.entrySet()) {
            System.out.println("Host with IP: " + entry.getKey().getFirst().toString() + ", port: " + entry.getKey().getSecond()
                    + ", last received msg: " + (System.currentTimeMillis() - entry.getValue()) + " ms ago.");
        }
        System.out.println();
    }
}
