package kr.ac.mjc.majang;

import java.util.*;

public class YakuChecker {

    public static class YakuResult {
        public String name;
        public int han;
        public YakuResult(String name, int han) {
            this.name = name;
            this.han = han;
        }
    }

    /**
     * 판수 큰 쪽만 남기고, 치또이 25부 보장, 오야 고려
     */
    public static List<YakuResult> getYakuList(HandState hand) {
        List<YakuResult> yaku = new ArrayList<>();

        // [1] 치또이/량페코(배타, 큰 쪽 우선)
        boolean chitoitsu = isChitoitsu(hand);
        boolean ryanpeko = isRyanpeko(hand);
        if (ryanpeko) {
            yaku.add(new YakuResult("량페코", 3));
        } else if (chitoitsu) {
            yaku.add(new YakuResult("치또이", 2));
        }

        // [2] 혼노두/준찬타/찬타(배타, 큰 쪽 우선)
        int honroutouHan = isHonroutou(hand) ? 2 : 0;
        int junchanHan = isJunchanMeldSearch(hand.tiles) ? (hand.isMenzen ? 2 : 1) : 0;
        int chantaHan = isChantaMeldSearch(hand.tiles) ? (hand.isMenzen ? 2 : 1) : 0;

        if (honroutouHan >= junchanHan && honroutouHan >= chantaHan && honroutouHan > 0) {
            yaku.add(new YakuResult("혼노두", honroutouHan));
        } else if (junchanHan >= chantaHan && junchanHan > 0) {
            yaku.add(new YakuResult("준찬타", junchanHan));
        } else if (chantaHan > 0) {
            yaku.add(new YakuResult("찬타", chantaHan));
        }



        // [3] 멘젠 한정
        if (hand.isMenzen) {
            if (isRiichi(hand)) yaku.add(new YakuResult("리치", 1));
            if (isPinfu(hand)) yaku.add(new YakuResult("핑후", 1));
            if (isTsumo(hand)) yaku.add(new YakuResult("쯔모", 1));
            if (isIppatsu(hand)) yaku.add(new YakuResult("일발", 1));
        }

        // [4] 기타 중복 허용 역
        if (isSanshokuDoujun(hand)) yaku.add(new YakuResult("삼색동순", hand.isMenzen ? 2 : 1));
        if (isIkkitsuukan(hand)) yaku.add(new YakuResult("일기통관", hand.isMenzen ? 2 : 1));
        if (isChinitsu(hand)) yaku.add(new YakuResult("청일색", hand.isMenzen ? 6 : 5));
        if (isHonitsu(hand)) yaku.add(new YakuResult("혼일색", hand.isMenzen ? 3 : 2));
        if (isYakuhai(hand)) yaku.add(new YakuResult("역패", 1));
        if (isSanshokuDokko(hand)) yaku.add(new YakuResult("삼색동각", 2));
        if (isShousangen(hand)) yaku.add(new YakuResult("소삼원", 2));
        if (isTanyao(hand)) yaku.add(new YakuResult("탕야오", 1));
        if (isToitoi(hand)) {yaku.add(new YakuResult("또이또이",1));
        }


        return yaku;
    }

    // ==================== [이하 각 역별 판정 함수 전체 구현] ====================

    // 리치(멘젠 한정)
    public static boolean isRiichi(HandState hand) {
        return hand.isMenzen && hand.yakuList != null && hand.yakuList.contains("리치");
    }

    // 쯔모(멘젠 한정)
    public static boolean isTsumo(HandState hand) {
        return hand.isMenzen && hand.isTsumo;
    }

    // 일발(멘젠 한정)
    public static boolean isIppatsu(HandState hand) {
        return hand.yakuList != null && hand.yakuList.contains("일발");
    }

