/*
    Copyright (C) 2011-2020 Andy Cheung

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package ac.adproj.mchat.service;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class CommonThreadPoolTest {

    @BeforeAll
    @SneakyThrows
    static void init() {
        // Invoke class initializer.
        Class.forName("ac.adproj.mchat.service.CommonThreadPool");
    }

    @Test
    @SneakyThrows
    void testExecuteNoComment() {
        AtomicBoolean invoked = new AtomicBoolean(false);

        var latch = new CountDownLatch(1);
        CommonThreadPool.execute(() -> {
            try {
                invoked.set(true);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        Assertions.assertTrue(invoked.get());
    }

    @Test
    @SneakyThrows
    void testExecute() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        AtomicReference<Thread> threadRunning = new AtomicReference<>();

        final String STATEMENT_TO_TEST = UUID.randomUUID().toString();

        var latch = new CountDownLatch(1);
        CommonThreadPool.execute(() -> {
            try {
                threadRunning.set(Thread.currentThread());
                invoked.set(true);
            } finally {
                latch.countDown();
            }
        }, STATEMENT_TO_TEST);

        latch.await();

        Assertions.assertTrue(invoked.get());
        Assertions.assertTrue(threadRunning.get().getName().contains(STATEMENT_TO_TEST));
    }
}