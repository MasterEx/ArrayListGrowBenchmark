# ArrayList Grow Benchmark

This benchmark is using [JMH](https://openjdk.org/projects/code-tools/jmh/) to evaluate the cost of growing an [ArrayList](https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/util/ArrayList.html).

While reading the Collections Framework and more specifically the ArrayList's `grow()` method code I found out that an optimization took place.

In case the `ArrayList` was initialized with the default length then the grow operation was creating a new internal array with the desired size. Otherwise, the `grow` method was copying all the objects of the internal array to a new one with the desired size using the `Arrays.copyOf()` method.

The second operation is significant slower, so this is why such an optimization is meaningful.

To my eyes, the way this method is written,  it makes sense to apply the same optimization in case the `ArrayList` was initialized with zero size or if the `ArrayList` is empty in general.

This benchmark aims to demonstrate the difference in this behavior.

## Benchmark

To perform the test I am using the JDK branch `jdk-18-ga`.

Fist I execued the benchmark wihout any change and I got the following results.

```shell
Benchmark                                   Mode  Cnt           Score            Error  Units
GrowBenchmark.initializeDefaultLengh       thrpt    5  1575687313.279 ±   85463452.441  ops/s
GrowBenchmark.initializeEmptyArrayAndGrow  thrpt    5  1323723080.658 ± 2144045374.479  ops/s
GrowBenchmark.initializeTargetLength       thrpt    5  1577617274.751 ±   70374698.471  ops/s
GrowBenchmark.initializeZeroLength         thrpt    5     2331180.014 ±      29769.013  ops/s
```

As expected, the `grow` operation when the `ArrayList` is initialized as `new ArrayList(0)` is much slower.

Aferwards, I have applied the following change to the `JDK` codebase:

```java
iff --git a/src/java.base/share/classes/java/util/ArrayList.java b/src/java.base/share/classes/java/util/ArrayList.java
index a36dcd8a796..dcc432c5ca4 100644
--- a/src/java.base/share/classes/java/util/ArrayList.java
+++ b/src/java.base/share/classes/java/util/ArrayList.java
@@ -230,13 +230,13 @@ public class ArrayList<E> extends AbstractList<E>
      */
     private Object[] grow(int minCapacity) {
         int oldCapacity = elementData.length;
-        if (oldCapacity > 0 || elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
+        if (size > 0) {
             int newCapacity = ArraysSupport.newLength(oldCapacity,
                     minCapacity - oldCapacity, /* minimum growth */
                     oldCapacity >> 1           /* preferred growth */);
             return elementData = Arrays.copyOf(elementData, newCapacity);
         } else {
-            return elementData = new Object[Math.max(DEFAULT_CAPACITY, minCapacity)];
+            return elementData = new Object[Math.max(oldCapacity, minCapacity)];
         }
     }
```

Then I executed the benchmark again using the update java version and I got the following results:

```shell
Benchmark                                   Mode  Cnt           Score            Error  Units
GrowBenchmark.initializeDefaultLengh       thrpt    5  1588138209.869 ±    1468520.084  ops/s
GrowBenchmark.initializeEmptyArrayAndGrow  thrpt    5  1336142344.879 ± 2165939835.029  ops/s
GrowBenchmark.initializeTargetLength       thrpt    5  1587721127.345 ±    2281982.468  ops/s
GrowBenchmark.initializeZeroLength         thrpt    5  1588047519.416 ±    3668536.066  ops/s
```

This time the zero length case performed similarly to the rest.

DISCLAIMER: The above change in the `ArrayList` code is performed only to illustrate the performance difference in this case. It is not tested for other side-effects and it is not evaluated if it is the best possible optimization.

## Benchmark Execution

 1. mvn clean install
 
 Depending the java version the pom.xml may need to be amended (maven.compiler.source, maven.compiler.target).
 
 2. java -jar target/benchmarks.jar
 
 In case of custom jdk build the produced java binary should be used.

## Conclusion

I cannot think many cases where initializing an empty `ArrayList` is required.

However, reading the `grow()` code I would expect the optimization performed for the default size `ArrayList` to also happen for the zero size `ArrayList`. I particularly believe this because as a reader the condition `oldCapacity > 0 || elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA` seems equivalen to `elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA` to me and I find this weird. 

In any case, as discussed in the [core-libs-dev](https://mail.openjdk.org/pipermail/core-libs-dev/2022-July/092401.html) mailing list this is something of minor importance.

I would suggest to always initialize an `ArrayList` with the default size (`new ArrayList()`). Internally, this is optimized to allocate similar amount of memory to the zero size `ArrayList` because both point to a single instance of a static array. However, it does not suffer from the performance penalty demonstrated above.

Another, more clear approach is to use an empty `ArrayList` by calling the `Collections.emptyList()` method and iniatilize it lazily if and when it is required.

What I mean is roughly something like that (following example is not thread safe):

```java
public class LazyList {

    private List<String> lazyList = Collections.emptyList();

    public void add(String s) {
        if (lazyList == Collections.emptyList()) {
            lazyList = new ArrayList<>();
        }
        lazyList.add(s);
    }

    public void grow(int size) {
        if (lazyList == Collections.emptyList()) {
            lazyList = new ArrayList<>(size);
        } else {
            lazyList.grow(size);
        }
    }
}
```

So, in the rare case you need to initialize a zero size `ArrayList` think twice if it is the best approach.
