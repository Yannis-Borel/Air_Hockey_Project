package projet.M1.hud;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * Quad JME3 dont la texture est générée par AWT à la volée.
 * Produit un texte net et vectoriel sans passer par le système BitmapFont.
 *
 * Unité du displayHeight : pixels pour le guiNode, world-units pour le rootNode.
 */
public class CrispLabel extends Geometry {

    // Police haute résolution : partagée par toutes les instances
    public static final Font FONT = new Font(Font.SANS_SERIF, Font.BOLD, 64);

    private final AssetManager assetManager;
    private final float displayHeight;
    public ColorRGBA color;
    public boolean depthTest = false; // true pour les labels 3D dans rootNode
    private String lastText  = null;

    /**
     * Crée un label vide avec la hauteur d'affichage et la couleur données.
     * Un matériau transparent est appliqué en attendant le premier setText().
     * displayHeight s'exprime en pixels pour le guiNode, en world-units pour le rootNode.
     */
    public CrispLabel(AssetManager assetManager, float displayHeight, ColorRGBA color) {
        super("CrispLabel");
        this.assetManager  = assetManager;
        this.displayHeight = displayHeight;
        this.color = color;
        setMesh(new Quad(displayHeight, displayHeight));

        // Placeholder matériel transparent pour que la gémoétrie soit valide avant setText()
        Material placeholder = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        placeholder.setColor("Color", new ColorRGBA(0, 0, 0, 0));
        placeholder.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        placeholder.getAdditionalRenderState().setDepthTest(false);
        placeholder.getAdditionalRenderState().setDepthWrite(false);
        setMaterial(placeholder);
    }

    /**
     * Met à jour le texte affiché.
     * Ne rebake la texture que si le texte a changé depuis le dernier appel.
     */
    public void setText(String text) {
        if (text.equals(lastText)) return;
        lastText = text;
        bake(text);
    }

    /**
     * Met à jour la couleur et le texte en forçant un rebake,
     * même si le texte est identique au précédent.
     */
    public void setColorAndText(ColorRGBA c, String text) {
        this.color = c;
        lastText = null;
        setText(text);
    }

    /** Largeur du quad après le dernier setText(). */
    public float getWidth()  { return ((Quad) getMesh()).getWidth(); }

    /** Hauteur du quad après le dernier setText(). */
    public float getHeight() { return ((Quad) getMesh()).getHeight(); }


    /**
     * Génère la texture AWT pour le texte donné et l'applique sur ce quad.
     *
     * Étapes :
     *   1. Calcule les dimensions du rendu via FontMetrics
     *   2. Rend le texte en blanc sur fond transparent via Graphics2D
     *   3. Convertit en ByteBuffer RGBA avec flip vertical (convention OpenGL)
     *   4. Crée une Texture2D et redimensionne le Quad proportionnellement
     */
    private void bake(String text) {
        FontMetrics fm = metrics(FONT);

        int texW = Math.max(fm.stringWidth(text) + 8, 4);
        int texH = fm.getHeight() + 6;

        // Rendu AWT
        BufferedImage img = new BufferedImage(texW, texH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setFont(FONT);
        g.setColor(new Color(color.r, color.g, color.b, color.a));
        g.drawString(text, 4, fm.getAscent() + 3);
        g.dispose();

        // Conversion ByteBuffer : flip vertical pour OpenGL (y=0 en bas)
        int[] px  = new int[texW * texH];
        img.getRGB(0, 0, texW, texH, px, 0, texW);
        ByteBuffer buf = ByteBuffer.allocateDirect(texW * texH * 4);
        for (int row = texH - 1; row >= 0; row--) {
            for (int col = 0; col < texW; col++) {
                int p = px[row * texW + col];
                buf.put((byte) ((p >> 16) & 0xFF));
                buf.put((byte) ((p >>  8) & 0xFF));
                buf.put((byte) (p & 0xFF));
                buf.put((byte) ((p >> 24) & 0xFF));
            }
        }
        buf.flip();

        Texture2D tex = new Texture2D(
                new Image(Image.Format.RGBA8, texW, texH, buf, ColorSpace.sRGB));
        tex.setMagFilter(Texture2D.MagFilter.Bilinear);
        tex.setMinFilter(Texture2D.MinFilter.BilinearNoMipMaps);

        // Quad proportionnel au texte
        float quadH = displayHeight;
        float quadW = quadH * ((float) texW / texH);
        setMesh(new Quad(quadW, quadH));

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.getAdditionalRenderState().setDepthTest(depthTest);
        mat.getAdditionalRenderState().setDepthWrite(false);
        setMaterial(mat);
    }

    /**
     * Retourne les métriques de la police donnée via un contexte graphique temporaire.
     */
    private static FontMetrics metrics(Font f) {
        Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.dispose();
        return fm;
    }
}