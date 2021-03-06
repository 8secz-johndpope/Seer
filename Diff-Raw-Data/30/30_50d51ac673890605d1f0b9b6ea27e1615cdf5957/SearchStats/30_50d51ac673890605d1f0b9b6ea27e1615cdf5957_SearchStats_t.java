 package net.intelie.lognit.cli.model;
 
 import com.google.common.base.Objects;
 import com.google.common.collect.Lists;
 import com.google.common.primitives.Longs;
 
 import java.io.Serializable;
 import java.util.*;
 
import static java.lang.Math.min;

 public class SearchStats implements Serializable {
     private final Map<String, List<FreqPoint<String>>> fields;
     private List<FreqPoint<Long>> hours;
     private List<FreqPoint<Long>> last;
 
     public SearchStats() {
         this(new ArrayList<FreqPoint<Long>>(), new ArrayList<FreqPoint<Long>>());
     }
 
     public SearchStats(List<FreqPoint<Long>> hours, List<FreqPoint<Long>> last) {
         this(hours, last, new HashMap<String, List<FreqPoint<String>>>());
     }
 
     public SearchStats(List<FreqPoint<Long>> hours, List<FreqPoint<Long>> last, Map<String, List<FreqPoint<String>>> fields) {
         this.hours = hours;
         this.last = last;
         this.fields = fields;
     }
 
     public List<FreqPoint<Long>> hours() {
         return hours;
     }
 
     public List<FreqPoint<Long>> last() {
         return last;
     }
 
     public Map<String, List<FreqPoint<String>>> fields() {
         return fields;
     }
 
     @Override
     public boolean equals(Object o) {
         if (this == o) return true;
         if (!(o instanceof SearchStats)) return false;
 
         SearchStats that = (SearchStats) o;
 
         return Objects.equal(this.hours, that.hours) &&
                 Objects.equal(this.last, that.last) &&
                 Objects.equal(this.fields, that.fields);
 
     }
 
     @Override
     public int hashCode() {
         return Objects.hashCode(this.hours, this.last, this.fields);
     }
 
     public void merge(SearchStats that) {
         if (that == null) return;
        this.hours = mergeFreqs(this.hours, that.hours, false, 24);
        this.last = mergeFreqs(this.last, that.last, false, 20);
 
         for (Map.Entry<String, List<FreqPoint<String>>> entry : that.fields.entrySet()) {
            List<FreqPoint<String>> merged = mergeFreqs(this.fields.get(entry.getKey()), entry.getValue(), true, 10);
             sortByMostFrequent(merged);
             this.fields.put(entry.getKey(), merged);
         }
     }
 
     private void sortByMostFrequent(List<FreqPoint<String>> merged) {
         Collections.sort(merged, new Comparator<FreqPoint<String>>() {
             @Override
             public int compare(FreqPoint<String> a, FreqPoint<String> b) {
                 return -Longs.compare(a.freq(), b.freq());
             }
         });
     }
 
    private <T> List<FreqPoint<T>> mergeFreqs(List<FreqPoint<T>> target, List<FreqPoint<T>> source, boolean sort, int max) {
        Map<T, Long> longMap = new LinkedHashMap<T, Long>();
 
         if (target != null)
             for (FreqPoint<T> item : target)
                 longMap.put(item.key(), def(longMap.get(item.key())) + item.freq());
 
         if (source != null)
             for (FreqPoint<T> item : source)
                 longMap.put(item.key(), def(longMap.get(item.key())) + item.freq());
 
 
        ArrayList<FreqPoint<T>> list = Lists.newArrayList();
         for (Map.Entry<T, Long> entry : longMap.entrySet()) {
             list.add(new FreqPoint<T>(entry.getKey(), entry.getValue()));
         }
        if (sort) {
            Collections.sort(list, new Comparator<FreqPoint<T>>() {
                @Override
                public int compare(FreqPoint<T> a, FreqPoint<T> b) {
                    return Longs.compare(b.freq(), a.freq());
                }
            });
        }

        return list.subList(0, min(max, list.size()));
     }
 
     private long def(Long value) {
         if (value == null) return 0;
         return value;
     }
 }
