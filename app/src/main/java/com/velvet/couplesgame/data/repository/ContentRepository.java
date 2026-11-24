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

    public interface FateCubesCallback {
        void onReady(AssetContentLoader.FateCubesBundleDto bundle);
    }

    public interface ScratchCardsCallback {
        void onReady(AssetContentLoader.ScratchCardsRootDto bundle);
    }

    public interface RoleplayCallback {
        void onReady(AssetContentLoader.RoleplayRootDto bundle);
    }

    public interface CoupleQuizCallback {
        void onReady(AssetContentLoader.QuizRootDto bundle);
    }

    public interface PartyDuelCallback {
        void onReady(AssetContentLoader.PartyDuelRootDto bundle);
    }

    public interface IntimateModesCallback {
        void onReady(AssetContentLoader.IntimateModesBundleDto bundle);
    }

    public interface ExtremeModesCallback {
        void onReady(AssetContentLoader.ExtremeModesBundleDto bundle);
    }

    public interface DatePlannerCallback {
        void onReady(List<com.velvet.couplesgame.core.dateplanner.DatePlannerEngine.Activity> activities);
    }

    public interface GeneratorBundleCallback {
        void onReady(AssetContentLoader.GeneratorBundleDto bundle);
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

    public void loadScratchCardsAsync(ScratchCardsCallback callback) {
        ioExecutor.execute(() -> {
            AssetContentLoader.ScratchCardsRootDto bundle = assetLoader.loadScratchCards();
            if (callback != null) callback.onReady(bundle);
        });
    }

    public void loadRoleplayAsync(RoleplayCallback callback) {
        ioExecutor.execute(() -> {
            AssetContentLoader.RoleplayRootDto bundle = assetLoader.loadRoleplayScenarios();
            if (callback != null) callback.onReady(bundle);
        });
    }

    public void loadCoupleQuizAsync(CoupleQuizCallback callback) {
        ioExecutor.execute(() -> {
            AssetContentLoader.QuizRootDto bundle = assetLoader.loadCoupleQuiz();
            if (callback != null) callback.onReady(bundle);
        });
    }

    public void loadExtremeModesAsync(ExtremeModesCallback callback) {
        ioExecutor.execute(() -> {
            AssetContentLoader.ExtremeModesBundleDto bundle = assetLoader.loadExtremeModes();
            if (callback != null) callback.onReady(bundle);
        });
    }

    /** Load the localized date-planner activity pool. */
    public void loadDatePlannerAsync(DatePlannerCallback callback) {
        ioExecutor.execute(() -> {
            AssetContentLoader.DatePlannerRootDto root = assetLoader.loadDatePlanner();
            List<com.velvet.couplesgame.core.dateplanner.DatePlannerEngine.Activity> activities =
                    new ArrayList<>();
            if (root != null && root.activities != null) {
                for (AssetContentLoader.DateActivityDto dto : root.activities) {
                    if (dto == null || dto.id == null) continue;
                    activities.add(new com.velvet.couplesgame.core.dateplanner.DatePlannerEngine.Activity(
                            dto.id,
                            dto.category,
                            dto.tier,
                            dto.durationMin,
                            dto.tags,
                            dto.title,
                            dto.detail
                    ));
                }
            }
            if (callback != null) callback.onReady(activities);
        });
    }

    public void loadGeneratorBundleAsync(GeneratorBundleCallback callback) {
        ioExecutor.execute(() -> {
            AssetContentLoader.GeneratorBundleDto bundle = assetLoader.loadGeneratorBundle();
            if (callback != null) callback.onReady(bundle);
        });
    }

    public void loadIntimateModesAsync(IntimateModesCallback callback) {
        ioExecutor.execute(() -> {
            AssetContentLoader.IntimateModesBundleDto bundle = assetLoader.loadIntimateModes();
            if (callback != null) callback.onReady(bundle);
        });
    }

    public void loadPartyDuelAsync(PartyDuelCallback callback) {
        ioExecutor.execute(() -> {
            AssetContentLoader.PartyDuelRootDto bundle = assetLoader.loadPartyDuel();
            if (callback != null) callback.onReady(bundle);
        });
    }

    public void loadFateCubesAsync(FateCubesCallback callback) {
        ioExecutor.execute(() -> {
            AssetContentLoader.FateCubesBundleDto bundle = assetLoader.loadFateCubesBundle();
            if (callback != null) callback.onReady(bundle);
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
