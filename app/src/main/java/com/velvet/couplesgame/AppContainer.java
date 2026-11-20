package com.velvet.couplesgame;

import android.content.Context;
import com.velvet.couplesgame.core.billing.EntitlementManager;
import com.velvet.couplesgame.core.billing.PlayBillingBridge;
import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.ProceduralTaskGenerator;
import com.velvet.couplesgame.core.engine.TaskEngine;
import com.velvet.couplesgame.core.i18n.AssetContentLoader;
import com.velvet.couplesgame.core.profile.CoupleProfileManager;
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
    public final ConsentFilter consentFilter;
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
        this.consentFilter = new ConsentFilter();
        this.proceduralGenerator = new ProceduralTaskGenerator(assetLoader);
        this.taskEngine = new TaskEngine(consentFilter, proceduralGenerator);
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
