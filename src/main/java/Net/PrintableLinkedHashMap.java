package Net;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public class PrintableLinkedHashMap<T, U> extends LinkedHashMap<Pair<InetAddress, Integer>, Long> {
    public void print() {
        for (Map.Entry<Pair<InetAddress, Integer>, Long> entry : this.entrySet()) {
            System.out.println("Host with ip: " + entry.getKey().getFirst() + " port: " + entry.getKey().getSecond()
                    + " " + entry.getValue() + " seconds ago.");
        }
    }
}
