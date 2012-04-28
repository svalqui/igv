/*
 * Copyright (c) 2007-2011 by Institute for Computational Biomedicine,
 *                                          Weill Medical College of Cornell University.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.goby;

import edu.cornell.med.icb.goby.alignments.Alignments;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.sf.samtools.util.CloseableIterator;
import org.broad.igv.sam.Alignment;
import org.broad.igv.util.TestUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Test Goby IGV classes.
 * User: jrobinso
 * Date: Jul 12, 2010
 * Time: 11:25:52 AM
 */
public class GobyAlignmentQueryReaderTest {


    @Test
    public void testGetSequenceNames() throws Exception {
        Set<String> expectedSequences = new HashSet(Arrays.asList("GL000229.1", "GL000200.1", "GL000228.1", "GL000201.1", "GL000241.1", "GL000219.1",
                "GL000191.1", "GL000242.1", "GL000243.1", "22", "GL000245.1", "MT", "3", "GL000217.1", "2", "1", "7", "6", "5",
                "GL000215.1", "4", "9", "8", "GL000244.1", "GL000216.1", "GL000218.1", "GL000210.1", "GL000248.1", "GL000224.1",
                "GL000203.1", "19", "17", "GL000194.1", "M", "18", "15", "16", "13", "14", "GL000195.1", "11", "12", "GL000225.1",
                "21", "20", "GL000193.1", "GL000204.1", "GL000237.1", "GL000246.1", "Y", "GL000205.1", "GL000247.1", "X", "GL000192.1",
                "GL000227.1", "GL000235.1", "GL000197.1", "GL000211.1", "GL000236.1", "GL000240.1", "GL000207.1", "GL000239.1", "GL000232.1",
                "GL000212.1", "GL000238.1", "GL000231.1", "GL000233.1", "GL000226.1", "GL000249.1", "GL000223.1", "GL000199.1", "10", "GL000196.1",
                "GL000209.1", "GL000202.1", "GL000214.1", "GL000220.1", "GL000198.1", "GL000208.1", "GL000221.1", "GL000213.1", "GL000234.1", "GL000222.1",
                "GL000206.1", "GL000230.1"));

        String thmFile = TestUtils.DATA_DIR + "goby/GDFQPGI-pickrellNA18486_yale.tmh";
        GobyAlignmentQueryReader reader = new GobyAlignmentQueryReader(thmFile);
        List<String> seqs = reader.getSequenceNames();
        assertEquals(expectedSequences.size(), seqs.size());
        for (String s : seqs) {
            assertTrue(expectedSequences.contains(s));
        }
    }

    @Test
    public void testIterator() throws Exception {

        String entriesFile = TestUtils.DATA_DIR + "goby/GDFQPGI-pickrellNA18486_yale.entries";
        GobyAlignmentQueryReader reader = new GobyAlignmentQueryReader(entriesFile);
        CloseableIterator<Alignment> iter = reader.iterator();

        assertTrue(iter.hasNext());
        iter.close();
        reader.close();
    }


    /**
     * Test a query interval that has alignments.
     *
     * @throws Exception
     */
    @Test
    public void testQueryPE() throws Exception {

        String entriesFile = TestUtils.DATA_DIR + "goby/paired-end/paired-alignment.entries";

        GobyAlignmentQueryReader.supportsFileType(entriesFile);
        GobyAlignmentQueryReader reader = new GobyAlignmentQueryReader(entriesFile);
        CloseableIterator<Alignment> iter = reader.query("chr1", 1, 240000000, false);

        assertTrue(iter.hasNext());

        iter.close();
        reader.close();
    }

    /**
     * Test a query interval with no alignments
     *
     * @throws Exception
     */
    @Test
    public void testQueryNoAlignments() throws Exception {

        String entriesFile = TestUtils.DATA_DIR + "goby/paired-end/paired-alignment.entries";

        GobyAlignmentQueryReader.supportsFileType(entriesFile);
        GobyAlignmentQueryReader reader = new GobyAlignmentQueryReader(entriesFile);
        CloseableIterator<Alignment> iter = reader.query("chr1", 1, 1000, false);

        assertFalse(iter.hasNext());

        iter.close();
        reader.close();
    }

    /**
     * Test a query interval with no alignments
     *
     * @throws Exception
     */
    @Test
    public void testHasNextBug() throws Exception {

        String entriesFile = TestUtils.DATA_DIR + "goby/paired-end/paired-alignment.entries";

        GobyAlignmentQueryReader.supportsFileType(entriesFile);
        GobyAlignmentQueryReader reader = new GobyAlignmentQueryReader(entriesFile);
        CloseableIterator<Alignment> iter = reader.iterator();

        while (iter.hasNext()) {
            Alignment alignment = iter.next();
            assertNotNull(alignment);
        }
    }

