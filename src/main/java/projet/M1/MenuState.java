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

public class MenuState extends AbstractAppState {

    private SimpleApplication app;
    private Node menuRoot;
    private int  screenW, screenH;

    private static class ButtonInfo {
        float x, y, w, h;
        Runnable  action;
        Geometry  bg;
        ColorRGBA base, hover;

        ButtonInfo(float x, float y, float w, float h,
                   Runnable action, Geometry bg, ColorRGBA base) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.action = action; this.bg = bg;
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

    private final List<ButtonInfo> mainButtons = new ArrayList<>();

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (SimpleApplication) app;
        screenW = app.getContext().getSettings().getWidth();
        screenH = app.getContext().getSettings().getHeight();
        buildMenu();
        registerInput();
        this.app.getInputManager().setCursorVisible(true);
    }

    private void buildMenu() {
        menuRoot = new Node("menuRoot");
        addQuad(0, 0, screenW, screenH, new ColorRGBA(0f, 0f, 0f, 0.65f), 0f);

        CrispLabel title = label("PAUSE", 44f, ColorRGBA.White);
        title.setLocalTranslation(screenW / 2f - title.getWidth() / 2f,
                screenH / 2f + 175f, 1f);
        menuRoot.attachChild(title);

        float btnW = 250f, btnH = 50f;
        float cx   = screenW / 2f - btnW / 2f;
        float topY = screenH / 2f + 80f;
        float gap  = 68f;

        addBtn("Resume", cx, topY,         btnW, btnH,
                new ColorRGBA(0.1f, 0.5f, 0.1f, 0.95f),   this::resume);
        addBtn("Leave",  cx, topY - gap,   btnW, btnH,
                new ColorRGBA(0.6f, 0.08f, 0.08f, 0.95f),  this::leave);

        app.getGuiNode().attachChild(menuRoot);
    }

    private void addBtn(String text, float x, float y, float w, float h,
                        ColorRGBA color, Runnable action) {
        Geometry bg = addQuad(x, y, w, h, color, 0.5f);
        CrispLabel lbl = label(text, 24f, ColorRGBA.White);
        lbl.setLocalTranslation(x + w / 2f - lbl.getWidth() / 2f,
                y + h / 2f - lbl.getHeight() / 2f, 1f);
        menuRoot.attachChild(lbl);
        mainButtons.add(new ButtonInfo(x, y, w, h, action, bg, color));
    }

    void resume() {
        app.getStateManager().detach(this);
        app.getStateManager().attach(new CountdownState((Main) app));
    }

    private void leave() { ((Main) app).returnToMainMenu(); }

    @Override
    public void update(float tpf) {
        Vector2f m = app.getInputManager().getCursorPosition();
        for (ButtonInfo b : mainButtons)
            b.bg.getMaterial().setColor("Color", b.contains(m.x, m.y) ? b.hover : b.base);
    }

    private final ActionListener clickListener = (name, isPressed, tpf) -> {
        if (!isPressed) return;
        Vector2f m = app.getInputManager().getCursorPosition();
        for (ButtonInfo b : mainButtons) if (b.contains(m.x, m.y)) { b.action.run(); return; }
    };

    private void registerInput() {
        app.getInputManager().addMapping("menuClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(clickListener, "menuClick");
    }

    private Geometry addQuad(float x, float y, float w, float h, ColorRGBA color, float z) {
        Geometry geo = new Geometry("mq_" + System.nanoTime(), new Quad(w, h));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geo.setMaterial(mat);
        geo.setLocalTranslation(x, y, z);
        menuRoot.attachChild(geo);
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
        app.getGuiNode().detachChild(menuRoot);
        app.getInputManager().removeListener(clickListener);
        if (app.getInputManager().hasMapping("menuClick"))
            app.getInputManager().deleteMapping("menuClick");
        mainButtons.clear();
    }
}
