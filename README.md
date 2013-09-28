Docs can be currently found at http://bit.ly/15LIq6S .

Enjoying the extension? Feel free to make a financial contribution to my work, 
PM me on IRC on irc.esper.net (nick __import__) for more details!

Quick example (place in main.ms):

    ##################################################################################
    # This example will connect the bot to irc.esper.net, using the nickname 'LolBot'
    # and join #commandhelper-testing. Auto-reconnects when there is a connection
    # issue.
    ##################################################################################

    @options = array()
    @options['username'] = 'testbot'
    @options['realname'] = 'Minecraft powered bot'

    irc_create(1)

    if (!irc_info(1)['connected']) {
        console('Connecting...')
        irc_connect(1, 'IRCtest', 'irc.esper.net', @options)
    }

    bind('irc_connection_exception', null, null, @event,
        console(@event['exceptionclass'] . 
            '(' . @event['message'] . ') . 
            Reconnecting...')

        irc_connect(1, 'IRCtest', 'irc.esper.net')
    )

    bind('irc_disconnected', null, null, @event,
        if(!@event['wasClean']) {
            console('Unclean disconnect. Reconnecting...')
            irc_connect(1, 'IRCtest', 'irc.esper.net')
        }
    )

    bind('irc_connected', null, null, @event,
        console('Connected.')
    )

    bind('irc_welcomed', null, null, @event,
        console('Welcomed.')
        irc_join(1, '#commandhelper-testing')
    )

    bind('irc_joined', null, null, @event,
        if (@event['who'] != irc_info(@event['id'])['nickname']) {
            return()
        }

        console('Joined' @event['channel'])
        irc_send_raw(1, 'MODE' @event['channel'])
    )

    bind('irc_msg', null, null, @event,
        ############################
        # Modify this if need be.
        # Defines the character that
        # activates the bot.
        ############################
        @trigchar = '$'

        ##########################
        # Modify this next line!!!
        ##########################
        @allowedNicks = array('__import__', 'LadyCailin')

        if (!array_contains_ic(@allowedNicks, @event['who'])) {
            return()
        }

        @match = reg_match('^' . reg_escape(@trigchar) . '([^ ]+)(\\s+(.*))?$', @event['message'])

        if (@match) {
            @trigger = @match[1]
            @args = @match[3]

            if (@args == '') {
                # This happens if someone says '$trigger ' (ends with a space)
                @args = null
            }

            ###########################
            # Signature for _irc_parse:
            # proc(_irc_parse, @trigger, @args, @match, @event, <code>)
            #
            # return(@val) to have that value spoken back.
            ###########################
            @retn = _irc_parse(@trigger, @args, @match, @event)

            if (@retn) {
                irc_msg(@event['id'], @event['target'], @retn)
            }
        }
    )

You will need to implement _irc_parse in auto_includes.ms, as per the above signature.

An example:

    proc(_irc_parse, @trigger, @args, @match, @event,
        if (@trigger == 'reload') {
            irc_msg(@event['id'], @event['target'], 'Reloaded!')
            run('/reloadaliases -x')

            die()
        }

        if (@trigger == 'eval') {
            if (@args == null) {
                return('You must specify code to evaluate!')
            }

            try(
                @out = eval(@args)
                if (!@out) {
                    @out = '(No output returned)'
                }

                return(@out)
            , @err, 
                return(@err[1])
            )

            return()
        }
    )
