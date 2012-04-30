# Robot Vision Control

Robot Vision Control (RVC) allows Chrome browsers to control a host machine the
same way one would through applications like RDP or VNC.

## Requirements

- [java][java] >= 1.6
- CLI users: n8han's [cs][cs] (follow instructions on README)
- Other users: [rvc app][app]

[cs]: https://github.com/n8han/conscript#readme
[java]: http://java.com/en/download/index.jsp
[app]: http://philcali.github.com/robot-vision/rvc.jar

## CLI Installation

These instructions are for those who would rather install this app for primary
launching from the command-line.

```
> cs philcali/robot-vision
```

Launch the app with:

```
> rvc -j web
```

## Mirrors

Whether you downloaded the thick swing app or the cli version, you can test it
out by directing your browser: [http://localhost:8080/robot-vision.html][locally].

[locally]: http://localhost:8080/robot-vision.html

If the app is running properly, you will be greeted with the endless mirrors of
your desktop! Of course, now it's time for you to connect to the host machine
_remotely_, which is the point of this application.

## Controlling and Viewing

RVC was made with the idea of sharing the desktop. Only one person can control
the machine, while others watch (or participate).

- `desktop.html` to control the machine
- `robot-vision.html` read only view of the machine

If you want to wrap the control scripts around basic auth, then pass in
a username and password via `rvc -u user -p password web`.

If you want to wrap the viewers in basic auth, use `rvc -v password web`. The
username for `robot-vision.html` authentication will always be `viewer`.

Keep in mind, this is plain text auth over http. Read below about using ssl
secured auth over https.

## Streaming vs Reloading

RVC allows for two specific forms of _reloading_ the desktop image. The first
way is a client side loop reloading the image. The second is the _jpeg_ camera
type streaming to the browser, where the server pushes images to clients.

The default behavior is to have javascript reload the scene, but testing showed
that jpeg camera streams proved to be more fluid. Don't use this method if
you know clients browsers don't support it.

## Using a Chrome Extension

Control communication must be allowed via passing a _key_ to an open web socket
connection. This means it's possible to __not__ serve the control code (with `-n`),
and inject the needed code using a Chrome Extension.

The extension is available at [robot-chrome][vision-ext].

[vision-ext]: https://chrome.google.com/webstore/detail/ieabafligicoomhcodhiolhlmljhmifi?utm_source=chrome-ntp-icon

__Note__: Chrome extension integration requires that the `gen` action is run at
least once to generate the 32 character random string for authentication:

```
rvc gen
```

## SSL Properties

RVC supports https with the `-s` flag. It is important to note that https
requests will __always__ fail until (at least) two system properties are set:

1. `netty.ssl.keyStore`
2. `netty.ssl.keyStorePassword`

Supply the properties with `rvc set prop.key prop.value`.

```
> rvc set netty.ssl.keyStore /path/to/cert.jks
> rvc set netty.ssl.keyStorePassword secret
```

__Note__: Chrome will obviously complain about trust issues until it is trusted
by a third party. 

## Screen recording

RVC allows simplistic screen recording by taking a series of snapshots that one
can later build (automatically or not) into a screencast. ClI users can do so by
 passing in the following arguments:

```
> rvc record /temp/path/to/jpgs
```

__Note__: in `web` mode, the controller will have an option to initiate and stop
a recording remotely. Currently, remote recordings are stored in:
```
{USER-HOME}/recording_{timestamp}
```

This program does not build the movie from the images. Instead, you can use
your favorite program to do that.

RVC looks at two properties for record:

- `record.command` - This is executed after the recording is finished and
- `record.dest` - optionally pass in the destination location

RVC will replace three strings in the command:

- `{location}` - the location set by the recorder
- `{filename}` - the filename of the movie
- `{dest}` - the directory destination

In Linux, one might use ffmpeg to transform the series of jpeg's to a movie
like so:

```
ffmpeg -r 7 -b 128k -i {location}/%07d.jpg {dest}/{filename}.mp4
```

## Control Library

The `capture-control` library is a wrapper around java utilities used in RVC
for screen capture image manipulation and remote control.

Feel free to use it in your projects.

`libraryDependencies += "com.github.philcali" %% "capture-control" % "0.0.2"`

## Client Library

The client interface is a submodule shared by this application and the Chrome
extension.

That code is found at [robot-interface][vision-int].

[vision-int]: https://github.com/philcali/robot-interface

## RVC Options

```
Usage: rvc [OPTIONS] action extras

OPTIONS

-b (0.0.0.0)
--bind-address (0.0.0.0)               Web server bind address

-f framerate 10 (per second)
--framerate framerate 10 (per second)  If in jpeg camera mode, push image data
                                       at specified framerate

-i 8080
--inet-port 8080                       Web server internet port

-j
--jpeg-camera                          Serves image data via jpeg camera
                                       transport

-k /path/to/ssl.properties
--key-store /path/to/ssl.properties    To be used with --secured. This is the
                                       properties file containing netty ssl
                                       info.

-n
--no-connect                           don't serve up connection js (ideal if
                                       using Chrome extension to connect)

-p <none>
--password <none>                      password to auth

-s
--secured                              https server (http)

-u <none>
--user <none>                          user to auth

-v <viewer password>
--viewer-password <viewer password>    Separate password for the 'viewer' user
                                       (leave blank for open)
```

## Vision Actions

Actions are meant to be run once, and done. The remote control server is the
primary action, but below is the output by running `rvc actions`:

```
 run, web                       Launches embedded server
 c, clean-keys                  Wipes stuck inputs
 record                         Records actions only
 gen, generate-key              Generates a Chrome extension connection key
 set, add                       Sets a vision property
 remove, rm                     Removes a vision property
 list, ls                       Lists vision properties
 actions, help                  Displays this list
```

## Known Issues

- Screen capturing can be really slow (JVM screencap performance is lousy)

## TODO's

- Popup notification upon successful login.
- interface for uploading and downloading files from browser
- Relay communication server

## License

The MIT.
