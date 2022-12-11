package com.instoof;

import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Log4j2
public class PrimeSearcher {

    private Boolean isLogIntoFile;

    public PrimeSearcher(Boolean isLogIntoFile) {
        this.isLogIntoFile = isLogIntoFile;
    }

    private void info(String text) {
        if (this.isLogIntoFile) {
            log.info(text);
        }
        System.out.println(text);
    }

    public List<Integer> search(Integer count, Integer perCount, Integer threadsCount){
        int workersCount = (count / perCount) + 1;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount, new NameableThreadFactory("P"));
        Collection<PrimeWorker> workers = new ArrayList<>();
        for (int i = 0; i < workersCount; i++) {
            int from = perCount*i, to = perCount*(i+1);
            if (from<count) {
                to = Math.min(count,to);
                workers.add(new PrimeWorker(workers, from,    to, true));
            }
        }
        info("Кількість чисел "+count);
        info("Кількість чисел на одну обробку "+perCount);
        info("Кількість обробок "+workers.size());
        info("Кількість процесорів "+threadsCount);
        Collection<Callable<int[]>> tasks = workers.stream().map(x->(Callable<int[]>)x).collect(Collectors.toList());
        try {
            executor.invokeAll(tasks);
        } catch(InterruptedException ex) {
            System.out.println(ex);
        }
        executor.shutdown();
        List<Integer> primes = new ArrayList<>();
        for (PrimeWorker worker : workers ) {
            for (Integer n : worker.getNumbers()) {
                primes.add(n);
            }
        }
        info("Кількість знайдених простих чисел : "+primes.size());
        return primes;
    }
    public List<Integer> search(Integer count, Integer perCount){
        return search(count,perCount,Runtime.getRuntime().availableProcessors());
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("До якого числа потрібно шукати прості число? "); int count = scanner.nextInt();
        System.out.print("Який розмір порції? "); int perCount = scanner.nextInt();
        PrimeSearcher searcher = new PrimeSearcher(true);
        searcher.search(count, perCount);
    }
}
