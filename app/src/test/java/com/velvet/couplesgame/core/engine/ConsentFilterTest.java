package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class ConsentFilterTest {
    @Test
    public void softLimitsRequireAWarningButRemainEligible() {
        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Arrays.asList(
                new BoundaryRuleEntity("roleplay", "SOFT_LIMIT", "ALLOWED"),
                new BoundaryRuleEntity("clothing_dare", "HARD_NO", "ALLOWED")
        ));

        assertTrue(filter.isAllowed(Collections.singletonList("roleplay")));
        assertTrue(filter.hasSoftLimit(Arrays.asList("roleplay", "other")));
        assertFalse(filter.isAllowed(Collections.singletonList("clothing_dare")));
        assertFalse(filter.hasSoftLimit(Collections.singletonList("clothing_dare")));
    }
}
