package com.kuronami.compasstomapxaeros.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


/**
 * ログイン直後の持ち越し判定と重複抑制の規則を固定する。
 * ここが落ちたら、それは規則を変えたということ。
 */
class PlayerStateTest {

    private static final String BAMBOO = DedupeKeys.biome("minecraft:bamboo_jungle");

    /** priming 窓を使い切る（実運用の onPlayerTick と同じ呼び方）。 */
    private static void runPriming(PlayerState s, Runnable duringEachTick) {
        for (int i = 0; i < PlayerState.PRIMING_TICKS; i++) {
            assertTrue(s.consumePrimingTick(), "窓の中のはず: tick " + i);
            duringEachTick.run();
        }
        assertFalse(s.consumePrimingTick(), "窓は PRIMING_TICKS で閉じる");
    }

    @Test
    @DisplayName("ログイン時に出ていた結果は登録しない")
    void carriedOverResultIsNotRegistered() {
        PlayerState s = new PlayerState();
        runPriming(s, () -> assertFalse(s.shouldRegister(BAMBOO, 419, -315, true)));
        // 窓が閉じた後も、同じ座標を返し続けている間は登録しない
        assertFalse(s.shouldRegister(BAMBOO, 419, -315, false));
    }

    @Test
    @DisplayName("ログイン後に検索し直せば登録される（座標が動く＝新しい検索）")
    void researchAfterLoginRegisters() {
        PlayerState s = new PlayerState();
        runPriming(s, () -> s.shouldRegister(BAMBOO, 419, -315, true));
        // 実測: 同じ Bamboo Jungle が検索のたびに違う座標を返す
        assertTrue(s.shouldRegister(BAMBOO, 415, -279, false));
    }

    @Test
    @DisplayName("コンパスを手放しただけでは持ち越し扱いが解除されない")
    void stashingTheCompassDoesNotReleaseTheCarryOver() {
        PlayerState s = new PlayerState();
        runPriming(s, () -> s.shouldRegister(BAMBOO, 419, -315, true));
        // チェストに預けている間は何も観測されない（tick は回るが shouldRegister は呼ばれない）
        for (int i = 0; i < 200; i++) {
            assertFalse(s.consumePrimingTick());
        }
        // 取り出しただけ＝座標は変わっていない → 登録しない
        assertFalse(s.shouldRegister(BAMBOO, 419, -315, false),
                "取り出しただけの持ち越し結果を新しい発見として登録してはいけない");
    }

    @Test
    @DisplayName("同じバイオームを再検索してもピンは増えない（これが直した不具合）")
    void reSearchingTheSameBiomeDoesNotRegisterTwice() {
        PlayerState s = new PlayerState();
        runPriming(s, () -> { });
        assertTrue(s.shouldRegister(BAMBOO, 419, -315, false), "初回は登録される");
        // Nature's Compass は同じバイオームでも毎回違う座標を返す
        assertFalse(s.shouldRegister(BAMBOO, 415, -279, false));
        assertFalse(s.shouldRegister(BAMBOO, 410, -277, false));
    }

    @Test
    @DisplayName("別の構造物は別のピンとして登録される")
    void differentStructuresBothRegister() {
        PlayerState s = new PlayerState();
        runPriming(s, () -> { });
        assertTrue(s.shouldRegister(DedupeKeys.structure("minecraft:village_plains", 100, 200), 100, 200, false));
        assertTrue(s.shouldRegister(DedupeKeys.structure("minecraft:village_plains", 800, 900), 800, 900, false));
    }

    @Test
    @DisplayName("priming 中の発見は seen に入らない（窓明けに登録できる余地を残す）")
    void primingDoesNotConsumeTheSeenSlot() {
        PlayerState s = new PlayerState();
        runPriming(s, () -> s.shouldRegister(BAMBOO, 419, -315, true));
        // 座標が動けば登録できる = seen に焼き付いていない
        assertTrue(s.shouldRegister(BAMBOO, 415, -279, false));
    }

    @Test
    @DisplayName("保持上限を超えると最古から捨てる")
    void seenIsBoundedAndEvictsOldest() {
        PlayerState s = new PlayerState();
        runPriming(s, () -> { });
        String first = DedupeKeys.biome("mod:biome0");
        assertTrue(s.shouldRegister(first, 0, 0, false));
        for (int i = 1; i <= PlayerState.MAX_SEEN; i++) {
            assertTrue(s.shouldRegister(DedupeKeys.biome("mod:biome" + i), i, i, false));
        }
        // 最古が押し出されたので、もう一度登録できる
        assertTrue(s.shouldRegister(first, 0, 0, false));
    }
}
