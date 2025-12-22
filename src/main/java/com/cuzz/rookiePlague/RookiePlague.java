package com.cuzz.rookiePlague;

import com.cuzz.rookiePlague.cache.EnvironmentCache;
import com.cuzz.rookiePlague.cache.InfectedAnimalCache;
import com.cuzz.rookiePlague.config.ConfigurationManager;
import com.cuzz.rookiePlague.config.PluginConfig;
import com.cuzz.rookiePlague.config.impl.FileConfigurationLoader;
import com.cuzz.rookiePlague.config.spi.ConfigurationLoader;
import com.cuzz.rookiePlague.controller.AnimalBreedController;
import com.cuzz.rookiePlague.controller.BreedCountModifyController;
import com.cuzz.rookiePlague.controller.ConfigReloadController;
import com.cuzz.rookiePlague.controller.PlagueSimulateController;
import com.cuzz.rookiePlague.dao.AnimalDataDao;
import com.cuzz.rookiePlague.scheduler.EnvironmentUpdateScheduler;
import com.cuzz.rookiePlague.scheduler.PlagueDamageScheduler;
import com.cuzz.rookiePlague.scheduler.PlagueCheckScheduler;
import com.cuzz.rookiePlague.service.AnimalBreedService;
import com.cuzz.rookiePlague.service.AnimalConfigService;
import com.cuzz.rookiePlague.service.AnimalDataService;
import com.cuzz.rookiePlague.service.AnimalNameService;
import com.cuzz.rookiePlague.service.CommandService;
import com.cuzz.rookiePlague.service.LanguageService;
import com.cuzz.rookiePlague.service.LoggerService;
import com.cuzz.rookiePlague.service.MythicMobsSpawnerService;
import com.cuzz.rookiePlague.service.PlagueFormulaService;
import com.cuzz.rookiePlague.service.PlagueInfectionService;
import com.cuzz.rookiePlague.service.spi.MythicMobSpawner;
import com.cuzz.rookiePlague.wrapper.AnimalBreedWrapper;
import com.cuzz.rookiePlague.wrapper.CommandTabCompleter;
import com.cuzz.rookiePlague.wrapper.CommandWrapper;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * RookiePlague 插件主类
 * 
 * <p>动物瘟疫系统插件</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public final class RookiePlague extends JavaPlugin {
    
    private ConfigurationManager configManager;
    private AnimalConfigService animalConfigService;
    private PluginConfig pluginConfig;
    private LoggerService loggerService;
    private LanguageService languageService;
    private AnimalDataDao animalDataDao;
    private AnimalDataService animalDataService;
    private AnimalNameService animalNameService;
    private AnimalBreedService animalBreedService;
    private CommandService commandService;
    private ConfigReloadController configReloadController;
    private PlagueSimulateController plagueSimulateController;
    private AnimalBreedController animalBreedController;
    private BreedCountModifyController breedCountModifyController;
    private AnimalBreedWrapper animalBreedWrapper;
    private CommandWrapper commandWrapper;
    private CommandTabCompleter commandTabCompleter;
    private PlagueFormulaService plagueFormulaService;
    private PlagueInfectionService plagueInfectionService;
    private EnvironmentUpdateScheduler environmentUpdateScheduler;
    private PlagueDamageScheduler plagueDamageScheduler;
    private PlagueCheckScheduler plagueCheckScheduler;
    private EnvironmentCache environmentCache;
    private InfectedAnimalCache infectedAnimalCache;
    private MythicMobSpawner mythicMobSpawner;

    @Override
    public void onEnable() {
        // 初始化日志服务
        initializeLogger();
        
        // 初始化配置系统
        initializeConfigSystem();
        
        // 初始化语言服务
        initializeLanguage();
        
        // 初始化服务层
        initializeServices();
        
        // 注册监听器
        registerListeners();
        
        // 加载配置数据
        loadConfigurations();
        
        // 注册命令
        registerCommands();
        
        // 启动瘟疫系统
        startPlagueSystem();
        
        // 启动完成
        String enableMsg = languageService.getMessage("on-enable");
        loggerService.info(enableMsg);
    }

    @Override
    public void onDisable() {
        // 停止瘟疫系统
        stopPlagueSystem();
        
        // 清理缓存
        if (configManager != null) {
            configManager.clearCache();
        }
        
        if (languageService != null && loggerService != null) {
            String disableMsg = languageService.getMessage("on-disable");
            loggerService.info(disableMsg);
        }
    }
    
    /**
     * 初始化日志服务
     * 
     * <p>创建统一的日志服务实例</p>
     */
    private void initializeLogger() {
        loggerService = new LoggerService(this);
    }
    
    /**
     * 初始化配置系统
     * 
     * <p>创建配置加载器和配置管理器</p>
     */
    private void initializeConfigSystem() {
        loggerService.info("正在初始化配置系统...");
        
        // 创建配置加载器
        ConfigurationLoader loader = new FileConfigurationLoader(this);
        
        // 创建配置管理器
        configManager = new ConfigurationManager(loader);
        
        // 加载主配置文件
        pluginConfig = new PluginConfig(configManager.getConfig("config.yml"));
        
        // 初始化动物配置服务
        animalConfigService = new AnimalConfigService(configManager);
        
        loggerService.success("配置系统初始化完成");
    }
    
    /**
     * 初始化语言服务
     * 
     * <p>根据配置文件加载对应的语言文件</p>
     */
    private void initializeLanguage() {
        loggerService.info("正在初始化语言服务...");
        
        // 创建语言服务（使用 ConfigurationManager）
        languageService = new LanguageService(configManager, loggerService);
        
        // 从配置获取语言设置
        String language = pluginConfig.getLanguage();
        
        // 加载语言文件
        if (languageService.loadLanguage(language)) {
            loggerService.success("语言服务初始化完成，当前语言: %s", language);
        } else {
            loggerService.warning("语言服务初始化失败，将使用默认语言");
        }
    }
    
    /**
     * 初始化服务层
     * 
     * <p>按依赖顺序初始化各个服务</p>
     */
    private void initializeServices() {
        loggerService.info("正在初始化服务层...");
        
        // 初始化 DAO 层
        animalDataDao = new AnimalDataDao(this);
        
        // 初始化命令服务
        commandService = new CommandService(this, loggerService);
        
        // 初始化动物数据服务
        animalDataService = new AnimalDataService(animalDataDao, loggerService);
        
        // 初始化动物名称服务
        animalNameService = new AnimalNameService(animalDataService, animalConfigService, pluginConfig, loggerService);
        
        // 初始化动物繁殖服务
        animalBreedService = new AnimalBreedService(animalDataService, animalNameService, loggerService);
        
        // 初始化染疫公式服务
        plagueFormulaService = new PlagueFormulaService(loggerService);
        
        // 加载并编译公式
        String formula = pluginConfig.getPlagueFormula();
        if (plagueFormulaService.setFormula(formula)) {
            loggerService.success("染疫公式初始化成功");
        } else {
            loggerService.warning("染疫公式初始化失败，使用默认公式");
        }
        
        // 初始化环境数据缓存
        environmentCache = new EnvironmentCache();
        
        // 初始化染疫动物缓存
        infectedAnimalCache = new InfectedAnimalCache();
        
        // 初始化 MythicMobs 生成器
        mythicMobSpawner = new MythicMobsSpawnerService(loggerService);
        
        // 初始化染疫感染服务
        plagueInfectionService = new PlagueInfectionService(
            plagueFormulaService,
            animalConfigService,
            animalDataService,
            animalNameService,
            pluginConfig,
            this,
            environmentCache,
            infectedAnimalCache,
            loggerService
        );
        
        loggerService.success("服务层初始化完成");
    }
    
    /**
     * 启动瘟疫系统
     * 
     * <p>初始化并启动所有调度器</p>
     */
    private void startPlagueSystem() {
        // 检查是否启用瘟疫系统
        if (!pluginConfig.isPlagueEnabled()) {
            loggerService.info("瘟疫系统已禁用（配置：plague.enabled=false）");
            return;
        }
        
        loggerService.info("正在启动瘟疫系统...");
        
        // 1. 启动环境数据更新调度器（主线程）
        environmentUpdateScheduler = new EnvironmentUpdateScheduler(
            this,
            environmentCache,
            loggerService
        );
        int envUpdateInterval = pluginConfig.getEnvironmentUpdateInterval();
        environmentUpdateScheduler.start(envUpdateInterval);
        
        loggerService.success("环境更新调度器已启动（更新间隔: %d秒）", envUpdateInterval);
        
        // 2. 启动瘟疫伤害调度器（主线程）
        if (pluginConfig.isPlagueDamageEnabled()) {
            plagueDamageScheduler = new PlagueDamageScheduler(
                this,
                animalDataService,
                animalConfigService,
                infectedAnimalCache,
                mythicMobSpawner,
                pluginConfig,
                loggerService
            );
            int damageInterval = pluginConfig.getPlagueDamageInterval();
            plagueDamageScheduler.start(damageInterval);
            
            loggerService.success("瘟疫伤害调度器已启动（伤害间隔: %d秒）", damageInterval);
        }
        
        // 3. 启动染疫检查调度器（异步）
        plagueCheckScheduler = new PlagueCheckScheduler(
            this,
            plagueInfectionService,
            animalDataService,
            infectedAnimalCache,
            loggerService
        );
        int checkInterval = pluginConfig.getInfectionCheckInterval();
        int checkDelay = pluginConfig.getInfectionCheckDelay();
        plagueCheckScheduler.start(checkInterval, checkDelay);
        
        loggerService.success("染疫检查调度器已启动（检查间隔: %d秒，首次延迟: %d秒）", checkInterval, checkDelay);
        
        loggerService.success("瘟疫系统已全部启动！");
    }
    
    /**
     * 停止瘟疫系统
     * 
     * <p>停止所有调度器</p>
     */
    private void stopPlagueSystem() {
        loggerService.info("正在停止瘟疫系统...");
        
        // 停止环境更新调度器
        if (environmentUpdateScheduler != null && environmentUpdateScheduler.isRunning()) {
            environmentUpdateScheduler.stop();
        }
        
        // 停止瘟疫伤害调度器
        if (plagueDamageScheduler != null && plagueDamageScheduler.isRunning()) {
            plagueDamageScheduler.stop();
        }
        
        // 停止染疫检查调度器
        if (plagueCheckScheduler != null && plagueCheckScheduler.isRunning()) {
            plagueCheckScheduler.stop();
        }
        
        loggerService.success("瘟疫系统已全部停止！");
    }
    
    /**
     * 注册监听器
     * 
     * <p>注册插件的所有事件监听器</p>
     */
    private void registerListeners() {
        loggerService.info("正在注册监听器...");
        
        // 创建并注册配置重载控制器
        configReloadController = new ConfigReloadController(
            configManager, 
            animalConfigService, 
            loggerService,
            languageService
        );
        configReloadController.setPluginConfig(pluginConfig);
        getServer().getPluginManager().registerEvents(configReloadController, this);
        
        // 创建并注册染疫模拟控制器
        plagueSimulateController = new PlagueSimulateController(
            animalConfigService,
            plagueFormulaService,
            environmentCache,
            loggerService,
            pluginConfig
        );
        getServer().getPluginManager().registerEvents(plagueSimulateController, this);
        
        // 创建并注册动物繁殖Wrapper
        animalBreedWrapper = new AnimalBreedWrapper(loggerService);
        getServer().getPluginManager().registerEvents(animalBreedWrapper, this);
        
        // 创建并注册动物繁殖控制器
        animalBreedController = new AnimalBreedController(
            animalConfigService,
            animalBreedService,
            languageService,
            loggerService
        );
        getServer().getPluginManager().registerEvents(animalBreedController, this);
        
        // 创建并注册繁殖次数修改控制器
        breedCountModifyController = new BreedCountModifyController(
            animalConfigService,
            animalDataService,
            loggerService
        );
        getServer().getPluginManager().registerEvents(breedCountModifyController, this);
        
        loggerService.success("已注册 5 个监听器");
    }
    
    /**
     * 加载所有配置数据
     * 
     * <p>加载动物配置并输出加载结果</p>
     */
    private void loadConfigurations() {
        loggerService.info("正在加载配置数据...");
        
        // 加载动物配置
        if (loadAnimalConfigs()) {
            printAnimalConfigInfo();
        } else {
            loggerService.warning("加载动物配置失败，插件可能无法正常工作");
        }
    }
    
    /**
     * 加载动物配置
     * 
     * @return 是否加载成功
     */
    private boolean loadAnimalConfigs() {
        boolean success = animalConfigService.loadConfig();
        
        if (success) {
            loggerService.info("成功加载 %d 个动物配置", animalConfigService.getAnimalCount());
        }
        
        return success;
    }
    
    /**
     * 打印动物配置信息
     * 
     * <p>在控制台输出所有已加载的动物配置详情</p>
     */
    private void printAnimalConfigInfo() {
        animalConfigService.getAllAnimals().forEach(config -> {
            loggerService.info(
                "  - %s(%s): 物种因子=%.2f, 区块上限=%d, 致死时间=%ds",
                config.getDesc(), 
                config.getType(),
                config.getSpeciesFactor(), 
                config.getChunkLimit(),
                config.getPlagueDeathTime()
            );
        });
    }
    
    /**
     * 注册命令
     * 
     * <p>注册插件的所有命令处理器和自动补全</p>
     */
    private void registerCommands() {
        loggerService.info("正在注册命令...");
        
        // 注册主命令
        commandWrapper = new CommandWrapper(loggerService);
        getCommand("rp").setExecutor(commandWrapper);
        
        // 注册 Tab 自动补全
        commandTabCompleter = new CommandTabCompleter(animalConfigService);
        getCommand("rp").setTabCompleter(commandTabCompleter);
        
        loggerService.success("已注册命令: /rp (含 Tab 自动补全)");
    }
    
    /**
     * 获取配置管理器
     * 
     * @return 配置管理器实例
     */
    public ConfigurationManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取动物配置服务
     * 
     * @return 动物配置服务实例
     */
    public AnimalConfigService getAnimalConfigService() {
        return animalConfigService;
    }
    
    /**
     * 获取插件配置
     * 
     * @return 插件配置实例
     */
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
    
    /**
     * 获取日志服务
     * 
     * @return 日志服务实例
     */
    public LoggerService getLoggerService() {
        return loggerService;
    }
    
    /**
     * 获取命令服务
     * 
     * @return 命令服务实例
     */
    public CommandService getCommandService() {
        return commandService;
    }
    
    /**
     * 获取语言服务
     * 
     * @return 语言服务实例
     */
    public LanguageService getLanguageService() {
        return languageService;
    }
    
    /**
     * 获取染疫公式服务
     * 
     * @return 染疫公式服务实例
     */
    public PlagueFormulaService getPlagueFormulaService() {
        return plagueFormulaService;
    }
    
    /**
     * 重载插件配置
     * 
     * <p>提供给外部调用的统一重载入口</p>
     * 
     * @return 是否重载成功
     */
    public boolean reloadPluginConfig() {
        try {
            loggerService.debug("RELOAD", "正在重载 PluginConfig...");
            
            // 重载主配置文件
            configManager.reloadConfig("config.yml");
            
            // 重新创建 PluginConfig 实例
            pluginConfig = new PluginConfig(configManager.getConfig("config.yml"));
            
            // 更新控制器中的引用
            if (configReloadController != null) {
                configReloadController.setPluginConfig(pluginConfig);
            }
            
            loggerService.debug("RELOAD", "PluginConfig 重载完成");
            return true;
        } catch (Exception e) {
            loggerService.error("PluginConfig 重载失败", e);
            return false;
        }
    }
}
