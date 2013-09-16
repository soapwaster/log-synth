package com.mapr.stats;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.SortedMap;

/**
 * Adaptive histogram based on something like streaming k-means.
 */
public class Histo {
    double compression = 1000;
    SortedMap<Double, Group> summary = Maps.newTreeMap();
    private int count = 0;

    public Histo(double compression) {
        this.compression = compression;
    }

    public void add(double x) {
        SortedMap<Double, Group> before = summary.headMap(x);
        SortedMap<Double, Group> after = summary.tailMap(x);

        Group closest;
        if (before.size() == 0) {
            if (after.size() == 0) {
                summary.put(x, new Group(x));
                count = 1;
                return;
            } else {
                closest = after.get(after.firstKey());
            }
        } else {
            if (after.size() == 0) {
                closest = before.get(before.firstKey());
            } else {
                double beforeGap = x - before.lastKey();
                double afterGap = after.firstKey() - x;
                if (beforeGap < afterGap) {
                    closest = before.get(before.lastKey());
                } else {
                    closest = after.get(after.firstKey());
                }
            }
        }
        if (closest.count > count / compression) {
            summary.put(x, new Group(x));
            count++;
        } else {
            summary.remove(closest.centroid);
            closest.add(x);
            summary.put(closest.centroid, closest);
            count++;
        }
    }

    public int size() {
        return count;
    }

    public Collection<Group> centroids() {
        return summary.values();
    }

    public static class Group implements Comparable<Group> {
        double min, max, centroid;
        int count;

        public Group(double x) {
            min = max = centroid = x;
            count = 1;
        }

        public void add(double x) {
            min = Math.min(x, min);
            max = Math.max(x, max);
            count++;
            centroid += (x - centroid) / count;
        }

        @Override
        public String toString() {
            return "Group{" +
                    "centroid=" + centroid +
                    ", count=" + count +
                    '}';
        }

        @Override
        public int compareTo(Group o) {
            int r = Double.compare(centroid, o.centroid);
            if (r == 0) {
                r = Double.compare(min, o.min);
                if (r == 0) {
                    r = Double.compare(max, o.max);
                    if (r == 0) {
                        r = count - o.count;
                    }
                }
            }
            return r;
        }
    }
}
