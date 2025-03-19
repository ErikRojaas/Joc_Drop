package io.github.some_example_name;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main implements ApplicationListener {
    Texture backgroundTexture, gameOverTexture, restartTexture;
    Texture bucketTexture, dropTexture;
    Sound dropSound, gameOverSound;
    Music music;
    SpriteBatch spriteBatch;
    FitViewport viewport;
    Sprite bucketSprite;
    Vector2 touchPos;
    Array<Sprite> dropSprites;
    float dropTimer;
    Rectangle bucketRectangle, dropRectangle;
    int score, lives, maxScore;
    boolean gameOver;
    GlyphLayout layout;
    private Rectangle restartButtonBounds;
    BitmapFont font;

    @Override
    public void create() {
        backgroundTexture = new Texture("background.png");
        gameOverTexture = new Texture("gameover.png");
        bucketTexture = new Texture("bucket.png");
        dropTexture = new Texture("drop.png");
        restartTexture = new Texture("restart.png");
        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
        gameOverSound = Gdx.audio.newSound(Gdx.files.internal("gameover.mp3"));
        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));
        spriteBatch = new SpriteBatch();
        viewport = new FitViewport(8, 5);
        bucketSprite = new Sprite(bucketTexture);
        bucketSprite.setSize(1, 1);
        touchPos = new Vector2();
        dropSprites = new Array<>();
        bucketRectangle = new Rectangle();
        dropRectangle = new Rectangle();
        layout = new GlyphLayout();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() + 0.05f);

        score = 0;
        lives = 3;
        gameOver = false;
        maxScore = 0; // Inicializa el máximo en 0

        restartButtonBounds = new Rectangle(3.5f, 1f, 1f, 0.5f);

        music.setLooping(true);
        music.setVolume(.5f);
        music.play();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        if (gameOver) {
            if (Gdx.input.justTouched()) {
                touchPos.set(Gdx.input.getX(), Gdx.input.getY());
                viewport.unproject(touchPos);
                if (restartButtonBounds.contains(touchPos)) {
                    restartGame();
                }
            }
        } else {
            input();
            logic();
        }
        draw();
    }

    private void input() {
        float speed = 4f;
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            bucketSprite.translateX(speed * delta);
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            bucketSprite.translateX(-speed * delta);
        }

        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(touchPos);
            bucketSprite.setCenterX(touchPos.x);
        }
    }

    private void logic() {
        float worldWidth = viewport.getWorldWidth();
        float bucketWidth = bucketSprite.getWidth();

        bucketSprite.setX(MathUtils.clamp(bucketSprite.getX(), 0, worldWidth - bucketWidth));

        float delta = Gdx.graphics.getDeltaTime();
        bucketRectangle.set(bucketSprite.getX(), bucketSprite.getY(), bucketWidth, bucketSprite.getHeight());

        for (int i = dropSprites.size - 1; i >= 0; i--) {
            Sprite dropSprite = dropSprites.get(i);
            dropSprite.translateY(-2f * delta);
            dropRectangle.set(dropSprite.getX(), dropSprite.getY(), dropSprite.getWidth(), dropSprite.getHeight());

            if (dropSprite.getY() < -dropSprite.getHeight()) {
                dropSprites.removeIndex(i);
                lives--;
                if (lives <= 0) {
                    gameOver = true;
                    if (score > maxScore) { // Guarda la mejor puntuación
                        maxScore = score;
                    }
                    dropSprites.clear();
                    music.stop();
                    gameOverSound.play();
                }
            } else if (dropRectangle.overlaps(bucketRectangle) && dropSprite.getY() > bucketSprite.getY()) {
                dropSprites.removeIndex(i);
                dropSound.play();
                score++;
            }
        }

        dropTimer += delta;
        if (dropTimer > 1f) {
            dropTimer = 0;
            createDroplet();
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.DARK_GRAY);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();

        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        spriteBatch.draw(gameOver ? gameOverTexture : backgroundTexture, 0, 0, worldWidth, worldHeight);

        if (!gameOver) {
            bucketSprite.draw(spriteBatch);
            for (Sprite dropSprite : dropSprites) {
                dropSprite.draw(spriteBatch);
            }
            layout.setText(font, "lives: " + lives);
            font.draw(spriteBatch, layout, 0f, worldHeight - 0.2f);

        } else {
            spriteBatch.draw(restartTexture, restartButtonBounds.x, restartButtonBounds.y,
                restartButtonBounds.width, restartButtonBounds.height);

            layout.setText(font, "score: " +  String.valueOf(score));
            font.draw(spriteBatch, layout, 2.5f, 4.5f);
        }

        spriteBatch.end();
    }

    private void createDroplet() {
        Sprite dropSprite = new Sprite(dropTexture);
        dropSprite.setSize(1, 1);
        dropSprite.setX(MathUtils.random(0f, viewport.getWorldWidth() - 1));
        dropSprite.setY(viewport.getWorldHeight());
        dropSprites.add(dropSprite);
    }

    private void restartGame() {
        dropSprites.clear();
        score = 0;
        lives = 3;
        gameOver = false;
        music.play();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        gameOverTexture.dispose();
        bucketTexture.dispose();
        dropTexture.dispose();
        dropSound.dispose();
        gameOverSound.dispose();
        music.dispose();
        spriteBatch.dispose();
        font.dispose();
    }
}
