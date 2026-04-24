package projet.M1.entities;

import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * Le terrain flotte au sommet d'un gratte-ciel.
 * Table : x∈[-5,+5]  z∈[-10,+10]  y≈0
 * Caméra : (0, 7, 19) → vue légèrement plongeante.
 */
public class ArenaDecor {

    private final Node root;

    public ArenaDecor(AssetManager am) {
        root = new Node("arenaDecor");
        buildRooftop(am);
        buildEdgeGlow(am);
        buildGlassRails(am);
        buildOwnBuilding(am);
        buildCityscape(am);
    }

    // ── Dalle du toit (dépasse légèrement du terrain) ───────────────────────

    private void buildRooftop(AssetManager am) {
        // Surface
        Geometry slab = box("rooftop", 10f, 0.18f, 14f, 0f, -0.18f, 0f);
        slab.setMaterial(litMat(am,
                new ColorRGBA(0.04f, 0.04f, 0.06f, 1f),
                new ColorRGBA(0.11f, 0.11f, 0.15f, 1f), 28f));
        root.attachChild(slab);

        // Liseré métallique sur les bords (4 tranches fines)
        Material edge = litMat(am,
                new ColorRGBA(0.20f, 0.20f, 0.24f, 1f),
                new ColorRGBA(0.55f, 0.55f, 0.62f, 1f), 60f);

        float t = 0.18f, h = 0.22f, y = h / 2f;
        // avant / arrière
        for (int s : new int[]{-1, 1}) {
            Geometry e = box("edgeZ_" + s, 10f, h, t, 0f, y, s * 14f);
            e.setMaterial(edge); root.attachChild(e);
        }
        // gauche / droite
        for (int s : new int[]{-1, 1}) {
            Geometry e = box("edgeX_" + s, t, h, 14f, s * 10f, y, 0f);
            e.setMaterial(edge); root.attachChild(e);
        }
    }

    // ── Lumières LED cyan sur le pourtour de la dalle ───────────────────────

    private void buildEdgeGlow(AssetManager am) {
        Material glow = unshaded(am, new ColorRGBA(0.05f, 0.65f, 0.90f, 1f));
        float t = 0.05f, strip = 0.08f;

        // avant / arrière
        for (int s : new int[]{-1, 1}) {
            Geometry g = box("glowZ_" + s, 10f, strip, t, 0f, strip, s * 14.02f);
            g.setMaterial(glow); root.attachChild(g);
        }
        // gauche / droite
        for (int s : new int[]{-1, 1}) {
            Geometry g = box("glowX_" + s, t, strip, 14f, s * 10.02f, strip, 0f);
            g.setMaterial(glow); root.attachChild(g);
        }
    }

    // ── Vitres de sécurité transparentes autour du toit ─────────────────────

    private void buildGlassRails(AssetManager am) {
        Material glass = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        glass.setColor("Color", new ColorRGBA(0.3f, 0.55f, 0.75f, 0.18f));
        glass.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        float railH = 1.1f, yc = railH / 2f + 0.04f;

        for (int s : new int[]{-1, 1}) {
            // côtés Z
            Geometry gz = box("railZ_" + s, 9.8f, railH, 0.04f, 0f, yc, s * 13.96f);
            gz.setMaterial(glass.clone());
            gz.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
            root.attachChild(gz);
            // côtés X
            Geometry gx = box("railX_" + s, 0.04f, railH, 13.96f, s * 9.96f, yc, 0f);
            gx.setMaterial(glass.clone());
            gx.setQueueBucket(com.jme3.renderer.queue.RenderQueue.Bucket.Transparent);
            root.attachChild(gx);
        }
    }

    // ── Corps du gratte-ciel (descend très bas) ──────────────────────────────

    private void buildOwnBuilding(AssetManager am) {
        float buildH = 80f;
        Geometry tower = box("ownTower", 8f, buildH / 2f, 11f, 0f, -(buildH / 2f + 0.36f), 0f);
        tower.setMaterial(litMat(am,
                new ColorRGBA(0.03f, 0.04f, 0.07f, 1f),
                new ColorRGBA(0.08f, 0.10f, 0.16f, 1f), 50f));
        root.attachChild(tower);

        // Fenêtres (rangées lumineuses)
        addWindowBand(am, 0f, -4f,  0f,  8.05f, 0.5f, 11f, true);
        addWindowBand(am, 0f, -9f,  0f,  8.05f, 0.5f, 11f, true);
        addWindowBand(am, 0f, -14f, 0f,  8.05f, 0.5f, 11f, true);
    }

    private void addWindowBand(AssetManager am, float x, float y, float z,
                                float hx, float hy, float hz, boolean bothSides) {
        Material winMat = unshaded(am, new ColorRGBA(0.90f, 0.82f, 0.50f, 1f));
        if (bothSides) {
            for (int s : new int[]{-1, 1}) {
                Geometry w = box("win_" + y + "_" + s, hx, hy, 0.05f,
                        x, y, s * (hz + 0.06f));
                w.setMaterial(winMat); root.attachChild(w);
            }
        }
    }

