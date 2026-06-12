/*
 * Copyright © 2026 biopatternsg (biopatternsg@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.biopatternsg.infrastructure.util;

public class SimpleRateLimiter {
    private final long intervalNanos;
    private long lastRequestNanos = 0;

    public SimpleRateLimiter(double permitsPerSecond) {
        this.intervalNanos = (long) (1_000_000_000.0 / permitsPerSecond);
    }

    public synchronized void acquire() {
        long now = System.nanoTime();
        long nextAllowedTime = lastRequestNanos + intervalNanos;
        if (now < nextAllowedTime) {
            long sleepNanos = nextAllowedTime - now;
            long sleepMs = sleepNanos / 1_000_000;
            int sleepNs = (int) (sleepNanos % 1_000_000);
            try {
                Thread.sleep(sleepMs, sleepNs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            lastRequestNanos = nextAllowedTime;
        } else {
            lastRequestNanos = now;
        }
    }
}
