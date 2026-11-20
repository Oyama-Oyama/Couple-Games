package com.velvet.couplesgame.data.repository;

import androidx.lifecycle.LiveData;
import com.velvet.couplesgame.core.i18n.AssetContentLoader;
import com.velvet.couplesgame.core.model.DeckMeta;
import com.velvet.couplesgame.core.model.DrawWord;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.model.TaskType;
import com.velvet.couplesgame.data.local.dao.BlindMatchDao;
import com.velvet.couplesgame.data.local.dao.CustomTaskDao;
import com.velvet.couplesgame.data.local.dao.FavoriteTaskDao;
import com.velvet.couplesgame.data.local.dao.SessionStatDao;
import com.velvet.couplesgame.data.local.entity.BlindMatchEntity;
import com.velvet.couplesgame.data.local.entity.CustomTaskEntity;
import com.velvet.couplesgame.data.local.entity.FavoriteTaskEntity;
import com.velvet.couplesgame.data.local.entity.SessionStatEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Unifies localized official JSON cards, Desire Match decks, custom cards,
 * favorites, mutual Blind Matches, and session statistics.
 */
public class ContentRepository {
    public interface PoolCallback {
        void onReady(List<TaskCard> allCards, List<TaskCard> mutualMatches);
    }

    public interface CardsCallback {
        void onReady(List<TaskCard> cards);
    }

    public interface DrawWordsCallback {
        void onReady(List<DrawWord> words);
    }

    public interface BooleanCallback {
        void onResult(boolean value);
    }

    private final AssetContentLoader assetLoader;
    private final CustomTaskDao customTaskDao;
    private final FavoriteTaskDao favoriteTaskDao;
    private final BlindMatchDao blindMatchDao;
    private final SessionStatDao sessionStatDao;
    private final ExecutorService ioExecutor;

    public ContentRepository(AssetContentLoader assetLoader,
                             CustomTaskDao customTaskDao,
                             FavoriteTaskDao favoriteTaskDao,
                             BlindMatchDao blindMatchDao,
                             SessionStatDao sessionStatDao,
                             ExecutorService ioExecutor) {
        this.assetLoader = assetLoader;
        this.customTaskDao = customTaskDao;
        this.favoriteTaskDao = favoriteTaskDao;
        this.blindMatchDao = blindMatchDao;
        this.sessionStatDao = sessionStatDao;
        this.ioExecutor = ioExecutor;
    }

    public void loadMergedPoolAsync(PoolCallback callback) {
        ioExecutor.execute(() -> {
            List<TaskCard> merged = new ArrayList<>();
            List<TaskCard> allIndexSource = new ArrayList<>(assetLoader.loadAllOfficialCards());

            for (TaskCard c : allIndexSource) {
                if (!TaskType.DESIRE_MATCH.equals(c.type)) {
                    merged.add(c);
                }
            }

            List<CustomTaskEntity> customEntities = customTaskDao.getAllSync();
            for (CustomTaskEntity e : customEntities) {
                TaskCard customCard = new TaskCard(
                        e.id, e.deckId, e.tier, e.type, e.tags, e.durationSec, e.text, false, true
                );
                merged.add(customCard);
                allIndexSource.add(customCard);
            }

            Map<String, TaskCard> cardIndex = new HashMap<>();
            for (TaskCard c : allIndexSource) {
                cardIndex.put(c.id, c);
            }

            List<TaskCard> mutualBoost = new ArrayList<>();
            for (BlindMatchEntity match : blindMatchDao.getMutualMatchesSync()) {
                TaskCard found = cardIndex.get(match.cardId);
                if (found != null) {
                    mutualBoost.add(found);
                }
            }

            if (callback != null) {
                callback.onReady(merged, mutualBoost);
            }
        });
    }

    public void loadDrawWordsAsync(DrawWordsCallback callback) {
        ioExecutor.execute(() -> {
            List<DrawWord> words = assetLoader.loadDrawWords();
            if (callback != null) {
                callback.onReady(words);
            }
        });
    }

    public void loadDesireMatchCardsAsync(CardsCallback callback) {
        ioExecutor.execute(() -> {
            List<TaskCard> desireCards = new ArrayList<>();
            for (DeckMeta deck : assetLoader.loadDecks()) {
                for (TaskCard card : deck.cards) {
                    if (TaskType.DESIRE_MATCH.equals(card.type)) {
                        desireCards.add(card);
                    }
                }
            }
            if (callback != null) {
                callback.onReady(desireCards);
            }
        });
    }

    public void saveBlindMatchResults(List<BlindMatchEntity> results, Runnable onComplete) {
        ioExecutor.execute(() -> {
            if (results != null) {
                for (BlindMatchEntity entity : results) {
                    blindMatchDao.upsert(entity);
                }
            }
            if (onComplete != null) onComplete.run();
        });
    }

    public void clearBlindMatches(Runnable onComplete) {
        ioExecutor.execute(() -> {
            blindMatchDao.clearAll();
            if (onComplete != null) onComplete.run();
        });
    }

    public LiveData<List<BlindMatchEntity>> observeMutualMatches() {
        return blindMatchDao.observeMutualMatches();
    }

    public void recordSessionStat(String gameMode, int playerACompleted, int playerBCompleted, int skipsUsed) {
        ioExecutor.execute(() -> sessionStatDao.insert(new SessionStatEntity(
                gameMode,
                playerACompleted,
                playerBCompleted,
                skipsUsed,
                System.currentTimeMillis()
        )));
    }

    public LiveData<List<FavoriteTaskEntity>> observeFavorites() {
        return favoriteTaskDao.observeAll();
    }

    public void removeFavorite(String cardId) {
        ioExecutor.execute(() -> favoriteTaskDao.deleteById(cardId));
    }

    public LiveData<List<SessionStatEntity>> observeSessionStats() {
        return sessionStatDao.observeHistory();
    }

    public void clearSessionHistory(Runnable onComplete) {
        ioExecutor.execute(() -> {
            sessionStatDao.clearAll();
            if (onComplete != null) onComplete.run();
        });
    }

    public LiveData<List<CustomTaskEntity>> observeCustomTasks() {
        return customTaskDao.observeAll();
    }

    public void addCustomTask(String text, int tier, String type) {
        ioExecutor.execute(() -> {
            String id = "custom_" + UUID.randomUUID().toString().substring(0, 8);
            CustomTaskEntity entity = new CustomTaskEntity(
                    id,
                    "custom_deck",
                    tier,
                    type != null ? type : TaskType.DARE,
                    Collections.singletonList("custom"),
                    0,
                    text.trim(),
                    System.currentTimeMillis()
            );
            customTaskDao.upsert(entity);
        });
    }

    public void deleteCustomTask(String id) {
        ioExecutor.execute(() -> customTaskDao.deleteById(id));
    }

    public void isFavoriteAsync(String cardId, BooleanCallback callback) {
        ioExecutor.execute(() -> {
            boolean saved = favoriteTaskDao.isFavoriteSync(cardId);
            if (callback != null) callback.onResult(saved);
        });
    }

    public void toggleFavorite(TaskCard card, BooleanCallback callback) {
        ioExecutor.execute(() -> {
            boolean exists = favoriteTaskDao.isFavoriteSync(card.id);
            if (exists) {
                favoriteTaskDao.deleteById(card.id);
                if (callback != null) callback.onResult(false);
            } else {
                favoriteTaskDao.upsert(new FavoriteTaskEntity(
                        card.id, card.text, card.tier, System.currentTimeMillis()
                ));
                if (callback != null) callback.onResult(true);
            }
        });
    }
}
