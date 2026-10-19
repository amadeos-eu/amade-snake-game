package eu.amadeos.games.snake.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.games.snake.ui.game.SnakeGame;
import eu.amadeos.games.snake.ui.main.SnakePresenter;
import eu.amadeos.games.snake.ui.main.SnakeWindow;

public final class AmadeSnakeApplication {
    public static final AppInfo APP = new AppInfo("amade-snake-game", "Snake", "snake");

    private AmadeSnakeApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeSnakeApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    // --8<-- [start:main]
    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            Mixer mixer = injector.getInstance(Mixer.class);
            mixer.start();
            SnakeWindow window = injector.getInstance(SnakeWindow.class);
            SnakePresenter presenter = injector.getInstance(SnakePresenter.class);
            SnakeGame game = injector.getInstance(SnakeGame.class);
            window.bind(presenter);
            presenter.start();
            window.whenClosed(() -> {
                game.stop();
                mixer.close();
                System.exit(0);
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
            game.start(window.canvas());
        }, new SnakeModule());
    }
    // --8<-- [end:main]
}