    // ── Paysage urbain autour ─────────────────────────────────────────────────

    private void buildCityscape(AssetManager am) {
        // Chaque bâtiment : {cx, cz, halfW, halfD, topY}
        // topY < 0 → son toit est en dessous de notre dalle
        float[][] buildings = {
            // Gauche proches
            {-17f,  -4f, 3.5f, 4.0f, -6f},
            {-22f,   5f, 5.0f, 3.5f, -3f},
            {-15f,  10f, 2.5f, 2.5f,-12f},
            // Droite proches
            { 17f,  -4f, 3.5f, 4.0f, -5f},
            { 22f,   5f, 5.0f, 3.5f, -4f},
            { 15f,  10f, 2.5f, 2.5f,-11f},
            // Fond
            { -8f, -22f, 4.0f, 3.0f, -2f},
            {  0f, -24f, 6.0f, 4.0f,  1f},   // ce bâtiment est presque aussi haut
            {  8f, -22f, 3.5f, 3.0f, -3f},
            {-18f, -18f, 4.5f, 3.5f, -8f},
            { 18f, -18f, 4.5f, 3.5f, -7f},
            // Côtés lointains
            {-32f,   0f, 6.0f, 5.0f,-14f},
            { 32f,   0f, 6.0f, 5.0f,-13f},
            {-26f, -14f, 4.0f, 4.0f, -9f},
            { 26f, -14f, 4.0f, 4.0f,-10f},
        };

        for (int i = 0; i < buildings.length; i++) {
            float cx = buildings[i][0], cz = buildings[i][1];
            float hw = buildings[i][2], hd = buildings[i][3];
            float top = buildings[i][4];
            float buildH = 60f;
            float cy = top - buildH / 2f;

            // Façade
            Geometry b = box("city_" + i, hw, buildH / 2f, hd, cx, cy, cz);
            b.setMaterial(cityFacade(am, i));
            root.attachChild(b);

            // Fenêtres lumineuses (2-3 rangées près du toit)
            Material wm = unshaded(am, new ColorRGBA(0.85f, 0.76f, 0.42f, 1f));
            for (int row = 0; row < 3; row++) {
                float wy = top - 1.2f - row * 2.5f;
                // façade Z+
                Geometry wf = box("win_" + i + "_" + row, hw * 0.9f, 0.35f, 0.06f,
                        cx, wy, cz + hd + 0.07f);
                wf.setMaterial(wm); root.attachChild(wf);
                // façade Z-
                Geometry wb = box("winB_" + i + "_" + row, hw * 0.9f, 0.35f, 0.06f,
                        cx, wy, cz - hd - 0.07f);
                wb.setMaterial(wm); root.attachChild(wb);
            }
        }

        // Sol de la ville très loin en bas (halo lumineux)
        Geometry cityFloor = box("cityFloor", 200f, 0.5f, 200f, 0f, -90f, 0f);
        cityFloor.setMaterial(unshaded(am, new ColorRGBA(0.06f, 0.07f, 0.12f, 1f)));
        root.attachChild(cityFloor);
    }

    private Material cityFacade(AssetManager am, int seed) {
        float v = 0.06f + (seed % 4) * 0.015f;
        return litMat(am,
                new ColorRGBA(v * 0.4f, v * 0.45f, v * 0.7f, 1f),
                new ColorRGBA(v,        v * 1.1f,  v * 1.6f, 1f), 40f);
    }

    // ── Éclairage de la scène ────────────────────────────────────────────────

    public void addLightsTo(Node scene) {
        // 4 projecteurs au-dessus du terrain, teinte bleue froide
        float[][] pos = {{-5f,-7f},{5f,-7f},{-5f,7f},{5f,7f}};
        for (float[] p : pos) {
            PointLight pl = new PointLight();
            pl.setPosition(new Vector3f(p[0], 10f, p[1]));
            pl.setRadius(26f);
            pl.setColor(new ColorRGBA(0.75f, 0.82f, 1.00f, 1f).mult(0.55f));
            scene.addLight(pl);
        }
        // Halo ambiant de la ville en bas
        PointLight city = new PointLight();
        city.setPosition(new Vector3f(0f, -30f, -10f));
        city.setRadius(120f);
        city.setColor(new ColorRGBA(0.15f, 0.20f, 0.40f, 1f).mult(0.8f));
        scene.addLight(city);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Geometry box(String name, float hx, float hy, float hz,
                                 float x, float y, float z) {
        Geometry g = new Geometry(name, new Box(hx, hy, hz));
        g.setLocalTranslation(x, y, z);
        return g;
    }

    private static Material litMat(AssetManager am,
                                    ColorRGBA ambient, ColorRGBA diffuse, float shine) {
        Material m = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Ambient",  ambient);
        m.setColor("Diffuse",  diffuse);
        m.setColor("Specular", new ColorRGBA(0.4f, 0.4f, 0.5f, 1f));
        m.setFloat("Shininess", shine);
        return m;
    }

    private static Material unshaded(AssetManager am, ColorRGBA color) {
        Material m = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", color);
        return m;
    }

    public Node getNode() { return root; }
}
