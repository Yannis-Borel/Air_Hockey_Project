package projet.M1;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import projet.M1.hud.CrispLabel;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu principal affiché au lancement du jeu.
 * Propose trois modes : Tournoi, Solo vs IA (avec sélecteur de difficulté) et Multijoueur.
 */
public class MainMenuState extends AbstractAppState {

    private SimpleApplication app;
    private Main mainApp;
    private Node root;
    private int screenW, screenH;

    private Node diffPanel = null;
    private boolean diffPanelOpen = false;

    /** Structure interne représentant un bouton du menu. */
    private static class Btn {
        float x, y, w, h;
        Geometry  bg;
        ColorRGBA base, hover;
        Runnable action;

        Btn(float x, float y, float w, float h, Geometry bg, ColorRGBA base, Runnable action) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.bg = bg; this.action = action;
            this.base = base.clone();
            this.hover = new ColorRGBA(
                    Math.min(base.r * 1.4f, 1f),
                    Math.min(base.g * 1.4f, 1f),
                    Math.min(base.b * 1.4f, 1f), base.a);
        }
        boolean hit(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private final List<Btn> mainBtns = new ArrayList<>();
    private final List<Btn> diffBtns = new ArrayList<>();

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        this.mainApp = (Main) app;
        screenW = app.getContext().getSettings().getWidth();
        screenH = app.getContext().getSettings().getHeight();
        buildUI();
        registerInput();
        this.app.getInputManager().setCursorVisible(true);
    }

    /**
     * Construit l'interface du menu principal : fond, titre, sous-titre,
     * rappel des contrôles et les trois boutons de mode de jeu.
     */
    private void buildUI() {
        root = new Node("mainMenuRoot");

        addQuad(0, 0, screenW, screenH, new ColorRGBA(0.03f, 0.04f, 0.10f, 1f), 0f);

        // Titre
        CrispLabel title = label("AIR HOCKEY", 68f, new ColorRGBA(0.15f, 0.65f, 1f, 1f));
        title.setLocalTranslation(screenW / 2f - title.getWidth() / 2f,
                screenH / 2f + 220f, 1f);
        root.attachChild(title);

        // Sous-titre
        CrispLabel sub = label("M1 Informatique — Université de Toulon",
                20f, new ColorRGBA(0.55f, 0.55f, 0.65f, 1f));
        sub.setLocalTranslation(screenW / 2f - sub.getWidth() / 2f,
                screenH / 2f + 162f, 1f);
        root.attachChild(sub);

        // Hint
        CrispLabel hint = label("P1 : ZQSD   P2 : Flèches   ESC : Pause",
                16f, new ColorRGBA(0.4f, 0.4f, 0.5f, 1f));
        hint.setLocalTranslation(screenW / 2f - hint.getWidth() / 2f, 30f, 1f);
        root.attachChild(hint);

        float btnW = 290f, btnH = 60f, gap = 76f;
        float cx = screenW / 2f - btnW / 2f;
        float topY = screenH / 2f + 60f;

        addMainBtn("Tournoi", cx, topY, btnW, btnH,
                new ColorRGBA(0.60f, 0.38f, 0.00f, 0.95f), mainApp::startTournament);
        addMainBtn("Solo vs IA  ▶", cx, topY - gap, btnW, btnH,
                new ColorRGBA(0.10f, 0.45f, 0.10f, 0.95f), this::toggleDiffPanel);
        addMainBtn("Multijoueur", cx, topY - gap * 2, btnW, btnH,
                new ColorRGBA(0.12f, 0.12f, 0.52f, 0.95f), mainApp::startMultiplayer);

        app.getGuiNode().attachChild(root);
    }

    /** Crée un bouton principal et l'ajoute à la liste mainBtns. */
    private void addMainBtn(String text, float x, float y, float w, float h,
                            ColorRGBA color, Runnable action) {
        Geometry bg = addQuad(x, y, w, h, color, 0.5f);
        CrispLabel t = label(text, 24f, ColorRGBA.White);
        t.setLocalTranslation(x + w / 2f - t.getWidth() / 2f,
                y + h / 2f - t.getHeight() / 2f, 1f);
        root.attachChild(t);
        mainBtns.add(new Btn(x, y, w, h, bg, color, action));
    }

    /** Affiche ou masque le panneau de sélection de difficulté IA. */
    private void toggleDiffPanel() {
        if (diffPanelOpen) hideDiffPanel();
        else showDiffPanel();
    }

