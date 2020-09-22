package Net;

import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.io.InputStream;

public class Main {
    protected static InetAddress groupAddress;

    public static void main(String[] args) {
        CopyFinder copyFinder = new CopyFinder(readTheGroupIP(System.in));
        copyFinder.run();
    }

    public static Pair<InetAddress, Integer> readTheGroupIP(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8);
        int port;
        System.out.println("Please, enter multicast IP address of the group and port of your process. " +
                "You can choose IPv4 address from 224.0.0.0 to 239.255.255.255 or try IPv6 one.");
        for (int it = 0; it < 5; ++it) {
            try {
                groupAddress = InetAddress.getByName(scanner.next());
                if (scanner.hasNextInt()) {
                    port = scanner.nextInt();
                    System.out.println("Success. Ip is " + groupAddress.getHostAddress() +
                            ", port is " + port + ". " + "Your host: " + InetAddress.getLocalHost() + ".");
                    return new Pair<InetAddress, Integer>(groupAddress, port);
                }
                else throw new IllegalArgumentException("Bad syntax.");
            } catch (UnknownHostException | IllegalArgumentException exc) {
                if (it == 4) {
                    System.out.println("Session closed. Try again later.");
                    System.exit(-1);
                }
                else System.out.println("Unable to identify the address or port, try again. " +
                        "You have " + (4 - it) + " tries remain.");
            }
        }
        return null;
    }
} 
