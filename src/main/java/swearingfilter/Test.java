package swearingfilter;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Test {

    static SwearingFilter sf;
    private static File chessDirectory = new File(getUserHome() + "/Chess/.scache");

    public static void main(String[] args) {
        sf = new SwearingFilter(chessDirectory, 10, SwearingFilter.SearchType.NORMAL);
        chessDirectory.deleteOnExit();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Starting!");
        String s = "Not Good!";
        System.out.println(s);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            FileInputStream fis = new FileInputStream(new File("swearing.txt"));
            StringBuilder sb = new StringBuilder();
            int c;
            while (!((c = fis.read()) == -1)) {
                sb.append((char) c);
            }
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getUserHome() {
        return System.getProperty("user.home");
    }
}
