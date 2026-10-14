package tech.illuin.wombat.core.compaction;

import java.util.List;

//TODO: move over to illuin-tech:wombat
public interface BucketCompactor
{
    int compactBucket(long bucketStartMs, long stepMs);

    List<Long> uncompactedBuckets(long stepMs, long beforeMs);
}
