package cn.rabitown.rabisystem.modules.spirit.utils;

import org.bukkit.entity.Allay;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MusicLibrary {

    public enum SongType {
        RANDOM("随性哼唱"),
        TWINKLE("小星星"),
        PADORU("Padoru Padoru"),
        FRIEREN("Where Hidden Magic Sleeps"),
        CHOCOBO("陆行鸟之歌"),
        BLUE_ARCHIVE("Blue Archive - Aoharu"),
        CRYSTAL_PRELUDE("最终幻想 - 水晶序曲"),
        GRASSWALK("植物大战僵尸 - Grasswalk");

        final String name;

        SongType(String name) {
            this.name = name;
        }
    }

    public static class MusicBox {
        // --- 精准音高常量表 ---
        private static final float A3 = 0.595f;
        private static final float B3 = 0.667f;
        private static final float C4 = 0.707f;
        private static final float D4 = 0.794f;
        private static final float Ds4 = 0.841f;
        private static final float E4 = 0.891f;
        private static final float F4 = 0.944f;
        private static final float Fs4 = 1.000f;
        private static final float G4 = 1.059f;
        private static final float Gs4 = 1.122f;
        private static final float A4 = 1.189f;
        private static final float As4 = 1.260f;
        private static final float B4 = 1.335f;
        private static final float C5 = 1.414f;
        private static final float Cs5 = 1.498f;
        private static final float D5 = 1.587f;
        private static final float Ds5 = 1.682f;
        private static final float E5 = 1.782f;
        private static final float F5 = 1.888f;
        private static final float Fs5 = 2.000f;
        private static final float G5 = 2.000f; // Limit
        private static final float REST = 0.0f;

        // 1. 小星星
        static final List<MusicNote> SONG_TWINKLE = Arrays.asList(
                new MusicNote(C4, 2), new MusicNote(C4, 2), new MusicNote(G4, 2), new MusicNote(G4, 2),
                new MusicNote(A4, 2), new MusicNote(A4, 2), new MusicNote(G4, 4),
                new MusicNote(F4, 2), new MusicNote(F4, 2), new MusicNote(E4, 2), new MusicNote(E4, 2),
                new MusicNote(D4, 2), new MusicNote(D4, 2), new MusicNote(C4, 4)
        );
        // 2. Padoru
        static final List<MusicNote> SONG_PADORU = Arrays.asList(
                new MusicNote(D5, 1), new MusicNote(E5, 1), new MusicNote(D5, 1), new MusicNote(C5, 2), new MusicNote(C5, 2),
                new MusicNote(C5, 1), new MusicNote(C5, 1), new MusicNote(D5, 1), new MusicNote(E5, 2), new MusicNote(E5, 2),
                new MusicNote(E5, 1), new MusicNote(E5, 1), new MusicNote(D5, 1), new MusicNote(C5, 1), new MusicNote(D5, 2),
                new MusicNote(G5, 1), new MusicNote(E5, 1), new MusicNote(C5, 1), new MusicNote(D5, 1), new MusicNote(C5, 4)
        );
        // 3. 芙莉莲
        static final List<MusicNote> SONG_FRIEREN = Arrays.asList(
                new MusicNote(A4, 2), new MusicNote(REST, 1), new MusicNote(G4, 2), new MusicNote(REST, 1),
                new MusicNote(F4, 2), new MusicNote(REST, 1), new MusicNote(E4, 6),
                new MusicNote(E4, 1), new MusicNote(F4, 1), new MusicNote(G4, 2),
                new MusicNote(C4, 4), new MusicNote(D4, 2), new MusicNote(E4, 4)
        );
        // 4. 陆行鸟
        static final List<MusicNote> SONG_CHOCOBO = Arrays.asList(
                new MusicNote(C5, 1), new MusicNote(REST, 1), new MusicNote(C5, 1), new MusicNote(D5, 1),
                new MusicNote(C5, 1), new MusicNote(B4, 1), new MusicNote(A4, 1), new MusicNote(B4, 1),
                new MusicNote(C5, 3), new MusicNote(REST, 1)
        );
        // 5. BA Aoharu
        static final List<MusicNote> SONG_BLUE_ARCHIVE = Arrays.asList(
                new MusicNote(A4, 1), new MusicNote(E5, 1), new MusicNote(Cs5, 1), new MusicNote(A4, 1),
                new MusicNote(Fs4, 1), new MusicNote(Cs5, 1), new MusicNote(A4, 1), new MusicNote(Fs4, 1),
                new MusicNote(D4, 1), new MusicNote(A4, 1), new MusicNote(Fs4, 1), new MusicNote(D4, 1),
                new MusicNote(E4, 1), new MusicNote(B4, 1), new MusicNote(Gs4, 1), new MusicNote(E4, 1)
        );
        // 6. 水晶序曲
        static final List<MusicNote> SONG_CRYSTAL = Arrays.asList(
                new MusicNote(C4, 1), new MusicNote(D4, 1), new MusicNote(E4, 1), new MusicNote(G4, 1),
                new MusicNote(C5, 1), new MusicNote(D5, 1), new MusicNote(E5, 1), new MusicNote(G5, 1),
                new MusicNote(E5, 1), new MusicNote(D5, 1), new MusicNote(C5, 1), new MusicNote(G4, 1)
        );
        // 7. Grasswalk
        static final List<MusicNote> SONG_GRASSWALK = Arrays.asList(
                new MusicNote(C4, 2), new MusicNote(G4, 2), new MusicNote(C5, 2), new MusicNote(Ds5, 2),
                new MusicNote(F5, 1), new MusicNote(G5, 3),
                new MusicNote(C4, 2), new MusicNote(G4, 2), new MusicNote(C5, 2), new MusicNote(Ds5, 2),
                new MusicNote(F5, 1), new MusicNote(Ds5, 3)
        );

        public static class MusicNote {
            public float pitch; // Minecraft Pitch 0.5 - 2.0
            public int delay;   // Ticks / 5 (Loops)

            public MusicNote(float pitch, int delay) {
                this.pitch = pitch;
                this.delay = delay;
            }
        }

        // 可以在这里放入原代码 MusicBox 中的 getSong 和 常量定义
        public static List<MusicNote> getSong(SongType type) {
            switch (type) {
                case TWINKLE:
                    return SONG_TWINKLE;
                case PADORU:
                    return SONG_PADORU;
                case FRIEREN:
                    return SONG_FRIEREN;
                case CHOCOBO:
                    return SONG_CHOCOBO;
                case BLUE_ARCHIVE:
                    return SONG_BLUE_ARCHIVE;
                case CRYSTAL_PRELUDE:
                    return SONG_CRYSTAL;
                case GRASSWALK:
                    return SONG_GRASSWALK;
                default:
                    return Collections.emptyList();
            }
        }
    }

    public static void spawnMusicParticle(Allay spirit) {
        double r = ThreadLocalRandom.current().nextDouble();
        spirit.getWorld().spawnParticle(org.bukkit.Particle.NOTE, spirit.getLocation().add(0, 0.6, 0), 0, r, 0, 0, 1);
    }

}