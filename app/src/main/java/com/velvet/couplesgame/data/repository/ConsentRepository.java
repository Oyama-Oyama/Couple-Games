package com.velvet.couplesgame.data.repository;

import androidx.lifecycle.LiveData;
import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.model.BoundaryStatus;
import com.velvet.couplesgame.data.local.dao.BoundaryDao;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Manages persisted Consent-First boundary rules and keeps in-memory ConsentFilter synchronized.
 */
public class ConsentRepository {
    private static final List<BoundaryRuleEntity> DEFAULT_RULES = Arrays.asList(
            new BoundaryRuleEntity("deep_talk", "YES", "YES"),
            new BoundaryRuleEntity("playful", "YES", "YES"),
            new BoundaryRuleEntity("eye_contact", "YES", "YES"),
            new BoundaryRuleEntity("physical_light", "YES", "YES"),
            new BoundaryRuleEntity("bite_lick", "YES", "YES"),
            new BoundaryRuleEntity("clothing_dare", "SOFT_LIMIT", "SOFT_LIMIT"),
            new BoundaryRuleEntity("restraint_light", "SOFT_LIMIT", "SOFT_LIMIT"),
            new BoundaryRuleEntity("blindfold", "SOFT_LIMIT", "SOFT_LIMIT"),
            new BoundaryRuleEntity("roleplay", "SOFT_LIMIT", "SOFT_LIMIT"),
            new BoundaryRuleEntity("alcohol", "SOFT_LIMIT", "SOFT_LIMIT"),
            new BoundaryRuleEntity("public_space", "HARD_NO", "HARD_NO")
    );

    private final BoundaryDao boundaryDao;
    private final ConsentFilter consentFilter;
    private final ExecutorService ioExecutor;

    public ConsentRepository(BoundaryDao boundaryDao, ConsentFilter consentFilter, ExecutorService ioExecutor) {
        this.boundaryDao = boundaryDao;
        this.consentFilter = consentFilter;
        this.ioExecutor = ioExecutor;
    }

    public void initializeAsync() {
        ioExecutor.execute(() -> {
            boundaryDao.insertDefaults(DEFAULT_RULES);
            consentFilter.replaceRules(boundaryDao.getAllSync());
        });
    }

    public LiveData<List<BoundaryRuleEntity>> observeRules() {
        return boundaryDao.observeAll();
    }

    public void cyclePlayerStatus(BoundaryRuleEntity current, boolean isPlayerA) {
        ioExecutor.execute(() -> {
            String nextA = isPlayerA
                    ? BoundaryStatus.fromString(current.playerAStatus).next().name()
                    : current.playerAStatus;
            String nextB = !isPlayerA
                    ? BoundaryStatus.fromString(current.playerBStatus).next().name()
                    : current.playerBStatus;

            boundaryDao.upsert(new BoundaryRuleEntity(current.tagKey, nextA, nextB));
            consentFilter.replaceRules(boundaryDao.getAllSync());
        });
    }
}
