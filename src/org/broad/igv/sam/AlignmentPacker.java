/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.sam;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.log4j.Logger;
import org.broad.igv.feature.Range;
import org.broad.igv.feature.Strand;

import java.util.*;

/**
 * Packs alignments such that there is no overlap
 *
 * @author jrobinso
 */
public class AlignmentPacker {

    private static final Logger log = Logger.getLogger(AlignmentPacker.class);

    /**
     * Minimum gap between the end of one alignment and start of another.
     */
    public static final int MIN_ALIGNMENT_SPACING = 5;
    private static final Comparator<Alignment> lengthComparator = new Comparator<Alignment>() {
        public int compare(Alignment row1, Alignment row2) {
            return (row2.getEnd() - row2.getStart()) -
                    (row1.getEnd() - row2.getStart());

        }
    };

    private static final String NULL_GROUP_VALUE = "Because google-guava tables don't support a null key, we use a special value" +
            " for null keys. It doesn't matter much what it is, but we want to avoid collisions. I find it unlikely that " +
            " this sentence will ever be used as a group value";

    /**
     * Allocates each alignment to the rows such that there is no overlap.
     *
     * @param intervalList The order of this list determines how alignments will be packed
     *                              Each {@code AlignmentInterval} must have alignments sorted by start position
     * @param renderOptions
     */
    public PackedAlignments packAlignments(
            List<AlignmentInterval> intervalList,
            AlignmentTrack.RenderOptions renderOptions) {

        if(renderOptions == null) renderOptions = new AlignmentTrack.RenderOptions();

        LinkedHashMap<String, List<Row>> packedAlignments = new LinkedHashMap<String, List<Row>>();
        boolean pairAlignments = renderOptions.isViewPairs() || renderOptions.isPairedArcView();

//        if (iter == null || !iter.hasNext()) {
//            return new PackedAlignments(intervalList, packedAlignments, renderOptions);
//        }

        if (renderOptions.groupByOption == null) {
            List<Row> alignmentRows = new ArrayList<Row>(10000);
            pack(intervalList, pairAlignments, alignmentRows);
            packedAlignments.put("", alignmentRows);
        } else {
            // Separate alignments into groups.
            Table<String, Range, List<Alignment>> groupedAlignments = HashBasedTable.create();

            for(AlignmentInterval interval: intervalList){
                Iterator<Alignment> iter = interval.getAlignmentIterator();
                while (iter.hasNext()) {
                    Alignment alignment = iter.next();
                    String groupKey = getGroupValue(alignment, renderOptions);
                    if (groupKey == null) {
                        groupKey = NULL_GROUP_VALUE;
                    }
                    List<Alignment> group = groupedAlignments.get(groupKey, interval);
                    if (group == null) {
                        group = new ArrayList<Alignment>(1000);
                        groupedAlignments.put(groupKey, interval, group);
                    }
                    group.add(alignment);
                }
            }


            // Now alphabetize (sort) and pack the groups
            List<String> keys = new ArrayList<String>(groupedAlignments.rowKeySet());
            Comparator<String> groupComparator = getGroupComparator(renderOptions.groupByOption);
            Collections.sort(keys, groupComparator);

            for (String key : keys) {
                List<Row> alignmentRows = new ArrayList<Row>(10000);
                Map<Range, List<Alignment>> group = groupedAlignments.row(key);
                pack(group, intervalList, pairAlignments, alignmentRows);
                packedAlignments.put(key, alignmentRows);
            }
            //Put null valued group at end
            List<Row> alignmentRows = new ArrayList<Row>(10000);
            Map<Range, List<Alignment>> group = groupedAlignments.row(NULL_GROUP_VALUE);
            pack(group, intervalList, pairAlignments, alignmentRows);
            packedAlignments.put("", alignmentRows);
        }
        return new PackedAlignments(intervalList, packedAlignments, renderOptions);
    }

    private void pack(List<AlignmentInterval> intervalList, boolean pairAlignments,
                      List<Row> alignmentRows) {
        Map<Range, List<Alignment>> rangeAlignmentMap = new LinkedHashMap<Range, List<Alignment>>(intervalList.size());
        for(AlignmentInterval interval: intervalList){
            rangeAlignmentMap.put(interval, interval.getAlignments());
        }
        pack(rangeAlignmentMap, intervalList, pairAlignments, alignmentRows);
    }

