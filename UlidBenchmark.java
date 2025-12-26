package example.benchmark;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.github.f4b6a3.ulid.UlidFactory;
import de.huxhorn.sulky.ulid.ULID;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * ULID実装のパフォーマンス比較ベンチマーク
 *
 * 実行方法:
 * mvn clean package
 * java -jar target/ulid-benchmark.jar
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {
        "-Xms2G",
        "-Xmx2G",
        "-XX:+UseG1GC",           // G1GCを明示的に指定
        "-XX:MaxGCPauseMillis=200",
        "-XX:+AlwaysPreTouch"     // メモリを事前確保してウォームアップを安定化
})
public class UlidBenchmark {

    // ============================================
    // 1. 基本的なULID生成（デフォルト設定）
    // ============================================

    @Benchmark
    public String sulkyDefault() {
        return new ULID().nextULID();
    }

    @Benchmark
    public String ulidCreatorDefault() {
        return UlidCreator.getUlid().toString();
    }

    // ============================================
    // 2. SecureRandom を使った生成
    // ============================================

    @State(Scope.Thread)
    public static class SecureRandomState {
        ULID sulkySecure = new ULID(new SecureRandom());
    }

    @Benchmark
    public String sulkySecureRandom(SecureRandomState state) {
        return new ULID(new SecureRandom()).nextULID();
    }

    @Benchmark
    public String ulidCreatorSecureRandom() {
        // ulid-creator は内部で最適化されたRandomを使用
        return UlidCreator.getUlid().toString();
    }

    // ============================================
    // 3. ThreadLocalRandom を使った生成
    // ============================================

    @State(Scope.Thread)
    public static class ThreadLocalRandomState {
        ULID sulkyThreadLocal = new ULID(ThreadLocalRandom.current());
    }

    @Benchmark
    public String sulkyThreadLocalRandom(ThreadLocalRandomState state) {
        return state.sulkyThreadLocal.nextULID();
    }

    // ============================================
    // 4. Monotonic ULID 生成
    // ============================================

    @State(Scope.Thread)
    public static class MonotonicState {
        ULID sulkyUlid = new ULID(ThreadLocalRandom.current());
        ULID.Value sulkyPrevious = sulkyUlid.nextValue();

        UlidFactory ulidCreatorFactory = UlidFactory.newMonotonicInstance();
    }

    @Benchmark
    public ULID.Value sulkyMonotonic(MonotonicState state) {
        ULID.Value result = state.sulkyUlid.nextMonotonicValue(state.sulkyPrevious);
        state.sulkyPrevious = result;
        return result;
    }

    @Benchmark
    public Ulid ulidCreatorMonotonic(MonotonicState state) {
        return state.ulidCreatorFactory.create();
    }

    // ============================================
    // 5. ULID パース（文字列 → オブジェクト）
    // ============================================

    @State(Scope.Benchmark)
    public static class ParseState {
        String ulidString = "01HQVJZ3G7KXJQYQVQXQVQXQVQ";
    }

    @Benchmark
    public ULID.Value sulkyParse(ParseState state) {
        return ULID.parseULID(state.ulidString);
    }

    @Benchmark
    public Ulid ulidCreatorParse(ParseState state) {
        return Ulid.from(state.ulidString);
    }

    // ============================================
    // 6. ULID 文字列化（オブジェクト → 文字列）
    // ============================================

    @State(Scope.Benchmark)
    public static class ToStringState {
        ULID sulkyUlid = new ULID(ThreadLocalRandom.current());
        ULID.Value sulkyValue = sulkyUlid.nextValue();

        Ulid ulidCreatorValue = UlidCreator.getUlid();
    }

    @Benchmark
    public String sulkyToString(ToStringState state) {
        return state.sulkyValue.toString();
    }

    @Benchmark
    public String ulidCreatorToString(ToStringState state) {
        return state.ulidCreatorValue.toString();
    }

    // ============================================
    // 7. バイナリ変換（bytes ↔ ULID）
    // ============================================

    @State(Scope.Benchmark)
    public static class BytesState {
        ULID sulkyUlid = new ULID(ThreadLocalRandom.current());
        ULID.Value sulkyValue = sulkyUlid.nextValue();
        byte[] sulkyBytes = sulkyValue.toBytes();

        Ulid ulidCreatorValue = UlidCreator.getUlid();
        byte[] ulidCreatorBytes = ulidCreatorValue.toBytes();
    }

    @Benchmark
    public byte[] sulkyToBytes(BytesState state) {
        return state.sulkyValue.toBytes();
    }

    @Benchmark
    public byte[] ulidCreatorToBytes(BytesState state) {
        return state.ulidCreatorValue.toBytes();
    }

    @Benchmark
    public ULID.Value sulkyFromBytes(BytesState state) {
        return ULID.fromBytes(state.sulkyBytes);
    }

    @Benchmark
    public Ulid ulidCreatorFromBytes(BytesState state) {
        return Ulid.from(state.ulidCreatorBytes);
    }

    // ============================================
    // 8. 高負荷シナリオ：連続生成
    // ============================================

    @Benchmark
    @OperationsPerInvocation(1000)
    public int sulkyBurst() {
        ULID ulid = new ULID(ThreadLocalRandom.current());
        int hash = 0;
        for (int i = 0; i < 1000; i++) {
            hash ^= ulid.nextULID().hashCode();
        }
        return hash;
    }

    @Benchmark
    @OperationsPerInvocation(1000)
    public int ulidCreatorBurst() {
        int hash = 0;
        for (int i = 0; i < 1000; i++) {
            hash ^= UlidCreator.getUlid().hashCode();
        }
        return hash;
    }

    // ============================================
    // 9. マルチスレッドでの生成（競合テスト）
    // ============================================

    @State(Scope.Benchmark)
    public static class SharedState {
        // 共有SecureRandom（ロック競合が発生）
        ULID sulkyShared = new ULID(new SecureRandom());
    }

    @Benchmark
    @Threads(8)
    public String sulkyMultiThreadShared(SharedState state) {
        return state.sulkyShared.nextULID();
    }

    @Benchmark
    @Threads(8)
    public String ulidCreatorMultiThread() {
        return UlidCreator.getUlid().toString();
    }

    // ============================================
    // 10. メモリフットプリント比較
    // ============================================

    @Benchmark
    public Object sulkyObjectCreation() {
        return new ULID(ThreadLocalRandom.current()).nextValue();
    }

    @Benchmark
    public Object ulidCreatorObjectCreation() {
        return UlidCreator.getUlid();
    }

    // ============================================
    // メイン実行
    // ============================================

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(UlidBenchmark.class.getSimpleName())
                .shouldFailOnError(true)
                .jvmArgs("-server")
                .build();

        new Runner(opt).run();
    }
}
