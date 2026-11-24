package com.velvet.couplesgame;

import android.content.Context;
import com.velvet.couplesgame.core.billing.EntitlementManager;
import com.velvet.couplesgame.core.billing.PlayBillingBridge;
import com.velvet.couplesgame.core.dateplanner.DatePlanManager;
import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.ModePromptSynthesizer;
import com.velvet.couplesgame.core.engine.ProceduralTaskGenerator;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.engine.TaskEngine;
import com.velvet.couplesgame.core.i18n.AssetContentLoader;
import com.velvet.couplesgame.core.journal.TonightNoteManager;
import com.velvet.couplesgame.core.letter.SealedLetterManager;
import com.velvet.couplesgame.core.profile.AgeGateManager;
import com.velvet.couplesgame.core.profile.CoupleProfileManager;
import com.velvet.couplesgame.core.profile.MoodCheckinManager;
import com.velvet.couplesgame.core.theme.ThemeManager;
import com.velvet.couplesgame.data.local.AppDatabase;
import com.velvet.couplesgame.data.repository.ConsentRepository;
import com.velvet.couplesgame.data.repository.ContentRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppContainer {
    public final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
    public final AppDatabase database;
    public final AssetContentLoader assetLoader;
    public final ThemeManager themeManager;
    public final CoupleProfileManager profileManager;
    public final AgeGateManager ageGateManager;
    public final TonightNoteManager tonightNoteManager;
    public final MoodCheckinManager moodCheckinManager;
    public final DatePlanManager datePlanManager;
    public final SealedLetterManager letterManager;
    public final ConsentFilter consentFilter;
    public final PromptHistoryStore promptHistory;
    public final ModePromptSynthesizer modeSynthesizer;
    public final ProceduralTaskGenerator proceduralGenerator;
    public final TaskEngine taskEngine;
    public final ConsentRepository consentRepository;
    public final ContentRepository contentRepository;
    public final EntitlementManager entitlementManager;
    public final PlayBillingBridge billingBridge;

    public AppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        this.database = AppDatabase.create(appContext);
        this.assetLoader = new AssetContentLoader(appContext);
        this.themeManager = new ThemeManager(appContext);
        this.profileManager = new CoupleProfileManager(appContext);
        this.ageGateManager = new AgeGateManager(appContext);
        this.tonightNoteManager = new TonightNoteManager(appContext);
        this.moodCheckinManager = new MoodCheckinManager(appContext);
        this.datePlanManager = new DatePlanManager(appContext);
        this.letterManager = new SealedLetterManager(appContext);
        this.consentFilter = new ConsentFilter();
        this.promptHistory = new PromptHistoryStore(appContext);
        this.modeSynthesizer = new ModePromptSynthesizer();
        this.proceduralGenerator = new ProceduralTaskGenerator(assetLoader);
        this.proceduralGenerator.attachHistoryStore(promptHistory);
        this.taskEngine = new TaskEngine(consentFilter, proceduralGenerator);
        this.taskEngine.attachHistoryStore(promptHistory);
        this.consentRepository = new ConsentRepository(database.boundaryDao(), consentFilter, ioExecutor);
        this.contentRepository = new ContentRepository(
                assetLoader,
                database.customTaskDao(),
                database.favoriteTaskDao(),
                database.blindMatchDao(),
                database.sessionStatDao(),
                ioExecutor
        );
        this.entitlementManager = new EntitlementManager(appContext);
        this.billingBridge = new PlayBillingBridge(appContext, entitlementManager);

        this.consentRepository.initializeAsync();
    }
}
