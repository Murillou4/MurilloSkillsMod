package com.murilloskills.core.platform;

import com.murilloskills.core.network.SkillSyncSnapshot;
import com.murilloskills.core.network.XpGainMessage;

public interface ClientUiPort {
    void updateSkillScreen(SkillSyncSnapshot snapshot);

    void showXpGain(XpGainMessage message);

    void openSkillsScreen();
}
