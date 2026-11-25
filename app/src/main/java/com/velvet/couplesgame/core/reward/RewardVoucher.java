package com.velvet.couplesgame.core.reward;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RewardVoucher {
    public final String id;
    public final String sourceContractId;
    public final String title;
    public final String description;
    public final String winnerName;
    public final int tier;
    public final List<String> tags;
    public final long createdAtMillis;
    public final long redeemedAtMillis;

    RewardVoucher(String id, String sourceContractId, String title, String description,
                  String winnerName, int tier, List<String> tags, long createdAtMillis,
                  long redeemedAtMillis) {
        this.id = id;
        this.sourceContractId = sourceContractId;
        this.title = title;
        this.description = description;
        this.winnerName = winnerName;
        this.tier = tier;
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
        this.createdAtMillis = createdAtMillis;
        this.redeemedAtMillis = redeemedAtMillis;
    }

    public boolean isRedeemed() {
        return redeemedAtMillis > 0L;
    }
}
