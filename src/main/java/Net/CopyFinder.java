package Net;

import java.net.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class CopyFinder implements Runnable {
    protected InetAddress groupAddress;
    private byte[] rcvBuf = new byte[512];

    private final PrintableLinkedHashMap<Pair<InetAddress, Integer>, Long> activityMap = new PrintableLinkedHashMap<>();
    private final int interval = 5000; //initial time interval
    private final int port;
    private final boolean seeAllMode = false;

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

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                activityMap.print();
                String message = "Disconnected.";
                DatagramPacket sndPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, groupAddress, port);
                try {
                    multiCastSocket.send(sndPacket);
                    System.out.println(message);
                }
                catch (IOException  exc) {
                    System.err.println(exc.getMessage());
                }
            }));

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
                    checkPacket(rcvPacket, lastRcvTime);
                    Arrays.fill(rcvBuf, (byte)0);
                }
                editActivityMap();
            }
        }
        catch (IOException exc) {
            System.err.println(exc.getMessage());
        }
    }

    public void checkPacket(DatagramPacket rcvPacket, long lastRcvTime) throws UnknownHostException {
        String message = new String(rcvPacket.getData());
        if (message.startsWith("Disconnected.")) {
            Pair<InetAddress, Integer> copy = new Pair<>(rcvPacket.getAddress(), rcvPacket.getPort());
            activityMap.remove(copy);
            System.out.println("- " +  copy.toString());
            return;
        }

        Pair<InetAddress, Integer> curCopy = seeAllMode ? new Pair<>(rcvPacket.getAddress(),
                rcvPacket.getPort()) : parseMessage(message);

        if (activityMap.containsKey(curCopy)) {
            activityMap.replace(curCopy, activityMap.getOrDefault(curCopy, 0L), lastRcvTime);
        }
        else {
            System.out.println("+ " + curCopy.toString());
            activityMap.put(curCopy, lastRcvTime);
        }
    }

    public static Pair<InetAddress, Integer> parseMessage(String message) throws UnknownHostException {
        List<String> list  = Stream.of(message.split("[/]"))
                .filter(s -> s.contains("]"))
                .flatMap(s -> Stream.of(s.split("[^A-Za-z0-9.:]")))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return new Pair<>(InetAddress.getByName(list.get(0)), Integer.parseInt(list.get(1)));
    }

    public NetworkInterface getLocalNetworkInterface() throws IOException {
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface inter = enumeration.nextElement();

            if (inter.supportsMulticast() && !inter.isLoopback() && inter.isUp()) {
                    return inter;
            }
        }
        return null;
    }

    public void editActivityMap() { //checking and editing
        int TTL = 5 * interval;
        Iterator<Map.Entry<Pair<InetAddress, Integer>, Long>> iterator = activityMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Pair<InetAddress, Integer>, Long> entry = iterator.next();
            if (System.currentTimeMillis() - entry.getValue() > TTL) {
                System.out.println("- " + entry.getKey().toString());
                iterator.remove();
            }
        }
    }
}