    // 핑후(멘젠 한정, 모든 멘츠가 슌츠, 헤드는 역패/자패 아님, 양면 대기)
    public static boolean isPinfu(HandState hand) {
        if (!hand.isMenzen) return false;
        List<String> tiles = hand.tiles;
        if (tiles == null || tiles.size() != 14) return false;

        Map<String, Integer> counts = new HashMap<>();
        for (String t : tiles) counts.put(t, counts.getOrDefault(t, 0) + 1);

        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() >= 2) pairs.add(e.getKey());
        }

        boolean validPinfu = false;
        for (String pair : pairs) {
            Map<String, Integer> tempCounts = new HashMap<>(counts);
            tempCounts.put(pair, tempCounts.get(pair) - 2);

            int meldCount = 0;
            while (meldCount < 4) {
                boolean found = false;
                for (char suit : new char[]{'m', 'p', 's'}) {
                    for (int n = 1; n <= 7; n++) {
                        String a = n + String.valueOf(suit);
                        String b = (n+1) + String.valueOf(suit);
                        String c = (n+2) + String.valueOf(suit);
                        if (tempCounts.getOrDefault(a,0) >= 1 &&
                                tempCounts.getOrDefault(b,0) >= 1 &&
                                tempCounts.getOrDefault(c,0) >= 1) {
                            tempCounts.put(a, tempCounts.get(a)-1);
                            tempCounts.put(b, tempCounts.get(b)-1);
                            tempCounts.put(c, tempCounts.get(c)-1);
                            found = true;
                            meldCount++;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (!found) break;
            }
            if (meldCount == 4) {
                if (pair.startsWith("z")) continue;
                validPinfu = true;
                break;
            }
        }
        return validPinfu;
    }

    // 치또이(멘젠 한정, 7쌍. 자패 포함 가능)
    public static boolean isChitoitsu(HandState hand) {
        if (!hand.isMenzen) return false;
        Map<String, Integer> map = new HashMap<>();
        for (String t : hand.tiles) map.put(t, map.getOrDefault(t, 0) + 1);
        if (map.size() != 7) return false; // 7쌍이어야 함
        for (int v : map.values()) {
            if (v != 2) return false;
        }
        return true;
    }


    // 량페코(멘젠 한정, 자패 포함 시 무효!)
    public static boolean isRyanpeko(HandState hand) {
        if (!hand.isMenzen) return false;

        // 자패 있으면 무효
        for (String t : hand.tiles) if (isHonor(t)) return false;

        // 카운트맵 생성
        Map<String, Integer> counts = new HashMap<>();
        for (String t : hand.tiles) counts.put(t, counts.getOrDefault(t, 0) + 1);

        // 모든 2장 이상 패를 헤드(짝패)로 잡고 나머지 멘츠 분해 시도
        for (String pair : counts.keySet()) {
            if (counts.get(pair) >= 2) {
                Map<String, Integer> pool = new HashMap<>(counts);
                pool.put(pair, pool.get(pair) - 2);

                List<String> melds = new ArrayList<>();
                if (ryanpekoMeldSearch(pool, 4, melds)) {
                    // 만들어진 멘츠 리스트에서 동일 슌츠가 2쌍씩 2종류 이상 있으면 true
                    Map<String, Integer> meldCount = new HashMap<>();
                    for (String meld : melds) meldCount.put(meld, meldCount.getOrDefault(meld, 0) + 1);
                    int pairMelds = 0;
                    for (int v : meldCount.values()) if (v >= 2) pairMelds++;
                    if (pairMelds >= 2) return true;
                }
            }
        }
        return false;
    }

    // 슌츠만 허용하는 멘츠 분해, 만들어진 슌츠를 melds에 추가
    private static boolean ryanpekoMeldSearch(Map<String, Integer> pool, int left, List<String> melds) {
        if (left == 0) {
            int sum = 0;
            for (int v : pool.values()) sum += v;
            return sum == 0;
        }
        for (String suit : Arrays.asList("m", "p", "s")) {
            for (int i = 1; i <= 7; i++) {
                String a = i + suit, b = (i+1) + suit, c = (i+2) + suit;
                if (pool.getOrDefault(a, 0) > 0 && pool.getOrDefault(b, 0) > 0 && pool.getOrDefault(c, 0) > 0) {
                    Map<String, Integer> tmp = new HashMap<>(pool);
                    tmp.put(a, tmp.get(a) - 1);
                    tmp.put(b, tmp.get(b) - 1);
                    tmp.put(c, tmp.get(c) - 1);
                    List<String> nextMelds = new ArrayList<>(melds);
                    nextMelds.add(a + b + c); // 멘츠(예: "1m2m3m") 형태로 기록
                    if (ryanpekoMeldSearch(tmp, left - 1, nextMelds)) {
                        melds.clear();
                        melds.addAll(nextMelds);
                        return true;
                    }
                }
            }
        }
        return false;
    }


    // 삼색동순(멘젠 2판, 후로 1판)
    public static boolean isSanshokuDoujun(HandState hand) {
        List<String> man = new ArrayList<>(), pin = new ArrayList<>(), sou = new ArrayList<>();
        for (String t : hand.tiles) {
            if (t.endsWith("m")) man.add(t);
            if (t.endsWith("p")) pin.add(t);
            if (t.endsWith("s")) sou.add(t);
        }
        for (int i = 1; i <= 7; i++) {
            String a = i + "m", b = (i+1) + "m", c = (i+2) + "m";
            String d = i + "p", e = (i+1) + "p", f = (i+2) + "p";
            String g = i + "s", h = (i+1) + "s", k = (i+2) + "s";
            if (man.contains(a) && man.contains(b) && man.contains(c) &&
                    pin.contains(d) && pin.contains(e) && pin.contains(f) &&
                    sou.contains(g) && sou.contains(h) && sou.contains(k))
                return true;
        }
        return false;
    }

    // 일기통관(멘젠 2판, 후로 1판)
    public static boolean isIkkitsuukan(HandState hand) {
        for (char suit : new char[]{'m', 'p', 's'}) {
            boolean[] found = new boolean[10];
            for (String t : hand.tiles) {
                if (t.endsWith(String.valueOf(suit))) {
                    int num = t.charAt(0) - '0';
                    found[num] = true;
                }
            }
            boolean has123 = found[1] && found[2] && found[3];
            boolean has456 = found[4] && found[5] && found[6];
            boolean has789 = found[7] && found[8] && found[9];
            if (has123 && has456 && has789) return true;
        }
        return false;
    }


    // --- 준찬타: 모든 멘츠/또이츠가 1,9만. 자패 있으면 불가. ---
    public static boolean isJunchanMeldSearch(List<String> tiles) {
        // 1. 자패 있으면 무조건 false
        for (String t : tiles) if (isHonor(t)) return false;
        // 2. 카운트맵 생성
        Map<String, Integer> counts = new HashMap<>();
        for (String t : tiles) counts.put(t, counts.getOrDefault(t, 0) + 1);
        // 3. 모든 1,9 패에 대해 또이츠(짝패)로 잡고 나머지 12장 4멘츠 분해 시도
        for (String pair : counts.keySet()) {
            if (!isTerminal(pair)) continue;
            if (counts.get(pair) >= 2) {
                Map<String, Integer> tmp = new HashMap<>(counts);
                tmp.put(pair, tmp.get(pair) - 2);
                if (isJunchanMelds(tmp, 4)) return true;
            }
        }
        return false;
    }
    private static boolean isJunchanMelds(Map<String, Integer> pool, int left) {
        if (left == 0) {
            int sum = 0;
            for (int v : pool.values()) sum += v;
            return sum == 0;
        }
        // 코츠(1,9만)
        for (String t : pool.keySet()) {
            if (!isTerminal(t)) continue;
            if (pool.get(t) >= 3) {
                Map<String, Integer> tmp = new HashMap<>(pool);
                tmp.put(t, tmp.get(t) - 3);
                if (isJunchanMelds(tmp, left - 1)) return true;
            }
        }
        // 슌츠(1,2,3 or 7,8,9만 허용)
        for (String suit : Arrays.asList("m", "p", "s")) {
            for (int i : Arrays.asList(1,7)) {
                String a = i + suit, b = (i+1) + suit, c = (i+2) + suit;
                if (pool.getOrDefault(a,0)>0 && pool.getOrDefault(b,0)>0 && pool.getOrDefault(c,0)>0) {
                    Map<String, Integer> tmp = new HashMap<>(pool);
                    tmp.put(a, tmp.get(a) - 1);
                    tmp.put(b, tmp.get(b) - 1);
                    tmp.put(c, tmp.get(c) - 1);
                    if (isJunchanMelds(tmp, left - 1)) return true;
                }
            }
        }
        return false;
    }
    private static boolean isTerminal(String t) {
        return t.length() == 2 && (t.charAt(0) == '1' || t.charAt(0) == '9');
    }
    private static boolean isHonor(String t) {
        return t.equals("E") || t.equals("S") || t.equals("W") || t.equals("N")
                || t.equals("P") || t.equals("F") || t.equals("C");
    }

    // --- 찬타: 모든 멘츠/또이츠가 반드시 1,9,자패 포함. ---
    public static boolean isChantaMeldSearch(List<String> tiles) {
        // 카운트맵 생성
        Map<String, Integer> counts = new HashMap<>();
        for (String t : tiles) counts.put(t, counts.getOrDefault(t, 0) + 1);
        // 모든 패에 대해 또이츠(짝패)로 잡고 나머지 12장 4멘츠 분해 시도
        for (String pair : counts.keySet()) {
            if (counts.get(pair) >= 2) {
                Map<String, Integer> tmp = new HashMap<>(counts);
                tmp.put(pair, tmp.get(pair) - 2);
                if (isChantaMelds(tmp, 4)) return true;
            }
        }
        return false;
    }
    private static boolean isChantaMelds(Map<String, Integer> pool, int left) {
        if (left == 0) {
            int sum = 0;
            for (int v : pool.values()) sum += v;
            return sum == 0;
        }
        // 코츠(3동패, 반드시 1,9,자패 포함)
        for (String t : pool.keySet()) {
            if (pool.get(t) >= 3 && isYaochu(t)) {
                Map<String, Integer> tmp = new HashMap<>(pool);
                tmp.put(t, tmp.get(t) - 3);
                if (isChantaMelds(tmp, left - 1)) return true;
            }
        }
        // 슌츠(3연속, 반드시 1,9 포함)
        for (String suit : Arrays.asList("m", "p", "s")) {
            for (int i = 1; i <= 7; i++) {
                String a = i + suit, b = (i+1) + suit, c = (i+2) + suit;
                if (pool.getOrDefault(a,0)>0 && pool.getOrDefault(b,0)>0 && pool.getOrDefault(c,0)>0) {
                    // 이 셋에 1,9가 하나라도 포함되어야 함
                    if (!(a.startsWith("1")||b.startsWith("1")||c.startsWith("1")||
                            a.startsWith("9")||b.startsWith("9")||c.startsWith("9")||
                            isHonor(a)||isHonor(b)||isHonor(c))) continue;
                    Map<String, Integer> tmp = new HashMap<>(pool);
                    tmp.put(a, tmp.get(a) - 1);
                    tmp.put(b, tmp.get(b) - 1);
                    tmp.put(c, tmp.get(c) - 1);
                    if (isChantaMelds(tmp, left - 1)) return true;
                }
            }
        }
        return false;
    }
    private static boolean isYaochu(String t) {
        return isHonor(t) ||
                (t.length() == 2 && (t.charAt(0) == '1' || t.charAt(0) == '9'));
    }


    // 청일색(멘젠 6판, 후로 5판)
    public static boolean isChinitsu(HandState hand) {
        char suit = 0;
        for (String t : hand.tiles) {
            if (t.endsWith("m") || t.endsWith("p") || t.endsWith("s")) {
                if (suit == 0) suit = t.charAt(1);
                else if (suit != t.charAt(1)) return false;
            } else {
                return false;
            }
        }
        return suit != 0;
    }

    // 혼일색(멘젠 3판, 후로 2판)
    public static boolean isHonitsu(HandState hand) {
        boolean hasSuit = false, hasHonor = false;
        char suit = 0;
        for (String t : hand.tiles) {
            if (t.endsWith("m") || t.endsWith("p") || t.endsWith("s")) {
                hasSuit = true;
                if (suit == 0) suit = t.charAt(1);
                else if (suit != t.charAt(1)) return false;
            } else {
                hasHonor = true;
            }
        }
        return hasSuit && hasHonor;
    }

    // 혼노두(2판, 멘젠/후로 동일, 1/9/자패로만)
    public static boolean isHonroutou(HandState hand) {
        for (String t : hand.tiles) {
            if (t.endsWith("m") || t.endsWith("p") || t.endsWith("s")) {
                char num = t.charAt(0);
                if (num != '1' && num != '9') return false;
            } else if (!isHonor(t)) {
                return false;
            }
        }
        return true;
    }

    // 탕야오(무조건 1판)
    public static boolean isTanyao(HandState hand) {
        for (String t : hand.tiles) {
            if (t.endsWith("m") || t.endsWith("p") || t.endsWith("s")) {
                char num = t.charAt(0);
                if (num == '1' || num == '9') return false;
            } else {
                return false;
            }
        }
        return true;
    }

    // 삼색동각(무조건 2판)
    public static boolean isSanshokuDokko(HandState hand) {
        int[][] count = new int[3][10];
        for (String t : hand.tiles) {
            int num = t.charAt(0) - '0';
            if (t.endsWith("m")) count[0][num]++;
            if (t.endsWith("p")) count[1][num]++;
            if (t.endsWith("s")) count[2][num]++;
        }
        for (int i = 1; i <= 9; i++) {
            if (count[0][i] >= 2 && count[1][i] >= 2 && count[2][i] >= 2) return true;
        }
        return false;
    }

    // 또이또이(무조건 2판)
    public static boolean isToitoi(HandState hand) {
        Map<String, Integer> count = new HashMap<>();

        for (String t : hand.tiles) {
            count.put(t, count.getOrDefault(t, 0) + 1);
        }

        int pair = 0;
        int set = 0;

        for (int c : count.values()) {
            if (c == 2) {
                pair++;
            } else if (c == 3 || c == 4) {
                set++;
            } else {
                // 셋트로 만들 수 없는 패가 있다면 또이또이 아님
                return false;
            }
        }

        // 1쌍 + 4셋트일 때만 인정
        return pair == 1 && set == 4;
    }


    // 역패(무조건 1판, 자패 3개 이상)
    public static boolean isYakuhai(HandState hand) {
        Map<String, Integer> counts = new HashMap<>();
        for (String t : hand.tiles) {
            counts.put(t, counts.getOrDefault(t, 0) + 1);
        }
        for (String honor : Arrays.asList("P","F","C","E","S","W","N")) {
            if (counts.getOrDefault(honor, 0) >= 3) return true;
        }
        return false;
    }

    // 소삼원(백/발/중 중 2개는 퐁, 1개는 또이츠)
    public static boolean isShousangen(HandState hand) {
        int P = 0, F = 0, C = 0;
        for (String t : hand.tiles) {
            if (t.equals("P")) P++;
            if (t.equals("F")) F++;
            if (t.equals("C")) C++;
        }
        int pair = 0, pon = 0;
        if (P == 2) pair++; else if (P == 3) pon++;
        if (F == 2) pair++; else if (F == 3) pon++;
        if (C == 2) pair++; else if (C == 3) pon++;
        return (pon == 2 && pair == 1);
    }
}
