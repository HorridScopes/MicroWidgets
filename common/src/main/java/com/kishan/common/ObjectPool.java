package com.kishan.common;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Supplier;

public class ObjectPool<T extends Poolable> {

    private final Queue<T> pool;
    private final Supplier<T> factory;

    public ObjectPool(Supplier<T> factory, int initialSize) {
        this.factory = factory;
        this.pool = new ArrayDeque<>(initialSize);

        for (int i = 0; i < initialSize; i++) {
            pool.offer(factory.get());
        }
    }

    /** Pulls from the front. Expands pool if empty. */
    public T acquire() {
        T obj = pool.isEmpty() ? factory.get() : pool.poll();
        obj.reset();
        obj.setActive(true);
        return obj;
    }

    /** Recycles to the back of the queue. */
    public void release(T obj) {
        obj.setActive(false);
        pool.offer(obj);
    }

    public int size() {
        return pool.size();
    }
}

