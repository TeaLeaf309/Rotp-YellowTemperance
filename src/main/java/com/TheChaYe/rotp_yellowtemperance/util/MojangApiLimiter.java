package com.TheChaYe.rotp_yellowtemperance.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mojang 公开 API 名字查询的统一限速 / 冷却。
 *
 * <p>依据（2026-10-30 资料 + 实测）：</p>
 * <ul>
 *   <li>Mojang API 大体是 <b>每 IP 200 请求 / 2 分钟</b>（IPv6 按 /56 子网分桶），
 *       超出返回 HTTP 429；</li>
 *   <li>名字查询响应头会带 <code>x-minecraft-rate-limit-result: UNDER_LIMIT</code>，
 *       429 时可能带 <code>Retry-After</code>；</li>
 *   <li>api.mojang.com 偶发 CloudFront 403（Mojang 自身配置问题，见 mojira WEB-1444），
 *       按临时错误处理。</li>
 * </ul>
 *
 * <p>所有名字查询（服务端 GameProfileFetcher、客户端 DisguiseProfileLookup）都走这里，
 * 保证整个 JVM 对外只有一条很保守的请求速率。</p>
 */
public final class MojangApiLimiter {
    /** 两次名字查询的最小间隔（≈50 req/min，远低于 ~100 req/min 的 IP 上限）。 */
    private static final long MIN_INTERVAL_MS = 1200L;
    /** 429 未给 Retry-After 时的默认冷却。 */
    private static final long DEFAULT_COOLDOWN_MS = 60_000L;
    /** CloudFront 403 的短暂冷却。 */
    private static final long TEMP_BLOCK_COOLDOWN_MS = 30_000L;

    private static long lastRequestAt = 0L;
    private static long cooldownUntil = 0L;

    private MojangApiLimiter() {}

    /**
     * 申请一次请求许可。调用方应在后台线程调用；最多阻塞 {@code maxWaitMs}。
     */
    public static synchronized boolean acquire(long maxWaitMs) {
        long now = System.currentTimeMillis();
        long earliest = Math.max(cooldownUntil, lastRequestAt + MIN_INTERVAL_MS);
        long wait = earliest - now;
        if (wait > maxWaitMs) {
            return false;
        }
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        lastRequestAt = System.currentTimeMillis();
        return true;
    }

    /**
     * 根据响应更新冷却状态（429 = 退避；403 = 短暂退避；2xx = 解除）。
     */
    public static synchronized void reportResult(int statusCode, String retryAfterHeader) {
        long now = System.currentTimeMillis();
        if (statusCode == 429) {
            long retryAfter = parseRetryAfterMs(retryAfterHeader);
            cooldownUntil = now + Math.max(retryAfter, DEFAULT_COOLDOWN_MS);
        } else if (statusCode == 403) {
            cooldownUntil = Math.max(cooldownUntil, now + TEMP_BLOCK_COOLDOWN_MS);
        } else if (statusCode >= 200 && statusCode < 300) {
            cooldownUntil = 0L;
        }
    }

    /** 当前是否处于冷却（用于日志/诊断）。 */
    public static synchronized boolean isCoolingDown() {
        return cooldownUntil > System.currentTimeMillis();
    }

    private static long parseRetryAfterMs(String header) {
        if (header == null || header.isEmpty()) {
            return 0L;
        }
        String value = header.trim();
        try {
            return Long.parseLong(value) * 1000L;
        } catch (NumberFormatException ignored) {
        }
        try {
            return Math.max(0L, ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant().toEpochMilli() - System.currentTimeMillis());
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
