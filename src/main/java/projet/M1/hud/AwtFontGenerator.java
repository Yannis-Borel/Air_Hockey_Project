package projet.M1.hud;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.font.BitmapFont;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Génère un BitmapFont JME3 haute résolution depuis les polices système Java AWT.
 * Écrit un PNG + fichier .fnt (format AngelCode) dans le dossier temp, puis
 * les charge via le pipeline standard de JME3 — UV et flip y gérés correctement.
 */
public final class AwtFontGenerator {

    private static final String GLYPHS =
            " !\"#$%&'()*+,-./0123456789:;<=>?@"
                    + "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`"
                    + "abcdefghijklmnopqrstuvwxyz{|}~"
                    + "éàèùâêîôûç"
                    + "ÉÀÈÙÂÊÎÔÛÇ";

    private static final int COLS = 16;

    // Cache statique : la police n'est générée qu'une seule fois par taille
    private static BitmapFont cache = null;

    private AwtFontGenerator() {}

    /**
     * Point d'entrée principal : retourne un BitmapFont à la taille demandée.
     * Si la police a déjà été générée, retourne le cache directement.
     * En cas d'erreur, replie sur la police par défaut de JME3.
     */
    public static BitmapFont generate(AssetManager assetManager, int ptSize) {
        if (cache != null) return cache;
        try {
            cache = build(assetManager, ptSize);
        } catch (Exception e) {
            System.err.println("[AwtFontGenerator] Erreur : " + e.getMessage());
            e.printStackTrace();
            cache = assetManager.loadFont("Interface/Fonts/Default.fnt");
        }
        return cache;
    }

    /**
     * Génère l'atlas de glyphes PNG et le fichier .fnt associé,
     * puis les enregistre dans un dossier temporaire et les charge via JME3.
     *
     * Étapes :
     *   1. Mesure les métriques AWT pour calculer la taille des cellules
     *   2. Rend chaque glyphe sur un atlas PNG (fond transparent, texte blanc)
     *   3. Écrit le fichier .fnt au format AngelCode avec les coordonnées UV
     *   4. Charge le tout via le pipeline standard de JME3
     */
    private static BitmapFont build(AssetManager assetManager, int ptSize) throws IOException {
        Font awtFont = new Font(Font.SANS_SERIF, Font.BOLD, ptSize);

        // Mesure des métriques de la police pour dimensionner les cellules
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gp = probe.createGraphics();
        gp.setFont(awtFont);
        FontMetrics fm = gp.getFontMetrics();
        gp.dispose();

        int pad = 2;
        int cellW = fm.getMaxAdvance() + pad * 2 + 2;
        int cellH = fm.getHeight() + pad * 2;
        int rows = (GLYPHS.length() + COLS - 1) / COLS;
        int atlasW = nextPow2(cellW * COLS);
        int atlasH = nextPow2(cellH * rows);

        // Rendu de l'atlas : fond transparent, texte blanc
        BufferedImage atlas = new BufferedImage(atlasW, atlasH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setFont(awtFont);
        g.setColor(Color.WHITE);

        // Métriques par glyphe pour le .fnt
        int[] ids = new int[GLYPHS.length()];
        int[] xs = new int[GLYPHS.length()];
        int[] ys = new int[GLYPHS.length()];  // y depuis le HAUT (convention AngelCode)
        int[] ws = new int[GLYPHS.length()];
        int[] advs = new int[GLYPHS.length()];

        // Dessin de chaque glyphe dans sa cellule de l'atlas
        for (int i = 0; i < GLYPHS.length(); i++) {
            char c = GLYPHS.charAt(i);
            int col = i % COLS;
            int row = i / COLS;
            int cx = col * cellW;
            int cy = row * cellH;

            g.drawString(String.valueOf(c), cx + pad, cy + pad + fm.getAscent());

            int w = Math.max(fm.charWidth(c), 2) + pad;
            ids[i] = c;
            xs[i] = cx;
            ys[i] = cy;   // y depuis le haut : AngelCode standard
            ws[i] = w;
            advs[i] = w;
        }
        g.dispose();

        // Écriture du PNG et du fichier .fnt dans le dossier temp
        File dir = new File(System.getProperty("java.io.tmpdir"), "ah_font_" + ptSize);
        dir.mkdirs();

        File png = new File(dir, "font.png");
        ImageIO.write(atlas, "PNG", png);

        // Écriture du fichier .fnt au format AngelCode
        File fnt = new File(dir, "font.fnt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(fnt))) {
            pw.printf("info face=\"AHFont\" size=%d bold=1 italic=0 charset=\"\" unicode=1 "
                    + "stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1%n", ptSize);
            pw.printf("common lineHeight=%d base=%d scaleW=%d scaleH=%d pages=1 packed=0%n",
                    cellH, fm.getAscent() + pad, atlasW, atlasH);
            pw.printf("page id=0 file=\"font.png\"%n");
            pw.printf("chars count=%d%n", GLYPHS.length());
            for (int i = 0; i < GLYPHS.length(); i++) {
                pw.printf("char id=%d x=%d y=%d width=%d height=%d "
                                + "xoffset=0 yoffset=0 xadvance=%d page=0 chnl=15%n",
                        ids[i], xs[i], ys[i], ws[i], cellH, advs[i]);
            }
        }

        // Chargement via le pipeline standard JME3 (gère le flip y automatiquement)
        assetManager.registerLocator(dir.getAbsolutePath(), FileLocator.class);
        return assetManager.loadFont("font.fnt");
    }

    /**
     * Retourne la plus petite puissance de 2 supérieure ou égale à n.
     * Utilisé pour dimensionner l'atlas en texture GPU-compatible.
     */
    private static int nextPow2(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }
}