package com.kuronami.compasstomapxaeros.event;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 1 player ぶんの観測状態。**Minecraft のクラスに一切触れない**（JUnit から直接テストするため）。
 *
 * <p>サーバ tick は単一スレッドだが、ログイン / ログアウトの event は別経路から来るので
 * synchronized で守る。
 */
final class PlayerState {

    /** 登録済み key の保持上限（player ごと）。 */
    static final int MAX_SEEN = 512;

    /**
     * ログイン直後に「登録しない」窓の長さ (tick)。1.5 秒。
     *
     * <p>コンパスは検索結果を持ち越すので、ログイン時点で FOUND なのは<b>前のセッションの結果</b>で
     * あって新しい発見ではない。そのまま登録すると、別の次元でログインした時にそこへ他次元の
     * 座標が立つ。この窓の間に実際の検索が完了することはない (GUI を開いて対象を選ぶ操作が要る)。
     */
    static final int PRIMING_TICKS = 30;

    /** 既に登録した key。LRU で {@link #MAX_SEEN} 件まで保持。 */
    private final Set<String> seen = new LinkedHashSet<>();

    /**
     * ログイン時点で既に FOUND だった発見の key → その時の座標。
     * **登録しないが {@link #seen} には入れない。**
     *
     * <p>seen に入れてしまうと、そのセッション中ずっとその対象を登録できなくなる。
     * 実害: 利用者がピンを削除して入り直し、同じ対象を再検索しても<b>何も起きない</b>。
     *
     * <p><b>解除は「座標が動いたこと」で判定する。「FOUND が手元から消えたこと」では判定しない。</b>
     * 理由は2つあり、どちらも実装を間違えるとこの窓が牙を剥く:
     *
     * <ul>
     *   <li><b>SEARCHING を観測できるとは限らない。</b> Nature's Compass の探索は
     *       {@code WorldWorkerManager} に載り、サーバ tick の先頭 ({@code ServerTickEvent.Pre}) で
     *       進む。検索要求の処理は tick の末尾なので、近くのバイオームなら
     *       <b>次の player tick が回る前に FOUND へ戻る</b>。「再検索は必ず SEARCHING を通るから
     *       そこで解除できる」という前提は成り立たず、それに頼ると<b>再検索しても永久に
     *       登録されない</b>（ログイン時に出ていたバイオームを、近くで引き直した時）。</li>
     *   <li><b>手元に無いことは「検索し直した」ことを意味しない。</b> チェストに預ける・死亡で落とす・
     *       額縁に飾るだけで FOUND は観測されなくなる。それで解除すると、取り出しただけの
     *       持ち越し結果が「新しい発見」として登録される（別の次元で取り出すと、そこへ
     *       他次元の座標が立つ＝この窓が防ぐはずだったもの）。</li>
     * </ul>
     *
     * <p>座標で見ればどちらも起きない。Nature's Compass の格子はプレイヤーの現在地に固定されるので、
     * <b>少しでも動いてから検索し直せば座標は変わる</b>。逆に座標が1ブロックも変わらないなら、
     * 立つはずのピンは既にあるものと同じ位置なので、抑制したままで実害が無い。
     */
    private final Map<String, Long> staleAtLogin = new HashMap<>();

    private int primingTicksLeft = PRIMING_TICKS;

    /** @return この走査が priming 窓の中か。窓は走査1回につき1 tick ぶん消費する。 */
    synchronized boolean consumePrimingTick() {
        if (primingTicksLeft <= 0) return false;
        primingTicksLeft--;
        return true;
    }

    /**
     * この発見を今すぐ登録してよいか。登録すると決めた時だけ {@link #seen} に記録する。
     *
     * @param key     {@link DedupeKeys} が作った判定 key
     * @param x       コンパスが返した X（持ち越し判定に使う。key には入っていないことがある）
     * @param z       コンパスが返した Z
     * @param priming この走査が priming 窓の中か
     */
    synchronized boolean shouldRegister(String key, int x, int z, boolean priming) {
        long pos = packPos(x, z);
        if (priming) {
            staleAtLogin.put(key, pos);
            return false;
        }
        Long stalePos = staleAtLogin.get(key);
        if (stalePos != null) {
            // 同じ座標＝前セッションの結果をそのまま持ち越しているだけ。
            if (stalePos.longValue() == pos) return false;
            // 座標が動いた＝検索し直した。以後は普通の発見として扱う。
            staleAtLogin.remove(key);
        }
        if (!seen.add(key)) return false;
        if (seen.size() > MAX_SEEN) {
            Iterator<String> it = seen.iterator();
            it.next();
            it.remove();
        }
        return true;
    }

    private static long packPos(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }
}
