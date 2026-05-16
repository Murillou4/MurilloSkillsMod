package com.murilloskills.core.platform;

import com.murilloskills.core.network.SkillSyncSnapshot;
import com.murilloskills.core.network.XpGainMessage;

public interface NetworkPort<P> {
    void registerChannels();

    void sendSkillSync(P player, SkillSyncSnapshot snapshot);

    void sendXpGain(P player, XpGainMessage message);
}
