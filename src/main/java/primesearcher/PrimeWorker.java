package primesearcher;

import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2(topic = "PrimeWorker")
public class PrimeWorker implements Callable {

    private Collection<PrimeWorker> workers;
    public  List<Integer> primes;
    private Integer from;
    private Integer to;
    private int[] numbers;

    private Boolean isLogIntoFile;

    public PrimeWorker(Collection<PrimeWorker> workers, Integer from, Integer to, Boolean isLogIntoFile) {
        this.workers = workers;
        this.from = from;
        this.to = to;
        this.numbers = IntStream.rangeClosed(from, to).toArray();
        this.primes = Collections.synchronizedList(new ArrayList<>());
        this.isLogIntoFile = isLogIntoFile;
    }

    private void info(String text) {
        info(text,false);
    }
    private void info(String text,Boolean noOut) {
        if (this.isLogIntoFile) {
            log.info(text);
        }
        if (noOut==false) {
            System.out.println(text);
        }
    }

    public static String mkString(String separator,int[] array) {
        String r = "";
        for (Integer x:array ) {
            r = r + (r.isEmpty()?"":separator) + x;
        }
        return r;
    }

    @Override
    public int[] call() {
        try {
            Collection<PrimeWorker> workersPrivate = new ArrayList<>();
            for (PrimeWorker worker : this.workers) {
                workersPrivate.add(worker);
            }
            this.workers = workersPrivate;
            Map<PrimeWorker,List<Integer>> processed = new HashMap<>();
            Set<PrimeWorker> workers = this.workers.stream().filter(w -> this.to > w.from && this.from != w.from).collect(Collectors.toSet());
            workers = workers.stream().filter(worker->{
                Boolean completed = false;
                while (!completed) {
                    // System.out.println(""+this+": Waiting a new prime number from "+ worker);
                    synchronized (worker.primes) {
                        for (Integer inPrime: worker.primes ) {
                            if (inPrime.equals(0)) {
                                completed = true; break;
                            }
                            if (processed.get(worker)==null) {
                                processed.put(worker,new ArrayList<>());
                            }
                            if (processed.get(worker).stream().filter((x)->x.equals(inPrime)).count()==0) {
                                StringBuffer crossedOut = new StringBuffer("");
                                if (inPrime>1) for (int i = 0; i < numbers.length; i++) if (numbers[i]>0 && numbers[i] % inPrime == 0) {
                                    crossedOut.append((crossedOut.isEmpty()?"":",")+numbers[i]);
                                    numbers[i] = 0;
                                }
                                info(""+this+": Знайдено нове просте число "+inPrime + (crossedOut.isEmpty()?"":(", викреслено " + crossedOut))  + "",true);
                                processed.get(worker).add(inPrime);
                            }
                        }
                    }
                }
                info(""+this+": Завершено для "+ worker, true);
                return false;
            }).collect(Collectors.toSet());
            if (workers.size()==0) {
                for (int i = 0; i < numbers.length; i++) if (numbers[i]!=0) {
                    for (int j = 0; j < numbers[i]; j++) if (j>1 && numbers[i] % j == 0) {
                        numbers[i] = 0;
                    }
                }
            }
            for (int number: numbers) if (number>1) {
                this.primes.add(number);
            }
            this.primes.add(0);
            this.numbers = Arrays.stream(this.numbers).filter(value -> value!=0).toArray();
            if (this.numbers.length>0) {
                info("\t"+ this +"\t\tНайдено "+this.numbers.length+": "+mkString(",",this.numbers)+"");
            } else {
                //System.out.println(""+ this +": Найдено "+this.numbers.length+" простих");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.numbers;
    }

    public int[] getNumbers() {
        return numbers;
    }

    @Override
    public String toString() {
        return Thread.currentThread().getName()+"\t"+this.from+"-"+this.to;
    }
}
