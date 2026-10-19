package eu.amadeos.games.snake.domain;

/** How a game ended: the points and the snake's length; {@code full} if it filled the whole field. */
public record GameResult(int score, int length, boolean full) {
}
