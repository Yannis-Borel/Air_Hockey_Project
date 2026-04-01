package projet.M1;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Menu pause (Echap) avec : Resume, sélecteur de caméra, Settings, Leave.
 * Utilise le guiNode de JME3 (coordonnées écran en pixels, Y vers le haut).
 */
public class MenuState extends AbstractAppState {

    private SimpleApplication app;
    private Node menuRoot;
    private BitmapFont font;

    private boolean camDropdownOpen = false;
    private Node   camDropdownNode;

    private final List<ButtonInfo> mainButtons = new ArrayList<>();
    private final List<ButtonInfo> camButtons  = new ArrayList<>();

    private int screenW, screenH;

    // ---- Structure bouton ----
    private static class ButtonInfo {
        float x, y, w, h;
        Runnable action;
        Geometry bg;
        ColorRGBA baseColor;
        ColorRGBA hoverColor;

        ButtonInfo(float x, float y, float w, float h,
                   Runnable action, Geometry bg, ColorRGBA base) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.action = action; this.bg = bg;
            this.baseColor  = base.clone();
            // hover = version plus claire
            this.hoverColor = new ColorRGBA(
                Math.min(base.r * 1.4f, 1f),
                Math.min(base.g * 1.4f, 1f),
                Math.min(base.b * 1.4f, 1f),
                base.a
            );
        }

        boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    // ------------------------------------------------------------------

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        screenW = app.getContext().getSettings().getWidth();
        screenH = app.getContext().getSettings().getHeight();
        font = this.app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        buildMenu();
        registerInput();

