# Robot Vision Control

Robot Vision Control (RVC) allows Chrome browsers to control a host machine the
same way one would through applications like RDP or VNC.

## Requirements

- [java][java] >= 1.6
- n8han's [cs][cs] (follow instructions on README)

[java]: http://java.com/en/download/index.jsp
[cs]: https://github.com/n8han/conscript#readme

## Installation

```
> cs philcali/robot-vision
```

Test it out with:

```
> rvc -j web
```

Direct your browser to [http://localhost:8080/robot-vision.html][locally]

[locally]: http://localhost:8080/robot-vision.html

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

[vision-ext]: https://github.com/philcali/robot-chrome

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

RVC allows simplistic screen recording by passing in the following arguments
in the commandline:

```
> rvc record /temp/path/to/jpgs
```

__Note__: in `web` mode, the controller will have an option to initiate and stop
a recording remotely.

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

`libraryDependencies += "com.github.philcali" %% "capture-control" % "0.0.1"`

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
--bind-address (0.0.0.0)               bind address

-f framerate 10 (per second)
--framerate framerate 10 (per second)  If in jpeg camera mode, push image data
                                       at specified framerate

-i 8080
--inet-port 8080                       internet port

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
--viewer-password <viewer password>    separate password for the 'viewer' user
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

- Very rarely in testing, I found that keyboard inputs would _stick_. The only
way to fix this at the moment, is to re-run `rvc` with the `c` or `clean-keys` action.
- Screen capturing can be really slow (JVM screencap performance is lousy)
- No way to control the image quality or scale from the client (TODO item)

## TODO's

- Popout interface for dynamic controls (mainly controlling scales and quality)
- interface for uploading and downloading files from browser

## License

The MIT.
