package Net;
import java.net.*;
import java.io.IOException;
import java.util.*;

public class CopyFinder implements Runnable {
    protected InetAddress groupAddress;
    private final byte[] rcvBuf = new byte[2048];

    private final Map<Pair<InetAddress, Integer>, Long> activityMap = new PrintableLinkedHashMap<Pair<InetAddress, Integer>, Long>();
    private final int interval = 5000; //initial time interval
    private final int port;

    public CopyFinder(InetAddress groupAddress, int port) {
        this.groupAddress = groupAddress;
        this.port = port;
    }

    public CopyFinder(Pair<InetAddress, Integer> info) {
        groupAddress = info.getFirst();
        port = info.getSecond();
    }

    @Override
    public void run() {

        try(MulticastSocket multiCastSocket = new MulticastSocket(port)) {
            Pair<InetAddress, Integer> identifier = new Pair<>(InetAddress.getLocalHost(), port);
            multiCastSocket.joinGroup(new InetSocketAddress(groupAddress, port), getLocalNetworkInterface());

            for (long it = 0;;++it) {
                String message = "That's a message #" + it + " from " + identifier.toString();
                DatagramPacket sndPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, groupAddress, port);
                multiCastSocket.send(sndPacket);
                long lastSendingTime = System.currentTimeMillis();

                DatagramPacket rcvPacket = new DatagramPacket(rcvBuf, rcvBuf.length);
                while (System.currentTimeMillis() - lastSendingTime < interval) {
                    int temp = (int)(lastSendingTime + interval - System.currentTimeMillis());
                    multiCastSocket.setSoTimeout(temp);
                    try {
                        multiCastSocket.receive(rcvPacket);
                    }
                    catch (SocketTimeoutException exc) {
                        break;
                    }
                    long lastRcvTime = System.currentTimeMillis();

                    Pair<InetAddress, Integer> curCopy = new Pair<>(rcvPacket.getAddress(), rcvPacket.getPort());
                    if (activityMap.containsKey(curCopy)) {
                        activityMap.replace(curCopy, activityMap.getOrDefault(curCopy, 0L), lastRcvTime);
                    }
                    else {
                        System.out.println("+ " + curCopy.toString());
                        activityMap.put(curCopy, lastRcvTime);
                    }
                }
                editActivityMap();
            }
        }

        catch (IOException exc) {
            System.err.println(exc.getMessage());
        }
    }

    public NetworkInterface getLocalNetworkInterface() throws IOException {
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface inter = enumeration.nextElement();

            if (inter.supportsMulticast() && !inter.isLoopback() && inter.isUp()) {
                try {
                    new MulticastSocket(port).joinGroup(new InetSocketAddress(groupAddress, port), inter);
                    return inter;
                }
                catch (IOException exc) {}
            }
        }
        return null;
    }

    public void editActivityMap() { //checking and editing
        int TTL = 5 * interval;
        for (Map.Entry<Pair<InetAddress, Integer>, Long> entry : activityMap.entrySet()) {
            long curTime = System.currentTimeMillis();
            if (curTime - entry.getValue() > TTL) {
                System.out.println("- " + entry.getKey().toString());
                activityMap.remove(entry.getKey(), entry.getValue());
            }
        }
    }
}