package eu.amadeos.games.snake.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.games.snake.repository.SnakeSettings;
import eu.amadeos.games.snake.repository.ReplayFile;
import eu.amadeos.games.snake.ui.game.Arena;
import eu.amadeos.games.snake.ui.game.SnakeGame;
import eu.amadeos.games.snake.ui.main.SnakePresenter;
import eu.amadeos.games.snake.ui.main.SnakeView;
import eu.amadeos.games.snake.ui.main.SnakeWindow;

import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * amade-snake-game bindings. Theme, language and the XDG directories come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class SnakeModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "eu.amadeos.games.snake.messages";

    @Override
    protected void configure() {
        bind(SnakeSettings.class).in(Scopes.SINGLETON);
        bind(Keyboard.class).in(Scopes.SINGLETON);
        bind(Mouse.class).in(Scopes.SINGLETON);
        bind(Mixer.class).in(Scopes.SINGLETON);
        bind(SnakeGame.class).in(Scopes.SINGLETON);
        bind(Arena.class).to(SnakeGame.class);
        bind(SnakeWindow.class).in(Scopes.SINGLETON);
        bind(SnakeView.class).to(SnakeWindow.class);
        bind(SnakePresenter.class).in(Scopes.SINGLETON);
    }

    /** The best scores in {@code ~/.local/share/amadeos/games/snake}. */
    @Provides
    @Singleton
    HighScores highScores(XdgDirectories xdg, AppInfo app) {
        return HighScores.of(xdg, app.configName(), Ranking.HIGHER_WINS);
    }

    @Provides
    @Singleton
    PlayerName playerName(AppDirectories directories) {
        return new PlayerName(directories.config(), System.getProperty("user.name"));
    }

    /** The replay of the best game, next to the best scores. */
    @Provides
    @Singleton
    ReplayFile replayFile(HighScores scores) {
        return new ReplayFile(scores.directory().resolve("best.replay"));
    }

    /** Chance for the effects only; the rules make their own from each game's seed. */
    @Provides
    RandomGenerator looks() {
        return new SeededRandom(SeededRandom.freshSeed());
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(SnakeModule.class, MESSAGES, locale);
    }
}