        // Rendre le curseur visible dans le menu
        this.app.getInputManager().setCursorVisible(true);
    }

    private void buildMenu() {
        menuRoot = new Node("menuRoot");

        // Fond semi-transparent noir
        addQuad(0, 0, screenW, screenH, new ColorRGBA(0f, 0f, 0f, 0.65f), 0f);

        // Titre
        BitmapText title = makeText("PAUSE", font.getCharSet().getRenderedSize() * 2.5f, ColorRGBA.White);
        title.setLocalTranslation(
            screenW / 2f - title.getLineWidth() / 2f,
            screenH / 2f + 190f, 1f
        );
        menuRoot.attachChild(title);

        float btnW = 250f, btnH = 50f;
        float cx   = screenW / 2f - btnW / 2f;
        float topY = screenH / 2f + 90f;
        float gap  = 68f;

        addMainButton("Resume",     cx, topY,          btnW, btnH, new ColorRGBA(0.1f,  0.5f,  0.1f,  0.95f), this::resume);
        addMainButton("Camera  v",  cx, topY - gap,    btnW, btnH, new ColorRGBA(0.15f, 0.2f,  0.6f,  0.95f), this::toggleCamDropdown);
        addMainButton("Settings",   cx, topY - gap*2,  btnW, btnH, new ColorRGBA(0.35f, 0.35f, 0.35f, 0.95f), this::openSettings);
        addMainButton("Leave",      cx, topY - gap*3,  btnW, btnH, new ColorRGBA(0.6f,  0.08f, 0.08f, 0.95f), this::leave);

        // Dropdown caméra (construit mais non attaché)
        buildCamDropdown(cx + btnW + 12f, topY - gap);

        app.getGuiNode().attachChild(menuRoot);
    }

    private void addMainButton(String label, float x, float y, float w, float h,
                                ColorRGBA color, Runnable action) {
        Geometry bg = addQuad(x, y, w, h, color, 0.5f);
        BitmapText text = makeText(label, font.getCharSet().getRenderedSize() * 1.3f, ColorRGBA.White);
        text.setLocalTranslation(
            x + w / 2f - text.getLineWidth() / 2f,
            y + h / 2f + text.getHeight() / 3f,
            1f
        );
        menuRoot.attachChild(text);
        mainButtons.add(new ButtonInfo(x, y, w, h, action, bg, color));
    }

    // Construit les 3 options de caméra dans un node séparé
    private void buildCamDropdown(float x, float anchorY) {
        camDropdownNode = new Node("camDropdown");

        String[]   labels  = { "Vue dessus", "Vue largeur", "Vue longueur" };
        Runnable[] actions = {
            () -> { ((Main) app).setCamTop();        resume(); },
            () -> { ((Main) app).setCamSideWidth();  resume(); },
            () -> { ((Main) app).setCamSideLength(); resume(); }
        };

        float dw = 180f, dh = 42f, gap = 48f;
        ColorRGBA base = new ColorRGBA(0.1f, 0.15f, 0.55f, 0.95f);

        for (int i = 0; i < 3; i++) {
            float by = anchorY + dh - i * gap;  // du haut vers le bas

            Geometry bg = new Geometry("camBg" + i, new Quad(dw, dh));
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", base.clone());
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            bg.setMaterial(mat);
            bg.setLocalTranslation(x, by, 0.6f);
            camDropdownNode.attachChild(bg);

            BitmapText text = makeText(labels[i], font.getCharSet().getRenderedSize(), ColorRGBA.White);
            text.setLocalTranslation(x + 10f, by + dh / 2f + text.getHeight() / 3f, 1.1f);
            camDropdownNode.attachChild(text);

            camButtons.add(new ButtonInfo(x, by, dw, dh, actions[i], bg, base));
        }
    }

    private void toggleCamDropdown() {
        camDropdownOpen = !camDropdownOpen;
        if (camDropdownOpen) {
            menuRoot.attachChild(camDropdownNode);
        } else {
            menuRoot.detachChild(camDropdownNode);
        }
    }

    // ------------------------------------------------------------------
    // Actions

    void resume() {
        app.getStateManager().detach(this);
        app.getStateManager().attach(new CountdownState());
    }

    private void openSettings() {
        // TODO: résolution, volume, contrôles
        System.out.println("[Menu] Settings : pas encore implémenté");
    }

    private void leave() {
        app.stop();
    }

    // ------------------------------------------------------------------
    // Hover + click

    @Override
    public void update(float tpf) {
        Vector2f m = app.getInputManager().getCursorPosition();
        applyHover(mainButtons, m);
        if (camDropdownOpen) applyHover(camButtons, m);
    }

    private void applyHover(List<ButtonInfo> list, Vector2f m) {
        for (ButtonInfo b : list) {
            ColorRGBA c = b.contains(m.x, m.y) ? b.hoverColor : b.baseColor;
            b.bg.getMaterial().setColor("Color", c);
        }
    }

    private final ActionListener clickListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        Vector2f m = app.getInputManager().getCursorPosition();

        // Priorité au dropdown
        if (camDropdownOpen) {
            for (ButtonInfo b : camButtons) {
                if (b.contains(m.x, m.y)) { b.action.run(); return; }
            }
        }
        for (ButtonInfo b : mainButtons) {
            if (b.contains(m.x, m.y)) { b.action.run(); return; }
        }
    };

    private void registerInput() {
        app.getInputManager().addMapping("menuClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(clickListener, "menuClick");
    }

    // ------------------------------------------------------------------
    // Helpers UI

    private Geometry addQuad(float x, float y, float w, float h, ColorRGBA color, float z) {
        Geometry geo = new Geometry("quad_" + System.nanoTime(), new Quad(w, h));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geo.setMaterial(mat);
        geo.setLocalTranslation(x, y, z);
        menuRoot.attachChild(geo);
        return geo;
    }

    private BitmapText makeText(String content, float size, ColorRGBA color) {
        BitmapText t = new BitmapText(font);
        t.setSize(size);
        t.setText(content);
        t.setColor(color);
        return t;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        app.getGuiNode().detachChild(menuRoot);
        app.getInputManager().removeListener(clickListener);
        if (app.getInputManager().hasMapping("menuClick")) {
            app.getInputManager().deleteMapping("menuClick");
        }
        mainButtons.clear();
        camButtons.clear();
    }
}
