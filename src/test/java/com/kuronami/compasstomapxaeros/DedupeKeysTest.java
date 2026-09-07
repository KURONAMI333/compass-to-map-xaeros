package com.kuronami.compasstomapxaeros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kuronami.compasstomapxaeros.event.DedupeKeys;

/**
 * 重複判定の規則を固定する。理由は {@link DedupeKeys} の javadoc。
 * ここが落ちたら、それは規則を変えたということ。
 */
class DedupeKeysTest {

    @Test
    @DisplayName("同じバイオームは座標が違っても同じ key（再検索でピンが増えない）")
    void biomeIgnoresCoordinates() {
        // 実測値: 同じ Bamboo Jungle が検索のたびに違う座標を返した。
        String a = DedupeKeys.biome("minecraft:bamboo_jungle");
        String b = DedupeKeys.biome("minecraft:bamboo_jungle");
        assertEquals(a, b);
        assertEquals("b|minecraft:bamboo_jungle", a);
    }

    @Test
    @DisplayName("違うバイオームは違う key")
    void differentBiomesDiffer() {
        assertNotEquals(DedupeKeys.biome("minecraft:desert"),
                DedupeKeys.biome("minecraft:badlands"));
    }

    @Test
    @DisplayName("同じ構造物の同じ位置は同じ key")
    void structureSamePositionSameKey() {
        assertEquals(DedupeKeys.structure("minecraft:village_plains", 100, -200),
                DedupeKeys.structure("minecraft:village_plains", 100, -200));
    }

    @Test
    @DisplayName("同じ構造物でも別の位置なら別の key（別の村に別のピンが立つ）")
    void structureDifferentPositionDiffersKey() {
        assertNotEquals(DedupeKeys.structure("minecraft:village_plains", 100, -200),
                DedupeKeys.structure("minecraft:village_plains", 100, -201));
    }

    @Test
    @DisplayName("構造物とバイオームは接頭辞で衝突しない")
    void kindsDoNotCollide() {
        assertNotEquals(DedupeKeys.biome("minecraft:desert"),
                DedupeKeys.structure("minecraft:desert", 0, 0));
    }
}
