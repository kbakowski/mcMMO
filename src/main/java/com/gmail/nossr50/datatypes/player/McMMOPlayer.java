package com.gmail.nossr50.datatypes.player;

import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.chat.ChatMode;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.experience.XPGainSource;
import com.gmail.nossr50.datatypes.interactions.NotificationType;
import com.gmail.nossr50.datatypes.party.Party;
import com.gmail.nossr50.datatypes.party.PartyTeleportRecord;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.datatypes.skills.ToolType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.runnables.skills.AbilityDisableTask;
import com.gmail.nossr50.runnables.skills.ToolLowerTask;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.skills.acrobatics.AcrobaticsManager;
import com.gmail.nossr50.skills.alchemy.AlchemyManager;
import com.gmail.nossr50.skills.archery.ArcheryManager;
import com.gmail.nossr50.skills.axes.AxesManager;
import com.gmail.nossr50.skills.child.FamilyTree;
import com.gmail.nossr50.skills.excavation.ExcavationManager;
import com.gmail.nossr50.skills.fishing.FishingManager;
import com.gmail.nossr50.skills.herbalism.HerbalismManager;
import com.gmail.nossr50.skills.mining.MiningManager;
import com.gmail.nossr50.skills.repair.RepairManager;
import com.gmail.nossr50.skills.salvage.SalvageManager;
import com.gmail.nossr50.skills.smelting.SmeltingManager;
import com.gmail.nossr50.skills.swords.SwordsManager;
import com.gmail.nossr50.skills.taming.TamingManager;
import com.gmail.nossr50.skills.unarmed.UnarmedManager;
import com.gmail.nossr50.skills.woodcutting.WoodcuttingManager;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.experience.ExperienceBarManager;
import com.gmail.nossr50.util.skills.PerksUtils;
import com.gmail.nossr50.util.skills.RankUtils;
import com.gmail.nossr50.util.sounds.SoundManager;
import com.gmail.nossr50.util.sounds.SoundType;
import org.apache.commons.lang.Validate;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class McMMOPlayer {
    private final mcMMO pluginRef;
    private final Map<PrimarySkillType, SkillManager> skillManagers = new HashMap<>();
    private final Map<SuperAbilityType, Boolean> abilityMode = new HashMap<>();
    private final Map<SuperAbilityType, Boolean> abilityInformed = new HashMap<>();
    private final Map<ToolType, Boolean> toolMode = new HashMap<>();
    private final FixedMetadataValue playerMetadata;
    private Player player;
    private PlayerProfile profile;
    private ExperienceBarManager experienceBarManager;
    private Party party;
    private Party invite;
    private Party allianceInvite;
    private int itemShareModifier;
    private PartyTeleportRecord ptpRecord;
    private boolean partyChatMode;
    private boolean adminChatMode;
    private boolean displaySkillNotifications = true;
    private boolean abilityUse = true;
    private boolean godMode;
    private boolean chatSpy = false; //Off by default
    private int recentlyHurt;
    private int respawnATS;
    private long teleportATS;
    private long databaseATS;
    //private int chimeraWingLastUse;
    private Location teleportCommence;
    private boolean isUsingUnarmed;
    private HashMap<PrimarySkillType, Double> personalXPModifiers;
    private String playerName;

    public McMMOPlayer(Player player, PlayerProfile profile, mcMMO pluginRef) {
        this.pluginRef = pluginRef;
        this.playerName = player.getName();
        UUID uuid = player.getUniqueId();

        this.player = player;
        playerMetadata = new FixedMetadataValue(pluginRef, playerName);
        this.profile = profile;

        if (profile.getUniqueId() == null) {
            profile.setUniqueId(uuid);
        }

        //What was here before initSkillManagers() was worse, trust me
        initSkillManagers();

        for (SuperAbilityType superAbilityType : SuperAbilityType.values()) {
            abilityMode.put(superAbilityType, false);
            abilityInformed.put(superAbilityType, true); // This is intended
        }

        for (ToolType toolType : ToolType.values()) {
            toolMode.put(toolType, false);
        }

        experienceBarManager = new ExperienceBarManager(pluginRef,this);
        fillPersonalXPModifiers(); //Cache players XP rates
    }

    private void initSkillManagers() {
        for(PrimarySkillType primarySkillType : PrimarySkillType.values()) {
            try {
                initManager(primarySkillType);
            } catch (InvalidSkillException e) {
                e.printStackTrace();
            }
        }
    }

    private void initManager(PrimarySkillType primarySkillType) throws InvalidSkillException {
        switch(primarySkillType) {
            case ACROBATICS:
                skillManagers.put(primarySkillType, new AcrobaticsManager(pluginRef, this));
                break;
            case ALCHEMY:
                skillManagers.put(primarySkillType, new AlchemyManager(pluginRef, this));
                break;
            case ARCHERY:
                skillManagers.put(primarySkillType, new ArcheryManager(pluginRef, this));
                break;
            case AXES:
                skillManagers.put(primarySkillType, new AxesManager(pluginRef, this));
                break;
            case EXCAVATION:
                skillManagers.put(primarySkillType, new ExcavationManager(pluginRef, this));
                break;
            case FISHING:
                skillManagers.put(primarySkillType, new FishingManager(pluginRef, this));
                break;
            case HERBALISM:
                skillManagers.put(primarySkillType, new HerbalismManager(pluginRef, this));
                break;
            case MINING:
                skillManagers.put(primarySkillType, new MiningManager(pluginRef, this));
                break;
            case REPAIR:
                skillManagers.put(primarySkillType, new RepairManager(pluginRef, this));
                break;
            case SALVAGE:
                skillManagers.put(primarySkillType, new SalvageManager(pluginRef, this));
                break;
            case SMELTING:
                skillManagers.put(primarySkillType, new SmeltingManager(pluginRef, this));
                break;
            case SWORDS:
                skillManagers.put(primarySkillType, new SwordsManager(pluginRef, this));
                break;
            case TAMING:
                skillManagers.put(primarySkillType, new TamingManager(pluginRef, this));
                break;
            case UNARMED:
                skillManagers.put(primarySkillType, new UnarmedManager(pluginRef, this));
                break;
            case WOODCUTTING:
                skillManagers.put(primarySkillType, new WoodcuttingManager(pluginRef, this));
                break;
            default:
                throw new InvalidSkillException("The skill named has no manager! Contact the devs!");
        }
    }

    /**
     * Grabs custom XP values for a player if they exist, if they don't defaults them to 1.0
     * Values are stored in a hash map for constant speed lookups
     */
    private void fillPersonalXPModifiers() {
        personalXPModifiers = new HashMap<>();

        //Check each skill for custom XP perks
        for (PrimarySkillType primarySkillType : PrimarySkillType.values()) {
            //Skip over child skills
            if (pluginRef.getSkillTools().isChildSkill(primarySkillType))
                continue;

            //Set the players custom XP modifier, defaults to 1.0D on missing entries
            personalXPModifiers.put(primarySkillType, pluginRef.getPlayerLevelUtils().determineXpPerkValue(player, primarySkillType));
        }
    }

    /**
     * Gets a players current active XP rate for a specific skill
     * This will default to 1.0D unless over written by custom XP perks
     *
     * @param primarySkillType target primary skill
     * @return this players personal XP multiplier for target PrimarySkillType
     */
    public Double getPlayerSpecificXPMult(PrimarySkillType primarySkillType) {
        return personalXPModifiers.get(primarySkillType);
    }

    public String getPlayerName() {
        return playerName;
    }

    /*public void hideXpBar(PrimarySkillType primarySkillType)
    {
        experienceBarManager.hideExperienceBar(primarySkillType);
    }*/

    public void processPostXpEvent(PrimarySkillType primarySkillType, Plugin plugin, XPGainSource xpGainSource)
    {
        //Check if they've reached the power level cap just now
        if(hasReachedPowerLevelCap()) {
            pluginRef.getNotificationManager().sendPlayerInformationChatOnly(player, "LevelCap.PowerLevel", String.valueOf(pluginRef.getConfigManager().getConfigLeveling().getPowerLevelCap()));
        } else if(hasReachedLevelCap(primarySkillType)) {
            pluginRef.getNotificationManager().sendPlayerInformationChatOnly(player, "LevelCap.Skill",
                    String.valueOf(pluginRef.getConfigManager().getConfigLeveling().getSkillLevelCap(primarySkillType)),
                    pluginRef.getSkillTools().getLocalizedSkillName(primarySkillType));
        }

        //Updates from Party sources
        if (xpGainSource == XPGainSource.PARTY_MEMBERS && !pluginRef.getConfigManager().getConfigLeveling().isPartyExperienceTriggerXpBarDisplay())
            return;

        //Updates from passive sources (Alchemy, Smelting, etc...)
        if (xpGainSource == XPGainSource.PASSIVE && !pluginRef.getConfigManager().getConfigLeveling().isPassiveGainXPBars())
            return;

        updateXPBar(primarySkillType, plugin);
    }

    public void processUnlockNotifications(mcMMO plugin, PrimarySkillType primarySkillType, int skillLevel) {
        RankUtils.executeSkillUnlockNotifications(plugin, this, primarySkillType, skillLevel);
    }

    public void updateXPBar(PrimarySkillType primarySkillType, Plugin plugin) {
        //Skill Unlock Notifications

        if (pluginRef.getSkillTools().isChildSkill(primarySkillType))
            return;

        //XP BAR UPDATES
        experienceBarManager.updateExperienceBar(primarySkillType, plugin);
    }

    public double getProgressInCurrentSkillLevel(PrimarySkillType primarySkillType) {
        double currentXP = profile.getSkillXpLevel(primarySkillType);
        double maxXP = profile.getXpToLevel(primarySkillType);

        return (currentXP / maxXP);
    }

    public AcrobaticsManager getAcrobaticsManager() {
        return (AcrobaticsManager) skillManagers.get(PrimarySkillType.ACROBATICS);
    }

    public AlchemyManager getAlchemyManager() {
        return (AlchemyManager) skillManagers.get(PrimarySkillType.ALCHEMY);
    }

    public ArcheryManager getArcheryManager() {
        return (ArcheryManager) skillManagers.get(PrimarySkillType.ARCHERY);
    }

    public AxesManager getAxesManager() {
        return (AxesManager) skillManagers.get(PrimarySkillType.AXES);
    }

    public ExcavationManager getExcavationManager() {
        return (ExcavationManager) skillManagers.get(PrimarySkillType.EXCAVATION);
    }

    public FishingManager getFishingManager() {
        return (FishingManager) skillManagers.get(PrimarySkillType.FISHING);
    }

    public HerbalismManager getHerbalismManager() {
        return (HerbalismManager) skillManagers.get(PrimarySkillType.HERBALISM);
    }

    public MiningManager getMiningManager() {
        return (MiningManager) skillManagers.get(PrimarySkillType.MINING);
    }

    public RepairManager getRepairManager() {
        return (RepairManager) skillManagers.get(PrimarySkillType.REPAIR);
    }

    public SalvageManager getSalvageManager() {
        return (SalvageManager) skillManagers.get(PrimarySkillType.SALVAGE);
    }

    public SmeltingManager getSmeltingManager() {
        return (SmeltingManager) skillManagers.get(PrimarySkillType.SMELTING);
    }

    public SwordsManager getSwordsManager() {
        return (SwordsManager) skillManagers.get(PrimarySkillType.SWORDS);
    }

    public TamingManager getTamingManager() {
        return (TamingManager) skillManagers.get(PrimarySkillType.TAMING);
    }

    public UnarmedManager getUnarmedManager() {
        return (UnarmedManager) skillManagers.get(PrimarySkillType.UNARMED);
    }

    public WoodcuttingManager getWoodcuttingManager() {
        return (WoodcuttingManager) skillManagers.get(PrimarySkillType.WOODCUTTING);
    }

    /*
     * Abilities
     */

    /**
     * Reset the mode of all abilities.
     */
    public void resetAbilityMode() {
        for (SuperAbilityType ability : SuperAbilityType.values()) {
            // Correctly disable and handle any special deactivate code
            new AbilityDisableTask(pluginRef,this, ability).run();
        }
    }

    /**
     * Get the mode of an ability.
     *
     * @param ability The ability to check
     * @return true if the ability is enabled, false otherwise
     */
    public boolean getAbilityMode(SuperAbilityType ability) {
        return abilityMode.get(ability);
    }

    /**
     * Set the mode of an ability.
     *
     * @param ability  The ability to check
     * @param isActive True if the ability is active, false otherwise
     */
    public void setAbilityMode(SuperAbilityType ability, boolean isActive) {
        abilityMode.put(ability, isActive);
    }

    /**
     * Get the informed state of an ability
     *
     * @param ability The ability to check
     * @return true if the ability is informed, false otherwise
     */
    public boolean getAbilityInformed(SuperAbilityType ability) {
        return abilityInformed.get(ability);
    }

    /**
     * Set the informed state of an ability.
     *
     * @param ability    The ability to check
     * @param isInformed True if the ability is informed, false otherwise
     */
    public void setAbilityInformed(SuperAbilityType ability, boolean isInformed) {
        abilityInformed.put(ability, isInformed);
    }

    /**
     * Get the current prep mode of a tool.
     *
     * @param tool Tool to get the mode for
     * @return true if the tool is prepped, false otherwise
     */
    public boolean getToolPreparationMode(ToolType tool) {
        return toolMode.get(tool);
    }

    public boolean getAbilityUse() {
        return abilityUse;
    }

    public void toggleAbilityUse() {
        abilityUse = !abilityUse;
    }

    /*
     * Tools
     */

    /**
     * Reset the prep modes of all tools.
     */
    public void resetToolPrepMode() {
        for (ToolType tool : ToolType.values()) {
            setToolPreparationMode(tool, false);
        }
    }

    /**
     * Set the current prep mode of a tool.
     *
     * @param tool       Tool to set the mode for
     * @param isPrepared true if the tool should be prepped, false otherwise
     */
    public void setToolPreparationMode(ToolType tool, boolean isPrepared) {
        toolMode.put(tool, isPrepared);
    }

    /*
     * Recently Hurt
     */

    public int getRecentlyHurt() {
        return recentlyHurt;
    }

    public void setRecentlyHurt(int value) {
        recentlyHurt = value;
    }

    public void actualizeRecentlyHurt() {
        recentlyHurt = (int) (System.currentTimeMillis() / Misc.TIME_CONVERSION_FACTOR);
    }

    /*
     * Teleportation cooldown & warmup
     */

    public int getChimeraWingLastUse() {
        return profile.getChimaerWingDATS();
    }

    public void actualizeChimeraWingLastUse() {
        profile.setChimaeraWingDATS((int) (System.currentTimeMillis() / Misc.TIME_CONVERSION_FACTOR));
    }

    public Location getTeleportCommenceLocation() {
        return teleportCommence;
    }

    public void setTeleportCommenceLocation(Location location) {
        teleportCommence = location;
    }

    public void actualizeTeleportCommenceLocation(Player player) {
        teleportCommence = player.getLocation();
    }

    /*
     * Exploit Prevention
     */

    public int getRespawnATS() {
        return respawnATS;
    }

    public void actualizeRespawnATS() {
        respawnATS = (int) (System.currentTimeMillis() / Misc.TIME_CONVERSION_FACTOR);
    }

    public long getTeleportATS() {
        return teleportATS;
    }

    public void actualizeTeleportATS() {
        teleportATS = System.currentTimeMillis() + (pluginRef.getConfigManager().getConfigExploitPrevention().getConfigSectionExploitAcrobatics().getTeleportCooldownSeconds() * 1000);
    }

    public long getDatabaseATS() {
        return databaseATS;
    }

    public void actualizeDatabaseATS() {
        databaseATS = System.currentTimeMillis();
    }

    /*
     * God Mode
     */

    public boolean getGodMode() {
        return godMode;
    }

    public void toggleGodMode() {
        godMode = !godMode;
    }

    /*
     * Party Chat Spy
     */

    public boolean isPartyChatSpying() {
        return chatSpy;
    }

    public void togglePartyChatSpying() {
        chatSpy = !chatSpy;
    }

    /*
     * Skill notifications
     */

    public boolean useChatNotifications() {
        return displaySkillNotifications;
    }

    public void toggleChatNotifications() {
        displaySkillNotifications = !displaySkillNotifications;
    }

    /**
     * Gets the power level of this player.
     *
     * @return the power level of the player
     */
    public int getPowerLevel() {
        int powerLevel = 0;

        for (PrimarySkillType primarySkillType : pluginRef.getSkillTools().NON_CHILD_SKILLS) {
            if (pluginRef.getSkillTools().doesPlayerHaveSkillPermission(primarySkillType, player)) {
                powerLevel += getSkillLevel(primarySkillType);
            }
        }

        return powerLevel;
    }

    /**
     * Whether or not a player is level capped
     * If they are at the power level cap, this will return true, otherwise it checks their skill level
     * @param primarySkillType
     * @return
     */
    public boolean hasReachedLevelCap(PrimarySkillType primarySkillType) {
        if(hasReachedPowerLevelCap())
            return true;

        if(getSkillLevel(primarySkillType) >= pluginRef.getConfigManager().getConfigLeveling().getSkillLevelCap(primarySkillType))
            return true;

        return false;
    }

    /**
     * Whether or not a player is power level capped
     * Compares their power level total to the current set limit
     * @return true if they have reached the power level cap
     */
    public boolean hasReachedPowerLevelCap() {
        return this.getPowerLevel() >= pluginRef.getConfigManager().getConfigLeveling().getPowerLevelCap();
    }

    /**
     * Begins an experience gain. The amount will be affected by skill modifiers, global rate, perks, and may be shared with the party
     *
     * @param primarySkillType Skill being used
     * @param xp    Experience amount to process
     */
    public void beginXpGain(PrimarySkillType primarySkillType, double xp, XPGainReason xpGainReason, XPGainSource xpGainSource) {
        Validate.isTrue(xp >= 0.0, "XP gained should be greater than or equal to zero.");

        if (xp <= 0.0) {
            return;
        }

        if (pluginRef.getSkillTools().isChildSkill(primarySkillType)) {
            Set<PrimarySkillType> parentSkills = FamilyTree.getParents(primarySkillType);
            double splitXp = xp / parentSkills.size();

            for (PrimarySkillType parentSkill : parentSkills) {
                if (pluginRef.getSkillTools().doesPlayerHaveSkillPermission(parentSkill, player)) {
                    beginXpGain(parentSkill, splitXp, xpGainReason, xpGainSource);
                }
            }

            return;
        }

        // Return if the experience has been shared
        if (party != null && party.getShareHandler().handleXpShare(xp, this, primarySkillType, party.getShareHandler().getSharedXpGainReason(xpGainReason))) {
            return;
        }

        beginUnsharedXpGain(primarySkillType, xp, xpGainReason, xpGainSource);
    }

    /**
     * Begins an experience gain. The amount will be affected by skill modifiers, global rate and perks
     *
     * @param skill Skill being used
     * @param xp    Experience amount to process
     */
    public void beginUnsharedXpGain(PrimarySkillType skill, double xp, XPGainReason xpGainReason, XPGainSource xpGainSource) {
        if(player.getGameMode() == GameMode.CREATIVE)
            return;

        applyXpGain(skill, modifyXpGain(skill, xp), xpGainReason, xpGainSource);

        if (party == null) {
            return;
        }

        if (!pluginRef.getConfigManager().getConfigParty().getPartyXP().getPartyLevel().isPartyLevelingNeedsNearbyMembers() || !pluginRef.getPartyManager().getNearMembers(this).isEmpty()) {
            party.applyXpGain(modifyXpGain(skill, xp));
        }
    }

    /**
     * Applies an experience gain
     *
     * @param primarySkillType Skill being used
     * @param xp               Experience amount to add
     */
    public void applyXpGain(PrimarySkillType primarySkillType, double xp, XPGainReason xpGainReason, XPGainSource xpGainSource) {
        if (!pluginRef.getSkillTools().doesPlayerHaveSkillPermission(primarySkillType, player)) {
            return;
        }

        if (pluginRef.getSkillTools().isChildSkill(primarySkillType)) {
            Set<PrimarySkillType> parentSkills = FamilyTree.getParents(primarySkillType);

            for (PrimarySkillType parentSkill : parentSkills) {
                applyXpGain(parentSkill, xp / parentSkills.size(), xpGainReason, xpGainSource);
            }

            return;
        }

        if (!pluginRef.getEventManager().handleXpGainEvent(player, primarySkillType, xp, xpGainReason)) {
            return;
        }

        isUsingUnarmed = (primarySkillType == PrimarySkillType.UNARMED);
        checkXp(primarySkillType, xpGainReason, xpGainSource);
    }

    /**
     * Check the XP of a skill.
     *
     * @param primarySkillType The skill to check
     */
    private void checkXp(PrimarySkillType primarySkillType, XPGainReason xpGainReason, XPGainSource xpGainSource) {
        if(hasReachedLevelCap(primarySkillType))
            return;

        if (getSkillXpLevelRaw(primarySkillType) < getXpToLevel(primarySkillType)) {
            processPostXpEvent(primarySkillType, pluginRef, xpGainSource);
            return;
        }

        int levelsGained = 0;
        double xpRemoved = 0;

        while (getSkillXpLevelRaw(primarySkillType) >= getXpToLevel(primarySkillType)) {
            if (pluginRef.getPlayerLevelingSettings().isSkillLevelCapEnabled(primarySkillType)
                    && hasReachedLevelCap(primarySkillType)) {
                setSkillXpLevel(primarySkillType, 0);
                break;
            }

            xpRemoved += profile.levelUp(primarySkillType);
            levelsGained++;
        }

        if (pluginRef.getEventManager().tryLevelChangeEvent(player, primarySkillType, levelsGained, xpRemoved, true, xpGainReason)) {
            return;
        }

        SoundManager.sendSound(player, player.getLocation(), SoundType.LEVEL_UP);

        /*
         * Check to see if the player unlocked any new skills
         */

        pluginRef.getNotificationManager().sendPlayerLevelUpNotification(this, primarySkillType, levelsGained, profile.getSkillLevel(primarySkillType));

        //UPDATE XP BARS
        processPostXpEvent(primarySkillType, pluginRef, xpGainSource);
    }

    /*
     * Players & Profiles
     */

    public Player getPlayer() {
        return player;
    }

    public PlayerProfile getProfile() {
        return profile;
    }

    /*
     * Party Stuff
     */

    public void setupPartyData() {
        party = pluginRef.getPartyManager().getPlayerParty(player.getName(), player.getUniqueId());
        ptpRecord = new PartyTeleportRecord(pluginRef);

        if (inParty()) {
            loginParty();
        }
    }

    public Party getPartyInvite() {
        return invite;
    }

    public void setPartyInvite(Party invite) {
        this.invite = invite;
    }

    public boolean hasPartyInvite() {
        return (invite != null);
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public boolean inParty() {
        return (party != null);
    }

    public void removeParty() {
        party = null;
    }

    public void removePartyInvite() {
        invite = null;
    }

    public PartyTeleportRecord getPartyTeleportRecord() {
        return ptpRecord;
    }

    public Party getPartyAllianceInvite() {
        return allianceInvite;
    }

    public void setPartyAllianceInvite(Party allianceInvite) {
        this.allianceInvite = allianceInvite;
    }

    public boolean hasPartyAllianceInvite() {
        return (allianceInvite != null);
    }

    public void removePartyAllianceInvite() {
        allianceInvite = null;
    }

    public void loginParty() {
        party.addOnlineMember(this.getPlayer());
    }

    public int getItemShareModifier() {
        if (itemShareModifier < 10) {
            setItemShareModifier(10);
        }

        return itemShareModifier;
    }

    public void setItemShareModifier(int modifier) {
        itemShareModifier = Math.max(10, modifier);
    }

    /*
     * Chat modes
     */

    public boolean isChatEnabled(ChatMode mode) {
        switch (mode) {
            case ADMIN:
                return adminChatMode;

            case PARTY:
                return partyChatMode;

            default:
                return false;
        }
    }

    public void disableChat(ChatMode mode) {
        switch (mode) {
            case ADMIN:
                adminChatMode = false;
                return;

            case PARTY:
                partyChatMode = false;
                return;

            default:
        }
    }

    public void enableChat(ChatMode mode) {
        switch (mode) {
            case ADMIN:
                adminChatMode = true;
                partyChatMode = false;
                return;

            case PARTY:
                partyChatMode = true;
                adminChatMode = false;
                return;

            default:
        }

    }

    public void toggleChat(ChatMode mode) {
        switch (mode) {
            case ADMIN:
                adminChatMode = !adminChatMode;
                partyChatMode = !adminChatMode && partyChatMode;
                return;

            case PARTY:
                partyChatMode = !partyChatMode;
                adminChatMode = !partyChatMode && adminChatMode;
                return;

            default:
        }
    }

    public boolean isUsingUnarmed() {
        return isUsingUnarmed;
    }

    /**
     * Modifies an experience gain using skill modifiers, global rate and perks
     *
     * @param primarySkillType Skill being used
     * @param xp               Experience amount to process
     * @return Modified experience
     */
    private double modifyXpGain(PrimarySkillType primarySkillType, double xp) {
        if (((pluginRef.getConfigManager().getConfigLeveling().getSkillLevelCap(primarySkillType) <= getSkillLevel(primarySkillType))
                && pluginRef.getPlayerLevelingSettings().isSkillLevelCapEnabled(primarySkillType))
                || (pluginRef.getPlayerLevelingSettings().getConfigSectionLevelCaps().getPowerLevelSettings().getLevelCap() <= getPowerLevel())) {
            return 0;
        }

        xp = (double) (xp / pluginRef.getSkillTools().getXpModifier(primarySkillType) * pluginRef.getDynamicSettingsManager().getExperienceManager().getGlobalXpMult());

        //Multiply by the players personal XP rate
        return xp * personalXPModifiers.get(primarySkillType);
    }

    public void checkGodMode() {
        if (godMode && !Permissions.mcgod(player)
                || godMode && pluginRef.getDynamicSettingsManager().isWorldBlacklisted(player.getWorld().getName())) {
            toggleGodMode();
            player.sendMessage(pluginRef.getLocaleManager().getString("Commands.GodMode.Forbidden"));
        }
    }

    public void checkParty() {
        if (inParty() && !Permissions.party(player)) {
            removeParty();
            player.sendMessage(pluginRef.getLocaleManager().getString("Party.Forbidden"));
        }
    }

    /**
     * Check to see if an ability can be activated.
     *
     * @param primarySkillType The skill the ability is based on
     */
    public void checkAbilityActivation(PrimarySkillType primarySkillType) {
        //TODO: Disgusting..
        ToolType tool = pluginRef.getSkillTools().getPrimarySkillToolType(primarySkillType);
        SuperAbilityType ability = pluginRef.getSkillTools().getSuperAbility(primarySkillType);

        if (getAbilityMode(ability) || !ability.getPermissions(player)) {
            return;
        }

        //TODO: This is hacky and temporary solution until skills are moved to the new system
        //Potential problems with this include skills with two super abilities (ie mining)
        if (!pluginRef.getSkillTools().isSuperAbilityUnlocked(primarySkillType, getPlayer())) {
            int diff = RankUtils.getSuperAbilityUnlockRequirement(pluginRef.getSkillTools().getSuperAbility(primarySkillType)) - getSkillLevel(primarySkillType);

            //Inform the player they are not yet skilled enough
            pluginRef.getNotificationManager().sendPlayerInformation(player, NotificationType.ABILITY_COOLDOWN, "Skills.AbilityGateRequirementFail", String.valueOf(diff), pluginRef.getSkillTools().getLocalizedSkillName(primarySkillType));
            return;
        }

        int timeRemaining = calculateTimeRemaining(ability);

        if (timeRemaining > 0) {
            /*
             * Axes and Woodcutting are odd because they share the same tool.
             * We show them the too tired message when they take action.
             */
            if (primarySkillType == PrimarySkillType.WOODCUTTING || primarySkillType == PrimarySkillType.AXES) {
                pluginRef.getNotificationManager().sendPlayerInformation(player, NotificationType.ABILITY_COOLDOWN, "Skills.TooTired", String.valueOf(timeRemaining));
                //SoundManager.sendSound(player, player.getLocation(), SoundType.TIRED);
            }

            return;
        }

        if (pluginRef.getEventManager().callPlayerAbilityActivateEvent(player, primarySkillType).isCancelled()) {
            return;
        }

        if (useChatNotifications()) {
            pluginRef.getNotificationManager().sendPlayerInformation(player, NotificationType.SUPER_ABILITY, ability.getAbilityOn());
            //player.sendMessage(ability.getAbilityOn());
        }

        pluginRef.getSkillTools().sendSkillMessage(player, NotificationType.SUPER_ABILITY_ALERT_OTHERS, ability.getAbilityPlayer());

        //Sounds
        SoundManager.worldSendSound(player.getWorld(), player.getLocation(), SoundType.ABILITY_ACTIVATED_GENERIC);

        int abilityLength = pluginRef.getSkillTools().calculateAbilityLengthPerks(this, primarySkillType, ability);

        // Enable the ability
        profile.setAbilityDATS(ability, System.currentTimeMillis() + (abilityLength * Misc.TIME_CONVERSION_FACTOR));
        setAbilityMode(ability, true);

        if (ability == SuperAbilityType.SUPER_BREAKER || ability == SuperAbilityType.GIGA_DRILL_BREAKER) {
            pluginRef.getSkillTools().handleAbilitySpeedIncrease(player);
        }

        setToolPreparationMode(tool, false);
        new AbilityDisableTask(pluginRef,   this, ability).runTaskLater(pluginRef, abilityLength * Misc.TICK_CONVERSION_FACTOR);
    }

    public void processAbilityActivation(PrimarySkillType primarySkillType) {
        if (pluginRef.getConfigManager().getConfigSuperAbilities().isMustSneakToActivate() && !player.isSneaking()) {
            return;
        }

        ItemStack inHand = player.getInventory().getItemInMainHand();

        /*if (mcMMO.getModManager().isCustomTool(inHand) && !mcMMO.getModManager().getTool(inHand).isAbilityEnabled()) {
            return;
        }*/

        if (!getAbilityUse()) {
            return;
        }

        for (SuperAbilityType superAbilityType : SuperAbilityType.values()) {
            if (getAbilityMode(superAbilityType)) {
                return;
            }
        }

        SuperAbilityType ability = pluginRef.getSkillTools().getSuperAbility(primarySkillType);
        ToolType tool = pluginRef.getSkillTools().getPrimarySkillToolType(primarySkillType);

        /*
         * Woodcutting & Axes need to be treated differently.
         * Basically the tool always needs to ready and we check to see if the cooldown is over when the user takes action
         */
        if (tool.inHand(inHand) && !getToolPreparationMode(tool)) {
            if (primarySkillType != PrimarySkillType.WOODCUTTING && primarySkillType != PrimarySkillType.AXES) {
                int timeRemaining = calculateTimeRemaining(ability);

                if (!getAbilityMode(ability) && timeRemaining > 0) {
                    pluginRef.getNotificationManager().sendPlayerInformation(player, NotificationType.ABILITY_COOLDOWN, "Skills.TooTired", String.valueOf(timeRemaining));
                    return;
                }
            }

            if (pluginRef.getConfigManager().getConfigNotifications().isSuperAbilityToolMessage()) {
                pluginRef.getNotificationManager().sendPlayerInformation(player, NotificationType.TOOL, tool.getRaiseTool());
                SoundManager.sendSound(player, player.getLocation(), SoundType.TOOL_READY);
            }

            setToolPreparationMode(tool, true);
            new ToolLowerTask(pluginRef,this, tool).runTaskLater(pluginRef, 4 * Misc.TICK_CONVERSION_FACTOR);
        }
    }

    /**
     * Calculate the time remaining until the ability's cooldown expires.
     *
     * @param ability SuperAbilityType whose cooldown to check
     * @return the number of seconds remaining before the cooldown expires
     */
    public int calculateTimeRemaining(SuperAbilityType ability) {
        long deactivatedTimestamp = profile.getAbilityDATS(ability) * Misc.TIME_CONVERSION_FACTOR;
        return (int) (((deactivatedTimestamp + (PerksUtils.handleCooldownPerks(player, ability.getCooldown()) * Misc.TIME_CONVERSION_FACTOR)) - System.currentTimeMillis()) / Misc.TIME_CONVERSION_FACTOR);
    }

    /*
     * These functions are wrapped from PlayerProfile so that we don't always have to store it alongside the McMMOPlayer object.
     */
    public int getSkillLevel(PrimarySkillType skill) {
        return profile.getSkillLevel(skill);
    }

    public double getSkillXpLevelRaw(PrimarySkillType skill) {
        return profile.getSkillXpLevelRaw(skill);
    }

    public int getSkillXpLevel(PrimarySkillType skill) {
        return profile.getSkillXpLevel(skill);
    }

    public void setSkillXpLevel(PrimarySkillType skill, double xpLevel) {
        profile.setSkillXpLevel(skill, xpLevel);
    }

    public int getXpToLevel(PrimarySkillType skill) {
        return profile.getXpToLevel(skill);
    }

    public void removeXp(PrimarySkillType skill, int xp) {
        profile.removeXp(skill, xp);
    }

    public void modifySkill(PrimarySkillType skill, int level) {
        profile.modifySkill(skill, level);
    }

    public void addLevels(PrimarySkillType skill, int levels) {
        profile.addLevels(skill, levels);
    }

    public void addXp(PrimarySkillType skill, double xp) {
        profile.addXp(skill, xp);
    }

    public void setAbilityDATS(SuperAbilityType ability, long DATS) {
        profile.setAbilityDATS(ability, DATS);
    }

    public void resetCooldowns() {
        profile.resetCooldowns();
    }

    public FixedMetadataValue getPlayerMetadata() {
        return playerMetadata;
    }

    /**
     * This method is called by PlayerQuitEvent to tear down the mcMMOPlayer.
     *
     * @param syncSave if true, data is saved synchronously
     */
    public void logout(boolean syncSave) {
        Player thisPlayer = getPlayer();
        resetAbilityMode();
        pluginRef.getBleedTimerTask().bleedOut(thisPlayer);

        if (syncSave) {
            getProfile().save(true);
        } else {
            getProfile().scheduleAsyncSave();
        }

        pluginRef.getUserManager().remove(thisPlayer);

        if (pluginRef.getScoreboardSettings().getScoreboardsEnabled())
            pluginRef.getScoreboardManager().teardownPlayer(thisPlayer);

        if (inParty()) {
            party.removeOnlineMember(thisPlayer);
        }

        //Remove user from cache
        pluginRef.getDatabaseManager().cleanupUser(thisPlayer.getUniqueId());
    }
}
