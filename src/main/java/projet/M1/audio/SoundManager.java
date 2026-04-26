package projet.M1.audio;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.scene.Node;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Génère tous les effets sonores proceduralement (PCM → WAV en mémoire).
 * Aucun fichier audio externe nécessaire.
 */
public class SoundManager {

    private static final int   SR  = 22050; // sample rate
    private static final float VOL = 0.85f;

    private final Node root;

    private AudioNode paddleHit;
    private AudioNode wallHit;
    private AudioNode goalSound;
    private AudioNode countBeep;
    private AudioNode goBeep;

    public SoundManager(AssetManager am, Node rootNode) {
        this.root = rootNode;
        try {
            Path dir = Files.createTempDirectory("ah_sfx_");
            am.registerLocator(dir.toString(), FileLocator.class);

            paddleHit = build(am, dir, "ah_paddle.wav", genHit(1050f, 0.09f, 48f, 0.35f));
            wallHit   = build(am, dir, "ah_wall.wav",   genHit(520f,  0.07f, 32f, 0.18f));
            goalSound = build(am, dir, "ah_goal.wav",   genGoal());
            countBeep = build(am, dir, "ah_beep.wav",   genBeep(880f,  0.13f));
            goBeep    = build(am, dir, "ah_go.wav",     genBeep(1320f, 0.28f));

            for (AudioNode n : new AudioNode[]{paddleHit, wallHit, goalSound, countBeep, goBeep}) {
                n.setPositional(false);
                n.setLooping(false);
                n.setVolume(VOL);
                rootNode.attachChild(n);
            }
        } catch (Exception e) {
            System.err.println("[SoundManager] init failed: " + e.getMessage());
        }
    }

    // ── API publique ────────────────────────────────────────────────────────

    public void playPaddleHit() { play(paddleHit); }
    public void playWallHit()   { play(wallHit);   }
    public void playGoal()      { play(goalSound);  }
    public void playCountBeep() { play(countBeep);  }
    public void playGo()        { play(goBeep);     }

    private void play(AudioNode n) {
        if (n != null) n.playInstance();
    }

    // ── Générateurs PCM ─────────────────────────────────────────────────────

    /** Choc physique : déclin exponentiel + composante bruit. */
    private static byte[] genHit(float freq, float dur, float decay, float noise) {
        int n = (int)(SR * dur);
        short[] pcm = new short[n];
        Random rng = new Random(7);
        for (int i = 0; i < n; i++) {
            float t   = (float) i / SR;
            float env = (float) Math.exp(-decay * t);
            float sig = (float) Math.sin(2 * Math.PI * freq * t) * (1f - noise)
                      + (float)(rng.nextGaussian() * 0.5) * noise;
            pcm[i] = clip(env * sig);
        }
        return wrap(pcm);
    }

    /** Bip propre avec attaque et relâche linéaires. */
    private static byte[] genBeep(float freq, float dur) {
        int n   = (int)(SR * dur);
        float a = 0.008f * SR; // 8 ms attack
        float r = 0.015f * SR; // 15 ms release
        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float env = Math.min(i / a, 1f) * Math.min((n - i) / r, 1f);
            pcm[i] = clip(env * (float) Math.sin(2 * Math.PI * freq * i / SR));
        }
        return wrap(pcm);
    }

    /** But : sweep fréquentiel montant avec quinte parfaite. */
    private static byte[] genGoal() {
        float dur = 0.55f;
        int   n   = (int)(SR * dur);
        float r   = 0.04f * SR;
        short[] pcm = new short[n];
        double ph1 = 0, ph2 = 0;
        for (int i = 0; i < n; i++) {
            float t     = (float) i / SR;
            float ratio = t / dur;
            double f1   = 350 + ratio * 900;
            double f2   = f1 * 1.5;
            float env   = Math.min(t * 25f, 1f) * Math.min((n - i) / r, 1f);
            ph1 += 2 * Math.PI * f1 / SR;
            ph2 += 2 * Math.PI * f2 / SR;
            float sig = (float)(Math.sin(ph1) * 0.65 + Math.sin(ph2) * 0.35);
            pcm[i] = clip(env * sig);
        }
        return wrap(pcm);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static short clip(float f) {
        return (short)(Math.max(-1f, Math.min(1f, f)) * 32767);
    }

    /** Encapsule des échantillons 16-bit mono dans un en-tête WAV. */
    private static byte[] wrap(short[] pcm) {
        int data  = pcm.length * 2;
        ByteBuffer b = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN);
        b.put(new byte[]{'R','I','F','F'});  b.putInt(36 + data);
        b.put(new byte[]{'W','A','V','E'});
        b.put(new byte[]{'f','m','t',' '});  b.putInt(16);
        b.putShort((short)1);                // PCM
        b.putShort((short)1);                // mono
        b.putInt(SR);                        // sampleRate
        b.putInt(SR * 2);                    // byteRate
        b.putShort((short)2);                // blockAlign
        b.putShort((short)16);               // bitsPerSample
        b.put(new byte[]{'d','a','t','a'});  b.putInt(data);
        for (short s : pcm) b.putShort(s);
        return b.array();
    }

    private static AudioNode build(AssetManager am, Path dir, String name, byte[] wav)
            throws Exception {
        Files.write(dir.resolve(name), wav);
        return new AudioNode(am, name, AudioData.DataType.Buffer);
    }

    public void cleanup() {
        for (AudioNode n : new AudioNode[]{paddleHit, wallHit, goalSound, countBeep, goBeep})
            if (n != null) root.detachChild(n);
    }
}
