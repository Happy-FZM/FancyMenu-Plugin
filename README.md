# FancyMenu-Plugin


## How To Use:

1. install the plugin for any bukkit server

2. install [FancyMenu](https://modrinth.com/mod/fancymenu) v3 for any client 



## Commands:
####
### /openguiscreen: 
The /openguiscreen command lets you open a GUI (Vanilla/mod and custom GUIs).
It can even remotely open GUIs for other players when FancyMenu is installed on both server and clients.

For a more in-detail description of this command, take a look at the Open GUIs by Command page.

#### Usage: /openguiscreen <screen_identifier> <target_player>
###
### /closeguiscreen:
The /closeguiscreen command lets you close the current GUI.

Huh? This is totally useless you say?
Well yes, but actually no.

This command is useful for when using mods that trigger commands on specific actions.
So yes, this command is absolutely useless when using it without other mods, but can be really helpful if you have the right mods installed!

#### Usage: /closeguiscreen <target_player>
###
### /fmvariable:
The /fmvariable command allows you to set and get FancyMenu variables.

To execute this command as another player on servers, you can use the /execute as Vanilla command.
So lets say you want to execute the /fmvariable command as the player ExamplePlayer. In that case you would type:
/execute as ExamplePlayer run fmvariable....

#### Usage: /fmvariable <get_or_set> <variable_name> [<send_chat_feedback>] [<set_to_value>] [<target_player>]

###
### Note: All commands only op permission can designated target player