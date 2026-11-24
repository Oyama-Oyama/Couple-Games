package com.velvet.couplesgame.testing;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * 让 LiveData 在纯 JVM 单测中可写。
 *
 * <p>默认 {@code DefaultTaskExecutor.isMainThread()} 会调用 android.os.Looper 的桩方法而抛
 * "not mocked"。本 Rule 临时把 ArchTaskExecutor 的 delegate 换成同步执行、且自认主线程的假实现，
 * 测试结束即恢复，避免污染其它测试类。
 */
public final class JvmTaskExecutorRule implements TestRule {

    private static final TaskExecutor JVM_EXECUTOR = new TaskExecutor() {
        @Override
        public void executeOnDiskIO(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void postToMainThread(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void executeOnMainThread(Runnable runnable) {
            runnable.run();
        }

        @Override
        public boolean isMainThread() {
            return true;
        }
    };

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ArchTaskExecutor.getInstance().setDelegate(JVM_EXECUTOR);
                try {
                    base.evaluate();
                } finally {
                    ArchTaskExecutor.getInstance().setDelegate(null);
                }
            }
        };
    }
}
