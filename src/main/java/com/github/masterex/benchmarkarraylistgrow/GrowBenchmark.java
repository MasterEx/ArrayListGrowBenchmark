/*
 * Periklis Ntanasis <pntanasis@gmail.com> 2022
 */
package com.github.masterex.benchmarkarraylistgrow;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.Throughput)
@Timeout(time = 300, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(timeUnit = TimeUnit.SECONDS, time = 5)
@Fork(value = 1)
public class GrowBenchmark {
    
    @Benchmark
    public void initializeDefaultLengh() {
        ArrayList l = new ArrayList();
        l.ensureCapacity(1000);
    }
    
    @Benchmark
    public void initializeZeroLength() {
        ArrayList l = new ArrayList(0);
        l.ensureCapacity(1000);
    }
    
    @Benchmark
    public void initializeTargetLength() {
        ArrayList l = new ArrayList(1000);
    }
    
    @Benchmark
    public void initializeEmptyArrayAndGrow() {
        ArrayList l = new ArrayList(1);
        l.ensureCapacity(1000);
    }
    
}
