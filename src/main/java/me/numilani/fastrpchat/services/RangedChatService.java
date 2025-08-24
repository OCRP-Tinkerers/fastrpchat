package me.numilani.fastrpchat.services;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import me.numilani.fastrpchat.FastRpChat;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Objects;

public class RangedChatService {

  private FastRpChat plugin;

  public RangedChatService(FastRpChat plugin) {
    this.plugin = plugin;
  }

  public int GetRangeRadius(String range) throws Exception {
    switch (range.toLowerCase()) {
      case "global":
        return -1;
      case "province":
        return 192;
      case "yell":
        return 64;
      case "local":
        return 20;
      case "quiet":
        return 8;
      case "whisper":
        return 3;
      case "staff":
        return -2;
      default:
        throw new Exception(String.format(
            ChatColor.DARK_RED + "Couldn't figure out what range you wanted to send in! (range value: %s)", range));
    }
  }

  public ChatColor GetRangeColor(String range) {
    switch (range) {
      case "global":
        return ChatColor.AQUA;
      case "province":
        return ChatColor.DARK_AQUA;
      case "yell":
        return ChatColor.YELLOW;
      case "local":
        return ChatColor.WHITE;
      case "quiet":
        return ChatColor.GRAY;
      case "whisper":
        return ChatColor.DARK_GRAY;
      case "staff":
        return ChatColor.DARK_GREEN;
      default:
        return ChatColor.WHITE;
    }
  }

  public void SendRangedMessage(Player player, BaseComponent[] componentMessage, String formatttedMessage, int radius) {

    if (radius == -1) {
      // do global chat
      for (var p : Bukkit.getOnlinePlayers()) {
        if (!plugin.UserRangeMutes.containsKey(p.getUniqueId())
            || !plugin.UserRangeMutes.get(p.getUniqueId()).contains(radius)) {
          p.spigot().sendMessage(componentMessage);
        }
      }
      return;
    }

    if (radius == -2) {
      for (var p : Bukkit.getOnlinePlayers()) {
        if (p.hasPermission("fastrpchat.staffchat")) {
          p.spigot().sendMessage(componentMessage);
        }
      }
      return;
    }

    if (plugin.ChatPausedPlayers.contains(player.getUniqueId())
        && !player.hasPermission("fastrpchat.ignorechatpauses")) {
      player.sendMessage("Your chat is paused! Please wait until chat is unpaused before continuing.");
      return;
    }

    // send message to self

    // inform player if no one is in range to hear
    if (player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius, x -> x instanceof Player)
        .size() <= 1) {
      player.spigot().sendMessage(componentMessage);
      Bukkit.getServer().getConsoleSender().sendMessage(formatttedMessage + ChatColor.DARK_GRAY + " [out of range]");
      for (var id : plugin.chatspyUsers) {
        var p = Bukkit.getPlayer(id);
        if (p == null)
          continue;
        // p.sendMessage(ChatColor.DARK_RED + "[SPY]" + ChatColor.RESET +
        // componentMessage + ChatColor.DARK_GRAY + " [out of range]");
        var fmt = new ComponentBuilder("[SPY]").color(net.md_5.bungee.api.ChatColor.DARK_RED).append(componentMessage)
            .append(" [out of range]").color(net.md_5.bungee.api.ChatColor.DARK_GRAY).create();
        p.spigot().sendMessage(fmt);
      }
      player.sendMessage(ChatColor.DARK_RED + "You speak, but there's no one close enough to hear you...");
      return;
    }

