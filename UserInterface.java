package f76goat.simpledos;

import java.util.Scanner;

public class UserInterface {

    Scanner scanner = new Scanner(System.in);

    public String getRequestUrl() {
        System.out.print("Enter the IP address or URL to attack: ");
        return scanner.nextLine();
    }

    public int getThreadCount() {
        System.out.print("Number of threads for attack: ");

        try {
            return scanner.nextInt();
        } catch (Exception e) {
            System.out.println("Defaulting to 2000 threads.");
            return 2000;
        }

    }

    public int getRequestInterval() {
        System.out.print("Request interval: ");

        try {
            return scanner.nextInt();
        } catch (Exception e) {
            System.out.println("Defaulting to 2000 threads.");
            return 10;
        }

    }
}
