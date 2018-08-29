package org.broadinstitute.hellbender.tools.spark.pipelines.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.hellbender.engine.*;
import org.broadinstitute.hellbender.tools.walkers.annotator.VariantAnnotatorEngine;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;


import java.util.*;

public class FindMismatchingReads extends GATKTool {
    private static final long serialVersionUID = 1L;

    private static final int LEADING_READS = 1000000;

    @Argument(doc="The first BAM",fullName = "control", optional = false)
    protected String input1;

    @Argument(doc="The second BAM",fullName = "test", optional = false)
    protected String input2;

    ReadsDataSource controlFile;
    ReadsDataSource testFile;


    @Override
    public boolean requiresReads() { return true; }

    public void onTraversalStart() {
        controlFile = new ReadsDataSource(IOUtils.getPath(input1));
        testFile = new ReadsDataSource(IOUtils.getPath(input2));
    }


    @Override
    public void traverse() {
        Set<GATKRead> bigMapOfAllReads = new HashSet<>();

        List<GATKRead> discoradantReads = new ArrayList<>();

        Iterator<GATKRead> controlFileIterator = controlFile.iterator();
        Iterator<GATKRead> testFileIterator = testFile.iterator();

        int i = 0;
        int j = 0;

        while (controlFileIterator.hasNext()) {
            GATKRead controlRead = controlFileIterator.next();
            if (bigMapOfAllReads.contains(controlRead)) System.out.println("Map already contained the read '"+controlRead.toString()+"' this will result in an error later");
            bigMapOfAllReads.add(controlFileIterator.next());
            i++;

            while (j < i-LEADING_READS) {
                GATKRead testRead = testFileIterator.next();
                if (!bigMapOfAllReads.remove(testRead)) {
                    discoradantReads.add(testRead);
                }
                j++;
            }
        }
        System.out.println("Traversed test file with "+i+" total reads");

        while (testFileIterator.hasNext()) {
            GATKRead testRead = testFileIterator.next();
            if (!bigMapOfAllReads.remove(testRead)) {
                discoradantReads.add(testRead);
            }
            j++;
        }
        System.out.println("Traversed test file with "+j+" total reads");

        System.out.println("The following reads were not matched (duplicated or created) from input1 to input2:");
        discoradantReads.forEach(read -> {
            if (!bigMapOfAllReads.remove(read)) {
                System.out.println(read.toString());
            }
        });

        System.out.println("The following reads did not apparently exist in input2:");
        bigMapOfAllReads.forEach(read -> System.out.println(read.toString()));

    }
}
