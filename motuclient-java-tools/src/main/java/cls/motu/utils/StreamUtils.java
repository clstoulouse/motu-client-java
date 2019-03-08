package cls.motu.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamUtils {
    private StreamUtils() {
        // do nothing
    }

    /**
     * A wrapper around lambdas that return potentially {@code null} collections and that you wish to use in a
     * flatmap.
     * <p>
     * Example:
     * <p>
     * Instead of:
     *
     * <pre>
     * {@code stream.flatMap(x -> {
     *     final List<Integer> list = x.getNullableList();
     *     return (list != null) ? list.stream() : Stream.empty();
     * })}
     * </pre>
     * <p>
     * You can write:
     *
     * <pre>
     * {@code stream.flatMap(nullableCollection(MyObject::getNullableList())}
     * </pre>
     *
     * @param f   Lambda expression to wrap.
     * @param <A> Input type of the lambda.
     * @param <B> Item output type of the lambda.
     * @return A function that is safe to use as a {@link Stream#flatMap(Function)}.
     */
    public static <A, B> Function<A, Stream<B>> nullableCollection(final Function<A, ? extends Collection<B>> f) {
        return x -> {
            final Collection<B> collection = f.apply(x);
            if (null == collection) {
                return Stream.empty();
            }

            return collection.stream();
        };
    }

    /**
     * A simple wrapper around lambdas that eventually return null values.
     * <p>
     * Example:
     * <p>
     * Instead of:
     *
     * <pre>
     * {@code stream.map(x -> x.getNullableValue())} // Will raise a NullPointerException when the value is null
     * </pre>
     * <p>
     * You can write:
     *
     * <pre>
     * {@code stream.flatMap(nullableValue(x -> x.getNullableValue()))} // null values are skipped
     * </pre>
     *
     * @param f   Lambda expression to wrap.
     * @param <A> Input type of the lambda.
     * @param <B> output type of the lambda.
     * @return A function that simply ignores the nulls.
     */
    public static <A, B> Function<A, Stream<B>> nullableValue(final Function<A, B> f) {
        return x -> {
            final B value = f.apply(x);
            if (null == value) {
                return Stream.empty();
            }

            return Stream.of(value);
        };
    }

    public static <T> Stream<T> toStream(final Iterator<T> it) {
        final Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED);

        return StreamSupport.stream(spliterator, false);
    }

    public static <K, T> Predicate<T> distinctByKey(final Function<? super T, K> keyExtractor) {
        final Set<K> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
