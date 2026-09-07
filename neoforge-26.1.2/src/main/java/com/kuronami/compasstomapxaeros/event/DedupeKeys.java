package com.kuronami.compasstomapxaeros.event;

/**
 * 発見の重複判定に使う key を組み立てる。**種別で座標の扱いが違う。**
 *
 * <p>Minecraft のクラスに一切触れない純 Java クラスにしてある（JUnit から直接叩くため）。
 * 規則を変えたら {@code DedupeKeysTest} が落ちる。
 *
 * <p><b>BIOME は座標を含めない。</b> Nature's Compass はプレイヤーの現在地を起点に
 * {@code sampleSpace}（{@code getBiomeSize()} 4 × {@code sampleSpaceModifier} 16 ＝ 既定 64
 * ブロック）刻みの格子でサンプリングし、最初に当たった点を返す（{@code BiomeSearchWorker}）。
 * 起点が動けば格子ごとずれるので、<b>同じバイオームでも検索のたびに違う座標が返るのが正常</b>。
 * 実測では同じ Bamboo Jungle が (419,-315) → (415,-279) → (410,-277) と返った。
 * 座標を key に入れると、再検索のたびに数十ブロック隣へピンが増える。
 *
 * <p>距離のしきい値では解けない。探索半径は既定 10000 ブロックまで届くので、同じバイオームの
 * 塊の中で起点が数千ブロック動きうる。半径を小さく取れば1つの塊を分割し、大きく取れば
 * 別の塊を同一視する。「同じ塊か」を決める情報がそもそもデータに無い。
 *
 * <p><b>STRUCTURE は座標を含める。</b> Explorer's Compass が返すのは
 * {@code placement.getLocatePos(structureStart.getChunkPos())}＝その構造物の実位置
 * （{@code StructureSearchWorker#succeed}）なので、同じ構造物なら常に同じ座標になる。
 * 座標を含めておけば、別の村を見つけた時にちゃんと別のピンが立つ。
 *
 * <p><b>次元はどちらにも含めない。</b> コンパスは「どの次元で検索したか」を持っておらず
 * （NC / EC のどの component にも次元が無い）、次元を移動しても FOUND のまま残る。
 * key に次元を入れると、FOUND のコンパスを持ってネザーへ入った瞬間に
 * <b>オーバーワールドの座標がネザーの地図へ登録される</b>。登録先の次元には観測時に
 * プレイヤーが居た次元を使うが、それは判定には使わない。
 *
 * <p><b>Y はどちらにも含めない。</b> Y は {@code estimateY} が Heightmap から推定する値で、
 * チャンクのロード状況で変わる。同じ構造物でもロード状況が違えば別の Y になり、
 * Y を含めた判定はすり抜ける。
 *
 * <p>出所と経緯: {@code kuronami-mods/knowledge/C2M_DECISIONS.md} §1・§2。
 */
public final class DedupeKeys {

    private DedupeKeys() {}

    /** 構造物（Explorer's Compass）の key。座標を含める。 */
    public static String structure(String structureId, int x, int z) {
        return "s|" + structureId + "|" + x + "|" + z;
    }

    /** バイオーム（Nature's Compass）の key。座標を含めない。 */
    public static String biome(String biomeId) {
        return "b|" + biomeId;
    }
}