    /**
     * Test a query interval with no alignments
     *
     * @throws Exception
     */
    @Test
    public void testOrdering() throws Exception {

        String entriesFile = TestUtils.DATA_DIR + "goby/GDFQPGI-pickrellNA18486_yale.entries";
        //   String entriesFile =  TestUtils.DATA_DIR + "goby/paired-end/paired-alignment.entries";

        GobyAlignmentQueryReader.supportsFileType(entriesFile);
        GobyAlignmentQueryReader reader = new GobyAlignmentQueryReader(entriesFile);
        CloseableIterator<Alignment> iter = reader.iterator();
        String previousChr = "";
        int previousAlignmentStart = -1;
        ObjectSet<String> chromosomeSeen = new ObjectArraySet<String>();
        // maximum number of entries to inspect (keep low for faster test).
        int maxEntries = 100000;
        int countVisit = 0;
        while (iter.hasNext()) {
            Alignment a = iter.next();
            final String entryChr = a.getChr();
            //   System.out.println("chr:" + entryChr);

            if (entryChr.equals(previousChr)) {
                assertTrue(a.getAlignmentStart() >= previousAlignmentStart);
            } else {
                assertFalse("Chromosomes should occur in blocks." +
                        " A chromosome that was used in a previous block of entry cannot occur again.",
                        chromosomeSeen.contains(a.getChr()));
                previousChr = a.getChr();
                chromosomeSeen.add(a.getChr());
            }
            countVisit++;
            if (countVisit > maxEntries) break;
        }

        iter.close();
        reader.close();
    }

    @Test
    public void testAlignmentTwoMutations() {


        Alignments.SequenceVariation mutation = Alignments.SequenceVariation.newBuilder().setFrom("AA").setTo("TC").
                setPosition(10).setReadIndex(10).build();//.setToQuality(???).build();
        Alignments.AlignmentEntry entry = Alignments.AlignmentEntry.newBuilder().
                setQueryLength(50).setPosition(1000).setMatchingReverseStrand(false).
                setQueryIndex(0).setTargetIndex(1).
                setQueryAlignedLength(50).addSequenceVariations(mutation).build();
        GobyAlignment gAlignment = new GobyAlignment(null, entry);
        gAlignment.buildBlocks(entry);
        assertEquals(1, gAlignment.block.length);
        assertEquals(50, gAlignment.block[0].getBases().length);
    }

    @Test
    public void testAlignmentLeftPadding() {


        Alignments.AlignmentEntry entry = Alignments.AlignmentEntry.newBuilder().
                setQueryLength(50).setPosition(1000).setMatchingReverseStrand(false).
                setQueryIndex(0).setTargetIndex(1).
                setQueryAlignedLength(30).setQueryPosition(20).build();
        GobyAlignment
                gAlignment = new GobyAlignment(null, entry);
        gAlignment.buildBlocks(entry);
        assertEquals(1, gAlignment.block.length);
        assertEquals(30, gAlignment.block[0].getBases().length);
    }

    @Test
    public void testAlignmentRightPadding() {

        Alignments.AlignmentEntry entry = Alignments.AlignmentEntry.newBuilder().
                setQueryLength(50).setPosition(1000).setMatchingReverseStrand(false).
                setQueryIndex(0).setTargetIndex(1).
                setQueryAlignedLength(30).setQueryPosition(0).build();
        GobyAlignment
                gAlignment = new GobyAlignment(null, entry);
        gAlignment.buildBlocks(entry);
        assertEquals(1, gAlignment.block.length);
        assertEquals(30, gAlignment.block[0].getBases().length);
    }

    @Test
    public void testAlignmentOneReadInsertion() {


        Alignments.SequenceVariation mutation = Alignments.SequenceVariation.newBuilder().setFrom("--").setTo("TC").
                setPosition(10).setReadIndex(10).build();
        Alignments.AlignmentEntry entry = Alignments.AlignmentEntry.newBuilder().setPosition(1000).setMatchingReverseStrand(false).
                setQueryLength(50).setQueryIndex(0).setTargetIndex(1).
                setQueryAlignedLength(50).addSequenceVariations(mutation).build();
        GobyAlignment
                gAlignment = new GobyAlignment(null, entry);
        gAlignment.buildBlocks(entry);
        assertEquals(1, gAlignment.block.length);
        assertEquals(1, gAlignment.insertionBlock.length);
        assertEquals(1010, gAlignment.insertionBlock[0].getStart());
        assertEquals(50, gAlignment.block[0].getBases().length);
    }