    // if players are in range, send msg to all of them
    for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius,
        x -> x instanceof Player)) {
      if (!plugin.UserRangeMutes.containsKey(entity.getUniqueId())
          || !plugin.UserRangeMutes.get(entity.getUniqueId()).contains(radius)) {
        entity.spigot().sendMessage(componentMessage);
      }
    }
    Bukkit.getServer().getConsoleSender().sendMessage(formatttedMessage);
    for (var id : plugin.chatspyUsers) {
      var p = Bukkit.getPlayer(id);
      if (p == null)
        continue;
      // p.sendMessage(ChatColor.DARK_RED + "[SPY]" + ChatColor.RESET +
      // componentMessage);
      var fmt = new ComponentBuilder("[SPY]").color(net.md_5.bungee.api.ChatColor.DARK_RED).append(componentMessage)
          .create();
      p.spigot().sendMessage(fmt);
    }
  }

  public void SendRangedChat(Player player, String message, String range) {

    int radius;

    try {
      radius = GetRangeRadius(range);
    } catch (Exception e) {
      player.sendMessage(e.getMessage());
      return;
    }

    var formattedMsg = String.format("[" + GetRangeColor(range) + range.toUpperCase().toCharArray()[0] + ChatColor.WHITE
        + "] %s:" + GetRangeColor(range) + " %s", player.getDisplayName(), message);

    var spltMessage = message.split(" ");

    var formattedComponent = new ComponentBuilder("[")
        .append(Character.toString(range.toUpperCase().toCharArray()[0]))
        .color(GetRangeColor(range).asBungee())
        .append("] ", ComponentBuilder.FormatRetention.NONE)
        .append(CreateNameHoverComponent(player))
        .append(": ", ComponentBuilder.FormatRetention.NONE);

    for (var str : spltMessage) {

      if (str.equals("@hand")) {
        formattedComponent = formattedComponent.append(CreateMainhandItemHoverComponent(player),
            ComponentBuilder.FormatRetention.NONE);
      } else if (str.equals("@offhand")) {
        formattedComponent = formattedComponent.append(CreateOffhandItemHoverComponent(player),
            ComponentBuilder.FormatRetention.NONE);
      } else {
        formattedComponent = formattedComponent.append(str + " ", ComponentBuilder.FormatRetention.NONE)
            .color(GetRangeColor(range).asBungee());
      }
    }
    var finalComponent = formattedComponent.create();

    SendRangedMessage(player, finalComponent, formattedMsg, radius);
  }

  public void SendRangedEmote(Player player, String message, String range) {
    int radius;

    try {
      radius = GetRangeRadius(range);
    } catch (Exception e) {
      player.sendMessage(e.getMessage());
      return;
    }

    var formattedMsg = String.format("[" + GetRangeColor(range) + range.toUpperCase().toCharArray()[0] + ChatColor.WHITE
        + "] %s" + GetRangeColor(range) + ChatColor.ITALIC + " %s", player.getDisplayName(), message);

    var spltMessage = message.split(" ");

    var formattedComponent = new ComponentBuilder("[")
        .append(Character.toString(range.toUpperCase().toCharArray()[0]))
        .color(GetRangeColor(range).asBungee())
        .append("] ", ComponentBuilder.FormatRetention.NONE)
        .append(CreateNameHoverComponent(player))
        .append(" ", ComponentBuilder.FormatRetention.NONE);

    for (var str : spltMessage) {

      if (str.equals("@hand")) {
        formattedComponent = formattedComponent.append(CreateMainhandItemHoverComponent(player),
            ComponentBuilder.FormatRetention.NONE);
      } else if (str.equals("@offhand")) {
        formattedComponent = formattedComponent.append(CreateOffhandItemHoverComponent(player),
            ComponentBuilder.FormatRetention.NONE);
      } else {
        formattedComponent = formattedComponent.append(str + " ", ComponentBuilder.FormatRetention.NONE)
            .color(GetRangeColor(range).asBungee()).italic(true);
      }
    }

    var finalComponent = formattedComponent.create();

    SendRangedMessage(player, finalComponent, formattedMsg, radius);
  }

  public void PauseChatsNearby(Player player, int radius) {
    for (var entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius,
        x -> x instanceof Player)) {
      plugin.ChatPausedPlayers.add(player.getUniqueId());
      player.sendMessage(String.format("Chat paused for all players in %d radius.", radius));
    }
  }

  public void UnpauseChats(Player player) {
    plugin.ChatPausedPlayers = new ArrayList<>();
    player.sendMessage("Unpaused all chats.");
  }

  public TextComponent CreateMainhandItemHoverComponent(Player player) {
    var item = NBTEditor.getNBTCompound(player.getInventory().getItemInMainHand());
    if (player.getInventory().getItemInMainHand().getItemMeta() == null) {
      return new TextComponent("[air] ");
    }
    if (!player.getInventory().getItemInMainHand().getItemMeta().hasDisplayName()) {
      var itemName = player.getInventory().getItemInMainHand().getType().name().replace("_", " ").toLowerCase();
      var x = new TextComponent("[" + itemName + ChatColor.WHITE + "] ");
      x.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(item.toJson()).create()));
      return x;
    }
    var x = new TextComponent(
        "[" + player.getInventory().getItemInMainHand().getItemMeta().getDisplayName() + ChatColor.WHITE + "] ");
    x.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(item.toJson()).create()));
    return x;
  }

  public TextComponent CreateOffhandItemHoverComponent(Player player) {
    var item = NBTEditor.getNBTCompound(player.getInventory().getItemInOffHand());
    if (player.getInventory().getItemInOffHand().getItemMeta() == null) {
      return new TextComponent("[air] ");
    }
    if (!Objects.requireNonNull(player.getInventory().getItemInOffHand().getItemMeta()).hasDisplayName()) {
      var itemName = player.getInventory().getItemInOffHand().getType().name().replace("_", " ").toLowerCase();
      var x = new TextComponent("[" + itemName + ChatColor.WHITE + "] ");
      x.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(item.toJson()).create()));
      return x;
    }
    var x = new TextComponent(
        "[" + player.getInventory().getItemInOffHand().getItemMeta().getDisplayName() + ChatColor.WHITE + "] ");
    x.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(item.toJson()).create()));
    return x;
  }

  public TextComponent CreateNameHoverComponent(Player player) {
    var x = new TextComponent(plugin.chat.getPlayerPrefix(player) + player.getDisplayName());
    x.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
        new ComponentBuilder(String.format("Username: %s", player.getName())).create()));
    return x;
  }

}
