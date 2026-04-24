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

public class GameOverState extends AbstractAppState {

    private SimpleApplication app;

    private final String    title;
    private final ColorRGBA titleColor;
    private final String    subtitle;
    private final String    btn1Label;
    private final Runnable  btn1Action;
    private final String    btn2Label;
    private final Runnable  btn2Action;

    private Node root;
    private int  screenW, screenH;

    private static class BtnInfo {
        float x, y, w, h;
        Geometry  bg;
        ColorRGBA base, hover;
        Runnable  action;

        BtnInfo(float x, float y, float w, float h,
                Geometry bg, ColorRGBA base, Runnable action) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.bg = bg; this.action = action;
            this.base  = base.clone();
            this.hover = new ColorRGBA(
                    Math.min(base.r * 1.4f, 1f),
                    Math.min(base.g * 1.4f, 1f),
                    Math.min(base.b * 1.4f, 1f), base.a);
        }
        boolean contains(float mx, float my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private final List<BtnInfo> buttons = new ArrayList<>();

    public GameOverState(Main mainApp,
                         String title, ColorRGBA titleColor, String subtitle,
                         String btn1Label, Runnable btn1Action,
                         String btn2Label, Runnable btn2Action) {
        this.title      = title;
        this.titleColor = titleColor;
        this.subtitle   = subtitle;
        this.btn1Label  = btn1Label;
        this.btn1Action = btn1Action;
        this.btn2Label  = btn2Label;
        this.btn2Action = btn2Action;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        screenW  = app.getContext().getSettings().getWidth();
        screenH  = app.getContext().getSettings().getHeight();
        buildScreen();
        registerInput();
        this.app.getInputManager().setCursorVisible(true);
    }

    private void buildScreen() {
        root = new Node("gameOverRoot");
        addQuad(0, 0, screenW, screenH, new ColorRGBA(0f, 0f, 0f, 0.82f), 0f);

        CrispLabel titleLbl = label(title, 52f, titleColor);
        titleLbl.setLocalTranslation(screenW / 2f - titleLbl.getWidth() / 2f,
                screenH / 2f + 175f, 1f);
        root.attachChild(titleLbl);

        if (subtitle != null && !subtitle.isEmpty()) {
            CrispLabel subLbl = label(subtitle, 28f, ColorRGBA.White);
            subLbl.setLocalTranslation(screenW / 2f - subLbl.getWidth() / 2f,
                    screenH / 2f + 100f, 1f);
            root.attachChild(subLbl);
        }

        float btnW = 220f, btnH = 52f, gap = 22f;
        float startX = screenW / 2f - (btnW * 2 + gap) / 2f;
        float btnY   = screenH / 2f - 40f;

        addButton(btn1Label, startX,            btnY, btnW, btnH,
                new ColorRGBA(0.10f, 0.50f, 0.10f, 0.95f), btn1Action);
        addButton(btn2Label, startX + btnW + gap, btnY, btnW, btnH,
                new ColorRGBA(0.60f, 0.08f, 0.08f, 0.95f), btn2Action);

        app.getGuiNode().attachChild(root);
    }

    private void addButton(String text, float x, float y, float w, float h,
                           ColorRGBA color, Runnable action) {
        Geometry bg = addQuad(x, y, w, h, color, 0.5f);
        CrispLabel lbl = label(text, 24f, ColorRGBA.White);
        lbl.setLocalTranslation(x + w / 2f - lbl.getWidth() / 2f,
                y + h / 2f - lbl.getHeight() / 2f, 1f);
        root.attachChild(lbl);
        buttons.add(new BtnInfo(x, y, w, h, bg, color, action));
    }

    @Override
    public void update(float tpf) {
        Vector2f m = app.getInputManager().getCursorPosition();
        for (BtnInfo b : buttons)
            b.bg.getMaterial().setColor("Color", b.contains(m.x, m.y) ? b.hover : b.base);
    }

    private final ActionListener clickListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        Vector2f m = app.getInputManager().getCursorPosition();
        for (BtnInfo b : buttons) if (b.contains(m.x, m.y)) { b.action.run(); return; }
    };

    private void registerInput() {
        app.getInputManager().addMapping("goClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(clickListener, "goClick");
    }

    private Geometry addQuad(float x, float y, float w, float h, ColorRGBA color, float z) {
        Geometry geo = new Geometry("goq_" + System.nanoTime(), new Quad(w, h));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geo.setMaterial(mat);
        geo.setLocalTranslation(x, y, z);
        root.attachChild(geo);
        return geo;
    }

    private CrispLabel label(String text, float h, ColorRGBA color) {
        CrispLabel l = new CrispLabel(app.getAssetManager(), h, color);
        l.setText(text);
        return l;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        app.getGuiNode().detachChild(root);
        app.getInputManager().removeListener(clickListener);
        if (app.getInputManager().hasMapping("goClick"))
            app.getInputManager().deleteMapping("goClick");
        app.getInputManager().setCursorVisible(false);
        buttons.clear();
    }
}
