package swearingfilter;

import yapi.manager.log.Logger;
import yapi.manager.log.Logging;
import yapi.regex.YAPIRegexBuilder;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SwearingFilter {

    private File cache;
    private HashMap<String, Integer> cacheMap = new HashMap<>();
    private int discardTime;
    private long discard;
    private HashMap<Integer, SwearingWorker> workers;
    private SearchType type;
    private int workerMax = 4;
    private boolean isLoaded;

    private Logging logging = new Logging("SwearingFilter");

    public SwearingFilter(File cacheFile, int discardTime, SearchType type) {
        this.discardTime = discardTime;
        this.cache = cacheFile;
        if (cache.exists()) cache.delete();
        this.type = type;
        this.isLoaded = false;
        workers = new HashMap<>();
        Thread discardThread = new Thread(() -> {
            while (true) {
                logging.add("Checking Discard...");
                if(!isLoaded) {
                    try {
                        Thread.sleep(discardTime*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }finally {
                        continue;
                    }
                }
                if (!cacheMap.isEmpty() && System.currentTimeMillis() >= discard) discard();
                try {
                    Thread.sleep(discardTime*1000/2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Chess-SwearingDiscardManager");
        discardThread.start();
        loadFromGithub("de");
        loadFromGithub("en");
    }

    private void writeCache() {
        int id = calcWorkerID();
        workers.put(id, new SwearingWorker(() -> {
            try {
                logging.add("Writing Cache!");
                if (!cache.exists()) cache.createNewFile();
                BufferedWriter out = new BufferedWriter(new FileWriter(cache));
                for (Map.Entry<String, Integer> e :
                        cacheMap.entrySet()) {
                    out.write(e.getKey() + "-" + e.getValue() + "\n");
                }
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                workers.remove(id, this);
            }
        }, SwearingWorker.WorkerType.CACHER));
    }

    public void loadCache() {
        int id = calcWorkerID();
        workers.put(id, new SwearingWorker(() -> {
            try {
                logging.add("Loading Cache!");
                resetDiscard();
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cache)));
                String in;
                while ((in = reader.readLine()) != null) {
                    String[] inSplit = in.split("-");
                    System.out.println(inSplit[0] + " : " + inSplit[1]);
                    if(inSplit[0].matches("[ !\"§$%&/()=?+*#'\\-_.:,;<>|~{}\\[\\]0-9]")) continue;
                    cacheMap.put(inSplit[0].toLowerCase(), Integer.parseInt(inSplit[1].replace(" ", "")));
                }
                resetDiscard();
                isLoaded = true;
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                workers.remove(id, this);
            }
        }, SwearingWorker.WorkerType.CACHER));
    }

    private void loadFromGithub(String language) {
        int id = calcWorkerID();
        workers.put(id, new SwearingWorker(() -> {
            URL url = null;
            try {
                String etc = "";
                if (type == SearchType.EXTENDED) etc += "-extended";
                url = new URL("https://raw.githubusercontent.com/Chaoscaot444/SwearingWords/master/words_" + language + etc + ".txt");
                URLConnection connection = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        connection.getInputStream()));
                String inputLine;
                boolean appand = true;
                if (!cache.exists()) {
                    cache.createNewFile();
                    appand = false;
                }
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cache, appand)));
                while ((inputLine = in.readLine()) != null) {
                    String[] inSplit = inputLine.split("-");
                    if(inSplit[0].matches("[ !\"§$%&/()=?+*#'\\-_.:,;<>|~{}\\[\\]0-9]")) continue;
                    try {
                        out.write(inSplit[0].toLowerCase() + "-" + inSplit[1].replace(" ", "") + "\n");
                    }catch (ArrayIndexOutOfBoundsException e) {
                        out.write(inSplit[0].toLowerCase() + "-" + 100 + "\n");
                    }
                }
                out.flush();
                in.close();
                logging.add("Loaded Github: " + language);

            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                workers.remove(id, this);
            }
        }, SwearingWorker.WorkerType.GITHUBLOADER));
    }

    public CompletableFuture<Result> lockForSwearing(String input) {
        if (cacheMap.isEmpty()) loadCache();
        CompletableFuture<Result> future = new CompletableFuture<>();
        int id = calcWorkerID();
        workers.put(id, new SwearingWorker(() -> {
            while(!isLoaded) {
                try {
                   Thread.sleep(1000);
                } catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
            resetDiscard();
            Result result = new Result(input.length());
            String toProcess = input.replaceAll("[ !\"§$%&/()=?+*#'\\-_.:,;<>|~{}\\[\\]0-9]", "").toLowerCase();
            for (Map.Entry<String, Integer> e : cacheMap.entrySet()) {
                resetDiscard();
                if (e.getKey().equals("")) continue;
                for (int i = 0; i < toProcess.length(); i++) {
                    String toCalc = toProcess.substring(i, Math.min(i + e.getKey().length(), toProcess.length()));
                    if (toCalc.startsWith(e.getKey())) {
                        if (!result.containsMap(e.getKey()))
                            result.addToMap(e.getKey(), e.getValue());
                        else result.countUp(e.getKey());
                    }
                }
            }
            resetDiscard();
            future.complete(result);
            workers.remove(id, this);
        }, SwearingWorker.WorkerType.FILTER));
        return future;
    }

    public void resetDiscard() {
        discard = System.currentTimeMillis() + discardTime * 1000;
    }

    public void discard() {
        logging.add("Discarding!");
        cacheMap.clear();
        isLoaded = false;
    }

    public enum SearchType {
        NORMAL(0),
        EXTENDED(1);

        private final int value1;

        SearchType(int value1) {
            this.value1 = value1;
        }
    }

    public int getWorkerMax() {
        return workerMax;
    }

    public void setWorkerMax(int workerMax) {
        this.workerMax = Math.min(workerMax, Runtime.getRuntime().availableProcessors() * 4);
    }

    private int calcWorkerID() {
        for (int i = 0; i < workerMax; i++) {
            if (!workers.containsKey(i)) return i;
        }
        return -1;
    }

    public boolean isWorkerMax() {
        return getWorkerMax() == workers.size();
    }
}
