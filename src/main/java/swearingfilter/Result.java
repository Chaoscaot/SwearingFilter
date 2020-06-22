package swearingfilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Result {

    Map<String, Integer> results;
    long lenght;
    Map<String, Integer> count;

    public Result(long lenght) {
        this.lenght = lenght;
        results = new HashMap<>();
        count = new HashMap<>();
    }

    public void addToMap(String string, Integer byt) {
        results.put(string, byt);
        count.put(string, 1);
    }

    public boolean containsMap(String string) {
        return results.containsKey(string);
    }

    public int calcSwearing() {
        int returns = 0;
        for (Map.Entry<String, Integer> e:
             results.entrySet()) {
            returns+= e.getValue() * count.get(e.getKey());
        }
        return returns;
    }

    public void countUp(String str) {
        int i = count.get(str);
        i++;
        count.remove(str);
        count.put(str, i);
    }

    public String[] getSwearing() {
        String[] strings = new String[results.size()];
        int i = 0;
        for (Map.Entry<String, Integer> e:
                results.entrySet()) {
            strings[i] = e.getKey();
            i++;
        }
        return strings;
    }

    public boolean toBlock(double limit) {
        double swearingPercent = (double) calcSwearing() / lenght;
        System.out.println(swearingPercent);
        return swearingPercent >= limit;
    }

    @Override
    public String toString() {
        return "Result{" +
                "results=" + results +
                ", lenght=" + lenght +
                ", count=" + count +
                '}';
    }
}
