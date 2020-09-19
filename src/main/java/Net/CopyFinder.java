package Net;
import java.net.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

                    Pair<InetAddress, Integer> curCopy = parseMessage(new String(rcvPacket.getData()));
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

            if (inter.supportsMulticast() && !inter.isLoopback()) {
                try {
                    new MulticastSocket(port).joinGroup(new InetSocketAddress(groupAddress, port), inter);
                    return inter;
                }
                catch (IOException exc) {}
            }
        }
        return null;
    }

    public static Pair<InetAddress, Integer> parseMessage(String message) throws UnknownHostException {
        List<String> list  = Stream.of(message.split("[/]"))
                .filter(s -> s.contains("]"))
                .flatMap(s -> Stream.of(s.split("[^A-Za-z0-9.:]")))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return new Pair<>(InetAddress.getByName(list.get(0)), Integer.parseInt(list.get(1)));
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

//            try {
//                    InetAddress i1 = InetAddress.getByName("255.255.255.255");
//                    InetAddress i2 = InetAddress.getByName("192.168.123.12");
//                    InetAddress i3 = InetAddress.getByName("172.31.12.12");
//                    InetAddress i4 = InetAddress.getByName("172.16.12.12");
//                    InetAddress i5 = InetAddress.getByName("169.254.3.15");
//                    InetAddress i6 = InetAddress.getByName("10.5.10.10");
//                    InetAddress i7 = InetAddress.getByName("127.0.0.1");
//                    chooseInetAddress(Arrays.asList(i1, i2, i3, i4, i5, i6, i7));
//             }
//             catch (UnknownHostException exc) {}

//    public InetAddress chooseInetAddress(List<InetAddress> list) {
//        List<InetAddress> addressList = list.stream().filter(s -> !s.isLoopbackAddress()
//                &&!s.getHostAddress().startsWith("192.") && !s.getHostAddress().startsWith("10.")
//                && !s.getHostAddress().startsWith("172.16.") && !s.getHostAddress().startsWith("172.31")
//                && !s.getHostAddress().equals("255.255.255.255") && !s.getHostAddress().startsWith("169.254"))
//                .collect(Collectors.toList());
//        for (InetAddress address : addressList) {
//            if (address.getHostAddress().contains("net") || address instanceof Inet4Address) {
//                return address;
//            }
//        }
//        return null;
//    }

//    public List<InetAddress> getInterfacesIp() throws SocketException {
//        List<InetAddress> list = new ArrayList<>();
//        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
//        while(en.hasMoreElements()) {
//            NetworkInterface inter = en.nextElement();
//            Enumeration<InetAddress> en1 = inter.getInetAddresses();
//            while (en1.hasMoreElements()) {
//                list.add(en1.nextElement());
//            }
//        }
//        return list;
//    }