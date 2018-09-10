package de.codingair.tradesystem.trade.commands;

import de.codingair.codingapi.server.commands.BaseComponent;
import de.codingair.codingapi.server.commands.CommandBuilder;
import de.codingair.codingapi.server.commands.CommandComponent;
import de.codingair.codingapi.server.commands.MultiCommandComponent;
import de.codingair.codingapi.tools.time.TimeList;
import de.codingair.codingapi.tools.time.TimeListener;
import de.codingair.codingapi.tools.time.TimeMap;
import de.codingair.tradesystem.TradeSystem;
import de.codingair.tradesystem.utils.Lang;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TradeCMD extends CommandBuilder {
    private static String PERMISSION = "TradeSystem.Trade";
    private TimeMap<String, TimeList<String>> invites = new TimeMap<>();

    public TradeCMD() {
        super("Trade", new BaseComponent(PERMISSION) {
            @Override
            public void noPermission(CommandSender sender, String label, CommandComponent child) {
                sender.sendMessage(Lang.getPrefix() + "§c" + Lang.get("Not_Able_To_Trade"));
            }

            @Override
            public void onlyFor(boolean player, CommandSender sender, String label, CommandComponent child) {
                sender.sendMessage(Lang.getPrefix() + "§cOnly for players!");
            }

            @Override
            public void unknownSubCommand(CommandSender sender, String label, String[] args) {
                sender.sendMessage(Lang.getPrefix() + Lang.get("Command_How_To"));
            }

            @Override
            public boolean runCommand(CommandSender sender, String label, String[] args) {
                sender.sendMessage(Lang.getPrefix() + Lang.get("Command_How_To"));
                return false;
            }
        }.setOnlyPlayers(true), true);

        //ACCEPT
        getBaseComponent().addChild(new CommandComponent("accept") {
            @Override
            public boolean runCommand(CommandSender sender, String label, String[] args) {
                sender.sendMessage(Lang.getPrefix() + Lang.get("Command_How_To_Accept"));
                return false;
            }
        });

        getComponent("accept").addChild(new MultiCommandComponent() {
            @Override
            public void addArguments(CommandSender sender, List<String> suggestions) {
                List<String> l = invites.get(sender.getName());
                if(l == null) return;
                suggestions.addAll(l);
            }

            @Override
            public boolean runCommand(CommandSender sender, String label, String argument, String[] args) {
                List<String> l = invites.get(sender.getName());
                if(l == null) return false;

                if(l.contains(argument)) {
                    Player other = Bukkit.getPlayer(argument);

                    if(other == null) {
                        sender.sendMessage(Lang.getPrefix() + Lang.get("Player_Of_Request_Not_Online"));
                        return false;
                    }

                    l.remove(argument);

                    sender.sendMessage(Lang.getPrefix() + Lang.get("Request_Accepted"));
                    other.sendMessage(Lang.getPrefix() + Lang.get("Request_Was_Accepted").replace("%PLAYER%", sender.getName()));

                    TradeSystem.getInstance().getTradeManager().startTrade((Player) sender, other);
                } else {
                    sender.sendMessage(Lang.getPrefix() + Lang.get("No_Request_Found"));
                }

                return false;
            }
        });

        //DENY
        getBaseComponent().addChild(new CommandComponent("deny") {
            @Override
            public boolean runCommand(CommandSender sender, String label, String[] args) {
                sender.sendMessage(Lang.getPrefix() + Lang.get("Command_How_To_Deny"));
                return false;
            }
        });

        getComponent("deny").addChild(new MultiCommandComponent() {
            @Override
            public void addArguments(CommandSender sender, List<String> suggestions) {
                List<String> l = invites.get(sender.getName());
                if(l == null) return;
                suggestions.addAll(l);
            }

            @Override
            public boolean runCommand(CommandSender sender, String label, String argument, String[] args) {
                List<String> l = invites.get(sender.getName());
                if(l == null) return false;

                if(l.contains(argument)) {
                    Player other = Bukkit.getPlayer(argument);

                    if(other == null) {
                        sender.sendMessage(Lang.getPrefix() + Lang.get("Player_Of_Request_Not_Online"));
                        return false;
                    }

                    l.remove(argument);

                    sender.sendMessage(Lang.getPrefix() + Lang.get("Request_Denied").replace("%PLAYER%", other.getName()));
                    other.sendMessage(Lang.getPrefix() + Lang.get("Request_Was_Denied").replace("%PLAYER%", sender.getName()));
                } else {
                    sender.sendMessage(Lang.getPrefix() + Lang.get("No_Request_Found"));
                }

                return false;
            }
        });

        //INVITE
        getBaseComponent().addChild(new MultiCommandComponent() {
            @Override
            public void addArguments(CommandSender sender, List<String> suggestions) {
                for(Player player : Bukkit.getOnlinePlayers()) {
                    if(player.getName().equals(sender.getName())) continue;

                    TimeList<String> l = invites.get(player.getName());
                    if(l != null && l.contains(sender.getName())) continue;
                    suggestions.add(player.getName());
                }
            }

            @Override
            public boolean runCommand(CommandSender sender, String label, String argument, String[] args) {
                request((Player) sender, Bukkit.getPlayer(argument));
                return false;
            }
        });
    }
    
    public static void request(Player p, Player other) {
        if(!TradeSystem.getInstance().getTradeManager().getAllowedGameModes().contains(p.getGameMode().name())) {
            p.sendMessage(Lang.getPrefix() + Lang.get("Cannot_trade_in_that_GameMode"));
            return;
        }

        if(other == null) {
            p.sendMessage(Lang.getPrefix() + Lang.get("Player_Not_Online"));
            return;
        }

        if(other.getName().equals(p.getName())) {
            p.sendMessage(Lang.getPrefix() + Lang.get("Cannot_Trade_With_Yourself"));
            return;
        }

        if(!TradeSystem.getInstance().getTradeManager().getAllowedGameModes().contains(other.getGameMode().name())) {
            p.sendMessage(Lang.getPrefix() + Lang.get("Other_cannot_trade_in_that_GameMode"));
            return;
        }

        if(!other.hasPermission(PERMISSION)) {
            p.sendMessage(Lang.getPrefix() + "§c" + Lang.get("Player_Is_Not_Able_Trade"));
            return;
        }

        if(TradeSystem.getInstance().getTradeManager().getDistance() > 0) {
            if(!p.getWorld().equals(other.getWorld()) || p.getLocation().distance(other.getLocation()) > TradeSystem.getInstance().getTradeManager().getDistance()) {
                p.sendMessage(Lang.getPrefix() + "§c" + Lang.get("Player_is_not_in_range").replace("%PLAYER%", other.getName()));
                return;
            }
        }


        TimeList<String> l = TradeSystem.getInstance().getTradeCMD().getInvites().get(p.getName());
        if(l != null && l.contains(other.getName())) {
            l.remove(other.getName());

            p.sendMessage(Lang.getPrefix() + Lang.get("Request_Accepted"));
            other.sendMessage(Lang.getPrefix() + Lang.get("Request_Was_Accepted").replace("%PLAYER%", p.getName()));

            TradeSystem.getInstance().getTradeManager().startTrade(p, other);
            return;
        }

        l = TradeSystem.getInstance().getTradeCMD().getInvites().get(other.getName());
        if(l != null && l.contains(p.getName())) {
            p.sendMessage(Lang.getPrefix() + "§c" + Lang.get("Trade_Spam"));
            return;
        }

        if(l == null) l = new TimeList<>();
        l.add(p.getName(), TradeSystem.getInstance().getTradeManager().getCooldown());
        TradeSystem.getInstance().getTradeCMD().getInvites().put(other.getName(), l, TradeSystem.getInstance().getTradeManager().getCooldown());

        List<TextComponent> parts = new ArrayList<>();

        TextComponent accept = new TextComponent(Lang.get("Want_To_Trade_Accept"));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trade accept " + p.getName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {new TextComponent(Lang.get("Want_To_Trade_Hover"))}));

        TextComponent deny = new TextComponent(Lang.get("Want_To_Trade_Deny"));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trade deny " + p.getName()));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {new TextComponent(Lang.get("Want_To_Trade_Hover"))}));

        String s = Lang.getPrefix() + Lang.get("Want_To_Trade").replace("%PLAYER%", p.getName());

        String[] a1 = s.split("%ACCEPT%");
        if(a1[0].contains("%DENY%")) {
            String[] a2 = a1[0].split("%DENY%");
            parts.add(new TextComponent(a2[0]));
            parts.add(deny);
            parts.add(new TextComponent(a2[1]));
            parts.add(accept);
            parts.add(new TextComponent(a1[1]));
        } else {
            parts.add(new TextComponent(a1[0]));
            parts.add(accept);

            String[] a2 = a1[1].split("%DENY%");
            parts.add(new TextComponent(a2[0]));
            parts.add(deny);
            parts.add(new TextComponent(a2[1]));
        }

        TextComponent basic = new TextComponent("");

        for(TextComponent part : parts) {
            basic.addExtra(part);
        }

        other.spigot().sendMessage(basic);
        p.sendMessage(Lang.getPrefix() + Lang.get("Player_Is_Invited").replace("%PLAYER%", other.getName()));
    }

    public TimeMap<String, TimeList<String>> getInvites() {
        return invites;
    }
}