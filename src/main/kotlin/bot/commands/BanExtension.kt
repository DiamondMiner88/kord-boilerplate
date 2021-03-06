@file:Suppress("DuplicatedCode")

package bot.commands

import bot.ERROR
import bot.canManage
import bot.configureAuthor
import bot.i18n
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalNumberChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.selfMember
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.ban
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock

class BanExtension : Extension() {
    override val name = "ban"

    override suspend fun setup() {
        publicSlashCommand(::BanArgs) {
            name = this@BanExtension.name
            description = "Ban a specific user"
            requireBotPermissions(Permission.BanMembers)

            check {
                anyGuild()
                hasPermission(Permission.BanMembers)
            }

            action {
                val providedReason = arguments.reason ?: i18n("bot.words.none")
                val author = user.asUser()

                val member = member ?: throw DiscordRelayedException(i18n("bot.errors.fetchUser"))
                val targetMember = arguments.target.asMember(guild!!.id)

                if (targetMember.id == channel.kord.selfId)
                    throw DiscordRelayedException(i18n("bot.ban.errors.self"))

                if (!member.asMember().canManage(targetMember))
                    throw DiscordRelayedException(i18n("bot.permissions.userTooLow", "kick"))

                if (!guild!!.selfMember().canManage(targetMember))
                    throw DiscordRelayedException(i18n("bot.permissions.botTooLow", "kick"))

                guild!!.ban(arguments.target.id) {
                    reason = i18n("bot.ban.reason", author.tag, author.id.value, providedReason)
                    deleteMessagesDays = arguments.deleteMessages?.toInt()
                }

                respond {
                    val target = arguments.target
                    embed {
                        timestamp = Clock.System.now()
                        configureAuthor(author)
                        color = Color.ERROR
                        description = i18n("bot.ban.embed",
                            target.tag, target.id.value, providedReason)
                    }
                }
            }
        }
    }

    inner class BanArgs : Arguments() {
        val target by user {
            name = "target"
            description = "User to ban"
        }

        val reason by optionalString {
            name = "reason"
            description = "Ban reason"
        }
        val deleteMessages by optionalNumberChoice {
            name = "delete_messages"
            description = "Days of messages to delete from this user"
            choices = mutableMapOf(
                "0 Days" to 0,
                "1 Day" to 1,
                "2 Days" to 2,
                "3 Days" to 3,
                "4 Days" to 4,
                "5 Days" to 5,
                "6 Days" to 6,
                "7 Days" to 7
            )
        }
    }
}