    /**
     * Construit et affiche le panneau de sélection de difficulté IA
     * avec un bouton par niveau, positionné à droite du bouton Solo.
     */
    private void showDiffPanel() {
        hideDiffPanel();
        diffPanel = new Node("diffPanel");
        diffPanelOpen = true;

        float btnW = 210f, btnH = 46f, gap = 54f;
        float panelX = screenW / 2f + 165f;
        float topY = screenH / 2f + 60f - 76f;

        AIController.Level[] levels = AIController.Level.values();
        ColorRGBA[] colors = {
                new ColorRGBA(0.15f, 0.55f, 0.15f, 0.95f),
                new ColorRGBA(0.10f, 0.40f, 0.55f, 0.95f),
                new ColorRGBA(0.50f, 0.35f, 0.00f, 0.95f),
                new ColorRGBA(0.55f, 0.15f, 0.45f, 0.95f),
                new ColorRGBA(0.60f, 0.08f, 0.08f, 0.95f),
        };

        for (int i = 0; i < levels.length; i++) {
            final AIController.Level lvl = levels[i];
            float y = topY - i * gap;

            Geometry bg = new Geometry("diffBg" + i, new Quad(btnW, btnH));
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", colors[i]);
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            bg.setMaterial(mat);
            bg.setLocalTranslation(panelX, y, 0.5f);
            diffPanel.attachChild(bg);

            CrispLabel t = label(lvl.label, 22f, ColorRGBA.White);
            t.setLocalTranslation(panelX + btnW / 2f - t.getWidth() / 2f,
                    y + btnH / 2f - t.getHeight() / 2f, 1f);
            diffPanel.attachChild(t);

            diffBtns.add(new Btn(panelX, y, btnW, btnH, bg, colors[i],
                    () -> mainApp.startSoloAI(lvl)));
        }
        root.attachChild(diffPanel);
    }

    /** Masque et détruit le panneau de sélection de difficulté. */
    private void hideDiffPanel() {
        if (diffPanel != null) root.detachChild(diffPanel);
        diffPanel = null;
        diffPanelOpen = false;
        diffBtns.clear();
    }

    /** Met à jour l'effet de survol des boutons principaux et du panneau de difficulté. */
    @Override
    public void update(float tpf) {
        Vector2f m = app.getInputManager().getCursorPosition();
        applyHover(mainBtns, m);
        if (diffPanelOpen) applyHover(diffBtns, m);
    }

    /** Applique la couleur hover ou base selon la position de la souris. */
    private void applyHover(List<Btn> list, Vector2f m) {
        for (Btn b : list)
            b.bg.getMaterial().setColor("Color", b.hit(m.x, m.y) ? b.hover : b.base);
    }

    private final ActionListener clickListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        Vector2f m = app.getInputManager().getCursorPosition();
        if (diffPanelOpen) {
            for (Btn b : diffBtns) if (b.hit(m.x, m.y)) { b.action.run(); return; }
        }
        for (Btn b : mainBtns) if (b.hit(m.x, m.y)) { b.action.run(); return; }
    };

    /** Enregistre le listener de clic souris pour les boutons du menu. */
    private void registerInput() {
        app.getInputManager().addMapping("mmClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(clickListener, "mmClick");
    }

    /** Crée un quad coloré semi-transparent et l'attache au nœud racine. */
    private Geometry addQuad(float x, float y, float w, float h, ColorRGBA color, float z) {
        Geometry geo = new Geometry("mmq_" + System.nanoTime(), new Quad(w, h));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geo.setMaterial(mat);
        geo.setLocalTranslation(x, y, z);
        root.attachChild(geo);
        return geo;
    }

    /** Crée un CrispLabel avec le texte, la hauteur et la couleur donnés. */
    private CrispLabel label(String text, float h, ColorRGBA color) {
        CrispLabel l = new CrispLabel(app.getAssetManager(), h, color);
        l.setText(text);
        return l;
    }

    /** Détache le menu et libère les ressources au retour en jeu. */
    @Override
    public void cleanup() {
        super.cleanup();
        app.getGuiNode().detachChild(root);
        app.getInputManager().removeListener(clickListener);
        if (app.getInputManager().hasMapping("mmClick"))
            app.getInputManager().deleteMapping("mmClick");
        mainBtns.clear();
        diffBtns.clear();
        app.getInputManager().setCursorVisible(false);
    }
}