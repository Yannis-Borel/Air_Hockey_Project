package projet.M1;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;

/**
 * Point d'entrée principal du jeu Air Hockey.
 *
 * Architecture globale :
 *   - Main hérite de SimpleApplication (JME3)
 *   - La boucle de jeu principale est gérée par simpleUpdate(float tpf)
 *   - Les entités (table, raquettes, rondelle) seront initialisées dans simpleInitApp()
 *   - Le GameState orchestrera les règles et l'état courant de la partie
 *
 * Projet M1 Informatique - Université de Toulon
 * Module : Vision par Ordinateur - Pr. Julien SEINTURIER
 */
public class Main extends SimpleApplication {

    // TODO: injecter le GameState ici quand il sera créé

    public static void main(String[] args) {
        Main app = new Main();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Air Hockey - M1 Informatique - Université de Toulon");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setFrameRate(60);
        settings.setSamples(4);         // anti-aliasing x4
        settings.setVSync(true);
        settings.setFullscreen(false);

        app.setSettings(settings);
        app.setShowSettings(false);     // pas de dialog de config au démarrage
        app.setPauseOnLostFocus(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Désactiver la caméra de survol par défaut de JME
        flyCam.setEnabled(false);

        // Vue 3/4 isométrique : au-dessus et légèrement en retrait
        cam.setLocation(new Vector3f(0f, 18f, 8f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        // Fond bleu très sombre, style arène
        viewPort.setBackgroundColor(new ColorRGBA(0.05f, 0.05f, 0.1f, 1f));

        System.out.println("=== Air Hockey - JME3 initialisé ===");
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Boucle principale — sera remplie au fur et à mesure
        // tpf = time per frame (secondes), pour des déplacements frame-rate indépendants
    }
}