    private void pack(Map<Range, List<Alignment>> rangeAlignmentMap, List<? extends Range> rangeList, boolean pairAlignments,
        List<Row> alignmentRows) {

        Map<String, PairedAlignment> pairs = null;
        if (pairAlignments) {
            pairs = new HashMap<String, PairedAlignment>(1000);
        }

        int bucketCount = 0;
        int lastEnd = rangeList.get(rangeList.size() - 1).getEnd();
        for(Range range: rangeList){
            bucketCount += range.getLength();
        }

        // Create buckets.  We use priority queues to keep the buckets sorted by alignment length.  However this
        // is probably a needless complication,  any collection type would do.
        PriorityQueue<Alignment> firstBucket = new PriorityQueue<Alignment>(5, lengthComparator);

        // Use dense buckets for < 1,000,000 bp windows sparse otherwise
        BucketCollection buckets;
        if (bucketCount < 10000000) {
            buckets = new DenseBucketCollection(bucketCount);
        } else {
            buckets = new SparseBucketCollection();
        }
        buckets.set(0, firstBucket);

        //Allocate alignments to buckets based on position
        int totalCount = 0;
        //We only allocate enough buckets for each Interval, skip the in-between regions
        int curBucketStart = 0;
        for(Range range: rangeList){
            List<Alignment> alList = rangeAlignmentMap.get(range);
            if(alList == null || alList.size() == 0) continue;

            int curRangeStart = range.getStart();
            for(Alignment al: alList) {

                if (al.isMapped()) {
                    Alignment alignment = al;
                    if (pairAlignments && al.isPaired() && al.getMate().isMapped()) {
                        String readName = al.getReadName();
                        PairedAlignment pair = pairs.get(readName);
                        if (pair == null) {
                            pair = new PairedAlignment(al);
                            pairs.put(readName, pair);
                            alignment = pair;
                        } else {
                            // Add second alignment to pair
                            pair.setSecondAlignment(al);
                            pairs.remove(readName);
                            continue;

                        }
                    }

                    int bucketNumber = al.getStart() - curRangeStart;
                    // We can get negative buckets if soft-clipping is on as the alignments are only approximately
                    // sorted.  Throw all alignments < start in the first bucket of this range.
                    bucketNumber = Math.max(0, bucketNumber);
                    //Offset for start of range
                    bucketNumber += curBucketStart;
                    if (bucketNumber < bucketCount) {
                        PriorityQueue<Alignment> bucket = buckets.get(bucketNumber);
                        if (bucket == null) {
                            bucket = new PriorityQueue<Alignment>(5, lengthComparator);
                            buckets.set(bucketNumber, bucket);
                        }
                        bucket.add(alignment);
                        totalCount++;
                    } else {
                        log.debug("Alignment out of bounds: " + alignment.getStart() + " (> " + lastEnd);
                    }


                }
            }
            curBucketStart += range.getLength();
        }

        buckets.finishedAdding();

        // Allocate alignments to rows
        long t0 = System.currentTimeMillis();
        int allocatedCount = 0;
        int curRangeIndex = 0;
        Range curRange = rangeList.get(curRangeIndex);
        curBucketStart = 0;
        int nextStart = curRange.getStart();
        Row currentRow = new Row();
        List<Integer> emptyBuckets = new ArrayList<Integer>(100);

        while (allocatedCount < totalCount) {

            // Loop through alignments until we reach the end of the interval
            while (curRange != null) {
                PriorityQueue<Alignment> bucket;

                // Advance to next occupied bucket
                int bucketNumber = nextStart - curRange.getStart() + curBucketStart;
                bucket = buckets.getNextBucket(bucketNumber, emptyBuckets);

                // Pull the next alignment out of the bucket and add to the current row
                if (bucket == null) {
                    break;
                } else {
                    Alignment alignment = bucket.remove();
                    currentRow.addAlignment(alignment);
                    allocatedCount++;

                    nextStart = currentRow.getLastEnd() + MIN_ALIGNMENT_SPACING;
                    //We have several discontinuous Ranges in the general case
                    //When we reach the end of one, move to the next
                    if(nextStart >= curRange.getEnd()){
                        curBucketStart += curRange.getLength();
                        curRangeIndex += 1;
                        if(curRangeIndex >= rangeList.size()){
                            curRange = null;
                            break;
                        }else{
                            curRange = rangeList.get(curRangeIndex);
                        }

                    }
                }
            }

            // We've reached the end of the interval,  start a new row
            if (currentRow.alignments.size() > 0) {
                alignmentRows.add(currentRow);
            }

            // If we have more than 20 empty buckets remove them.  This has no affect on the dense implementation,
            // they are removed on the fly, but is needed for the sparse implementation
            buckets.removeBuckets(emptyBuckets);
            emptyBuckets.clear();
            currentRow = new Row();
            curRangeIndex = 0;
            curRange = rangeList.get(curRangeIndex);
            curBucketStart = 0;
            nextStart = curRange.getStart();
        }
        if (log.isDebugEnabled()) {
            long dt = System.currentTimeMillis() - t0;
            log.debug("Packed alignments in " + dt);
        }

        // Add the last row
        if (currentRow != null && currentRow.alignments.size() > 0) {
            alignmentRows.add(currentRow);
        }


    }