    @Test
    public void testAlignmentOneReadDeletion() {


        Alignments.SequenceVariation mutation = Alignments.SequenceVariation.newBuilder().setFrom("TC").setTo("--").
                setPosition(10).setReadIndex(10).build();
        Alignments.AlignmentEntry entry = Alignments.AlignmentEntry.newBuilder().setPosition(1000).setMatchingReverseStrand(false).
                setQueryLength(50).setQueryIndex(0).setTargetIndex(1).
                setQueryAlignedLength(50).addSequenceVariations(mutation).build();
        GobyAlignment
                gAlignment = new GobyAlignment(null, entry);
        gAlignment.buildBlocks(entry);
        assertEquals(2, gAlignment.block.length);
        assertEquals(9, gAlignment.block[0].getBases().length);
        assertEquals(41, gAlignment.block[1].getBases().length);
    }

    @Test
    public void testAlignmentActualEntry1() {

        /**
         *  query_index: 26
         target_index: 0
         position: 31
         score: 43.0
         query_position: 1
         matching_reverse_strand: true
         multiplicity: 1
         number_of_mismatches: 2
         number_of_indels: 3
         query_length: 50
         query_aligned_length: 48
         target_aligned_length: 45
         sequence_variations {
         to: "AA"
         from: "CC"
         position: 10
         read_index: 40
         }
         sequence_variations {
         to: "ATC"
         from: "---"
         position: 25
         read_index: 24
         }

         Alignment start position = chrsynth1:32
         read-sequence
         */
        Alignments.SequenceVariation mutation1 = Alignments.SequenceVariation.newBuilder().setFrom("ATC").setTo("---").
                setPosition(25).setReadIndex(24).build();
        Alignments.SequenceVariation mutation2 = Alignments.SequenceVariation.newBuilder().setFrom("AA").setTo("CC").
                setPosition(10).setReadIndex(40).build();
        Alignments.AlignmentEntry entry = Alignments.AlignmentEntry.newBuilder().setPosition(31).setMatchingReverseStrand(true).
                setQueryLength(50).setQueryIndex(26).setTargetIndex(1).
                setQueryAlignedLength(40).setNumberOfMismatches(2).setNumberOfIndels(3).addSequenceVariations(mutation1).
                addSequenceVariations(mutation2).build();
        GobyAlignment
                gAlignment = new GobyAlignment(null, entry);
        gAlignment.buildBlocks(entry);
        assertEquals(2, gAlignment.block.length);
        assertEquals(24, gAlignment.block[0].getBases().length);
        assertEquals(16, gAlignment.block[1].getBases().length);
    }


    @Test
    public void testAlignmentWithSplicing() throws IOException {


        Alignments.SequenceVariation mutation = Alignments.SequenceVariation.newBuilder().setFrom("T").setTo("A").
                setPosition(10).setReadIndex(10).build();
        Alignments.RelatedAlignmentEntry linkForward = Alignments.RelatedAlignmentEntry.newBuilder().
                setFragmentIndex(1).setPosition(1200).setTargetIndex(1).build();

        Alignments.RelatedAlignmentEntry linkBackward = Alignments.RelatedAlignmentEntry.newBuilder().
                setFragmentIndex(0).setPosition(1000).setTargetIndex(1).build();
        Alignments.AlignmentEntry entry1 = Alignments.AlignmentEntry.newBuilder().setPosition(1000).setMatchingReverseStrand(false).
                setQueryLength(50).setQueryIndex(0).setTargetIndex(1).setFragmentIndex(0).setSplicedForwardAlignmentLink(linkForward).
                setQueryAlignedLength(20).addSequenceVariations(mutation).build();
        Alignments.AlignmentEntry entry2 = Alignments.AlignmentEntry.newBuilder().setPosition(1200).setMatchingReverseStrand(false).
                setQueryLength(50).setQueryIndex(0).setTargetIndex(1).setFragmentIndex(1).setSplicedBackwardAlignmentLink(linkBackward).
                setQueryAlignedLength(30).build();
        ObjectArrayList<Alignments.AlignmentEntry> alignments = new ObjectArrayList<Alignments.AlignmentEntry>();
        alignments.add(entry1);
        alignments.add(entry2);
        final ObjectListIterator<Alignments.AlignmentEntry> iterator = alignments.iterator();
        GobyAlignmentIterator alignIterator = new GobyAlignmentIterator(1, 0, 10000) {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Alignment next() {
                return new GobyAlignment(this, iterator.next());
            }
        };

        ObjectArrayList<GobyAlignment> visitedEntries = new ObjectArrayList<GobyAlignment>();

        while (alignIterator.hasNext()) {
            GobyAlignment align = (GobyAlignment) alignIterator.next();
            visitedEntries.add(align);

        }
        int index = 0;
        for (GobyAlignment align : visitedEntries) {
            if (index == 0) {
                assertEquals(2, align.block.length);
                assertEquals(20, align.block[0].getBases().length);
                assertEquals(30, align.block[1].getBases().length);

            }
            if (index == 1) {
                assertEquals(0, align.block.length);

            }
            index++;
        }


    }

}
