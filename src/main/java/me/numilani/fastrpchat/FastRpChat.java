package me.numilani.fastrpchat;

import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import me.numilani.fastrpchat.commands.ChatCommandHandler;
import me.numilani.fastrpchat.commands.NameUtilityCommandHandler;
import me.numilani.fastrpchat.data.IDataSourceConnector;
import me.numilani.fastrpchat.data.SqliteDataSourceConnector;
import me.numilani.fastrpchat.listeners.FastRpChatListeners;
import me.numilani.fastrpchat.services.RangedChatService;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.meta.SimpleCommandMeta;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.paper.PaperCommandManager;
import net.milkbowl.vault.chat.Chat;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class FastRpChat extends JavaPlugin {

  // public CloudSimpleHandler cmdHandler = new CloudSimpleHandler();
  public LegacyPaperCommandManager<CommandSender> cmdHandler;
  public AnnotationParser<CommandSender> cmdParser;
  public FileConfiguration cfg;
  public IDataSourceConnector dataSource;
  public RangedChatService rangedChatService = new RangedChatService(this);

  public List<UUID> chatspyUsers = new ArrayList<>();

  public Multimap<UUID, Integer> UserRangeMutes = ArrayListMultimap.create();
  public List<UUID> ChatPausedPlayers = new ArrayList<>();

  public net.milkbowl.vault.chat.Chat chat = null;

  @Override
  public void onEnable() {

    RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
    chat = rsp.getProvider();

    try {
      cmdHandler = LegacyPaperCommandManager.createNative(this, ExecutionCoordinator.simpleCoordinator());
      cmdParser = new AnnotationParser<>(cmdHandler, CommandSender.class,
          parserParameters -> SimpleCommandMeta.empty());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // First run setup
    var isFirstRun = false;
    if (!(new FileConfiguration(this, "config.yml").exists())) {
      isFirstRun = true;
      doPluginInit();
    }

    cfg = new FileConfiguration(this, "config.yml");
    cfg.load();

    // do a check for datasourcetype once that's added to config
    // for now, just set datasource to sqlite always
    try {
      dataSource = new SqliteDataSourceConnector(this);
      if (isFirstRun)
        dataSource.initDatabase();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    // if
    // (cmdHandler.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
    // {
    // cmdHandler.registerAsynchronousCompletions();
    // }
    //
    // if (cmdHandler.queryCapability(CloudBukkitCapabilities.BRIGADIER)) {
    // cmdHandler.registerBrigadier();
    // }

    cmdParser.parse(new ChatCommandHandler(this));
    cmdParser.parse(new NameUtilityCommandHandler(this));

    // Register events
    getServer().getPluginManager().registerEvents(new FastRpChatListeners(this), this);

    // Register commands
    // cmdHandler.enable(this);
    // cmdHandler.getParser().parse(new ChatCommandHandler(this));
    // cmdHandler.getParser().parse(new NameUtilityCommandHandler(this));

    // var testCmd = cmdHandler.getManager().commands().stream()
    // .filter(x ->
    // x.getArguments().stream().findFirst().get().getName().equals("test"))
    // .findFirst().get();
    // cmdHandler.getManager().command(cmdHandler.getManager().commandBuilder("asdf",
    // testCmd.getCommandMeta()).proxies(testCmd).build());

    // var chatRangeCmd = cmdHandler.getManager().commands().stream()
    // .filter(x ->
    // x.getArguments().stream().findFirst().get().getName().equals("cr"))
    // .findFirst().get();

    // String[] ranges = {"global", "yell", "local", "quiet", "whisper"};
    // String[] aliases = {"g", "y", "l", "q", "w"};
    // for (var x : aliases){
    // cmdHandler.getManager().commandBuilder(x,
    // chatRangeCmd.getCommandMeta()).proxies(chatRangeCmd).argument(StringArgument.of(x));
    // }
  }

  private void doPluginInit() {
    var cfgFile = new FileConfiguration(this, "config.yml");
    cfgFile.addHeader("This config file is unused. It will be populated in the future.");
    cfgFile.set("unusedValue", "");

    cfgFile.saveSync();
  }

  @Override
  public void onDisable() {
    // Plugin shutdown logic
  }
}