    private Comparator<String> getGroupComparator(AlignmentTrack.GroupOption groupByOption) {
        switch (groupByOption) {
            case PAIR_ORIENTATION:
                return new PairOrientationComparator();
            default:
                //Sort null values towards the end
                return new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        if (o1 != null) {
                            return o1.compareToIgnoreCase(o2);
                        } else if (o2 != null) {
                            return o2.compareToIgnoreCase(o1);
                        } else {
                            //Both null;
                            return 0;
                        }

                    }
                };
        }
    }

    private String getGroupValue(Alignment al, AlignmentTrack.RenderOptions renderOptions) {

        AlignmentTrack.GroupOption groupBy = renderOptions.groupByOption;
        String tag = renderOptions.getGroupByTag();

        switch (groupBy) {
            case STRAND:
                return String.valueOf(al.isNegativeStrand());
            case SAMPLE:
                return al.getSample();
            case READ_GROUP:
                return al.getReadGroup();
            case TAG:
                Object tagValue = al.getAttribute(tag);
                return tagValue == null ? null : tagValue.toString();
            case FIRST_OF_PAIR_STRAND:
                Strand strand = al.getFirstOfPairStrand();
                String strandString = strand == Strand.NONE ? null : strand.toString();
                return strandString;
            case PAIR_ORIENTATION:
                PEStats peStats = AlignmentRenderer.getPEStats(al, renderOptions);
                AlignmentTrack.OrientationType type = AlignmentRenderer.getOrientationType(al, peStats);
                if (type == null) {
                    return AlignmentTrack.OrientationType.UNKNOWN.name();
                }
                return type.name();
            case MATE_CHROMOSOME:
                ReadMate mate = al.getMate();
                if (mate == null) return null;
                return mate.getChr();
        }
        return null;
    }

    static interface BucketCollection {

        void set(int idx, PriorityQueue<Alignment> bucket);

        PriorityQueue<Alignment> get(int idx);

        PriorityQueue<Alignment> getNextBucket(int bucketNumber, Collection<Integer> emptyBuckets);

        void removeBuckets(Collection<Integer> emptyBuckets);

        void finishedAdding();

    }

    /**
     * Dense array implementation of BucketCollection.  Assumption is all or nearly all the genome region is covered
     * with reads.
     */
    static class DenseBucketCollection implements BucketCollection {

        int lastBucketNumber = -1;

        final PriorityQueue[] bucketArray;

        DenseBucketCollection(int bucketCount) {
            bucketArray = new PriorityQueue[bucketCount];
        }

        public void set(int idx, PriorityQueue<Alignment> bucket) {
            bucketArray[idx] = bucket;
        }

        public PriorityQueue<Alignment> get(int idx) {
            return bucketArray[idx];
        }


        /**
         * Return the next occupied bucket after bucketNumber
         *
         * @param bucketNumber
         * @param emptyBuckets ignored
         * @return
         */
        public PriorityQueue<Alignment> getNextBucket(int bucketNumber, Collection<Integer> emptyBuckets) {

            if (bucketNumber == lastBucketNumber) {
                // TODO -- detect inf loop here
            }

            PriorityQueue<Alignment> bucket = null;
            while (bucketNumber < bucketArray.length) {
                bucket = bucketArray[bucketNumber];
                if (bucket != null) {
                    if (bucket.isEmpty()) {
                        bucketArray[bucketNumber] = null;
                        bucket = null;
                    } else {
                        return bucket;
                    }
                }
                bucketNumber++;
            }
            return null;
        }

        public void removeBuckets(Collection<Integer> emptyBuckets) {
            // Nothing to do, empty buckets are removed "on the fly"
        }

        public void finishedAdding() {
            // nothing to do
        }
    }


    /**
     * "Sparse" implementation of an alignment BucketCollection.  Assumption is there are small clusters of alignments
     * along the genome, with mostly "white space".
     */
    static class SparseBucketCollection implements BucketCollection {

        boolean finished = false;
        List<Integer> keys;
        final HashMap<Integer, PriorityQueue<Alignment>> buckets;

        SparseBucketCollection() {
            buckets = new HashMap(1000);
        }

        public void set(int idx, PriorityQueue<Alignment> bucket) {
            if (finished) {
                log.error("Error: bucket added after finishAdding() called");
            }
            buckets.put(idx, bucket);
        }

        public PriorityQueue<Alignment> get(int idx) {
            return buckets.get(idx);
        }

        /**
         * Return the next occupied bucket at or after after bucketNumber.
         *
         * @param bucketNumber -- the hash bucket index for the alignments, essential the position relative to the start
         *                     of this packing interval
         * @return the next occupied bucket at or after bucketNumber, or null if there are none.
         */
        public PriorityQueue<Alignment> getNextBucket(int bucketNumber, Collection<Integer> emptyBuckets) {

            PriorityQueue<Alignment> bucket = null;
            int min = 0;
            int max = keys.size() - 1;

            // Get close to the right index, rather than scan from the beginning
            while ((max - min) > 5) {
                int mid = (max + min) / 2;
                Integer key = keys.get(mid);
                if (key > bucketNumber) {
                    max = mid;
                } else {
                    min = mid;
                }
            }

            // Now march from min to max until we cross bucketNumber
            for (int i = min; i < keys.size(); i++) {
                Integer key = keys.get(i);
                if (key >= bucketNumber) {
                    bucket = buckets.get(key);
                    if (bucket.isEmpty()) {
                        emptyBuckets.add(key);
                        bucket = null;
                    } else {
                        return bucket;
                    }
                }
            }
            return null;     // No bucket found
        }

        public void removeBuckets(Collection<Integer> emptyBuckets) {

            if (emptyBuckets.isEmpty()) {
                return;
            }

            for (Integer i : emptyBuckets) {
                buckets.remove(i);
            }
            keys = new ArrayList<Integer>(buckets.keySet());
            Collections.sort(keys);
        }

        public void finishedAdding() {
            finished = true;
            keys = new ArrayList<Integer>(buckets.keySet());
            Collections.sort(keys);
        }
    }

    private class PairOrientationComparator implements Comparator<String> {
        private final List<AlignmentTrack.OrientationType> orientationTypes;
        //private final Set<String> orientationNames = new HashSet<String>(AlignmentTrack.OrientationType.values().length);

        public PairOrientationComparator() {
            orientationTypes = Arrays.asList(AlignmentTrack.OrientationType.values());
//            for(AlignmentTrack.OrientationType type: orientationTypes){
//                orientationNames.add(type.name());
//            }
        }

        @Override
        public int compare(String s0, String s1) {
            if (s0 != null && s1 != null) {
                AlignmentTrack.OrientationType t0 = AlignmentTrack.OrientationType.valueOf(s0);
                AlignmentTrack.OrientationType t1 = AlignmentTrack.OrientationType.valueOf(s1);
                return orientationTypes.indexOf(t0) - orientationTypes.indexOf(t1);
            } else if (s0 == null ^ s1 == null) {
                //exactly one is null
                return s0 == null ? 1 : -1;
            } else {
                //both null
                return 0;
            }

        }
    }

}
