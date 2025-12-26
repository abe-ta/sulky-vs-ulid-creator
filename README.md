# sulky-vs-ulid-creator

## 計測結果

```
Benchmark                                 Mode  Cnt    Score    Error   Units
UlidBenchmark.sulkyBurst                 thrpt    5   17.449 ±  1.038  ops/us
UlidBenchmark.sulkyDefault               thrpt    5    2.675 ±  0.057  ops/us
UlidBenchmark.sulkyFromBytes             thrpt    5  257.875 ± 35.219  ops/us
UlidBenchmark.sulkyMonotonic             thrpt    5   42.869 ±  2.412  ops/us
UlidBenchmark.sulkyMultiThreadShared     thrpt    5    1.630 ±  0.067  ops/us
UlidBenchmark.sulkyObjectCreation        thrpt    5   43.849 ±  0.293  ops/us
UlidBenchmark.sulkyParse                 thrpt    5   10.908 ±  0.130  ops/us
UlidBenchmark.sulkySecureRandom          thrpt    5    3.393 ±  0.057  ops/us
UlidBenchmark.sulkyThreadLocalRandom     thrpt    5   28.216 ±  0.491  ops/us
UlidBenchmark.sulkyToBytes               thrpt    5  236.648 ± 16.266  ops/us
UlidBenchmark.sulkyToString              thrpt    5   68.504 ±  2.325  ops/us
UlidBenchmark.ulidCreatorBurst           thrpt    5    8.266 ±  0.063  ops/us
UlidBenchmark.ulidCreatorDefault         thrpt    5    6.183 ±  0.139  ops/us
UlidBenchmark.ulidCreatorFromBytes       thrpt    5  288.654 ±  4.919  ops/us
UlidBenchmark.ulidCreatorMonotonic       thrpt    5   36.458 ±  0.585  ops/us
UlidBenchmark.ulidCreatorMultiThread     thrpt    5    4.878 ±  0.093  ops/us
UlidBenchmark.ulidCreatorObjectCreation  thrpt    5    8.100 ±  0.146  ops/us
UlidBenchmark.ulidCreatorParse           thrpt    5   26.836 ±  0.526  ops/us
UlidBenchmark.ulidCreatorSecureRandom    thrpt    5    6.551 ±  0.112  ops/us
UlidBenchmark.ulidCreatorToBytes         thrpt    5  233.286 ±  3.372  ops/us
UlidBenchmark.ulidCreatorToString        thrpt    5   27.667 ±  0.433  ops/us
```

## 総合分析

### **sulky が優れている領域** 🏆

#### 1. **ThreadLocalRandom使用時の生成速度**
```
sulkyThreadLocalRandom:     28.216 ops/us  ★圧倒的
ulidCreatorDefault:          6.183 ops/us
→ sulkyが約4.6倍高速
```
**理由**: sulkyのシンプルな実装 + ThreadLocalRandomの組み合わせが最強

#### 2. **Monotonic生成**
```
sulkyMonotonic:             42.869 ops/us  ★
ulidCreatorMonotonic:       36.458 ops/us
→ sulkyが約1.2倍高速
```
**理由**: 
- sulkyは単純なインクリメント実装
- ulid-creatorは状態管理のオーバーヘッド

#### 3. **バースト生成（連続1000回）**
```
sulkyBurst:                 17.449 ops/us  ★
ulidCreatorBurst:            8.266 ops/us
→ sulkyが約2.1倍高速
```
**理由**: インスタンス再利用で効率的

#### 4. **ToString変換**
```
sulkyToString:              68.504 ops/us  ★
ulidCreatorToString:        27.667 ops/us
→ sulkyが約2.5倍高速
```
**理由**: sulkyの`char[]`バッファ実装が高効率

#### 5. **オブジェクト生成**
```
sulkyObjectCreation:        43.849 ops/us  ★圧倒的
ulidCreatorObjectCreation:   8.100 ops/us
→ sulkyが約5.4倍高速
```
**理由**: 
- sulkyはシンプルな`Value`クラス（2つのlong）
- ulid-creatorはより複雑なオブジェクト構造

---

### **ulid-creator が優れている領域** 🏆

#### 1. **デフォルト生成（何も考えずに使う場合）**
```
sulkyDefault:                2.675 ops/us
ulidCreatorDefault:          6.183 ops/us  ★
→ ulid-creatorが約2.3倍高速
```
**理由**: 内部最適化とThreadLocal戦略

#### 2. **マルチスレッド環境**
```
sulkyMultiThreadShared:      1.630 ops/us  (SecureRandom共有でロック)
ulidCreatorMultiThread:      4.878 ops/us  ★
→ ulid-creatorが約3倍高速
```
**理由**: 
- sulkyはSecureRandomのロック競合
- ulid-creatorはスレッドセーフな設計

#### 3. **パース（文字列→オブジェクト）**
```
sulkyParse:                 10.908 ops/us
ulidCreatorParse:           26.836 ops/us  ★
→ ulid-creatorが約2.5倍高速
```
**理由**: 最適化されたパースアルゴリズム

#### 4. **FromBytes**
```
sulkyFromBytes:            257.875 ops/us
ulidCreatorFromBytes:      288.654 ops/us  ★
→ ulid-creatorが約1.1倍高速
```
**理由**: わずかだが最適化されている

