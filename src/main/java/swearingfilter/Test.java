package swearingfilter;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Test {

    static SwearingFilter sf;
    private static File chessDirectory = new File(getUserHome() + "/Chess/.scache");

    public static void main(String[] args) {
        sf = new SwearingFilter(chessDirectory, 10, SwearingFilter.SearchType.NORMAL);
        /*try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        System.out.println("Starting!");
        //FileInputStream fis = new FileInputStream(new File("swearing.txt"));
        Scanner sc = new Scanner(System.in);
        StringBuilder sb = new StringBuilder();
        int c;
        /*while (!((c = sc.nextInt()) == -1)) {
            sb.append((char) c);
        }*/
        sb.append(sc.next());
        CompletableFuture<Result> futere = sf.lockForSwearing(sb.toString());
        try {
            Result result = futere.get();
            System.out.println(result);
            System.out.println(result.toBlock(15.0));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static String getUserHome() {
        return System.getProperty("user.home");
    }
}
