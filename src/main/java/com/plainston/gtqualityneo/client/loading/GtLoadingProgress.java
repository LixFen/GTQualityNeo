package com.plainston.gtqualityneo.client.loading;

import java.util.ArrayDeque;

/** GT6's loading thread publishes immutable snapshots; the render thread only reads them. */
public final class GtLoadingProgress {
    public record Snapshot(String stage, String detail, int current, int total) {}
    private record Published(Thread thread, Snapshot snapshot) {}
    private static final ThreadLocal<ArrayDeque<Stage>> STAGES = ThreadLocal.withInitial(ArrayDeque::new);
    private static volatile Published published;
    private static volatile Runnable refresh = () -> {};

    private GtLoadingProgress() {}
    public static void onChange(Runnable listener) { refresh = listener; }
    public static Snapshot snapshot() { var value = published; return value == null ? null : value.snapshot(); }
    public static boolean active() { return !STAGES.get().isEmpty(); }
    public static Stage begin(String stage, int total) {
        return begin(stage, total, false);
    }
    private static Stage begin(String stage, int total, boolean bar) {
        var value = new Stage(stage, total, bar);
        STAGES.get().push(value);
        publish();
        return value;
    }
    public static void beginBar(String stage, int total) { if (active()) begin(stage, total, true); }
    public static void stepBar(String detail) {
        var current = STAGES.get().peek();
        if (current != null && current.bar) step(detail);
    }
    public static void finishBar() {
        var current = STAGES.get().peek();
        if (current != null && current.bar) current.close();
    }
    public static void describe(String stage) {
        var stages = STAGES.get();
        // Some legacy scans skip finish() when Forge's old progress manager is unavailable.
        while (!stages.isEmpty() && stages.peek().bar) stages.pop();
        var current = stages.peek();
        if (current != null) {
            current.name = stage; current.detail = ""; current.current = current.total = 0;
            publish();
        }
    }
    public static void step(String detail) {
        var current = STAGES.get().peek();
        if (current != null) { current.current++; current.detail = detail; publish(); }
    }
    public static void queuedTask(int remaining) {
        var current = STAGES.get().peek();
        if (current != null) {
            current.current++;
            current.total = current.current + remaining;
            publish();
        }
    }
    private static void publish() {
        var stage = STAGES.get().peek();
        var counter = STAGES.get().stream().filter(value -> value.total > 0).findFirst().orElse(stage);
        published = stage == null ? null : new Published(Thread.currentThread(),
            new Snapshot(stage.name, stage.detail, counter.current, counter.total));
        refresh.run();
    }

    public static final class Stage implements AutoCloseable {
        private String name, detail = "";
        private int current, total;
        private final boolean bar;
        private Stage(String name, int total, boolean bar) { this.name = name; this.total = total; this.bar = bar; }
        @Override public void close() {
            var stages = STAGES.get();
            if (!stages.contains(this)) return;
            // An exception may skip a legacy LoadingBar.finish(); never leave the overlay behind.
            while (stages.pop() != this) {}
            var value = published;
            if (value != null && value.thread() == Thread.currentThread()) publish();
            if (stages.isEmpty()) STAGES.remove();
        }
    }
}
