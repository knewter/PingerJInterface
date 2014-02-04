# Erlang <-> Android JInterface test

So this is a repo that shows that you can use erlang on android via jinterface
to talk to erlang on a remote node, easily enough (now that I've done all the
hard part).

There's a hard-coded IP address in the android app - you'll want to change that
to your android application's local network IP address.

Then locally, run (from this directory):

```sh
erl -name server@192.168.1.136 -setcookie test
```

then:

```
c(hello_jinterface).
hello_jinterface:start().
```

Then run the android app.  There are buttons.  Press them in order.  The first
button takes a long-ass time to run.