---

## 実用的な性能比較表

| シナリオ | sulky (ops/us) | ulid-creator (ops/us) | 勝者 | 倍率 |
|---------|----------------|---------------------|------|------|
| **何も考えずに使う** | 2.675 | 6.183 | ulid-creator | 2.3x |
| **ThreadLocalRandom最適化** | 28.216 | 6.183 | **sulky** | **4.6x** |
| **Monotonic生成** | 42.869 | 36.458 | **sulky** | **1.2x** |
| **バースト1000件** | 17.449 | 8.266 | **sulky** | **2.1x** |
| **マルチスレッド8並列** | 1.630 | 4.878 | ulid-creator | 3.0x |
| **文字列化** | 68.504 | 27.667 | **sulky** | **2.5x** |
| **パース** | 10.908 | 26.836 | ulid-creator | 2.5x |

## スループット換算（実用的な数値）

```
ThreadLocalRandom使用時の秒間生成数:
sulkyThreadLocalRandom:  28,216,000 ULID/秒  ★最速
ulidCreatorDefault:       6,183,000 ULID/秒

Monotonic生成:
sulky:                   42,869,000 ULID/秒  ★
ulid-creator:            36,458,000 ULID/秒

マルチスレッド（8スレッド合計）:
sulky (共有SecureRandom): 1,630,000 ULID/秒
ulid-creator:             4,878,000 ULID/秒  ★
```

## 推奨戦略

### **パターン1: 最高性能重視（sulky + ThreadLocal）**

```java
@Component
public class HighPerformanceUlidGenerator {
    private static final ThreadLocal<ULID> ULID_GENERATOR = 
        ThreadLocal.withInitial(() -> new ULID(ThreadLocalRandom.current()));
    
    public String generate() {
        return ULID_GENERATOR.get().nextULID();
    }
    
    // 性能: 28.216 ops/us = 秒間2800万件
}
```

**適用シーン:**
- 高頻度API（秒間1万リクエスト以上）
- バッチ処理での大量生成
- Kafkaプロデューサー

### **パターン2: 使いやすさ重視（ulid-creator）**

```java
@Component
public class SimpleUlidGenerator {
    public String generate() {
        return UlidCreator.getUlid().toString();
    }
    
    // 性能: 6.183 ops/us = 秒間600万件
    // 十分高速 & コードがシンプル
}
```

**適用シーン:**
- 通常のREST API
- 開発者が最適化を意識しなくて良い
- マルチスレッド環境での安全性重視

### **パターン3: Monotonic高性能（sulky）**

```java
@Component
public class MonotonicUlidGenerator {
    private static final ThreadLocal<MonotonicState> STATE = 
        ThreadLocal.withInitial(MonotonicState::new);
    
    private static class MonotonicState {
        final ULID ulid = new ULID(ThreadLocalRandom.current());
        ULID.Value previous = ulid.nextValue();
    }
    
    public String generate() {
        MonotonicState state = STATE.get();
        ULID.Value current = state.ulid.nextMonotonicValue(state.previous);
        state.previous = current;
        return current.toString();
    }
    
    // 性能: 42.869 ops/us = 秒間4200万件
}
```

**適用シーン:**
- ログ集約（CloudWatch Logs）
- イベントストリーム
- 時系列データ

## 意外な発見

### 1. **sulkyのシンプル実装が勝つケース多数**
```java
// sulkyのコア実装（約500行）
// → シンプルさが高速化に貢献

// ulid-creatorの実装（数千行）
// → 機能豊富だがオーバーヘッドあり
```

### 2. **ThreadLocalRandomの威力**
```
SecureRandom:        3.393 ops/us
ThreadLocalRandom:  28.216 ops/us
→ 8.3倍の差！
```

### 3. **マルチスレッドでの決定的差**
```
sulky (shared SecureRandom): 1.630 ops/us  ← ロック地獄
ulid-creator:                4.878 ops/us  ← スレッドセーフ設計
```

## 最終推奨： 使い分け

### **ケース1: 低〜中頻度API（推奨: ulid-creator）**
```java
// 秒間1000リクエスト以下
String userId = UlidCreator.getUlid().toString();
```
- コードがシンプル
- メンテナンス性高い
- 十分な性能（秒間600万件）

### **ケース2: 高頻度API（推奨: sulky + ThreadLocal）**
```java
// 秒間1万リクエスト以上
String eventId = highPerformanceGenerator.generate();
```
- 最高性能が必要
- 秒間2800万件の生成能力
- わずかなコード追加で大幅な性能向上

### **ケース3: マルチスレッド重視（推奨: ulid-creator）**
```java
// ECS Fargate の複数スレッド
String taskId = UlidCreator.getUlid().toString();
```
- スレッドセーフが保証
- ロック競合なし
- 安定した性能

## 結論

**sulky の強み:**
- ThreadLocalRandom使用時は圧倒的高速
- シンプルで理解しやすい
- カスタマイズしやすい
- Monotonic生成が高速

**ulid-creator の強み:**
- デフォルトで高速・安全
- マルチスレッド環境で安定
- パースが高速
- メンテナンスされている

両方の良いところを使い分けるのがベストプラクティスです！
